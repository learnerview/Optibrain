package com.optibrain.costs.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating AWS cost reports using Cost Explorer API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CostReportService {

    @Value("${aws.region:us-east-1}")
    private String region;

    /**
     * Generate a daily cost report for the past N days
     */
    public CostReport generateDailyCostReport(int days) {
        log.info("Generating daily cost report for the past {} days", days);
        
        try (CostExplorerClient costExplorer = createCostExplorerClient()) {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);
            
            GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                    .timePeriod(DateInterval.builder()
                            .start(startDate.format(DateTimeFormatter.ISO_DATE))
                            .end(endDate.format(DateTimeFormatter.ISO_DATE))
                            .build())
                    .granularity(Granularity.DAILY)
                    .metrics("UnblendedCost", "UsageQuantity")
                    .groupBy(GroupDefinition.builder()
                            .type(GroupDefinitionType.DIMENSION)
                            .key("SERVICE")
                            .build())
                    .build();
            
            GetCostAndUsageResponse response = costExplorer.getCostAndUsage(request);
            
            return buildCostReport(response, "DAILY", startDate, endDate);
            
        } catch (Exception e) {
            log.error("Failed to generate daily cost report: {}", e.getMessage());
            return CostReport.builder()
                    .reportType("DAILY")
                    .startDate(LocalDate.now().minusDays(days))
                    .endDate(LocalDate.now())
                    .totalCost(0.0)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Generate a weekly cost report
     */
    public CostReport generateWeeklyCostReport(int weeks) {
        log.info("Generating weekly cost report for the past {} weeks", weeks);
        
        try (CostExplorerClient costExplorer = createCostExplorerClient()) {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusWeeks(weeks);
            
            GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                    .timePeriod(DateInterval.builder()
                            .start(startDate.format(DateTimeFormatter.ISO_DATE))
                            .end(endDate.format(DateTimeFormatter.ISO_DATE))
                            .build())
                    .granularity(Granularity.DAILY)
                    .metrics("UnblendedCost", "UsageQuantity")
                    .groupBy(GroupDefinition.builder()
                            .type(GroupDefinitionType.DIMENSION)
                            .key("SERVICE")
                            .build())
                    .build();
            
            GetCostAndUsageResponse response = costExplorer.getCostAndUsage(request);
            
            return buildCostReport(response, "WEEKLY", startDate, endDate);
            
        } catch (Exception e) {
            log.error("Failed to generate weekly cost report: {}", e.getMessage());
            return CostReport.builder()
                    .reportType("WEEKLY")
                    .startDate(LocalDate.now().minusWeeks(weeks))
                    .endDate(LocalDate.now())
                    .totalCost(0.0)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Get cost forecast for the next N days
     */
    public CostForecast getCostForecast(int days) {
        log.info("Generating cost forecast for the next {} days", days);
        
        try (CostExplorerClient costExplorer = createCostExplorerClient()) {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(days);
            
            GetCostForecastRequest request = GetCostForecastRequest.builder()
                    .timePeriod(DateInterval.builder()
                            .start(startDate.format(DateTimeFormatter.ISO_DATE))
                            .end(endDate.format(DateTimeFormatter.ISO_DATE))
                            .build())
                    .metric(software.amazon.awssdk.services.costexplorer.model.Metric.UNBLENDED_COST)
                    .granularity(Granularity.DAILY)
                    .build();
            
            GetCostForecastResponse response = costExplorer.getCostForecast(request);
            
            double totalForecast = Double.parseDouble(response.total().amount());
            
            log.info("Cost forecast for next {} days: ${}", days, String.format("%.2f", totalForecast));
            
            return CostForecast.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .forecastedCost(totalForecast)
                    .meanValue(totalForecast)
                    .predictionIntervalLowerBound(0.0) // Would need more data from API
                    .predictionIntervalUpperBound(0.0)
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to generate cost forecast: {}", e.getMessage());
            return CostForecast.builder()
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusDays(days))
                    .forecastedCost(0.0)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Get cost by service for a specific time period
     */
    public Map<String, Double> getCostByService(LocalDate startDate, LocalDate endDate) {
        try (CostExplorerClient costExplorer = createCostExplorerClient()) {
            GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                    .timePeriod(DateInterval.builder()
                            .start(startDate.format(DateTimeFormatter.ISO_DATE))
                            .end(endDate.format(DateTimeFormatter.ISO_DATE))
                            .build())
                    .granularity(Granularity.MONTHLY)
                    .metrics("UnblendedCost")
                    .groupBy(GroupDefinition.builder()
                            .type(GroupDefinitionType.DIMENSION)
                            .key("SERVICE")
                            .build())
                    .build();
            
            GetCostAndUsageResponse response = costExplorer.getCostAndUsage(request);
            
            Map<String, Double> costByService = new HashMap<>();
            
            for (ResultByTime result : response.resultsByTime()) {
                for (Group group : result.groups()) {
                    String service = group.keys().get(0);
                    double cost = Double.parseDouble(group.metrics().get("UnblendedCost").amount());
                    costByService.merge(service, cost, Double::sum);
                }
            }
            
            log.info("Cost by service: {}", costByService);
            return costByService;
            
        } catch (Exception e) {
            log.error("Failed to get cost by service: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private CostReport buildCostReport(GetCostAndUsageResponse response, String reportType, 
                                       LocalDate startDate, LocalDate endDate) {
        Map<String, Double> costByService = new HashMap<>();
        Map<String, List<DailyCost>> costTimeSeries = new HashMap<>();
        double totalCost = 0.0;
        
        for (ResultByTime result : response.resultsByTime()) {
            String date = result.timePeriod().start();
            
            for (Group group : result.groups()) {
                String service = group.keys().get(0);
                double cost = Double.parseDouble(group.metrics().get("UnblendedCost").amount());
                
                costByService.merge(service, cost, Double::sum);
                totalCost += cost;
                
                costTimeSeries.computeIfAbsent(service, k -> new ArrayList<>())
                        .add(new DailyCost(date, cost));
            }
        }
        
        log.info("{} cost report generated: Total=${}", reportType, String.format("%.2f", totalCost));
        
        return CostReport.builder()
                .reportType(reportType)
                .startDate(startDate)
                .endDate(endDate)
                .totalCost(totalCost)
                .costByService(costByService)
                .costTimeSeries(costTimeSeries)
                .topServices(getTopServices(costByService, 5))
                .build();
    }

    private List<String> getTopServices(Map<String, Double> costByService, int limit) {
        return costByService.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private CostExplorerClient createCostExplorerClient() {
        // Cost Explorer API is only available in us-east-1
        return CostExplorerClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Data
    @lombok.Builder
    public static class CostReport {
        private String reportType;
        private LocalDate startDate;
        private LocalDate endDate;
        private double totalCost;
        private Map<String, Double> costByService;
        private Map<String, List<DailyCost>> costTimeSeries;
        private List<String> topServices;
        private String error;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class DailyCost {
        private String date;
        private double cost;
    }

    @Data
    @lombok.Builder
    public static class CostForecast {
        private LocalDate startDate;
        private LocalDate endDate;
        private double forecastedCost;
        private double meanValue;
        private double predictionIntervalLowerBound;
        private double predictionIntervalUpperBound;
        private String error;
    }
}
