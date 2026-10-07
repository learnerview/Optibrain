package com.optibrain.cloud.governance;

import lombok.Builder;

import java.util.List;

/**
 * A resource missing one or more required tag keys.
 *
 * @param resourceId provider-native identifier
 * @param resourceType canonical resource type name
 * @param region region the resource occupies
 * @param monthlyCost attributed monthly cost, or null where attribution fails
 * @param missingKeys required keys absent or blank on this resource
 */
@Builder
public record TagOffender(
        String resourceId,
        String resourceType,
        String region,
        Double monthlyCost,
        List<String> missingKeys
) {

    public TagOffender {
        missingKeys = missingKeys == null ? List.of() : List.copyOf(missingKeys);
        // Two decimal places: this is a currency amount, and a raw double renders as
        // 5.840000000000001 in an API response.
        monthlyCost = monthlyCost == null ? null
                : java.math.BigDecimal.valueOf(monthlyCost)
                        .setScale(2, java.math.RoundingMode.HALF_UP)
                        .doubleValue();
    }
}