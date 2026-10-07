package com.optibrain.copilot.service;

import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.CostReport;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.CostQuery;
import com.optibrain.cloud.port.ResourceQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Answers cost questions from the account's real data.
 *
 * <p>This previously answered with invented figures - "3 instances with <5% CPU",
 * "total monthly spend: 128,450", "potential monthly savings: 13,000" - presented in the
 * confident register of an analysis tool. A natural-language surface is the most
 * persuasive place in a product to fabricate, because the answer reads as retrieved
 * rather than generated.
 *
 * <p>The behaviour now is: measure first, answer from the measurement, and say plainly
 * when the data needed is not available. An honest "I could not find that" is recoverable;
 * a confident wrong number is not.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CopilotService {

    /** Above this, CPU alone is not enough evidence to call a resource idle. */
    private static final double IDLE_CPU_THRESHOLD = 5.0;

    /** Window over which average utilisation is judged. */
    private static final Duration IDLE_WINDOW = Duration.ofDays(7);

    private final CloudProviderPort cloudProvider;

    public Map<String, Object> generateResponse(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);

        try {
            if (q.contains("cost") || q.contains("spend") || q.contains("driver")
                    || q.contains("biggest") || q.contains("breakdown")) {
                return costDrivers();
            }
            if (q.contains("idle") || q.contains("underutil") || q.contains("unused")) {
                return underutilised();
            }
            if (q.contains("save") || q.contains("reduc") || q.contains("optimis")
                    || q.contains("optimiz") || q.contains("recommend")) {
                return savingsOpportunities();
            }
            if (q.contains("resource") || q.contains("inventory") || q.contains("instance")) {
                return inventorySummary();
            }
        } catch (Exception e) {
            log.warn("Copilot query failed: {}", e.getMessage());
            return unavailable("I could not complete that analysis: " + e.getMessage());
        }

        return Map.of(
                "type", "text",
                "text", """
                        I can answer from your AWS account. Try asking about:
                        - cost drivers / spend breakdown
                        - idle or underutilised resources
                        - savings opportunities
                        - resource inventory

                        I answer only from measured data. If a figure is not available I \
                        will say so rather than estimate it."""
        );
    }

    private Map<String, Object> costDrivers() {
        CostReport report = cloudProvider.costReport(CostQuery.lastDays(30));
        if (report.total().signum() == 0 && report.byService().isEmpty()) {
            return unavailable("Cost Explorer returned no spend for the last 30 days. "
                    + "Cost Explorer is unavailable in the LocalStack sandbox, so this "
                    + "figure will stay empty until you run against a real account.");
        }

        List<Map<String, Object>> items = new ArrayList<>();
        report.byService().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", e.getKey());
                    row.put("cost", e.getValue().doubleValue());
                    row.put("percentage", percentage(e.getValue(), report.total()));
                    items.add(row);
                });

        return Map.of(
                "type", "chart",
                "text", "Spend over the last 30 days, by service. Total: "
                        + report.currency() + " " + report.total().setScale(2, RoundingMode.HALF_UP),
                "data", Map.of("items", items)
        );
    }

    private Map<String, Object> underutilised() {
        List<CloudResource> instances = cloudProvider.discover(
                ResourceQuery.ofTypes(ResourceType.EC2_INSTANCE, ResourceType.GPU_INSTANCE));

        if (instances.isEmpty()) {
            return unavailable("No compute instances were discovered in this account.");
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        Instant now = Instant.now();
        for (CloudResource instance : instances) {
            var cpu = cloudProvider.ec2InstanceMetrics(
                    instance.id(), CloudProviderPort.CPU_UTILIZATION,
                    now.minus(IDLE_WINDOW), now, IDLE_WINDOW);
            if (cpu.isEmpty() || cpu.average() <= 0.0) {
                // No telemetry is not evidence of idleness.
                continue;
            }
            if (cpu.average() < IDLE_CPU_THRESHOLD) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", instance.id());
                row.put("name", instance.name());
                row.put("type", instance.specOrDefault("instanceType", "unknown"));
                row.put("averageCpu", Math.round(cpu.average() * 10) / 10.0);
                row.put("monthlyCost", instance.monthlyCost());
                rows.add(row);
            }
        }

        if (rows.isEmpty()) {
            return Map.of("type", "text", "text",
                    "No compute instance averaged under " + (int) IDLE_CPU_THRESHOLD
                            + "% CPU over the last 7 days. "
                            + instances.size() + " instance(s) had usable telemetry.");
        }
        return Map.of(
                "type", "analysis",
                "text", rows.size() + " of " + instances.size()
                        + " instance(s) averaged under " + (int) IDLE_CPU_THRESHOLD
                        + "% CPU over the last 7 days.",
                "data", Map.of("items", rows)
        );
    }

    private Map<String, Object> savingsOpportunities() {
        // Orphan detection is measured, not modelled.
        long orphans = cloudProvider.discover(
                        ResourceQuery.ofTypes(ResourceType.EBS_VOLUME))
                .stream()
                .filter(r -> "0".equals(r.spec("attachmentCount")))
                .count();

        if (orphans == 0) {
            return Map.of("type", "text", "text",
                    "No unattached EBS volumes were found. "
                            + "Cost Explorer and Compute Optimizer recommendations are not "
                            + "available in the LocalStack sandbox.");
        }
        return Map.of(
                "type", "analysis",
                "text", orphans + " unattached EBS volume(s) are incurring storage cost.",
                "data", Map.of("unattachedVolumes", orphans)
        );
    }

    private Map<String, Object> inventorySummary() {
        List<CloudResource> all = cloudProvider.discover(ResourceQuery.all());
        Map<String, Long> byType = new LinkedHashMap<>();
        all.forEach(r -> byType.merge(r.type().typeName(), 1L, Long::sum));

        return Map.of(
                "type", "chart",
                "text", all.size() + " resource(s) discovered in " + cloudProvider.region() + ".",
                "data", Map.of("byType", byType)
        );
    }

    private double percentage(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return 0.0;
        }
        return part.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    private Map<String, Object> unavailable(String reason) {
        return Map.of("type", "text", "text", reason, "available", false);
    }
}