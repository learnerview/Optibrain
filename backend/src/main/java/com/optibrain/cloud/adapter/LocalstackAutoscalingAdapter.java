package com.optibrain.cloud.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.*;

import java.net.URI;
import java.util.List;

@Component("localstackAutoscalingAdapter")
@Slf4j
public class LocalstackAutoscalingAdapter {

    private final String endpoint;
    private final String region;
    private final String accessKey;
    private final String secretKey;

    public LocalstackAutoscalingAdapter(
            @Value("${cloud.localstack.endpoint}") String endpoint,
            @Value("${cloud.localstack.region}") String region,
            @Value("${aws.access-key}") String accessKey,
            @Value("${aws.secret-key}") String secretKey) {
        this.endpoint = endpoint;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    private AutoScalingClient getClient() {
        return AutoScalingClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build();
    }

    public boolean createMockASG(String asgName, int min, int max, int desired) {
        try (AutoScalingClient asg = getClient()) {
            CreateAutoScalingGroupRequest request = CreateAutoScalingGroupRequest.builder()
                    .autoScalingGroupName(asgName)
                    .minSize(min)
                    .maxSize(max)
                    .desiredCapacity(desired)
                    .availabilityZones(region + "a")
                    .launchTemplate(LaunchTemplateSpecification.builder()
                            .launchTemplateName("optibrain-template")
                            .version("$Latest")
                            .build())
                    .build();
            asg.createAutoScalingGroup(request);
            log.info("[LOCALSTACK] Created ASG {} (min={}, max={}, desired={})", asgName, min, max, desired);
            return true;
        } catch (Exception e) {
            log.warn("[LOCALSTACK] Failed to create ASG: {}", e.getMessage());
            return false;
        }
    }

    public boolean updateDesiredCapacity(String asgName, int desired) {
        try (AutoScalingClient asg = getClient()) {
            SetDesiredCapacityRequest request = SetDesiredCapacityRequest.builder()
                    .autoScalingGroupName(asgName)
                    .desiredCapacity(desired)
                    .build();
            asg.setDesiredCapacity(request);
            log.info("[LOCALSTACK] Updated ASG {} desired capacity to {}", asgName, desired);
            return true;
        } catch (Exception e) {
            log.warn("[LOCALSTACK] Failed to update ASG desired capacity: {}", e.getMessage());
            return false;
        }
    }

    public List<String> listASGs() {
        try (AutoScalingClient asg = getClient()) {
            DescribeAutoScalingGroupsResponse response = asg.describeAutoScalingGroups();
            return response.autoScalingGroups().stream()
                    .map(AutoScalingGroup::autoScalingGroupName)
                    .toList();
        } catch (Exception e) {
            log.warn("[LOCALSTACK] Failed to list ASGs: {}", e.getMessage());
            return List.of();
        }
    }
}
