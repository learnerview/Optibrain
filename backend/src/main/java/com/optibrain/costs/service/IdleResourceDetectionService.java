package com.optibrain.costs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting idle EC2 instances based on CPU utilization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdleResourceDetectionService {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${app.cost.idle-cpu-threshold:10}")
    private double idleCpuThreshold;

    @Value("${app.cost.idle-days-threshold:7}")
    private int idleDaysThreshold;

    /**
     * Detects idle EC2 instances based on CPU utilization over a period
     * @return Map of instance ID to average CPU utilization
     */
    public Map<String, IdleInstanceInfo> detectIdleEC2Instances() {
        Map<String, IdleInstanceInfo> idleInstances = new HashMap<>();
        
        try (Ec2Client ec2 = createEc2Client();
             CloudWatchClient cloudWatch = createCloudWatchClient()) {
            
            // Get all running instances
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .filters(Filter.builder()
                            .name("instance-state-name")
                            .values("running")
                            .build())
                    .build();
            
            DescribeInstancesResponse response = ec2.describeInstances(request);
            
            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    String instanceId = instance.instanceId();
                    
                    // Get CPU utilization for the past N days
                    double avgCpu = getAverageCpuUtilization(cloudWatch, instanceId, idleDaysThreshold);
                    
                    if (avgCpu < idleCpuThreshold && avgCpu >= 0) {
                        log.info("Found idle instance: {} with CPU: {}%", instanceId, avgCpu);
                        idleInstances.put(instanceId, IdleInstanceInfo.builder()
                                .instanceId(instanceId)
                                .instanceType(instance.instanceType().toString())
                                .averageCpuUtilization(avgCpu)
                                .state(instance.state().nameAsString())
                                .launchTime(instance.launchTime())
                                .tags(instance.tags().stream()
                                        .collect(Collectors.toMap(
                                                software.amazon.awssdk.services.ec2.model.Tag::key,
                                                software.amazon.awssdk.services.ec2.model.Tag::value)))
                                .build());
                    }
                }
            }
            
            log.info("Detected {} idle instances out of threshold", idleInstances.size());
            
        } catch (Exception e) {
            log.error("Failed to detect idle instances: {}", e.getMessage());
        }
        
        return idleInstances;
    }

    /**
     * Detects stopped EC2 instances that have been stopped for a long time
     */
    public List<StoppedInstanceInfo> detectStoppedInstances() {
        List<StoppedInstanceInfo> stoppedInstances = new ArrayList<>();
        
        try (Ec2Client ec2 = createEc2Client()) {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .filters(Filter.builder()
                            .name("instance-state-name")
                            .values("stopped")
                            .build())
                    .build();
            
            DescribeInstancesResponse response = ec2.describeInstances(request);
            
            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    log.info("Found stopped instance: {} ({})", 
                            instance.instanceId(), instance.instanceType());
                    
                    stoppedInstances.add(StoppedInstanceInfo.builder()
                            .instanceId(instance.instanceId())
                            .instanceType(instance.instanceType().toString())
                            .state(instance.state().nameAsString())
                            .launchTime(instance.launchTime())
                            .stateTransitionReason(instance.stateTransitionReason())
                            .tags(instance.tags().stream()
                                    .collect(Collectors.toMap(
                                            software.amazon.awssdk.services.ec2.model.Tag::key,
                                            software.amazon.awssdk.services.ec2.model.Tag::value)))
                            .build());
                }
            }
            
            log.info("Found {} stopped instances", stoppedInstances.size());
            
        } catch (Exception e) {
            log.error("Failed to detect stopped instances: {}", e.getMessage());
        }
        
        return stoppedInstances;
    }

    private double getAverageCpuUtilization(CloudWatchClient cloudWatch, String instanceId, int days) {
        try {
            Instant endTime = Instant.now();
            Instant startTime = endTime.minusSeconds(days * 86400L);
            
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/EC2")
                    .metricName("CPUUtilization")
                    .dimensions(Dimension.builder()
                            .name("InstanceId")
                            .value(instanceId)
                            .build())
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(3600) // 1 hour periods
                    .statistics(Statistic.AVERAGE)
                    .build();
            
            GetMetricStatisticsResponse response = cloudWatch.getMetricStatistics(request);
            
            if (response.datapoints().isEmpty()) {
                log.warn("No CPU metrics available for instance: {}", instanceId);
                return -1.0; // Indicate no data available
            }
            
            double avgCpu = response.datapoints().stream()
                    .mapToDouble(Datapoint::average)
                    .average()
                    .orElse(-1.0);
            
            return avgCpu;
            
        } catch (Exception e) {
            log.error("Failed to get CPU metrics for {}: {}", instanceId, e.getMessage());
            return -1.0;
        }
    }

    private Ec2Client createEc2Client() {
        return Ec2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    private CloudWatchClient createCloudWatchClient() {
        return CloudWatchClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class IdleInstanceInfo {
        private String instanceId;
        private String instanceType;
        private double averageCpuUtilization;
        private String state;
        private Instant launchTime;
        private Map<String, String> tags;
    }

    @lombok.Data
    @lombok.Builder
    public static class StoppedInstanceInfo {
        private String instanceId;
        private String instanceType;
        private String state;
        private Instant launchTime;
        private String stateTransitionReason;
        private Map<String, String> tags;
    }
}
