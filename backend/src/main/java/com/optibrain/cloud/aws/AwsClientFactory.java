package com.optibrain.cloud.aws;

import com.optibrain.common.context.TenantContext;
import com.optibrain.cloud.config.CloudMode;
import com.optibrain.cloud.config.CloudProperties;
import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.service.TenantCredentialService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single place where AWS SDK clients are constructed.
 *
 * <p>Client construction was previously copy-pasted across eleven call sites using four
 * credential strategies and four region sources. The result was a bug where
 * {@code cloud.mode=LOCALSTACK} still produced clients pointed at real AWS endpoints
 * with placeholder credentials. Every AWS-backed service must obtain its client here.
 *
 * <p>Because {@link AwsClientBuilder} is implemented by every AWS SDK v2 client
 * builder, region, credentials and endpoint override are applied in one place and can
 * never drift between services.
 *
 * <p>There is no separate sandbox adapter: {@link CloudMode#SANDBOX} changes only the
 * endpoint and the credentials here, so the same adapter code drives both targets.
 *
 * <p>Credentials are scoped to the tenant on the current thread. In AWS mode, a tenant
 * configured with an IAM role is served credentials obtained by AssumeRole with that
 * tenant's external id; a tenant with no role is refused unless
 * {@code cloud.aws.allow-ambient-fallback=true}, because letting a tenant silently use
 * the ambient chain would break the isolation boundary this factory exists to enforce.
 * Requests made outside a tenant context, and the STS client itself, use the ambient
 * credential chain. Callers never touch a raw key: config stores a role to assume, not
 * long-lived access keys.
 */
@Component
@Slf4j
public class AwsClientFactory {

    /** How long before expiry a cached role session is treated as stale and re-assumed. */
    private static final Duration SESSION_RENEW_LEAD = Duration.ofMinutes(5);

    private final CloudProperties cloudProperties;
    private final TenantCredentialService tenants;
    private final Map<String, CachedRoleSession> roleSessions = new ConcurrentHashMap<>();

    private volatile AwsCredentialsProvider ambientProvider;

    public AwsClientFactory(CloudProperties cloudProperties) {
        this(cloudProperties, null);
    }

    /** The constructor Spring uses; the single-arg form exists for unit tests. */
    @Autowired
    public AwsClientFactory(CloudProperties cloudProperties, TenantCredentialService tenants) {
        this.cloudProperties = cloudProperties;
        this.tenants = tenants;
    }

    @PostConstruct
    void initialise() {
        if (cloudProperties.getMode() == CloudMode.SANDBOX) {
            this.ambientProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test"));
            log.info("Cloud mode SANDBOX: AWS SDK routed to {}",
                    cloudProperties.getSandbox().getEndpoint());
            return;
        }

        // Fail fast with an actionable message rather than letting the first API call
        // fail later from a background job.
        AwsCredentialsProvider resolver = DefaultCredentialsProvider.create();
        try {
            resolver.resolveCredentials();
            this.ambientProvider = resolver;
            log.info("Cloud mode AWS: credentials resolved for region {}",
                    cloudProperties.getAws().getRegion());
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "cloud.mode=AWS but no AWS credentials could be resolved. Set "
                            + "AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY, configure ~/.aws/credentials, "
                            + "or attach an IAM role. To work without an AWS account use "
                            + "CLOUD_MODE=SANDBOX. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * The configured region, for scanners that need it to populate a resource.
     *
     * <p>Exposed so scanners do not re-read configuration or re-derive a region, which is
     * how region drift between services started.
     */
    public String region() {
        return cloudProperties.getAws().getRegion();
    }

    public Ec2Client ec2() {
        return configure(Ec2Client.builder(), cloudProperties.getAws().getRegion());
    }

    public CloudWatchClient cloudWatch() {
        return configure(CloudWatchClient.builder(), cloudProperties.getAws().getRegion());
    }

    public AutoScalingClient autoScaling() {
        return configure(AutoScalingClient.builder(), cloudProperties.getAws().getRegion());
    }

    public ElasticLoadBalancingV2Client elasticLoadBalancing() {
        return configure(ElasticLoadBalancingV2Client.builder(), cloudProperties.getAws().getRegion());
    }

    public RdsClient rds() {
        return configure(RdsClient.builder(), cloudProperties.getAws().getRegion());
    }

    public LambdaClient lambda() {
        return configure(LambdaClient.builder(), cloudProperties.getAws().getRegion());
    }

    public DynamoDbClient dynamoDb() {
        return configure(DynamoDbClient.builder(), cloudProperties.getAws().getRegion());
    }

    public S3Client s3() {
        return configure(S3Client.builder(), cloudProperties.getAws().getRegion());
    }

    public EcsClient ecs() {
        return configure(EcsClient.builder(), cloudProperties.getAws().getRegion());
    }

    /**
     * STS clients always use the ambient credential chain: they exist precisely to
     * exchange ambient credentials for a tenant's scoped session, so a tenant-scoped
     * client would be circular.
     */
    public StsClient sts() {
        return configure(StsClient.builder(), cloudProperties.getAws().getRegion(), ambientProvider);
    }

    /**
     * Cost Explorer is a global service that rejects any region other than us-east-1.
     * Callers must use this rather than a regional client.
     */
    public CostExplorerClient costExplorer() {
        return configure(CostExplorerClient.builder(), cloudProperties.getAws().getCostExplorerRegion());
    }

    /**
     * The credentials for the tenant on the current thread: its assumed role if one is
     * configured, and in SANDBOX mode the sandbox pair.
     *
     * <p>In AWS mode a tenant that is missing or has no role is a hard failure unless
     * {@code cloud.aws.allow-ambient-fallback} is explicitly enabled: a tenant that
     * silently used the OptiBrain instance's own account would be the exact
     * cross-account isolation leak this factory exists to stop. Requests made outside
     * any tenant context still use the ambient chain; there is no tenant yet to scope
     * them.
     */
    private AwsCredentialsProvider tenantedProvider() {
        if (cloudProperties.getMode() == CloudMode.SANDBOX) {
            return ambientProvider;
        }
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return ambientProvider;
        }
        Tenant tenant = tenants == null ? null : tenants.findTenant(tenantId).orElse(null);
        if (tenant == null) {
            return tenantless(tenantId,
                    "no tenant with id '" + tenantId + "' is configured with AWS credentials");
        }
        if (tenant.getAwsRoleArn() == null || tenant.getAwsRoleArn().isBlank()) {
            return tenantless(tenantId, "tenant '" + tenantId + "' has no awsRoleArn configured");
        }
        return cachedRole(tenant);
    }

    /**
     * Resolves a tenantless credential request: the ambient chain when the operator
     * opted in, otherwise a failure loud enough that the missing role cannot be missed.
     */
    private AwsCredentialsProvider tenantless(String tenantId, String reason) {
        if (cloudProperties.getAws().isAllowAmbientFallback()) {
            return ambientProvider;
        }
        throw new IllegalStateException(
                "cloud.mode=AWS but " + reason + ". Refusing to fall back to the ambient "
                        + "account, which would break tenant isolation. Set the tenant's "
                        + "awsRoleArn, or opt into a single-account deployment with "
                        + "cloud.aws.allow-ambient-fallback=true.");
    }

    private AwsCredentialsProvider cachedRole(Tenant tenant) {
        String key = roleCacheKey(tenant);
        Instant now = Instant.now();
        CachedRoleSession cached = roleSessions.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.provider();
        }
        roleSessions.remove(key);
        CachedRoleSession fresh = assumeRole(tenant);
        roleSessions.put(key, fresh);
        return fresh.provider();
    }

    private String roleCacheKey(Tenant tenant) {
        String externalId = tenant.getAwsExternalId() == null ? "" : tenant.getAwsExternalId();
        return tenant.getId() + "|" + tenant.getAwsRoleArn() + "|" + externalId;
    }

    /**
     * Assumes the tenant's role and caches the short-lived session until just before
     * expiry.
     *
     * <p>Concurrent requests may race here; both assume the role, both receive valid
     * sessions, and the last write wins. A duplicate AssumeRole call on an idle tenant is
     * cheaper than synchronising the hot path.
     */
    private CachedRoleSession assumeRole(Tenant tenant) {
        try (StsClient sts = sts()) {
            AssumeRoleRequest.Builder request = AssumeRoleRequest.builder()
                    .roleArn(tenant.getAwsRoleArn())
                    .roleSessionName(sessionNameFor(tenant.getId()));
            String externalId = tenant.getAwsExternalId();
            if (externalId != null && !externalId.isBlank()) {
                request.externalId(externalId);
            }
            AssumeRoleResponse response = sts.assumeRole(request.build());
            Credentials session = response.credentials();
            if (session == null || session.accessKeyId() == null || session.secretAccessKey() == null) {
                throw new IllegalStateException(
                        "AssumeRole for tenant '" + tenant.getId() + "' returned no credentials");
            }
            Instant expiry = session.expiration() == null
                    ? Instant.now().plus(Duration.ofHours(1))
                    : session.expiration();
            AwsCredentialsProvider provider = StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(
                            session.accessKeyId(), session.secretAccessKey(), session.sessionToken()));
            log.info("Assumed role {} for tenant {}", tenant.getAwsRoleArn(), tenant.getId());
            return new CachedRoleSession(provider, expiry);
        } catch (SdkException e) {
            throw new IllegalStateException(
                    "Failed to assume role '" + tenant.getAwsRoleArn() + "' for tenant '"
                            + tenant.getId() + "': " + e.getMessage(), e);
        }
    }

    private static String sessionNameFor(String tenantId) {
        String safe = (tenantId == null ? "tenant" : tenantId)
                .replaceAll("[^a-zA-Z0-9=,.@\\-]", "-");
        if (safe.length() > 60) {
            safe = safe.substring(0, 60);
        }
        return "optibrain-" + safe;
    }

    private <C> C configure(AwsClientBuilder<?, C> builder, String regionName) {
        return configure(builder, regionName, tenantedProvider());
    }

    private <C> C configure(AwsClientBuilder<?, C> builder, String regionName,
                            AwsCredentialsProvider provider) {
        builder.region(Region.of(regionName));
        builder.credentialsProvider(provider);

        if (cloudProperties.getMode() == CloudMode.SANDBOX) {
            builder.endpointOverride(URI.create(cloudProperties.getSandbox().getEndpoint()));
        }
        return builder.build();
    }

    private record CachedRoleSession(AwsCredentialsProvider provider, Instant expiresAt) {
    }
}