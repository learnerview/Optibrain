package com.optibrain.tenant.service;

import com.optibrain.tenant.model.CloudCredentials;
import com.optibrain.tenant.model.Tenant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.util.Collections;
import java.util.List;

/**
 * Service for managing tenant credentials and information.
 * 
 * NOTE: This is a simplified implementation for development.
 * For production, implement proper database-backed tenant management with:
 * - Multi-tenant database architecture
 * - Encrypted credential storage (AWS Secrets Manager, HashiCorp Vault)
 * - Proper tenant onboarding workflow
 * - Role-based access control per tenant
 */
@Service
public class TenantCredentialService {

    @Value("${cloud.mode:LOCALSTACK}")
    private String cloudMode;
    
    @Value("${cloud.aws.region:us-east-1}")
    private String awsRegion;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Get all tenants.
     * TODO: Implement database-backed tenant repository
     */
    public List<Tenant> getAllTenants() {
        // For development, return a default tenant
        if ("dev".equals(activeProfile)) {
            return Collections.singletonList(createDefaultTenant());
        }
        
        // In production, this should query a database
        throw new UnsupportedOperationException(
            "Multi-tenant support requires database implementation. " +
            "Implement TenantRepository and replace this method."
        );
    }
    
    /**
     * Get a specific tenant by ID.
     * TODO: Implement database-backed tenant retrieval
     */
    public Tenant getTenant(String tenantId) {
        // For development, return a default tenant
        if ("dev".equals(activeProfile)) {
            return createDefaultTenant();
        }
        
        // In production, this should query a database
        throw new UnsupportedOperationException(
            "Multi-tenant support requires database implementation. " +
            "Implement TenantRepository and replace this method."
        );
    }

    /**
     * Get cloud credentials for a tenant.
     * 
     * SECURITY WARNING: This method should NOT return credentials directly.
     * Instead, use AWS SDK's DefaultCredentialsProvider which supports:
     * - Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
     * - AWS credentials file (~/.aws/credentials)
     * - IAM roles for EC2/ECS/Lambda (RECOMMENDED for production)
     * - IAM roles for service accounts (EKS)
     * 
     * For multi-tenant scenarios:
     * - Store encrypted credentials in AWS Secrets Manager or HashiCorp Vault
     * - Use AWS STS to assume different roles per tenant
     * - Never return credentials in API responses
     */
    public CloudCredentials getCredentials(String tenantId) {
        // For LocalStack development, return test credentials
        if ("LOCALSTACK".equals(cloudMode)) {
            return CloudCredentials.builder()
                .accessKeyId("test")
                .secretAccessKey("test")
                .region(awsRegion)
                .build();
        }
        
        // For production AWS, credentials should come from environment or IAM roles
        // This method should not store or return actual credentials
        return CloudCredentials.builder()
            .accessKeyId(null)  // Use DefaultCredentialsProvider instead
            .secretAccessKey(null)  // Use DefaultCredentialsProvider instead
            .region(awsRegion)
            .build();
    }

    public void updateTenantActivity(String tenantId) {
        // TODO: Implement tenant activity tracking in database
        // This could track last access time, API usage metrics, etc.
    }
    
    private Tenant createDefaultTenant() {
        return Tenant.builder()
            .id("default-tenant")
            .name("Default Tenant")
            .active(true)
            .cloudProvider("AWS")
            .mode("MANUAL")  // Changed from AUTONOMOUS for safety
            .plan("BASIC")
            .settings(Tenant.TenantSettings.builder()
                .autonomousMode(false)  // Disabled by default for safety
                .requireApprovalForChanges(true)  // Enabled for safety
                .monthlyBudgetLimit(1000.0)
                .protectedResources(Collections.emptyList())
                .build())
            .build();
    }
}
