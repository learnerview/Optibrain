package com.optibrain.cloud.adapter.aws;

import java.util.Locale;
import java.util.Map;

/**
 * On-demand list prices, us-east-1, in major units per hour (or per GiB-month).
 *
 * <p>These live apart from the adapter because they are a different concern with a
 * different lifetime: the adapter talks to AWS, this is a reference table that must be
 * reviewed and dated by a human. Keeping it inline made it indistinguishable from
 * computed logic and invited it to drift.
 *
 * <p>Used only to attribute a cost to a resource that Cost Explorer cannot attribute
 * (Cost Explorer groups by service, not by resource id). Authoritative totals always
 * come from Cost Explorer; a wrong number here affects a single resource's label, never
 * a total.
 */
final class AwsPriceList {

    /** Monthly hours used to convert an hourly price to a monthly figure. */
    static final double HOURS_PER_MONTH = 730.0;

    private static final double FALLBACK_HOURLY = 0.05;

    private static final Map<String, Double> INSTANCE_HOURLY = Map.ofEntries(
            Map.entry("t3.micro", 0.0104),
            Map.entry("t3.small", 0.0208),
            Map.entry("t3.medium", 0.0416),
            Map.entry("t3.large", 0.0832),
            Map.entry("t3.xlarge", 0.1664),
            Map.entry("m5.large", 0.096),
            Map.entry("m5.xlarge", 0.192),
            Map.entry("m5.2xlarge", 0.384),
            Map.entry("c5.large", 0.085),
            Map.entry("c5.xlarge", 0.17),
            Map.entry("r5.large", 0.126),
            Map.entry("r5.xlarge", 0.252));

    /** On-demand price per GiB-month for each EBS volume type. */
    private static final Map<String, Double> VOLUME_PER_GIB_MONTH = Map.of(
            "gp2", 0.10,
            "gp3", 0.08,
            "io2", 0.125,
            "io2 Block Express", 0.125,
            "st1", 0.045,
            "sc1", 0.015,
            "standard", 0.05);

    private AwsPriceList() {
    }

    /**
     * Hourly on-demand price for an instance type.
     *
     * <p>An unknown type returns a conservative default rather than zero. Zero would
     * render a resource as free and hide it from cost review, which is the worse error.
     */
    static double instanceHourly(String instanceType) {
        if (instanceType == null) {
            return FALLBACK_HOURLY;
        }
        return INSTANCE_HOURLY.getOrDefault(instanceType, FALLBACK_HOURLY);
    }

    /** Per-GiB-month price for a volume type. */
    static double volumePerGibMonth(String volumeType) {
        if (volumeType == null) {
            return VOLUME_PER_GIB_MONTH.get("gp3");
        }
        return VOLUME_PER_GIB_MONTH.getOrDefault(volumeType,
                VOLUME_PER_GIB_MONTH.get("gp3"));
    }

    /**
     * Whether an instance type belongs to an accelerator family.
     *
     * <p>These are tracked as their own resource type because 2026 AI spend is dominated
     * by them and needs different advice than general-purpose compute. Graviton ("a1")
     * is deliberately excluded: it is a CPU architecture, not an accelerator, and
     * counting it as one would mislabel a large class of instances.
     */
    static boolean isAccelerator(String instanceType) {
        if (instanceType == null) {
            return false;
        }
        String t = instanceType.toLowerCase(Locale.ROOT);
        return t.startsWith("p4") || t.startsWith("p5") || t.startsWith("p6")
                || t.startsWith("g4") || t.startsWith("g5") || t.startsWith("g6")
                || t.startsWith("inf") || t.startsWith("trn")
                || t.startsWith("dl1") || t.startsWith("f1");
    }
}