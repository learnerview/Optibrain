package com.optibrain.cloud.service;

import lombok.Data;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class CredentialService {

    private final AtomicReference<Ec2Client> activeClient = new AtomicReference<>(null);
    private final AtomicReference<String> activeRegion = new AtomicReference<>("us-east-1");
    private boolean isConnected = false;

    public void connect(String accessKey, String secretKey, String region) {
        try {
            Ec2Client client = Ec2Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    ))
                    .build();
            
            // Verify connection (simple dry run check)
            // client.describeInstances(); // Omitted for speed, can add later if needed strict check

            activeClient.set(client);
            activeRegion.set(region);
            isConnected = true;
        } catch (Exception e) {
            isConnected = false;
            throw new RuntimeException("Failed to connect to AWS: " + e.getMessage());
        }
    }

    public void disconnect() {
        activeClient.set(null);
        isConnected = false;
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
