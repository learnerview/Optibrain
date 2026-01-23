package com.optibrain.cleanup.service;

import com.optibrain.cleanup.dto.OrphanedResourceResponseDTO;
import com.optibrain.cleanup.model.OrphanedResource;
import com.optibrain.cleanup.repository.OrphanedResourceRepository;
import com.optibrain.common.context.TenantContext;
import com.optibrain.tenant.model.CloudCredentials;
import com.optibrain.tenant.service.TenantCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {

    private final TenantCredentialService tenantCredentialService;
    private final com.optibrain.config.service.TenantConfigurationService configService;
    private final OrphanedResourceRepository orphanedResourceRepository;

    public List<OrphanedResourceResponseDTO> detectOrphanedResources() {
        String tenantId = TenantContext.getTenantId();
        try {
            var config = configService.getConfiguration(tenantId);
            CloudCredentials credentials = tenantCredentialService.getCredentials(tenantId);
            if (credentials == null) {
                return orphanedResourceRepository.findByTenantId(tenantId).stream()
                        .map(this::mapToDTO).collect(Collectors.toList());
            }

            List<OrphanedResource> resources = new ArrayList<>();
            try (Ec2Client ec2 = createEc2Client(credentials)) {
                resources.addAll(detectUnattachedVolumes(tenantId, ec2, config.getEbsUnusedDaysThreshold()));
                resources.addAll(detectUnattachedIps(tenantId, ec2));
                resources.addAll(detectOldSnapshots(tenantId, ec2, config.getSnapshotOldDaysThreshold()));
            }
            
            resources.addAll(detectIdleLoadBalancers(tenantId, credentials));
            orphanedResourceRepository.saveAll(resources);
            
            return resources.stream().map(this::mapToDTO).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to detect orphaned resources for tenant {}: {}", tenantId, e.getMessage());
            return orphanedResourceRepository.findByTenantId(tenantId).stream()
                    .map(this::mapToDTO).collect(Collectors.toList());
        }
    }

    private List<OrphanedResource> detectUnattachedVolumes(String tenantId, Ec2Client ec2, int daysThreshold) {
        List<OrphanedResource> result = new ArrayList<>();
        try {
            DescribeVolumesRequest request = DescribeVolumesRequest.builder()
                    .filters(Filter.builder().name("status").values("available").build())
                    .build();

            DescribeVolumesResponse response = ec2.describeVolumes(request);
            for (Volume vol : response.volumes()) {
                int age = (int) ChronoUnit.DAYS.between(vol.createTime(), Instant.now());
                if (age > daysThreshold) {
                    OrphanedResource res = OrphanedResource.builder()
                            .resourceId(vol.volumeId())
                            .resourceType("EBS_VOLUME")
                            .region(ec2.serviceClientConfiguration().region().id())
                            .estimatedMonthlyCost(vol.size() * 0.10)
                            .resolved(false)
                            .build();
                    res.setTenantId(tenantId);
                    result.add(res);
                }
            }
        } catch (Exception e) {
            log.warn("Error detecting volumes: {}", e.getMessage());
        }
        return result;
    }

    private List<OrphanedResource> detectUnattachedIps(String tenantId, Ec2Client ec2) {
        List<OrphanedResource> result = new ArrayList<>();
        try {
            DescribeAddressesResponse response = ec2.describeAddresses();
            for (Address addr : response.addresses()) {
                if (addr.associationId() == null) {
                    OrphanedResource res = OrphanedResource.builder()
                            .resourceId(addr.allocationId())
                            .resourceType("ELASTIC_IP")
                            .region(ec2.serviceClientConfiguration().region().id())
                            .estimatedMonthlyCost(3.60)
                            .resolved(false)
                            .build();
                    res.setTenantId(tenantId);
                    result.add(res);
                }
            }
        } catch (Exception e) {
            log.warn("Error detecting IPs: {}", e.getMessage());
        }
        return result;
    }

    private List<OrphanedResource> detectOldSnapshots(String tenantId, Ec2Client ec2, int daysThreshold) {
        List<OrphanedResource> result = new ArrayList<>();
        try {
            DescribeSnapshotsRequest request = DescribeSnapshotsRequest.builder()
                    .ownerIds("self")
                    .build();

            DescribeSnapshotsResponse response = ec2.describeSnapshots(request);
            for (Snapshot snap : response.snapshots()) {
                int age = (int) ChronoUnit.DAYS.between(snap.startTime(), Instant.now());
                if (age > daysThreshold) { 
                    OrphanedResource res = OrphanedResource.builder()
                            .resourceId(snap.snapshotId())
                            .resourceType("SNAPSHOT")
                            .region(ec2.serviceClientConfiguration().region().id())
                            .estimatedMonthlyCost(snap.volumeSize() * 0.05)
                            .resolved(false)
                            .build();
                    res.setTenantId(tenantId);
                    result.add(res);
                }
            }
        } catch (Exception e) {
            log.warn("Error detecting snapshots: {}", e.getMessage());
        }
        return result;
    }

    private List<OrphanedResource> detectIdleLoadBalancers(String tenantId, CloudCredentials credentials) {
        List<OrphanedResource> result = new ArrayList<>();
        try (ElasticLoadBalancingV2Client elb = createElbClient(credentials);
             CloudWatchClient cw = createCwClient(credentials)) {
            
            DescribeLoadBalancersResponse response = elb.describeLoadBalancers();
            for (LoadBalancer lb : response.loadBalancers()) {
                if (isLoadBalancerIdle(cw, lb)) {
                    OrphanedResource res = OrphanedResource.builder()
                            .resourceId(lb.loadBalancerArn())
                            .resourceType("LOAD_BALANCER")
                            .region(elb.serviceClientConfiguration().region().id())
                            .estimatedMonthlyCost(18.00)
                            .resolved(false)
                            .build();
                    res.setTenantId(tenantId);
                    result.add(res);
                }
            }
        } catch (Exception e) {
            log.warn("Error detecting ELBs: {}", e.getMessage());
        }
        return result;
    }

    public String executeCleanup(String resourceId, String resourceType, boolean dryRun) {
        String tenantId = TenantContext.getTenantId();
        if (dryRun) return "DRY_RUN: Simulation completed for " + resourceType + " " + resourceId;
        
        try {
            CloudCredentials credentials = tenantCredentialService.getCredentials(tenantId);
            if (credentials == null) return "FAILED_NO_CREDS";

            try (Ec2Client ec2 = createEc2Client(credentials)) {
                switch (resourceType) {
                    case "EBS_VOLUME":
                        ec2.deleteVolume(DeleteVolumeRequest.builder().volumeId(resourceId).build());
                        break;
                    case "ELASTIC_IP":
                        ec2.releaseAddress(ReleaseAddressRequest.builder().allocationId(resourceId).build());
                        break;
                    case "SNAPSHOT":
                        ec2.deleteSnapshot(DeleteSnapshotRequest.builder().snapshotId(resourceId).build());
                        break;
                    default:
                        return "FAILED_UNKNOWN_TYPE";
                }
                
                orphanedResourceRepository.findByTenantId(tenantId).stream()
                    .filter(res -> res.getResourceId().equals(resourceId))
                    .forEach(res -> {
                        res.setResolved(true);
                        orphanedResourceRepository.save(res);
                    });
                
                return "SUCCESS: Deleted " + resourceType + " " + resourceId;
            }
        } catch (Exception e) {
            log.error("Error deleting resource: {}", e.getMessage());
            return "FAILED";
        }
    }

    private OrphanedResourceResponseDTO mapToDTO(OrphanedResource entity) {
        return new OrphanedResourceResponseDTO(
            entity.getResourceId(),
            entity.getResourceType(),
            entity.getRegion(),
            entity.getEstimatedMonthlyCost(),
            entity.isResolved(),
            entity.getCreatedAt()
        );
    }

    private Ec2Client createEc2Client(CloudCredentials credentials) {
        return Ec2Client.builder()
                .region(Region.of(credentials.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())))
                .build();
    }

    private ElasticLoadBalancingV2Client createElbClient(CloudCredentials credentials) {
        return ElasticLoadBalancingV2Client.builder()
                .region(Region.of(credentials.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())))
                .build();
    }
    
    private CloudWatchClient createCwClient(CloudCredentials credentials) {
        return CloudWatchClient.builder()
                .region(Region.of(credentials.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())))
                .build();
    }

    private boolean isLoadBalancerIdle(CloudWatchClient cw, LoadBalancer lb) {
        try {
            String lbName = lb.loadBalancerArn().substring(lb.loadBalancerArn().indexOf("loadbalancer/") + 13);
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/ApplicationELB")
                    .metricName("RequestCount")
                    .dimensions(software.amazon.awssdk.services.cloudwatch.model.Dimension.builder()
                            .name("LoadBalancer").value(lbName).build())
                    .startTime(Instant.now().minus(7, ChronoUnit.DAYS))
                    .endTime(Instant.now())
                    .period(86400 * 7)
                    .statistics(software.amazon.awssdk.services.cloudwatch.model.Statistic.SUM)
                    .build();
            var response = cw.getMetricStatistics(request);
            if (response.datapoints().isEmpty()) return true;
            return response.datapoints().get(0).sum() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
