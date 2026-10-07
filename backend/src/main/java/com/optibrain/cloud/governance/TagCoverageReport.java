package com.optibrain.cloud.governance;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Tag coverage across the account.
 *
 * <p>{@code available} distinguishes a measured report from an account with no inventory.
 * A report with no data and a report showing zero coverage are different findings.
 *
 * @param requiredKeys tag keys every resource must carry
 * @param resourcesAssessed resources included in the assessment
 * @param compliantResources resources carrying every required key
 * @param coveragePercentage compliant as a percentage of assessed
 * @param attributedMonthlyCost summed monthly cost where attribution exists
 * @param uncoveredMonthlyCost attributed cost belonging to non-compliant resources
 * @param uncoveredShareOfSpend uncovered as a percentage of attributed cost
 * @param uncostedResourceCount resources whose cost cannot be attributed at all
 * @param violationsByService non-compliant resource count per resource type
 * @param uncoveredByService uncovered monthly cost per resource type
 * @param offenders individual non-compliant resources, most expensive first
 * @param enforcement how enforcement is handled
 * @param available false when the inventory is empty and no assessment was possible
 * @param reason why the report is unavailable, when it is
 */
@Builder
public record TagCoverageReport(
        List<String> requiredKeys,
        int resourcesAssessed,
        int compliantResources,
        double coveragePercentage,
        BigDecimal attributedMonthlyCost,
        BigDecimal uncoveredMonthlyCost,
        BigDecimal uncoveredShareOfSpend,
        int uncostedResourceCount,
        Map<String, Integer> violationsByService,
        Map<String, BigDecimal> uncoveredByService,
        List<TagOffender> offenders,
        String enforcement,
        boolean available,
        String reason
) {

    public TagCoverageReport {
        requiredKeys = requiredKeys == null ? List.of() : List.copyOf(requiredKeys);
        violationsByService = violationsByService == null ? Map.of() : Map.copyOf(violationsByService);
        uncoveredByService = uncoveredByService == null ? Map.of() : Map.copyOf(uncoveredByService);
        offenders = offenders == null ? List.of() : List.copyOf(offenders);
    }

    static TagCoverageReport unavailable(List<String> required, String reason) {
        return TagCoverageReport.builder()
                .requiredKeys(required)
                .enforcement("AWS tag policies and service control policies perform "
                        + "enforcement. This report is audit-only.")
                .available(false)
                .reason(reason)
                .build();
    }
}