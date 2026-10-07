package com.optibrain.resource.service;

import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.ResourceQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves the resource API from the live provider inventory.
 *
 * <p>This used to return a fixed list of seven invented resources, including ids like
 * {@code db-instance-1} that no AWS account contains. Because the sandbox runs the same
 * discovery code as production, there is no reason for a hardcoded list to exist: the
 * real inventory is available at the same cost and cannot drift from reality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private final CloudProviderPort cloudProvider;

    @Override
    public List<Map<String, Object>> getAllResources() {
        return toMaps(cloudProvider.discover(ResourceQuery.all()));
    }

    @Override
    public Map<String, Object> getResourceById(String id) {
        return cloudProvider.find(id).map(this::toMap).orElse(null);
    }

    @Override
    public List<Map<String, Object>> getResourcesByStatus(String status) {
        return toMaps(cloudProvider.discover(ResourceQuery.all()).stream()
                .filter(r -> r.state() != null && r.state().equalsIgnoreCase(status))
                .toList());
    }

    private List<Map<String, Object>> toMaps(List<CloudResource> resources) {
        List<Map<String, Object>> rows = new ArrayList<>(resources.size());
        for (CloudResource resource : resources) {
            rows.add(toMap(resource));
        }
        return rows;
    }

    private Map<String, Object> toMap(CloudResource resource) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resource.id());
        row.put("type", resource.type().typeName());
        row.put("name", resource.name());
        row.put("status", resource.state());
        row.put("region", resource.region());
        row.put("monthlyCost", resource.monthlyCost());
        row.put("hourlyCost", resource.hourlyCost());
        row.put("protected", resource.protectedResource());
        row.put("specs", resource.specs());
        row.put("tags", resource.tags());
        return row;
    }
}