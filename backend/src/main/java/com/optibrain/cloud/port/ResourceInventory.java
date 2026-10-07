package com.optibrain.cloud.port;

import com.optibrain.cloud.model.CloudResource;

import java.util.List;
import java.util.Optional;

/**
 * Read access to the resource inventory.
 *
 * <p>Every resource type funnels through here, so consumers depend on this narrow
 * interface instead of on EC2, RDS or S3 SDK types.
 */
public interface ResourceInventory {

    /**
     * Discover resources matching the query.
     *
     * <p>Implementations must degrade to an empty list rather than throwing when the
     * underlying API is unavailable, so a single failing service never takes down a
     * whole dashboard.
     */
    List<CloudResource> discover(ResourceQuery query);

    /** Look up a single resource by provider-native id. */
    Optional<CloudResource> find(String resourceId);

    /** Convenience for callers that only care about compute instances. */
    default List<CloudResource> computeInstances() {
        return discover(ResourceQuery.ofTypes(
                com.optibrain.cloud.model.ResourceType.EC2_INSTANCE,
                com.optibrain.cloud.model.ResourceType.GPU_INSTANCE));
    }
}