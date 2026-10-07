package com.optibrain.cloud.port;

import com.optibrain.cloud.model.CloudResource;

import java.util.List;

/**
 * Discovers one category of billable resource.
 *
 * <p>This is the extension point for resource coverage. Adding a resource type means
 * adding one implementation of this interface and registering it as a Spring bean;
 * no interface changes, no edits to the provider adapter, and every new type
 * participates in {@link ResourceQuery} filtering automatically.
 *
 * <p>Implementations must be individually fault-tolerant. A scanner for an unavailable
 * service returns an empty list and logs, so an inventory response omits that section
 * rather than failing entirely.
 */
public interface ResourceScanner {

    /** Short identifier used in logs, for example {@code ec2:instances}. */
    String name();

    /**
     * Discovers resources.
     *
     * @param query filter criteria; scanners narrow their own API calls where the
     *              service supports it and rely on the caller to filter otherwise
     * @return matching resources, or an empty list when unavailable
     */
    List<CloudResource> scan(ResourceQuery query);
}