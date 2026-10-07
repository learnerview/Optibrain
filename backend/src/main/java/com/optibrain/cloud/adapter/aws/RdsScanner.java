package com.optibrain.cloud.adapter.aws;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.policy.ProtectionPolicy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rds.model.DBCluster;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RDS instances and Aurora clusters.
 *
 * <p>Clusters appear alongside instances because an Aurora cluster bills separately from
 * its member instances. Reporting only instances hides a large recurring charge.
 */
@Component
public class RdsScanner extends AwsResourceScanner {

    public RdsScanner(AwsClientFactory clients) {
        super(clients, "rds:database");
    }

    @Override
    protected List<CloudResource> load() {
        List<CloudResource> all = new ArrayList<>();
        all.addAll(instances());
        all.addAll(clusters());
        return all;
    }

    private List<CloudResource> instances() {
        return safe(() -> clients.rds()
                .describeDBInstances(DescribeDbInstancesRequest.builder().build())
                .dbInstances().stream()
                .map(this::toResource)
                .toList());
    }

    private List<CloudResource> clusters() {
        return safe(() -> clients.rds()
                .describeDBClusters(DescribeDbClustersRequest.builder().build())
                .dbClusters().stream()
                .map(this::toResource)
                .toList());
    }

    private CloudResource toResource(DBInstance db) {
        Map<String, String> tags = rdsTags(db.tagList());
        Map<String, String> specs = new LinkedHashMap<>();

        specs.put("engine", String.valueOf(db.engine()));
        specs.put("engineVersion", String.valueOf(db.engineVersion()));
        specs.put("instanceClass", db.dbInstanceClass() == null ? "" : db.dbInstanceClass());
        specs.put("allocatedStorageGiB",
                db.allocatedStorage() == null ? "" : String.valueOf(db.allocatedStorage()));
        specs.put("multiAz", String.valueOf(db.multiAZ()));
        specs.put("publiclyAccessible", String.valueOf(db.publiclyAccessible()));
        specs.put("storageEncrypted", String.valueOf(db.storageEncrypted()));
        specs.put("deletionProtection", String.valueOf(db.deletionProtection()));
        specs.put("backupRetentionDays", db.backupRetentionPeriod() == null
                ? "" : String.valueOf(db.backupRetentionPeriod()));
        specs.put("availabilityZone",
                db.availabilityZone() == null ? "" : db.availabilityZone());
        specs.put("endpoint", db.endpoint() == null ? "" : db.endpoint().address());

        return new CloudResource(
                db.dbInstanceIdentifier(), ResourceType.RDS_INSTANCE, region(),
                db.dbInstanceStatus() == null ? "unknown" : db.dbInstanceStatus(),
                tags.getOrDefault("Name", db.dbInstanceIdentifier()), tags, specs,
                null, null, db.instanceCreateTime(), ProtectionPolicy.isProtected(tags));
    }

    private CloudResource toResource(DBCluster db) {
        Map<String, String> tags = rdsTags(db.tagList());
        Map<String, String> specs = new LinkedHashMap<>();

        specs.put("engine", String.valueOf(db.engine()));
        specs.put("engineMode", String.valueOf(db.engineMode()));
        specs.put("engineVersion", String.valueOf(db.engineVersion()));
        // dbClusterClass() is absent from this SDK version; the engine and engine mode
        // identify a cluster adequately for cost purposes.
        specs.put("memberCount", String.valueOf(
                db.dbClusterMembers() == null ? 0 : db.dbClusterMembers().size()));
        specs.put("storageEncrypted", String.valueOf(db.storageEncrypted()));
        specs.put("deletionProtection", String.valueOf(db.deletionProtection()));
        specs.put("endpoint", db.endpoint() == null ? "" : db.endpoint());

        return new CloudResource(
                db.dbClusterIdentifier(), ResourceType.RDS_CLUSTER, region(),
                db.status() == null ? "unknown" : db.status(),
                tags.getOrDefault("Name", db.dbClusterIdentifier()), tags, specs,
                null, null, db.clusterCreateTime(), ProtectionPolicy.isProtected(tags));
    }

    /**
     * Flattens an RDS tag list.
     *
     * <p>RDS defines its own {@code Tag} type, so the shared EC2 helper cannot be
     * reused. The conversion is repeated rather than abstracted over two unrelated SDK
     * types.
     */
    private static Map<String, String> rdsTags(List<Tag> tags) {
        Map<String, String> result = new LinkedHashMap<>();
        if (tags != null) {
            tags.forEach(t -> {
                if (t.key() != null) {
                    result.put(t.key(), t.value() == null ? "" : t.value());
                }
            });
        }
        return result;
    }
}