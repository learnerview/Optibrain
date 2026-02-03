package com.optibrain.cloud.service;

import com.optibrain.cloud.config.CloudConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for managing AWS credentials and client connections.
 * 
 * Credential Resolution Order (Production):
 * 1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 * 2. AWS credentials file (~/.aws/credentials)
 * 3. IAM role for EC2/ECS/Lambda (RECOMMENDED for production)
 * 4. IAM role for service accounts (EKS)
 * 
 * LocalStack Mode:
 * - Uses test credentials for local development
 * - Overrides endpoint to point to LocalStack
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialService {

    private final CloudConfig cloudConfig;
    
    @Value("${aws.access-key:}")
    private String accessKey;
    
    @Value("${aws.secret-key:}")
    private String secretKey;

    private final AtomicReference<Ec2Client> activeClient = new AtomicReference<>(null);
    private final AtomicReference<String> activeRegion = new AtomicReference<>("us-east-1");
    private boolean isConnected = false;

    /**
     * Connect using provided credentials (for multi-tenant scenarios).
     * Note: In production, prefer IAM roles over explicit credentials.
     */
    public void connect(String accessKey, String secretKey, String region) {
        try {
            Ec2Client client = Ec2Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(createCredentialsProvider(accessKey, secretKey))
                    .build();
            
            activeClient.set(client);
            activeRegion.set(region);
            isConnected = true;
            log.info("Connected to AWS region: {}", region);
        } catch (Exception e) {
            isConnected = false;
            log.error("Failed to connect to AWS: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to AWS: " + e.getMessage());
        }
    }
    
    /**
     * Get a credentials provider based on configuration.
     * - LocalStack: Use test credentials
     * - Production: Use DefaultCredentialsProvider (IAM roles, env vars, etc.)
     * - Explicit credentials: Use provided access key/secret key
     */
    public AwsCredentialsProvider createCredentialsProvider(String explicitAccessKey, String explicitSecretKey) {
        // LocalStack mode - use test credentials
        if ("LOCALSTACK".equalsIgnoreCase(cloudConfig.getMode())) {
            log.debug("Using LocalStack test credentials");
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test")
            );
        }
        
        // If explicit credentials provided (multi-tenant scenario)
        if (explicitAccessKey != null && !explicitAccessKey.isEmpty() 
            && explicitSecretKey != null && !explicitSecretKey.isEmpty()) {
            log.debug("Using explicit credentials");
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(explicitAccessKey, explicitSecretKey)
            );
        }
        
        // Production: Use default credentials chain (IAM roles, env vars, etc.)
        log.debug("Using default AWS credentials chain");
        return DefaultCredentialsProvider.create();
    }

    public void disconnect() {
        if (activeClient.get() != null) {
            try {
                activeClient.get().close();
            } catch (Exception e) {
                log.warn("Error closing EC2 client: {}", e.getMessage());
            }
        }
        activeClient.set(null);
        isConnected = false;
        log.info("Disconnected from AWS");
    }

    public boolean isConnected() {
        return isConnected;
    }

    public Ec2Client getClient() {
        return activeClient.get();
    }
    
    public String getRegion() {
        return activeRegion.get();
    }
}
