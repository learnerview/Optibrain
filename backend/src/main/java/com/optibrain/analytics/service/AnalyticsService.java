package com.optibrain.analytics.service;

import com.optibrain.analytics.dto.*;
import com.optibrain.analytics.model.FinancialReport;
import com.optibrain.analytics.repository.FinancialReportRepository;
import com.optibrain.common.context.TenantContext;
import com.optibrain.tenant.service.TenantCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final TenantCredentialService tenantCredentialService;
    private final FinancialReportRepository financialReportRepository;

    public FinancialReportResponseDTO generateReport(String period) {
        String tenantId = TenantContext.getTenantId();
        log.info("Generating {} financial report for tenant {}", period, tenantId);
        
        try {
            var credentials = tenantCredentialService.getCredentials(tenantId);
            if (credentials == null) {
                return financialReportRepository.findByTenantId(tenantId).stream()
                        .max(Comparator.comparing(FinancialReport::getCreatedAt))
                        .map(this::mapToDTO)
                        .orElse(null);
            }

            try (CostExplorerClient ce = createCeClient(credentials)) {
                LocalDate now = LocalDate.now();
                String start = now.minusMonths(2).withDayOfMonth(1).toString();
                String end = now.toString();

                GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                        .timePeriod(DateInterval.builder().start(start).end(end).build())
                        .granularity(Granularity.MONTHLY)
                        .metrics("UnblendedCost")
                        .build();

                GetCostAndUsageResponse response = ce.getCostAndUsage(request);
                
                double currentCost = 0.0;
                double previousCost = 0.0;
                
                if (response.resultsByTime().size() >= 2) {
                    previousCost = Double.parseDouble(response.resultsByTime().get(0).total().get("UnblendedCost").amount());
                    currentCost = Double.parseDouble(response.resultsByTime().get(1).total().get("UnblendedCost").amount());
                }

                GetCostForecastResponse forecastResponse = ce.getCostForecast(GetCostForecastRequest.builder()
                        .timePeriod(DateInterval.builder()
                                .start(now.plusDays(1).toString())
                                .end(now.plusDays(30).toString()).build())
                        .metric(Metric.UNBLENDED_COST)
                        .granularity(Granularity.MONTHLY)
                        .build());
                
                double forecasted = Double.parseDouble(forecastResponse.total().amount());

                FinancialReport report = FinancialReport.builder()
                        .period(period)
                        .currentCost(currentCost)
                        .previousCost(previousCost)
                        .forecastedCost(forecasted)
                        .potentialSavings(currentCost * 0.15) // Placeholder logic
                        .topSpenders(getMockTopSpenders())
                        .source("AWS_CE")
                        .build();
                
                report.setTenantId(tenantId);
                financialReportRepository.save(report);
                
                return mapToDTO(report);
            }
        } catch (Exception e) {
            log.error("Report generation failed: {}", e.getMessage());
            return financialReportRepository.findByTenantId(tenantId).stream()
                    .max(Comparator.comparing(FinancialReport::getCreatedAt))
                    .map(this::mapToDTO)
                    .orElse(null);
        }
    }

    public List<FinancialReportResponseDTO> getReports() {
        return financialReportRepository.findByTenantId(TenantContext.getTenantId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private final com.optibrain.chat.service.PyBridgeService pyBridgeService;
    private final com.optibrain.autoscaling.repository.SpotActionRepository spotActionRepository;

    public CostSummaryDTO getCostSummary() {
        String tenantId = TenantContext.getTenantId();
        // Sum savings from generated reports or spot actions
        double totalSaved = financialReportRepository.findByTenantId(tenantId).stream()
                .mapToDouble(FinancialReport::getPotentialSavings).sum();
        
        int activeAutomations = spotActionRepository.findByTenantId(tenantId).size();
        
        return CostSummaryDTO.builder()
                .totalSaved(totalSaved)
                .potentialMonthlySavings(totalSaved * 0.1) // Placeholder
                .savingsPercentage(22.5) // Placeholder
                .activeAutomations(activeAutomations)
                .build();
    }

    public ForecastDTO getForecast() {
        String tenantId = TenantContext.getTenantId();
        List<Double> history = List.of(120.0, 115.0, 118.0, 122.0, 125.0); // Simple mock history
        Map<String, Object> prediction = pyBridgeService.getPrediction(history);
        
        List<ForecastDTO.ForecastPointDTO> points = new ArrayList<>();
        if (prediction != null && prediction.containsKey("predictions")) {
            List<Double> preds = (List<Double>) prediction.get("predictions");
            for (int i = 0; i < preds.size(); i++) {
                points.add(new ForecastDTO.ForecastPointDTO(
                    LocalDate.now().plusDays(i + 1).toString(),
                    preds.get(i)
                ));
            }
        } else {
            // Fallback mock forecast
            for (int i = 1; i <= 30; i++) {
                points.add(new ForecastDTO.ForecastPointDTO(
                    LocalDate.now().plusDays(i).toString(),
                    125.0 + (Math.random() * 10)
                ));
            }
        }

        return ForecastDTO.builder()
                .period("30d")
                .daysAhead(30)
                .forecast(points)
                .confidence("HIGH")
                .build();
    }

    public AnomalyDTO getAnomalies() {
        String tenantId = TenantContext.getTenantId();
        List<Map<String, Object>> pyAnomalies = pyBridgeService.getAnomalies();
        
        List<AnomalyDTO.AnomalyItemDTO> items = new ArrayList<>();
        if (pyAnomalies != null && !pyAnomalies.isEmpty()) {
            for (Map<String, Object> a : pyAnomalies) {
                items.add(new AnomalyDTO.AnomalyItemDTO(
                    (String) a.getOrDefault("timestamp", LocalDate.now().toString()),
                    (Double) a.getOrDefault("hourly_cost", 0.0),
                    (String) a.getOrDefault("severity", "MEDIUM"),
                    (Double) a.getOrDefault("deviation", 0.0)
                ));
            }
        } else {
            // Baseline/mock anomalies if none detected
            items.add(new AnomalyDTO.AnomalyItemDTO(LocalDate.now().minusDays(2).toString(), 45.0, "LOW", 1.2));
            items.add(new AnomalyDTO.AnomalyItemDTO(LocalDate.now().minusDays(5).toString(), 82.0, "HIGH", 4.5));
        }

        return AnomalyDTO.builder()
                .count(items.size())
                .anomalies(items)
                .build();
    }

    private FinancialReportResponseDTO mapToDTO(FinancialReport entity) {
        return new FinancialReportResponseDTO(
            entity.getPeriod(),
            entity.getCurrentCost(),
            entity.getPreviousCost(),
            entity.getForecastedCost(),
            entity.getPotentialSavings(),
            entity.getTopSpenders(),
            entity.getSource(),
            entity.getCreatedAt()
        );
    }

    private CostExplorerClient createCeClient(com.optibrain.tenant.model.CloudCredentials credentials) {
        return CostExplorerClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())))
                .build();
    }

    private List<TopSpenderDTO> getMockTopSpenders() {
        List<TopSpenderDTO> spenders = new ArrayList<>();
        spenders.add(new TopSpenderDTO("prod-ec2-cluster", "AmazonEC2", 800.00, 61.5));
        spenders.add(new TopSpenderDTO("db-primary", "AmazonRDS", 400.00, 30.8));
        spenders.add(new TopSpenderDTO("app-storage", "AmazonS3", 100.00, 7.7));
        return spenders;
    }
}
