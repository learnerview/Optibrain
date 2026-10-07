package com.optibrain.cloud.adapter.aws;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.policy.ProtectionPolicy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Networking resources that carry recurring cost or represent attachment risk.
 *
 * <p>Network interfaces and security groups are free, but unattached interfaces
 * accumulate and open security groups are a real finding, so both surface in the
 * inventory alongside the billed resources they sit beside.
 */
@Component
public class NetworkScanner extends AwsResourceScanner {

    public NetworkScanner(AwsClientFactory clients) {
        super(clients, "ec2:network");
    }

    @Override
    protected List<CloudResource> load() {
        return merge(natGateways(), elasticIps(), loadBalancers(), networkInterfaces(), securityGroups());
    }

    private List<CloudResource> natGateways() {
        return safe(() -> clients.ec2().describeNatGateways(
                        DescribeNatGatewaysRequest.builder()
                                .filter(Filter.builder()
                                        .name("state").values("available", "pending").build())
                                .build())
                .natGateways().stream()
                .map(gateway -> {
                    Map<String, String> tags = tagsOf(gateway.tags());
                    Map<String, String> specs = new LinkedHashMap<>();
                    specs.put("vpcId", gateway.vpcId());
                    specs.put("subnetId", gateway.subnetId());
                    specs.put("connectivityType", gateway.connectivityTypeAsString());
                    // Charged per hour for the whole month regardless of traffic.
                    double hourly = 0.045;
                    return new CloudResource(
                            gateway.natGatewayId(), ResourceType.NAT_GATEWAY, region(),
                            gateway.state() == null ? "unknown" : gateway.stateAsString(),
                            tags.getOrDefault("Name", gateway.natGatewayId()), tags, specs,
                            hourly, hourly * AwsPriceList.HOURS_PER_MONTH,
                            gateway.createTime(), ProtectionPolicy.isProtected(tags));
                })
                .toList());
    }

    private List<CloudResource> elasticIps() {
        return safe(() -> clients.ec2().describeAddresses(DescribeAddressesRequest.builder().build())
                .addresses().stream()
                .map(address -> {
                    Map<String, String> tags = tagsOf(address.tags());
                    Map<String, String> specs = new LinkedHashMap<>();
                    specs.put("publicIp", String.valueOf(address.publicIp()));
                    specs.put("allocationId", String.valueOf(address.allocationId()));
                    specs.put("associationId", String.valueOf(address.associationId()));
                    specs.put("instanceId", String.valueOf(address.instanceId()));
                    specs.put("privateIp", String.valueOf(address.privateIpAddress()));
                    boolean associated = address.associationId() != null;
                    specs.put("associated", String.valueOf(associated));

                    // Only unassociated addresses are billed; an in-use address is free.
                    double hourly = associated ? 0.0 : 0.005;

                    return new CloudResource(
                            address.allocationId() == null ? address.publicIp() : address.allocationId(),
                            ResourceType.ELASTIC_IP, region(),
                            associated ? "in-use" : "available",
                            tags.getOrDefault("Name", address.publicIp()), tags, specs,
                            hourly, hourly * AwsPriceList.HOURS_PER_MONTH,
                            null, ProtectionPolicy.isProtected(tags));
                })
                .toList());
    }

    private List<CloudResource> loadBalancers() {
        return safe(() -> {
            var lbs = clients.elasticLoadBalancing()
                    .describeLoadBalancers(
                            software.amazon.awssdk.services.elasticloadbalancingv2.model
                                    .DescribeLoadBalancersRequest.builder().build())
                    .loadBalancers();
            Map<String, Map<String, String>> tagsByArn = new java.util.HashMap<>();
            List<String> arns = lbs.stream()
                    .map(lb -> lb.loadBalancerArn())
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (!arns.isEmpty()) {
                clients.elasticLoadBalancing().describeTags(
                                software.amazon.awssdk.services.elasticloadbalancingv2.model
                                        .DescribeTagsRequest.builder().resourceArns(arns).build())
                        .tagDescriptions()
                        .forEach(td -> {
                            if (td.resourceArn() != null) {
                                tagsByArn.put(td.resourceArn(), elbTagsOf(td.tags()));
                            }
                        });
            }
            return lbs.stream()
                    .map(lb -> {
                        Map<String, String> tags = tagsByArn.getOrDefault(
                                lb.loadBalancerArn(), Map.of());
                        Map<String, String> specs = new LinkedHashMap<>();
                        specs.put("type", lb.type() == null ? "" : lb.typeAsString());
                        specs.put("scheme", lb.scheme() == null ? "" : lb.schemeAsString());
                        specs.put("arn", String.valueOf(lb.loadBalancerArn()));
                        specs.put("dnsName", String.valueOf(lb.dnsName()));
                        String state = lb.state() == null ? "unknown"
                                : String.valueOf(lb.state().code());
                        specs.put("state", state);
                        specs.put("createdTime", String.valueOf(lb.createdTime()));
                        return new CloudResource(
                                lb.loadBalancerName(), ResourceType.LOAD_BALANCER, region(), state,
                                tags.getOrDefault("Name", lb.loadBalancerName()), tags, specs, null, null,
                                lb.createdTime(), ProtectionPolicy.isProtected(tags));
                    })
                    .toList();
        });
    }

    /** ELBv2 uses its own Tag model, distinct from EC2's, so it is flattened here. */
    private static Map<String, String> elbTagsOf(
            List<software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag> tags) {
        Map<String, String> result = new java.util.HashMap<>();
        if (tags != null) {
            tags.forEach(t -> {
                if (t.key() != null) {
                    result.put(t.key(), t.value() == null ? "" : t.value());
                }
            });
        }
        return result;
    }

    private List<CloudResource> networkInterfaces() {
        return safe(() -> clients.ec2()
                .describeNetworkInterfacesPaginator(DescribeNetworkInterfacesRequest.builder().build())
                .stream()
                .flatMap(page -> page.networkInterfaces().stream())
                .map(nic -> {
                    Map<String, String> tags = tagsOf(nic.tagSet());
                    Map<String, String> specs = new LinkedHashMap<>();
                    specs.put("status", nic.status() == null ? "" : nic.statusAsString());
                    specs.put("vpcId", nic.vpcId() == null ? "" : nic.vpcId());
                    specs.put("subnetId", nic.subnetId() == null ? "" : nic.subnetId());
                    specs.put("attachmentCount", String.valueOf(
                            nic.attachment() == null ? 0 : 1));
                    return new CloudResource(
                            nic.networkInterfaceId(), ResourceType.NETWORK_INTERFACE, region(),
                            nic.status() == null ? "unknown" : nic.statusAsString(),
                            tags.getOrDefault("Name", nic.networkInterfaceId()), tags, specs,
                            null, null, null, ProtectionPolicy.isProtected(tags));
                })
                .toList());
    }

    private List<CloudResource> securityGroups() {
        return safe(() -> clients.ec2()
                .describeSecurityGroups(DescribeSecurityGroupsRequest.builder().build())
                .securityGroups().stream()
                .map(group -> toResource(group))
                .toList());
    }

    private CloudResource toResource(SecurityGroup group) {
        Map<String, String> tags = tagsOf(group.tags());
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("vpcId", group.vpcId() == null ? "" : group.vpcId());
        specs.put("description", group.description() == null ? "" : group.description());
        specs.put("ingressRuleCount", String.valueOf(group.ipPermissions() == null
                ? 0 : group.ipPermissions().size()));
        specs.put("egressRuleCount", String.valueOf(group.ipPermissionsEgress() == null
                ? 0 : group.ipPermissionsEgress().size()));
        // The default security group allows all traffic in and out. Worth surfacing
        // because it is the single most common network finding.
        specs.put("isDefault", String.valueOf(
                group.groupName() != null && group.groupName().equals("default")));

        return new CloudResource(
                group.groupId(), ResourceType.SECURITY_GROUP, region(), "active",
                group.groupName() == null ? group.groupId() : group.groupName(),
                tags, specs, null, null, null, ProtectionPolicy.isProtected(tags));
    }

    private static List<CloudResource> merge(List<CloudResource>... groups) {
        List<CloudResource> all = new java.util.ArrayList<>();
        for (List<CloudResource> group : groups) {
            if (group != null) {
                all.addAll(group);
            }
        }
        return all;
    }
}