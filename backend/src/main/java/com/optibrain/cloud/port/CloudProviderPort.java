package com.optibrain.cloud.port;

import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceAction;

import java.util.List;
import java.util.Map;

/**
 * The application's single port onto a cloud account.
 *
 * <p>Everything above this interface - services, controllers, the ML bridge, the agent
 * tools - is written once and runs unchanged against either execution target. That is
 * the whole point: the demo account and the real account differ only in which adapter
 * is bound, so the sandbox exercises production code rather than a parallel fake.
 *
 * <p>It composes five focused interfaces rather than one wide one, so a consumer can
 * depend on just the capability it needs ({@link CostSource} for a report, say) and
 * still have everything available when it needs the full surface.
 */
public interface CloudProviderPort
        extends ProviderDescriptor, ResourceInventory, TelemetrySource, CostSource, RemediationExecutor {

    // ---------------------------------------------------------------------
    // Compatibility surface.
    //
    // These default methods preserve the call signatures used by the existing
    // services while routing every call through the new port. They are expressed in
    // terms of the primitives above so there is exactly one implementation of each
    // behaviour, and they can be deleted once the legacy call sites are migrated.
    // ---------------------------------------------------------------------

    default String getProviderName() {
        return provider();
    }

    /** Ids of compute instances visible to the account. */
    default List<String> discoverInstances() {
        return computeInstances().stream().map(CloudResource::id).toList();
    }

    /** Ids of every discovered resource, across all registered types. */
    default List<String> discoverAllResourceIds() {
        return discover(ResourceQuery.all()).stream().map(CloudResource::id).toList();
    }

    default boolean scaleUp(String resourceId) {
        return execute(ResourceAction.of(
                com.optibrain.cloud.model.ActionType.START_INSTANCE, resourceId,
                isSandboxed(), "scale up")).success();
    }

    default boolean scaleDown(String resourceId) {
        return execute(ResourceAction.of(
                com.optibrain.cloud.model.ActionType.STOP_INSTANCE, resourceId,
                isSandboxed(), "scale down")).success();
    }

    default boolean terminateResource(String resourceId) {
        return execute(ResourceAction.of(
                com.optibrain.cloud.model.ActionType.TERMINATE_INSTANCE, resourceId,
                isSandboxed(), "terminate")).success();
    }

    /** Monthly cost estimate for a resource; falls back to zero when unattributable. */
    default double getCostEstimate(String resourceId) {
        Double monthly = monthlyCostOf(resourceId);
        return monthly == null ? 0.0 : monthly;
    }

    /**
     * Fraction of recent spend already absorbed by a Savings Plan or Reserved Instance,
     * measured from Cost Explorer, or {@code null} when unmeasured.
     *
     * <p>This is what makes a recommendation's on-demand-rate savings honest: a workload
     * that is already committed realizes less of an on-demand-priced recommendation.
     */
    default Double commitmentCoverage() {
        return null;
    }

    default String getResourceType(String resourceId) {
        return find(resourceId).map(r -> r.spec("instanceType"))
                .or(() -> find(resourceId).map(r -> r.type().typeName()))
                .orElse("unknown");
    }

    default Map<String, String> getCurrentSpecs(String resourceId) {
        return find(resourceId).map(CloudResource::specs).orElse(Map.of());
    }
}