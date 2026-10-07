package com.optibrain.cloud.adapter.aws;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.policy.ProtectionPolicy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auto Scaling groups.
 *
 * <p>Cost derives from the instance types of current members, which the API reports
 * directly. The launch template name is not an instance type and must not be used as
 * one: reading it as a price lookup silently produced wrong costs for every group whose
 * template name happened to collide with a known instance type.
 */
@Component
public class AutoScalingScanner extends AwsResourceScanner {

    public AutoScalingScanner(AwsClientFactory clients) {
        super(clients, "autoscaling:groups");
    }

    @Override
    protected List<CloudResource> load() {
        return safe(() -> clients.autoScaling()
                .describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder().build())
                .autoScalingGroups().stream()
                .map(this::toResource)
                .toList());
    }

    private CloudResource toResource(AutoScalingGroup group) {
        Map<String, String> tags = asgTags(group);
        Map<String, String> specs = new LinkedHashMap<>();

        specs.put("minSize", String.valueOf(group.minSize()));
        specs.put("maxSize", String.valueOf(group.maxSize()));
        specs.put("desiredCapacity", String.valueOf(group.desiredCapacity()));
        specs.put("launchTemplate", group.launchTemplate() == null
                ? "" : String.valueOf(group.launchTemplate().launchTemplateName()));
        specs.put("instanceCount", String.valueOf(
                group.instances() == null ? 0 : group.instances().size()));
        specs.put("healthCheckType", String.valueOf(group.healthCheckType()));
        specs.put("createdTime", group.createdTime() == null
                ? "" : group.createdTime().toString());

        double hourly = group.instances() == null ? 0.0
                : group.instances().stream()
                        .mapToDouble(i -> AwsPriceList.instanceHourly(i.instanceType()))
                        .sum();

        return new CloudResource(
                group.autoScalingGroupName(), ResourceType.ASG, region(),
                group.status() == null ? "unknown" : group.status(),
                tags.getOrDefault("Name", group.autoScalingGroupName()),
                tags, specs,
                hourly, hourly * AwsPriceList.HOURS_PER_MONTH,
                group.createdTime(), ProtectionPolicy.isProtected(tags));
    }

    /** Auto Scaling defines its own tag type, so the EC2 helper cannot be reused. */
    private static Map<String, String> asgTags(AutoScalingGroup group) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (group.tags() != null) {
            group.tags().forEach(t -> {
                if (t.key() != null) {
                    tags.put(t.key(), t.value() == null ? "" : t.value());
                }
            });
        }
        return tags;
    }
}