package com.optibrain.cloud.port;

import com.optibrain.cloud.model.ResourceType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Criteria for an inventory lookup.
 *
 * <p>An empty criteria object means "everything", so callers can always express intent
 * incrementally rather than building query maps by hand.
 *
 * @param types resource types to include; empty means all supported types
 * @param region single region, or empty for all regions
 * @param states lifecycle states to include, for example {@code running}
 * @param tagFilters resources must carry every one of these tags
 * @param limit maximum number of resources to return
 */
public record ResourceQuery(
        Set<ResourceType> types,
        String region,
        Set<String> states,
        Map<String, String> tagFilters,
        int limit
) {

    public ResourceQuery {
        types = types == null ? Set.of() : Set.copyOf(types);
        states = states == null ? Set.of() : Set.copyOf(states);
        tagFilters = tagFilters == null ? Map.of() : Map.copyOf(tagFilters);
    }

    public static ResourceQuery all() {
        return new ResourceQuery(Set.of(), null, Set.of(), Map.of(), 0);
    }

    public static ResourceQuery ofTypes(ResourceType... types) {
        return new ResourceQuery(new LinkedHashSet<>(java.util.List.of(types)), null,
                Set.of(), Map.of(), 0);
    }

    public ResourceQuery inRegion(String r) {
        return new ResourceQuery(types, r, states, tagFilters, limit);
    }

    public ResourceQuery withState(String... s) {
        return new ResourceQuery(types, region, new LinkedHashSet<>(java.util.List.of(s)),
                tagFilters, limit);
    }

    public ResourceQuery tagged(String key, String value) {
        Map<String, String> merged = new LinkedHashMap<>(tagFilters);
        merged.put(key, value);
        return new ResourceQuery(types, region, states, merged, limit);
    }

    public ResourceQuery withLimit(int l) {
        return new ResourceQuery(types, region, states, tagFilters, l);
    }

    public boolean matchesType(ResourceType type) {
        return types.isEmpty() || types.contains(type);
    }

    public boolean matchesState(String state) {
        return states.isEmpty() || states.stream().anyMatch(s -> s.equalsIgnoreCase(state));
    }

    public boolean matchesRegion(String r) {
        return region == null || region.isBlank() || region.equalsIgnoreCase(r);
    }

    public boolean matchesTags(Map<String, String> resourceTags) {
        return tagFilters.entrySet().stream()
                .allMatch(e -> e.getValue().equalsIgnoreCase(resourceTags.getOrDefault(e.getKey(), "")));
    }

}