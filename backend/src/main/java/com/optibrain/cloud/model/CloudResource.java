package com.optibrain.cloud.model;

import java.time.Instant;
import java.util.Map;

/**
 * A single billable resource in the account, normalised across resource types.
 *
 * <p>This is the shape every downstream analysis works against, which is what allows
 * one policy engine and one registry to serve hundreds of AWS resource types instead
 * of one code path per service.
 *
 * @param id provider-native identifier (for example {@code i-0abc123})
 * @param type canonical {@link ResourceType}
 * @param region AWS region the resource lives in
 * @param state provider-reported lifecycle state, or a synthetic state when derived
 * @param name human-friendly name, falling back to the id
 * @param tags resource tags - the basis for ownership, environment and protection policy
 * @param specs provider-reported specifications (instance type, vCPU, size, ...)
 * @param hourlyCost current blended hourly cost, derived from Cost Explorer when available
 * @param monthlyCost projected monthly cost from {@code hourlyCost}
 * @param createdAt when the resource was created, if known
 * @param protectedResource true when policy forbids destructive actions on this resource
 */
public record CloudResource(
        String id,
        ResourceType type,
        String region,
        String state,
        String name,
        Map<String, String> tags,
        Map<String, String> specs,
        Double hourlyCost,
        Double monthlyCost,
        Instant createdAt,
        boolean protectedResource
) {

    public CloudResource {
        tags = tags == null ? Map.of() : Map.copyOf(tags);
        specs = specs == null
                ? Map.of()
                : specs.entrySet().stream()
                        .filter(e -> e.getKey() != null && e.getValue() != null)
                        // "null" and "" are not values, they are the absence of one. String.valueOf
                        // turns a null SDK field into the literal string "null", so a scanner that
                        // forgets to omit it would otherwise reconstruct a presence where none
                        // exists. The boundary that owns the response shape enforces it.
                        .filter(e -> !e.getValue().isBlank() && !"null".equals(e.getValue()))
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue,
                                (first, ignored) -> first,
                                java.util.LinkedHashMap::new));
    }

    public static CloudResource of(String id, ResourceType type, String region) {
        return new CloudResource(id, type, region, "unknown", id, Map.of(), Map.of(),
                null, null, null, false);
    }

    public String tag(String key) {
        return tags.get(key);
    }

    public String spec(String key) {
        return specs.get(key);
    }

    /** Tag or spec value, or the fallback when absent. */
    public String specOrDefault(String key, String fallback) {
        return specs.getOrDefault(key, fallback);
    }
}