package com.optibrain.cloud.aws;

import com.optibrain.cloud.config.CloudMode;
import com.optibrain.cloud.config.CloudProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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

import java.net.URI;

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
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AwsClientFactory {

    private final CloudProperties cloudProperties;

    private volatile AwsCredentialsProvider credentialsProvider;

    @PostConstruct
    void initialise() {
        if (cloudProperties.getMode() == CloudMode.SANDBOX) {
            this.credentialsProvider = StaticCredentialsProvider.create(
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
            this.credentialsProvider = resolver;
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

    public StsClient sts() {
        return configure(StsClient.builder(), cloudProperties.getAws().getRegion());
    }

    /**
     * Cost Explorer is a global service that rejects any region other than us-east-1.
     * Callers must use this rather than a regional client.
     */
    public CostExplorerClient costExplorer() {
        return configure(CostExplorerClient.builder(), cloudProperties.getAws().getCostExplorerRegion());
    }

    private <C> C configure(AwsClientBuilder<?, C> builder, String regionName) {
        builder.region(Region.of(regionName));
        builder.credentialsProvider(credentialsProvider);

        if (cloudProperties.getMode() == CloudMode.SANDBOX) {
            builder.endpointOverride(URI.create(cloudProperties.getSandbox().getEndpoint()));
        }
        return builder.build();
    }
}