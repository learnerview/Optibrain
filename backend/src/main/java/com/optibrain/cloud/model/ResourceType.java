package com.optibrain.cloud.model;

/**
 * Canonical resource types OptiBrain understands.
 *
 * <p>This enum is the backbone of the resource registry: a handler is registered per
 * type, and discovery, assessment and remediation are all driven from this single
 * vocabulary rather than from provider-specific strings scattered through services.
 *
 * <p>Adding support for the long tail of AWS services means adding entries here plus a
 * handler - not touching the services that consume the port.
 */
public enum ResourceType {

    // Compute
    EC2_INSTANCE("EC2", "Instance"),
    EC2_SPOT_INSTANCE("EC2", "SpotInstance"),
    ASG("AutoScaling", "AutoScalingGroup"),
    EKS_NODE_GROUP("EKS", "NodeGroup"),
    EKS_CLUSTER("EKS", "Cluster"),
    ECS_SERVICE("ECS", "Service"),
    LAMBDA_FUNCTION("Lambda", "Function"),

    // Accelerator / AI
    GPU_INSTANCE("EC2", "AcceleratedInstance"),

    // Storage
    EBS_VOLUME("EBS", "Volume"),
    EBS_SNAPSHOT("EBS", "Snapshot"),
    S3_BUCKET("S3", "Bucket"),
    EFS_FILESYSTEM("EFS", "FileSystem"),

    // Network
    NAT_GATEWAY("VPC", "NatGateway"),
    ELASTIC_IP("EC2", "ElasticIp"),
    LOAD_BALANCER("ElasticLoadBalancing", "LoadBalancer"),
    CLOUDFRONT_DISTRIBUTION("CloudFront", "Distribution"),

    // Database / cache
    RDS_INSTANCE("RDS", "DBInstance"),
    RDS_CLUSTER("RDS", "DBCluster"),
    DYNAMODB_TABLE("DynamoDB", "Table"),
    ELASTICACHE_CLUSTER("ElastiCache", "CacheCluster"),
    OPENSEARCH_DOMAIN("OpenSearch", "Domain"),

    // Analytics / streaming
    MSK_CLUSTER("Kafka", "Cluster"),
    REDSHIFT_CLUSTER("Redshift", "Cluster"),
    KINESIS_STREAM("Kinesis", "Stream"),
GLUE_JOB("Glue", "Job"),
ATHENA_WORKGROUP("Athena", "WorkGroup"),
NETWORK_INTERFACE("EC2", "NetworkInterface"),
SECURITY_GROUP("EC2", "SecurityGroup"),

    // Messaging
    SQS_QUEUE("SQS", "Queue"),
    SNS_TOPIC("SNS", "Topic"),

    // Containers / registry
    ECR_REPOSITORY("ECR", "Repository"),

    // Cost instruments
    SAVINGS_PLAN("SavingsPlans", "SavingsPlan"),
    RESERVED_INSTANCE("EC2", "ReservedInstance"),
    SPOT_FLEET("EC2", "SpotFleetRequest");

    private final String service;
    private final String typeName;

    ResourceType(String service, String typeName) {
        this.service = service;
        this.typeName = typeName;
    }

    /** The AWS billing service this resource is charged to. */
    public String service() {
        return service;
    }

    public String typeName() {
        return typeName;
    }

    

    /** Accelerator-backed instance types dominate 2026 AI spend and need distinct advice. */
    public boolean isAccelerator() {
        return this == GPU_INSTANCE;
    }
}