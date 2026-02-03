package com.optibrain.cloud.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * Microsoft Azure Cloud Adapter
 * 
 * NOTE: This is a placeholder implementation.
 * For production Azure support, implement using the Azure SDK for Java:
 * - Add dependency: com.azure.resourcemanager:azure-resourcemanager
 * - Implement authentication via Azure Active Directory (AAD)
 * - Use ComputeManager for VM management
 * - Integrate with Azure Cost Management API for cost data
 * 
 * @see <a href="https://docs.microsoft.com/azure/developer/java/">Azure SDK for Java</a>
 */
@Service
@Slf4j
public class AzureCloudAdapter implements CloudAdapter {
    
    @Override
    public String getProviderName() { 
        return "AZURE"; 
    }

    @Override
    public List<String> discoverInstances() {
        throw new UnsupportedOperationException(
            "Azure integration not implemented. To add Azure support:\n" +
            "1. Add azure-resourcemanager dependency\n" +
            "2. Configure Azure Active Directory credentials\n" +
            "3. Implement ComputeManager integration\n" +
            "4. See: https://docs.microsoft.com/azure/developer/java/"
        );
    }

    @Override
    public boolean scaleUp(String resourceId) {
        throw new UnsupportedOperationException("Azure scaling not implemented");
    }

    @Override
    public boolean scaleDown(String resourceId) {
        throw new UnsupportedOperationException("Azure scaling not implemented");
    }

    @Override
    public boolean terminateResource(String resourceId) {
        throw new UnsupportedOperationException("Azure resource termination not implemented");
    }
    
    @Override
    public double getCostEstimate(String resourceId) {
        throw new UnsupportedOperationException("Azure cost estimation not implemented");
    }

    @Override
    public String getResourceType(String resourceId) {
        throw new UnsupportedOperationException("Azure resource type detection not implemented");
    }

    @Override
    public Map<String, String> getCurrentSpecs(String resourceId) {
        throw new UnsupportedOperationException("Azure resource spec retrieval not implemented");
    }
}
