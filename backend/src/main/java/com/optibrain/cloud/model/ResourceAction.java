package com.optibrain.cloud.model;

import java.util.Map;

/**
 * A requested change to a resource.
 *
 * <p>Every mutation in the system is expressed as one of these and flows through the
 * same executor, so dry-run, policy checks, audit and idempotency are implemented once
 * rather than in each service that happens to touch a cloud API.
 *
 * @param type the operation to perform
 * @param resourceId target resource
 * @param parameters operation-specific arguments (target instance type, etc.)
 * @param dryRun when true the action is validated and reported but never applied
 * @param reason human-readable justification recorded in the audit trail
 * @param idempotencyKey optional key used to make retries safe
 */
public record ResourceAction(
        ActionType type,
        String resourceId,
        Map<String, String> parameters,
        boolean dryRun,
        String reason,
        String idempotencyKey
) {

    public ResourceAction {
        parameters = safe(parameters);
    }

    /**
     * A parameter with a null value carries no information and {@code Map.copyOf}
     * rejects it, so null-valued entries are dropped rather than allowed to turn a
     * malformed body into a 500.
     */
    private static Map<String, String> safe(Map<String, String> source) {
        if (source == null) {
            return Map.of();
        }
        return source.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, Map.Entry::getValue));
    }

    public static ResourceAction of(ActionType type, String resourceId, boolean dryRun, String reason) {
        return new ResourceAction(type, resourceId, Map.of(), dryRun, reason, null);
    }

    public static ResourceAction of(ActionType type, String resourceId, Map<String, String> parameters,
                                    boolean dryRun, String reason) {
        return new ResourceAction(type, resourceId, parameters, dryRun, reason, null);
    }

    public static ResourceAction of(ActionType type, String resourceId, Map<String, String> parameters,
                                    boolean dryRun, String reason, String idempotencyKey) {
        return new ResourceAction(type, resourceId, parameters, dryRun, reason, idempotencyKey);
    }

    public String parameter(String key) {
        return parameters.get(key);
    }
}