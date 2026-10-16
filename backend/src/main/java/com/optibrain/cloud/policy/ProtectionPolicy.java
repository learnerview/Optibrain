package com.optibrain.cloud.policy;

import java.util.Collection;
import java.util.Map;

/**
 * Resource protection policy.
 *
 * <p>Lives outside {@code adapter.aws} because protection is a product rule, not an AWS
 * detail. A business service that needs to explain a blocked action must not have to
 * import an adapter to learn the tag name, and the rule must stay legible to anyone
 * auditing why a change was refused.
 *
 * <p>Applied in {@code AwsCloudProviderAdapter#execute}, the only site that mutates AWS,
 * so no caller can reach a mutation without passing through this check.
 */
public final class ProtectionPolicy {

    /** Tag marking a resource as excluded from destructive actions. */
    public static final String TAG = "optibrain:protected";

    /** Tag value that constitutes protection. */
    public static final String VALUE = "true";

    private ProtectionPolicy() {
    }

    /** True when the tag set marks this resource as protected. */
    public static boolean isProtected(Map<String, String> tags) {
        if (tags == null) {
            return false;
        }
        return VALUE.equalsIgnoreCase(tags.get(TAG));
    }

    /**
     * True when a tenant has listed this resource id as protected.
     *
     * <p>Tenant entries are resource ids, not tag keys: tag-key protection is what the
     * {@link #TAG} tag already provides, and overloading the tenant list with a second
     * meaning would make it ambiguous which one a given entry is.
     */
    public static boolean isProtectedById(Collection<String> protectedResources,
                                          String resourceId) {
        return protectedResources != null
                && resourceId != null
                && protectedResources.contains(resourceId);
    }
}
