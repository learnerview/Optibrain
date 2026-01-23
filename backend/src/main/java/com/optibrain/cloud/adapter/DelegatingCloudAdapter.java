package com.optibrain.cloud.adapter;
import com.optibrain.cloud.service.CredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class DelegatingCloudAdapter implements CloudAdapter {

    private final CredentialService credentialService;
    private final MockCloudAdapter mockAdapter;
    private final AwsCloudAdapter awsAdapter;
    private final AzureCloudAdapter azureAdapter;
    private final GcpCloudAdapter gcpAdapter;

    private CloudAdapter getActiveAdapter() {
        if (!credentialService.isConnected()) {
            return mockAdapter;
        }
        
        String provider = com.optibrain.common.context.TenantContext.getProvider();
        if ("azure".equalsIgnoreCase(provider)) return azureAdapter;
        if ("gcp".equalsIgnoreCase(provider)) return gcpAdapter;
        
        return awsAdapter;
    }

    @Override
    public boolean scaleUp(String resourceId) {
        return getActiveAdapter().scaleUp(resourceId);
    }

    @Override
    public boolean scaleDown(String resourceId) {
        return getActiveAdapter().scaleDown(resourceId);
    }

    @Override
    public java.util.List<String> discoverInstances() {
        return getActiveAdapter().discoverInstances();
    }

    @Override
    public boolean terminateResource(String resourceId) {
        return getActiveAdapter().terminateResource(resourceId);
    }

    @Override
    public String getProviderName() {
        return getActiveAdapter().getProviderName();
    }

    @Override
    public double getCostEstimate(String resourceId) {
        return getActiveAdapter().getCostEstimate(resourceId);
    }

    @Override
    public String getResourceType(String resourceId) {
        return getActiveAdapter().getResourceType(resourceId);
    }

    @Override
    public java.util.Map<String, String> getCurrentSpecs(String resourceId) {
        return getActiveAdapter().getCurrentSpecs(resourceId);
    }
}
