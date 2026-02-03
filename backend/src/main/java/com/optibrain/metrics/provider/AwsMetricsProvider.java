package com.optibrain.metrics.provider;

import com.optibrain.metrics.model.MetricData;
import com.optibrain.metrics.model.MetricSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component("awsMetricsProvider")
@Slf4j
public class AwsMetricsProvider implements MetricsProvider {

    private final CloudWatchClient cloudWatchClient;
    private final CostExplorerClient costExplorerClient;
    private final String region;

    public AwsMetricsProvider(@Value("${aws.region:us-east-1}") String region) {
        this.region = region;
        this.cloudWatchClient = CloudWatchClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.costExplorerClient = CostExplorerClient.builder()
                .region(Region.US_EAST_1) // Cost Explorer only works in us-east-1
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Override
    public List<MetricData> getCurrentMetrics() {
        try {
            double cpu = getAverageCpuUtilization();
            double memory = getAverageMemoryUtilization();
            int instanceCount = getActiveInstanceCount();
            double hourlyCost = getHourlyCost();
            List<MetricSnapshot> history = getHistoricalData(24);

            MetricData data = MetricData.builder()
                    .cpuUtilization(cpu)
                    .memoryUtilization(memory)
                    .hourlyCost(hourlyCost)
                    .instanceCount(instanceCount)
                    .timestamp(Instant.now())
                    .region(region)
                    .serviceBreakdown(getServiceCostBreakdown())
                    .historicalData(history)
                    .build();
            
            return Collections.singletonList(data);
        } catch (Exception e) {
            log.error("Failed to fetch AWS metrics, using fallback: {}", e.getMessage());
            return Collections.singletonList(getFallbackMetrics());
        }
    }

    private double getAverageCpuUtilization() {
        try {
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/EC2")
                    .metricName("CPUUtilization")
                    .startTime(Instant.now().minusSeconds(3600))
                    .endTime(Instant.now())
                    .period(300)
                    .statistics(Statistic.AVERAGE)
                    .build();

            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
            Optional<Datapoint> latest = response.datapoints().stream()
                    .max(Comparator.comparing(Datapoint::timestamp));
            
            if (latest.isPresent()) {
                return latest.get().average();
            } else {
                log.warn("No CPU metrics available from CloudWatch");
                return 0.0; // Return 0 to indicate no data, not fake data
            }
        } catch (Exception e) {
            log.error("Failed to fetch CPU metrics from CloudWatch: {}", e.getMessage());
            return 0.0;
        }
    }

    private double getAverageMemoryUtilization() {
        try {
            // Note: Memory metrics require CloudWatch agent to be installed on EC2 instances
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("CWAgent")  // CloudWatch agent namespace
                    .metricName("mem_used_percent")
                    .startTime(Instant.now().minusSeconds(3600))
                    .endTime(Instant.now())
                    .period(300)
                    .statistics(Statistic.AVERAGE)
                    .build();

            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
            Optional<Datapoint> latest = response.datapoints().stream()
                    .max(Comparator.comparing(Datapoint::timestamp));
            
            if (latest.isPresent()) {
                return latest.get().average();
            } else {
                log.warn("No memory metrics available from CloudWatch. Ensure CloudWatch agent is installed.");
                return 0.0; // Return 0 to indicate no data
            }
        } catch (Exception e) {
            log.error("Failed to fetch memory metrics from CloudWatch: {}", e.getMessage());
            return 0.0;
        }
    }

    private int getActiveInstanceCount() {
        try {
            // Use EC2 API to get actual running instance count
            software.amazon.awssdk.services.ec2.Ec2Client ec2 = software.amazon.awssdk.services.ec2.Ec2Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
            
            software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest request = 
                software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest.builder()
                    .filters(software.amazon.awssdk.services.ec2.model.Filter.builder()
                        .name("instance-state-name")
                        .values("running")
                        .build())
                    .build();
            
            software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse response = ec2.describeInstances(request);
            int count = (int) response.reservations().stream()
                    .flatMap(r -> r.instances().stream())
                    .count();
            
            ec2.close();
            log.info("Active EC2 instances: {}", count);
            return count;
        } catch (Exception e) {
            log.error("Failed to get active instance count: {}", e.getMessage());
            return 0;
        }
    }

    private double getHourlyCost() {
        try {
            // Get yesterday's date for completed data
            Instant yesterday = Instant.now().minusSeconds(86400);
            String startDate = yesterday.toString().substring(0, 10);
            String endDate = Instant.now().toString().substring(0, 10);
            
            GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                    .timePeriod(DateInterval.builder()
                            .start(startDate)
                            .end(endDate)
                            .build())
                    .granularity(Granularity.DAILY)
                    .metrics("UnblendedCost")
                    .build();

            GetCostAndUsageResponse response = costExplorerClient.getCostAndUsage(request);
            double totalCost = response.resultsByTime().stream()
                    .mapToDouble(result -> {
                        String amount = result.total().get("UnblendedCost").amount();
                        return Double.parseDouble(amount);
                    })
                    .sum();

            // Convert daily to hourly
            double hourlyCost = totalCost / 24;
            log.info("Hourly cost estimate: ${}", String.format("%.2f", hourlyCost));
            return hourlyCost;
        } catch (Exception e) {
            log.error("Failed to fetch cost data from Cost Explorer: {}", e.getMessage());
            return 0.0;
        }
    }

    private Map<String, Double> getServiceCostBreakdown() {
        try {
            // Get yesterday's date for completed data
            Instant yesterday = Instant.now().minusSeconds(86400);
            String startDate = yesterday.toString().substring(0, 10);
            String endDate = Instant.now().toString().substring(0, 10);
            
            GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                    .timePeriod(DateInterval.builder()
                            .start(startDate)
                            .end(endDate)
                            .build())
                    .granularity(Granularity.DAILY)
                    .metrics("UnblendedCost")
                    .groupBy(GroupDefinition.builder()
                            .type(GroupDefinitionType.DIMENSION)
                            .key("SERVICE")
                            .build())
                    .build();

            GetCostAndUsageResponse response = costExplorerClient.getCostAndUsage(request);
            Map<String, Double> breakdown = new HashMap<>();
            
            response.resultsByTime().stream()
                    .flatMap(result -> result.groups().stream())
                    .forEach(group -> {
                        String service = group.keys().get(0);
                        double cost = Double.parseDouble(
                            group.metrics().get("UnblendedCost").amount()) / 24;
                        breakdown.put(service, cost);
                    });
            
            log.info("Service cost breakdown: {}", breakdown);
            return breakdown;
        } catch (Exception e) {
            log.error("Failed to fetch service cost breakdown: {}", e.getMessage());
            return new HashMap<>(); // Return empty map instead of fake data
        }
    }

    private List<MetricSnapshot> getHistoricalData(int hours) {
        List<MetricSnapshot> history = new ArrayList<>();
        try {
            // Fetch actual historical CPU data from CloudWatch
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/EC2")
                    .metricName("CPUUtilization")
                    .startTime(Instant.now().minusSeconds(hours * 3600L))
                    .endTime(Instant.now())
                    .period(3600) // 1 hour periods
                    .statistics(Statistic.AVERAGE)
                    .build();

            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
            
            for (Datapoint datapoint : response.datapoints()) {
                history.add(MetricSnapshot.builder()
                        .timestamp(datapoint.timestamp())
                        .cpu(datapoint.average())
                        .memory(0.0) // Memory not available without CloudWatch agent
                        .build());
            }
            
            log.info("Fetched {} historical data points", history.size());
        } catch (Exception e) {
            log.error("Failed to fetch historical data: {}", e.getMessage());
        }
        return history;
    }

    private MetricData getFallbackMetrics() {
        log.warn("Using fallback metrics - AWS data unavailable");
        return MetricData.builder()
                .cpuUtilization(0.0)
                .memoryUtilization(0.0)
                .hourlyCost(0.0)
                .instanceCount(0)
                .timestamp(Instant.now())
                .region(region)
                .serviceBreakdown(new HashMap<>())
                .historicalData(new ArrayList<>())
                .build();
    }

    @Override
    public String getProviderSource() {
        return "AWS_CLOUDWATCH";
    }
    
    @Override
    public MetricSnapshot getMetricSnapshot(String tenantId) {
        List<MetricData> metrics = getCurrentMetrics();
        return MetricSnapshot.builder()
                .tenantId(tenantId)
                .timestamp(Instant.now())
                .metrics(metrics)
                .totalCost(metrics.stream()
                        .filter(m -> m.getName() != null && m.getName().contains("Cost"))
                        .mapToDouble(MetricData::getValue)
                        .sum())
                .build();
    }
}
