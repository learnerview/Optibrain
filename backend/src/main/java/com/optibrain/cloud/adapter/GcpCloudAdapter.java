package com.optibrain.cloud.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * Google Cloud Platform (GCP) Adapter
 * 
 * NOTE: This is a placeholder implementation.
 * For production GCP support, implement using the Google Cloud Java SDK:
 * - Add dependency: com.google.cloud:google-cloud-compute
 * - Implement authentication via service account credentials
 * - Use ComputeClient for instance management
 * - Integrate with Cloud Billing API for cost data
 * 
 * @see <a href="https://cloud.google.com/compute/docs/reference/rest/v1">GCP Compute API</a>
 */
@Service
@Slf4j
public class GcpCloudAdapter implements CloudAdapter {
    
    @Override
    public String getProviderName() { 
        return "GCP"; 
    }

    @Override
    public List<String> discoverInstances() {
        throw new UnsupportedOperationException(
            "GCP integration not implemented. To add GCP support:\n" +
            "1. Add google-cloud-compute dependency\n" +
            "2. Configure GCP service account credentials\n" +
            "3. Implement ComputeClient integration\n" +
            "4. See: https://cloud.google.com/compute/docs/reference/rest/v1"
        );
    }

    @Override
    public boolean scaleUp(String resourceId) {
        throw new UnsupportedOperationException("GCP scaling not implemented");
    }

    @Override
    public boolean scaleDown(String resourceId) {
        throw new UnsupportedOperationException("GCP scaling not implemented");
    }

    @Override
    public boolean terminateResource(String resourceId) {
        throw new UnsupportedOperationException("GCP resource termination not implemented");
    }
    
    @Override
    public double getCostEstimate(String resourceId) {
        throw new UnsupportedOperationException("GCP cost estimation not implemented");
    }

    @Override
    public String getResourceType(String resourceId) {
        throw new UnsupportedOperationException("GCP resource type detection not implemented");
    }

    @Override
    public Map<String, String> getCurrentSpecs(String resourceId) {
        throw new UnsupportedOperationException("GCP resource spec retrieval not implemented");
    }
}
