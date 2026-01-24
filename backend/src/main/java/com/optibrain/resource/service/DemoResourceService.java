package com.optibrain.resource.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Demo implementation of ResourceService
 * Returns deterministic resource data aligned with recommendations story
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoResourceService implements ResourceService {

    private final List<Map<String, Object>> resources;

    public DemoResourceService() {
        // Initialize demo resources - aligned with recommendations story
        resources = new ArrayList<>();
        
        // EC2 Instance from recommendations (idle, optimization candidate)
        resources.add(createResource(
            "i-023ab",
            "EC2 Instance i-023ab",
            "EC2",
            "Idle",
            8400,
            "ap-south-1",
            8,
            true
        ));
        
        // RDS from recommendations (over-provisioned storage)
        resources.add(createResource(
            "db-prod",
            "RDS MySQL db-prod",
            "RDS",
            "Running",
            31400,
            "ap-south-1",
            65,
            true
        ));
        
        // S3 bucket (running normally)
        resources.add(createResource(
            "bucket-logs",
            "S3 Bucket logs",
            "S3",
            "Running",
            21200,
            "ap-south-1",
            100,
            false
        ));
        
        // CloudWatch (running normally)
        resources.add(createResource(
            "cw-metrics",
            "CloudWatch Metrics",
            "CloudWatch",
            "Running",
            13450,
            "ap-south-1",
            85,
            false
        ));
        
        // Additional EC2 instances
        resources.add(createResource(
            "i-045cd",
            "EC2 Instance i-045cd",
            "EC2",
            "Running",
            12500,
            "ap-south-1",
            78,
            false
        ));
        
        resources.add(createResource(
            "i-067ef",
            "EC2 Instance i-067ef",
            "EC2",
            "Orphaned",
            4200,
            "us-east-1",
            0,
            true
        ));
    }

    @Override
    public List<Map<String, Object>> getAllResources() {
        log.info("[DEMO] Getting all resources");
        return new ArrayList<>(resources);
    }

    @Override
    public Map<String, Object> getResourceById(String id) {
        log.info("[DEMO] Getting resource by ID: {}", id);
        return resources.stream()
                .filter(r -> id.equals(r.get("id")))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Map<String, Object>> getResourcesByStatus(String status) {
        log.info("[DEMO] Getting resources by status: {}", status);
        return resources.stream()
                .filter(r -> status.equalsIgnoreCase((String) r.get("status")))
                .collect(Collectors.toList());
    }

    private Map<String, Object> createResource(
            String id,
            String name,
            String type,
            String status,
            double monthlyCost,
            String region,
            int utilizationPercent,
            boolean optimizationCandidate) {
        
        Map<String, Object> resource = new HashMap<>();
        resource.put("id", id);
        resource.put("name", name);
        resource.put("type", type);
        resource.put("status", status);
        resource.put("monthlyCost", monthlyCost);
        resource.put("region", region);
        resource.put("utilizationPercent", utilizationPercent);
        resource.put("optimizationCandidate", optimizationCandidate);
        
        return resource;
    }
}
