package com.optibrain.cloud.adapter.aws;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.policy.ProtectionPolicy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.Snapshot;
import software.amazon.awssdk.services.ec2.model.Volume;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EBS volumes and snapshots.
 *
 * <p>Snapshots backing an AMI are excluded. AWS removes them implicitly with the AMI, so
 * they are not standalone waste, and recommending their deletion is both wrong and
 * dangerous. Some EC2 implementations also ignore the owner filter, so ownership and
 * purpose are both checked here rather than trusted to the API.
 */
@Component
public class EbsScanner extends AwsResourceScanner {

    public EbsScanner(AwsClientFactory clients) {
        super(clients, "ec2:ebs");
    }

    @Override
    protected List<CloudResource> load() {
        return merge(scanVolumes(), scanSnapshots());
    }

    private List<CloudResource> scanVolumes() {
        return safe(() -> clients.ec2()
                .describeVolumesPaginator(DescribeVolumesRequest.builder().build())
                .stream()
                .flatMap(page -> page.volumes().stream())
                .map(this::toResource)
                .toList());
    }

    private List<CloudResource> scanSnapshots() {
        return safe(() -> clients.ec2()
                .describeSnapshotsPaginator(DescribeSnapshotsRequest.builder()
                        .ownerIds("self")
                        .build())
                .stream()
                .flatMap(page -> page.snapshots().stream())
                .filter(snapshot -> !isAmiBacking(snapshot))
                .map(this::toResource)
                .toList());
    }

    private CloudResource toResource(Volume volume) {
        Map<String, String> tags = tagsOf(volume.tags());
        Map<String, String> specs = new LinkedHashMap<>();

        spec(specs, "sizeGiB", volume.size());
        spec(specs, "volumeType", volume.volumeTypeAsString());
        // Iops and throughput are only meaningful on volume types that support them, so
        // the keys are omitted rather than filled with an empty string or the word "null".
        spec(specs, "iops", volume.iops());
        spec(specs, "throughputMiBps", volume.throughput());
        spec(specs, "encrypted", volume.encrypted());
        spec(specs, "availabilityZone", volume.availabilityZone());

        int attachments = volume.attachments() == null ? 0 : volume.attachments().size();
        spec(specs, "attachmentCount", attachments);
        if (attachments > 0) {
            spec(specs, "attachedTo", volume.attachments().get(0).instanceId());
        }

        double hourly = volume.size() * AwsPriceList.volumePerGibMonth(volume.volumeTypeAsString())
                / AwsPriceList.HOURS_PER_MONTH;

        return new CloudResource(
                volume.volumeId(),
                ResourceType.EBS_VOLUME,
                region(),
                volume.state() == null ? "unknown" : volume.stateAsString(),
                tags.getOrDefault("Name", volume.volumeId()),
                tags,
                specs,
                hourly,
                hourly * AwsPriceList.HOURS_PER_MONTH,
                volume.createTime(),
                ProtectionPolicy.isProtected(tags));
    }

    private CloudResource toResource(Snapshot snapshot) {
        Map<String, String> tags = tagsOf(snapshot.tags());
        Map<String, String> specs = new LinkedHashMap<>();

        spec(specs, "sizeGiB", snapshot.volumeSize());
        spec(specs, "volumeId", snapshot.volumeId());
        spec(specs, "description", snapshot.description());
        spec(specs, "ownerId", snapshot.ownerId());

        // Snapshot storage is billed at the same rate as magnetic ("standard")
        // block storage in us-east-1, not at the volume's own rate; pricing a
        // snapshot as a live gp3 volume overstates it.
        double monthly = snapshot.volumeSize() == null ? 0.0
                : snapshot.volumeSize() * AwsPriceList.volumePerGibMonth("standard");

        return new CloudResource(
                snapshot.snapshotId(),
                ResourceType.EBS_SNAPSHOT,
                region(),
                snapshot.state() == null ? "unknown" : snapshot.stateAsString(),
                tags.getOrDefault("Name", snapshot.snapshotId()),
                tags,
                specs,
                monthly / AwsPriceList.HOURS_PER_MONTH,
                monthly,
                snapshot.startTime(),
                ProtectionPolicy.isProtected(tags));
    }

    /**
     * Whether a snapshot exists to back an AMI rather than to hold data.
     *
     * <p>AWS names these exactly "Auto-created snapshot for AMI ami-...". The empty
     * volume id is the same condition seen from the other side.
     */
    private boolean isAmiBacking(Snapshot snapshot) {
        String description = snapshot.description();
        if (description != null && description.contains("Auto-created snapshot for AMI")) {
            return true;
        }
        return snapshot.volumeId() == null || snapshot.volumeId().isBlank();
    }

    private static List<CloudResource> merge(List<CloudResource> a, List<CloudResource> b) {
        java.util.List<CloudResource> all = new java.util.ArrayList<>(a);
        all.addAll(b);
        return all;
    }
}