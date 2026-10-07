package com.optibrain.savings.ri.service;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.savings.ri.model.ReservedInstance;
import com.optibrain.tenant.service.TenantCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RIService {

    private final TenantCredentialService tenantCredentialService;
    private final com.optibrain.config.service.TenantConfigurationService configService;
    private final AwsClientFactory awsClientFactory;

    public List<ReservedInstance> getRecommendations(String tenantId) {
        try {
            var config = configService.getConfiguration(tenantId);
            if (tenantCredentialService.findTenant(tenantId).isEmpty()) {
                return new ArrayList<>();
            }

            // Using CostExplorerClient to get reservation recommendations
            try (CostExplorerClient ceClient = createCostExplorerClient()) {

                TermInYears term = "3yr".equalsIgnoreCase(String.valueOf(config.getRiPreferredTerm())) ? TermInYears.THREE_YEARS : TermInYears.ONE_YEAR;
                PaymentOption payment = "ALL_UPFRONT".equalsIgnoreCase(config.getRiPaymentOption()) ? PaymentOption.ALL_UPFRONT :
                                      "PARTIAL_UPFRONT".equalsIgnoreCase(config.getRiPaymentOption()) ? PaymentOption.PARTIAL_UPFRONT : PaymentOption.NO_UPFRONT;

                GetReservationPurchaseRecommendationRequest request = GetReservationPurchaseRecommendationRequest.builder()
                        .service("Amazon Elastic Compute Cloud - Compute")
                        .termInYears(term)
                        .paymentOption(payment)
                        .lookbackPeriodInDays(LookbackPeriodInDays.SEVEN_DAYS)
                        .build();

                GetReservationPurchaseRecommendationResponse response = ceClient.getReservationPurchaseRecommendation(request);

                return mapToReservedInstances(response, tenantId);
            }
        } catch (Exception e) {
            log.error("Failed to fetch RI recommendations for tenant {}: {}", tenantId, e.getMessage());
            return new ArrayList<>();
        }
    }

    public double calculateCoverage(String tenantId) {
         try {
            if (tenantCredentialService.findTenant(tenantId).isEmpty()) {
                return 0.0;
            }

            try (CostExplorerClient ceClient = createCostExplorerClient()) {
                DateInterval interval = DateInterval.builder()
                        .start(Instant.now().minusSeconds(86400 * 30).toString().split("T")[0])
                        .end(Instant.now().toString().split("T")[0])
                        .build();

                GetReservationCoverageRequest request = GetReservationCoverageRequest.builder()
                        .timePeriod(interval)
                        .build();

                GetReservationCoverageResponse response = ceClient.getReservationCoverage(request);
                if (!response.coveragesByTime().isEmpty()) {
                   String coverageStr = response.coveragesByTime().get(response.coveragesByTime().size() - 1)
                           .total().coverageHours().coverageHoursPercentage();
                   return Double.parseDouble(coverageStr);
                }
            }
            return 0.0;
        } catch (Exception e) {
            log.error("Failed to calculate RI coverage for tenant {}: {}", tenantId, e.getMessage());
            return 0.0;
        }
    }

    private CostExplorerClient createCostExplorerClient() {
        // Region, credentials and the SANDBOX endpoint override are decided centrally by
        // AwsClientFactory. Per-tenant clients bypassed the override and exposed raw
        // access keys to every caller.
        return awsClientFactory.costExplorer();
    }

    private List<ReservedInstance> mapToReservedInstances(GetReservationPurchaseRecommendationResponse response, String tenantId) {
        List<ReservedInstance> recommendations = new ArrayList<>();
        
        if (response.recommendations() == null) return recommendations;

        for (ReservationPurchaseRecommendation recommendation : response.recommendations()) {
            for (ReservationPurchaseRecommendationDetail detail : recommendation.recommendationDetails()) {
                double recurringCost = Double.parseDouble(detail.recurringStandardMonthlyCost());
                double onDemandCost = Double.parseDouble(detail.estimatedMonthlyOnDemandCost());

                // Read the upfront payment from the API rather than assuming zero. The
                // request may ask for ALL_UPFRONT or PARTIAL_UPFRONT, and reporting a
                // $0 fixed price for those would understate the commitment.
                double upfront = detail.upfrontCost() == null
                        ? 0.0 : Double.parseDouble(detail.upfrontCost());

                recommendations.add(ReservedInstance.builder()
                        .tenantId(tenantId)
                        .provider("AWS")
                        .instanceType(detail.instanceDetails().ec2InstanceDetails().instanceType())
                        .region(detail.instanceDetails().ec2InstanceDetails().region())
                        .platform(detail.instanceDetails().ec2InstanceDetails().platform())
                        .term("1yr") // Matching request
                        .offeringType("No Upfront") // Matching request
                        .instanceCount(Integer.parseInt(detail.recommendedNumberOfInstancesToPurchase()))
                        .fixedPrice(upfront) 
                        .recurringCharges(recurringCost)
                        .usagePrice(onDemandCost) // Using this field to store On-Demand equivalent for comparison
                        .currency(detail.currencyCode())
                        .state("RECOMMENDED")
                        .build());
            }
        }
        return recommendations;
    }
    
    public double calculateBreakEvenMonths(double upfront, double monthlyOnDemand, double monthlyRI) {
        double monthlySavings = monthlyOnDemand - monthlyRI;
        if (monthlySavings <= 0) return -1; // Never breaks even
        return upfront / monthlySavings;
    }
}
