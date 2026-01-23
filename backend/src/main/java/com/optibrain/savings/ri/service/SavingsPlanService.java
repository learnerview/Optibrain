package com.optibrain.savings.ri.service;

import com.optibrain.savings.ri.model.SavingsPlan;
import com.optibrain.tenant.model.CloudCredentials;
import com.optibrain.tenant.service.TenantCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsPlanService {

    private final TenantCredentialService tenantCredentialService;

    public List<SavingsPlan> getRecommendations(String tenantId) {
        try {
            CloudCredentials credentials = tenantCredentialService.getCredentials(tenantId);
            if (credentials == null) {
                return new ArrayList<>();
            }

            try (CostExplorerClient ceClient = createCostExplorerClient(credentials)) {
                
                GetSavingsPlansPurchaseRecommendationRequest request = GetSavingsPlansPurchaseRecommendationRequest.builder()
                        .savingsPlansType(SupportedSavingsPlansType.COMPUTE_SP)
                        .termInYears(TermInYears.ONE_YEAR)
                        .paymentOption(PaymentOption.NO_UPFRONT)
                        .lookbackPeriodInDays(LookbackPeriodInDays.SEVEN_DAYS)
                        .build();

                GetSavingsPlansPurchaseRecommendationResponse response = ceClient.getSavingsPlansPurchaseRecommendation(request);
                
                return mapToSavingsPlans(response, tenantId);
            }
        } catch (Exception e) {
            log.error("Failed to fetch SP recommendations for tenant {}: {}", tenantId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private CostExplorerClient createCostExplorerClient(CloudCredentials credentials) {
        return CostExplorerClient.builder()
                .region(Region.of(credentials.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey())))
                .build();
    }

    private List<SavingsPlan> mapToSavingsPlans(GetSavingsPlansPurchaseRecommendationResponse response, String tenantId) {
        List<SavingsPlan> recommendations = new ArrayList<>();
        
        if (response.savingsPlansPurchaseRecommendation() == null) return recommendations;
        
        // Note: The response structure is slightly different for SPs
        // This is a simplified mapping
        var recommendation = response.savingsPlansPurchaseRecommendation();
        for (SavingsPlansPurchaseRecommendationDetail detail : recommendation.savingsPlansPurchaseRecommendationDetails()) {
             recommendations.add(SavingsPlan.builder()
                    .tenantId(tenantId)
                    .provider("AWS")
                    .savingsPlanType("Compute")
                    .term("1yr")
                    .paymentOption("No Upfront")
                    .commitment(Double.parseDouble(detail.hourlyCommitmentToPurchase()))
                    .currency(detail.currencyCode())
                    .upfrontPaymentAmount(0.0)
                    .recurringPaymentAmount(Double.parseDouble(detail.estimatedMonthlySavingsAmount())) // This is savings, not cost, simplified for now
                    .state("RECOMMENDED")
                    .build());
        }

        return recommendations;
    }
}
