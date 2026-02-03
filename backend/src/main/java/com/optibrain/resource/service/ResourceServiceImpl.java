package com.optibrain.resource.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of ResourceService
 */
@Service
public class ResourceServiceImpl implements ResourceService {
    
    private List<Map<String, Object>> resources = new ArrayList<>();
    
    public ResourceServiceImpl() {
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        resources.add(createResource("i-1234567890abcdef0", "EC2", "t3.medium", "Running", "us-east-1", 45.30));
        resources.add(createResource("i-abcdef1234567890", "EC2", "t3.large", "Idle", "us-east-1", 67.50));
        resources.add(createResource("vol-1234567890", "EBS", "gp3 100GB", "Orphaned", "us-east-1", 10.00));
        resources.add(createResource("db-instance-1", "RDS", "db.t3.medium", "Running", "us-west-2", 120.75));
        resources.add(createResource("bucket-prod", "S3", "Standard", "Running", "us-east-1", 35.20));
        resources.add(createResource("i-fedcba0987654321", "EC2", "t3.small", "Idle", "eu-west-1", 28.40));
        resources.add(createResource("vol-0987654321", "EBS", "gp2 50GB", "Orphaned", "us-east-1", 5.00));
    }
    
    private Map<String, Object> createResource(String id, String type, String size, String status, String region, double cost) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("id", id);
        resource.put("type", type);
        resource.put("size", size);
        resource.put("status", status);
        resource.put("region", region);
        resource.put("monthlyCost", cost);
        return resource;
    }
    
    @Override
    public List<Map<String, Object>> getAllResources() {
        return new ArrayList<>(resources);
    }
    
    @Override
    public Map<String, Object> getResourceById(String id) {
        return resources.stream()
                .filter(resource -> id.equals(resource.get("id")))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public List<Map<String, Object>> getResourcesByStatus(String status) {
        return resources.stream()
                .filter(resource -> status.equalsIgnoreCase((String) resource.get("status")))
                .collect(Collectors.toList());
    }
}
