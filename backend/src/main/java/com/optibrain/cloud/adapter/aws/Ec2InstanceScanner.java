package com.optibrain.cloud.adapter.aws;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.policy.ProtectionPolicy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceTypeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EC2 instances, including accelerator families.
 *
 * <p>Cost is left null. Cost Explorer attributes spend to a service rather than a
 * resource id, so a per-instance figure here would be a list-price estimate presented
 * beside measured data. {@code AwsPriceList} holds the prices for callers that need an
 * explicit attribution.
 */
@Component
public class Ec2InstanceScanner extends AwsResourceScanner {

    /** Cached catalogue of instance type specifications. */
    private volatile Map<String, InstanceTypeInfo> typeCatalogue;

    public Ec2InstanceScanner(AwsClientFactory clients) {
        super(clients, "ec2:instances");
    }

    @Override
    protected List<CloudResource> load() {
        List<CloudResource> collected = new ArrayList<>();
        for (Instance instance : clients.ec2()
                .describeInstancesPaginator(DescribeInstancesRequest.builder().build())
                .stream()
                .flatMap(page -> page.reservations().stream())
                .flatMap(reservation -> reservation.instances().stream())
                .toList()) {
            collected.add(toResource(instance));
        }
        return collected;
    }

    private CloudResource toResource(Instance instance) {
        Map<String, String> tags = tagsOf(instance.tags());

        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("instanceType", instance.instanceTypeAsString());

        InstanceTypeInfo info = instanceTypeCatalogue().get(instance.instanceTypeAsString());
        specs.put("vCPU", info == null || info.vCpuInfo() == null
                || info.vCpuInfo().defaultVCpus() == null
                ? "" : String.valueOf(info.vCpuInfo().defaultVCpus()));
        specs.put("memoryGiB", info == null || info.memoryInfo() == null
                || info.memoryInfo().sizeInMiB() == null
                ? "" : String.format(Locale.ROOT, "%.1f", info.memoryInfo().sizeInMiB() / 1024.0));

        specs.put("architecture", String.valueOf(instance.architecture()));
        specs.put("platform", String.valueOf(instance.platformDetails()));
        specs.put("availabilityZone",
                instance.placement() == null ? "" : instance.placement().availabilityZone());
        specs.put("vpcId", instance.vpcId() == null ? "" : instance.vpcId());
        specs.put("subnetId", instance.subnetId() == null ? "" : instance.subnetId());
        specs.put("privateIp", instance.privateIpAddress() == null ? "" : instance.privateIpAddress());
        specs.put("launchTime", instance.launchTime() == null ? "" : instance.launchTime().toString());

        ResourceType type = AwsPriceList.isAccelerator(instance.instanceTypeAsString())
                ? ResourceType.GPU_INSTANCE
                : ResourceType.EC2_INSTANCE;

        return new CloudResource(
                instance.instanceId(),
                type,
                region(),
                instance.state() == null ? "unknown" : instance.state().nameAsString(),
                tags.getOrDefault("Name", instance.instanceId()),
                tags,
                specs,
                null,
                null,
                instance.launchTime(),
                ProtectionPolicy.isProtected(tags));
    }

    /**
     * vCPU and memory are properties of the instance <em>type</em>, so they require a
     * separate call. The catalogue changes only when AWS launches hardware, so it is
     * fetched once per process; without the cache every inventory refresh re-requests it.
     */
    private Map<String, InstanceTypeInfo> instanceTypeCatalogue() {
        Map<String, InstanceTypeInfo> cached = typeCatalogue;
        if (cached != null) {
            return cached;
        }
        Map<String, InstanceTypeInfo> loaded = new HashMap<>();
        try (var ec2 = clients.ec2()) {
            ec2.describeInstanceTypesPaginator(DescribeInstanceTypesRequest.builder().build())
                    .stream()
                    .flatMap(page -> page.instanceTypes().stream())
                    .forEach(info -> loaded.put(info.instanceTypeAsString(), info));
            typeCatalogue = Map.copyOf(loaded);
            return typeCatalogue;
        } catch (Exception e) {
            log.debug("DescribeInstanceTypes unavailable; instance specs will be blank: {}",
                    e.getMessage());
            return Map.of();
        }
    }
}