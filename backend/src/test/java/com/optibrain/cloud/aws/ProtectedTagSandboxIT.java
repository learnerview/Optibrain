package com.optibrain.cloud.aws;

import com.optibrain.cloud.adapter.aws.EbsScanner;
import com.optibrain.cloud.config.CloudMode;
import com.optibrain.cloud.config.CloudProperties;
import com.optibrain.cloud.port.ResourceQuery;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.ec2.model.Volume;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the protection flag against a real AWS API response rather than a stub's
 * assumptions, because a scanner bug here silently disarms the guard against data loss.
 *
 * <p>Lives in this package to use the factory's package-private initialisation. The unit
 * tests elsewhere pin the tag flattening in isolation; only this one proves that AWS
 * actually returns the tag and that the scanner surfaces it.
 *
 * <p>Skipped rather than failed when the sandbox is unreachable or unseeded: a test that
 * cannot observe the condition must not report success.
 */
class ProtectedTagSandboxIT {

    private AwsClientFactory sandboxFactory() {
        CloudProperties properties = new CloudProperties();
        properties.setMode(CloudMode.SANDBOX);
        AwsClientFactory factory = new AwsClientFactory(properties);
        factory.initialise();
        return factory;
    }

    /** True when the sandbox holds a volume carrying the protection tag. */
    private boolean sandboxHasProtectedVolume(AwsClientFactory factory) {
        try (var ec2 = factory.ec2()) {
            return ec2.describeVolumes(b -> b.build()).volumes().stream()
                    .anyMatch(v -> v.hasTags() && v.tags().stream()
                            .anyMatch(t -> "optibrain:protected".equals(t.key())));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "LocalStack unreachable at " + factory.region()
                            + "; start it and run scripts/sandbox/seed.sh", e);
        }
    }

    @Test
    @DisplayName("a volume AWS reports as protected is reported as protected by the scanner")
    void protectedVolumeIsReportedAsProtected() {
        AwsClientFactory factory = sandboxFactory();
        Assumptions.assumeTrue(sandboxHasProtectedVolume(factory),
                "sandbox has no protected volume; run scripts/sandbox/seed.sh");

        var protectedResources = new EbsScanner(factory).scan(ResourceQuery.all()).stream()
                .filter(r -> r.protectedResource())
                .toList();

        assertThat(protectedResources).isNotEmpty();
        assertThat(protectedResources).allSatisfy(r ->
                assertThat(r.tags()).containsEntry("optibrain:protected", "true"));
    }

    @Test
    @DisplayName("the protection flag matches the tag on every scanned volume, with no false positives")
    void protectionFlagMatchesTheTagExactly() {
        AwsClientFactory factory = sandboxFactory();
        Assumptions.assumeTrue(sandboxHasProtectedVolume(factory),
                "sandbox has no protected volume; run scripts/sandbox/seed.sh");

        var volumes = new EbsScanner(factory).scan(ResourceQuery.all());

        assertThat(volumes).isNotEmpty();
        assertThat(volumes).allSatisfy(volume ->
                assertThat(volume.protectedResource())
                        .isEqualTo(volume.tags().containsKey("optibrain:protected")));
    }

    @Test
    @DisplayName("the protection flag matches the tag for snapshots too")
    void protectionFlagMatchesTheTagForSnapshots() {
        AwsClientFactory factory = sandboxFactory();

        new EbsScanner(factory).scan(ResourceQuery.all()).stream()
                .filter(r -> r.type() == com.optibrain.cloud.model.ResourceType.EBS_SNAPSHOT)
                .forEach(snapshot -> assertThat(snapshot.protectedResource())
                        .isEqualTo(snapshot.tags().containsKey("optibrain:protected")));
    }
}