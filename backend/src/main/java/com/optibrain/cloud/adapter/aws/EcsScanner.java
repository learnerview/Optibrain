package com.optibrain.cloud.adapter.aws;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.policy.ProtectionPolicy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ecs.model.Cluster;
import software.amazon.awssdk.services.ecs.model.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ECS services, including Fargate launch type.
 *
 * <p>LocalStack's community distribution does not implement ECS, so in a sandbox the
 * first call fails and {@link AwsResourceScanner} omits the section rather than failing
 * the request. Against a real account the same guard omits services that cannot be
 * described.
 *
 * <p>Cost is not attributed. Fargate pricing is task-geometry dependent, so attributing
 * a service's monthly cost from its desired count would present a fabricated figure as
 * a measured one. The service's specs carry the launch type and counts so a reader can
 * judge cost drivers from the inventory.
 */
@Component
public class EcsScanner extends AwsResourceScanner {

    public EcsScanner(AwsClientFactory clients) {
        super(clients, "ecs:services");
    }

    @Override
    protected List<CloudResource> load() {
        return safe(() -> {
            Map<String, CloudResource> byArn = new LinkedHashMap<>();
            try (var ecs = clients.ecs()) {
                for (String clusterArn : ecs.listClusters(b -> b.build()).clusterArns()) {
                    Cluster cluster = null;
                    try {
                        cluster = ecs.describeClusters(b -> b.clusters(clusterArn)).clusters()
                                .stream().findFirst().orElse(null);
                    } catch (Exception e) {
                        log.debug("ECS cluster not describable: {}", e.getMessage());
                    }
                    String clusterName = (cluster != null && cluster.clusterName() != null)
                            ? cluster.clusterName() : clusterArn;
                    for (String serviceArn : ecs.listServices(b -> b.cluster(clusterArn)).serviceArns()) {
                        try {
                            var resp = ecs.describeServices(b -> b.cluster(clusterArn).services(serviceArn));
                            for (Service service : resp.services()) {
                                byArn.put(service.serviceArn(), toResource(clusterName, service));
                            }
                        } catch (Exception e) {
                            log.debug("ECS service not describable: {}", e.getMessage());
                        }
                    }
                }
            }
            return new ArrayList<>(byArn.values());
        });
    }

    private CloudResource toResource(String clusterName, Service service) {
        Map<String, String> tags = new HashMap<>();
        if (service.tags() != null) {
            for (var t : service.tags()) {
                if (t.key() != null) {
                    tags.put(t.key(), t.value() == null ? "" : t.value());
                }
            }
        }
        Map<String, String> specs = new LinkedHashMap<>();
        spec(specs, "cluster", clusterName);
        spec(specs, "launchType", service.launchTypeAsString());
        spec(specs, "desiredCount", service.desiredCount());
        spec(specs, "runningCount", service.runningCount());
        spec(specs, "pendingCount", service.pendingCount());
        spec(specs, "status", service.status());
        spec(specs, "taskDefinition", service.taskDefinition());

        return new CloudResource(
                service.serviceArn(),
                ResourceType.ECS_SERVICE,
                region(),
                service.status() == null ? "unknown" : service.status(),
                service.serviceName() == null ? service.serviceArn() : service.serviceName(),
                tags,
                specs,
                null,
                null,
                null,
                ProtectionPolicy.isProtected(tags));
    }
}