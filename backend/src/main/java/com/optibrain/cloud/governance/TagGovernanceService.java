package com.optibrain.cloud.governance;

import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.ResourceQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reports tag coverage and the spend it fails to explain.
 *
 * <p>Tag coverage is an input to allocation, unit economics, budgeting and anomaly
 * routing. Spend on untagged resources cannot be attributed to a team, so it is
 * effectively un-billable. Surfacing it is the cheapest leverage available in a cost
 * programme, because every downstream capability depends on the same tags.
 *
 * <p>The report is audit-only. Enforcement is a separate concern: AWS tag policies and
 * service control policies already perform it, and reimplementing enforcement in the
 * application would produce a worse version of something that exists.
 *
 * <p>Uncovered spend is summed from {@link CloudResource#monthlyCost()}, which is a
 * list-price estimate. Resources whose cost cannot be attributed report {@code null}
 * and are counted separately as uncosted rather than being treated as free.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagGovernanceService {

    /**
     * Tag keys considered required.
     *
     * <p>AWS convention. Configurable per deployment in a fuller implementation; fixed
     * here because the product has no per-tenant policy store yet.
     */
    static final List<String> REQUIRED_KEYS = List.of("Environment", "Owner");

    private final CloudProviderPort cloudProvider;

    /**
     * @param requiredKeys tag keys a resource must carry; defaults to {@link #REQUIRED_KEYS}
     */
    public TagCoverageReport report(List<String> requiredKeys) {
        List<String> required = requiredKeys == null || requiredKeys.isEmpty()
                ? REQUIRED_KEYS : requiredKeys;

        List<CloudResource> resources = cloudProvider.discover(ResourceQuery.all());

        if (resources.isEmpty()) {
            return TagCoverageReport.unavailable(required,
                    "The inventory returned no resources, so coverage cannot be assessed.");
        }

        int compliant = 0;
        BigDecimal totalAttributed = BigDecimal.ZERO;
        BigDecimal uncoveredAttributed = BigDecimal.ZERO;
        int uncostedCount = 0;
        Map<String, Integer> violationsByService = new LinkedHashMap<>();
        Map<String, BigDecimal> uncoveredByService = new LinkedHashMap<>();

        for (CloudResource resource : resources) {
            Set<String> missing = missingKeys(resource, required);

            if (resource.monthlyCost() != null) {
                BigDecimal monthly = BigDecimal.valueOf(resource.monthlyCost());
                totalAttributed = totalAttributed.add(monthly);
            } else {
                uncostedCount++;
            }

            if (missing.isEmpty()) {
                compliant++;
                continue;
            }

            if (resource.monthlyCost() != null) {
                BigDecimal monthly = BigDecimal.valueOf(resource.monthlyCost());
                uncoveredAttributed = uncoveredAttributed.add(monthly);
                uncoveredByService.merge(resource.type().typeName(),
                        monthly, BigDecimal::add);
            }
            violationsByService.merge(resource.type().typeName(), 1, Integer::sum);
        }

        BigDecimal uncoveredShare = totalAttributed.signum() == 0
                ? BigDecimal.ZERO
                : uncoveredAttributed.divide(totalAttributed, 4, RoundingMode.HALF_UP);

        return TagCoverageReport.builder()
                .requiredKeys(required)
                .resourcesAssessed(resources.size())
                .compliantResources(compliant)
                .coveragePercentage(percentage(compliant, resources.size()))
                .attributedMonthlyCost(money(totalAttributed))
                .uncoveredMonthlyCost(money(uncoveredAttributed))
                .uncoveredShareOfSpend(share(uncoveredShare))
                .uncostedResourceCount(uncostedCount)
                .violationsByService(violationsByService)
                .uncoveredByService(uncoveredByService.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> money(e.getValue()),
                                (a, b) -> a,
                                java.util.LinkedHashMap::new)))
                .offenders(offenders(resources, required))
                .enforcement("AWS tag policies and service control policies perform "
                        + "enforcement. This report is audit-only.")
                .available(true)
                .build();
    }

    /**
     * Rounds a monetary total to two decimal places.
     *
     * <p>Accumulating doubles and emitting them produces values such as
     * {@code 6.77440000000000012} in a currency field. Every monetary value leaving this
     * service is a currency amount, so it is normalised here.
     */
    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private List<TagOffender> offenders(List<CloudResource> resources, List<String> required) {
        Map<String, TagOffender> byId = new LinkedHashMap<>();
        for (CloudResource resource : resources) {
            Set<String> missing = missingKeys(resource, required);
            if (missing.isEmpty()) {
                continue;
            }
            TagOffender offender = TagOffender.builder()
                    .resourceId(resource.id())
                    .resourceType(resource.type().typeName())
                    .region(resource.region())
                    .monthlyCost(resource.monthlyCost())
                    .missingKeys(List.copyOf(missing))
                    .build();
            byId.put(resource.id(), offender);
        }

        // Largest cost first: that is the order in which an operator should act, and a
        // report sorted by identifier buries the expensive offenders.
        return byId.values().stream()
                .sorted(Comparator.comparing(
                        (TagOffender o) -> o.monthlyCost() == null ? -1.0 : o.monthlyCost(),
                        Comparator.reverseOrder()))
                .limit(50)
                .toList();
    }

    private Set<String> missingKeys(CloudResource resource, List<String> required) {
        Set<String> missing = new LinkedHashSet<>();
        for (String key : required) {
            String value = resource.tag(key);
            if (value == null || value.isBlank()) {
                missing.add(key);
            }
        }
        return missing;
    }

    private static double percentage(int part, int total) {
        return total == 0 ? 0.0 : BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static BigDecimal share(BigDecimal fraction) {
        return fraction.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
}