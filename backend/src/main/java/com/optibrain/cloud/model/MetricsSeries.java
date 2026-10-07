package com.optibrain.cloud.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * A time-ordered series of observations for one metric on one resource.
 *
 * @param resourceId the resource the metric belongs to
 * @param metric canonical metric name (for example {@code CPUUtilization})
 * @param unit CloudWatch unit (Percent, Count, Bytes...)
 * @param granularity spacing between points, as a duration rather than an instant
 * @param points observations, oldest first
 */
public record MetricsSeries(
        String resourceId,
        String metric,
        String unit,
        Duration granularity,
        List<Point> points
) {

    public MetricsSeries {
        points = points == null ? List.of() : List.copyOf(points);
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public double average() {
        return points.stream().mapToDouble(Point::average).average().orElse(0.0);
    }

    public double max() {
        return points.isEmpty() ? 0.0
                : points.stream().mapToDouble(Point::max).reduce(Double.NEGATIVE_INFINITY, Math::max);
    }

    public double min() {
        return points.isEmpty() ? 0.0
                : points.stream().mapToDouble(Point::min).reduce(Double.POSITIVE_INFINITY, Math::min);
    }

    /** Mean of the most recent {@code n} points - the basis for idle detection. */
    public double averageOverLast(int n) {
        if (points.isEmpty()) {
            return 0.0;
        }
        int size = Math.min(n, points.size());
        List<Point> tail = points.subList(points.size() - size, points.size());
        return tail.stream().mapToDouble(Point::average).average().orElse(0.0);
    }

    /**
     * One aggregated observation.
     *
     * @param timestamp start of the aggregation window
     * @param average mean over the window
     * @param minimum lowest sample in the window
     * @param maximum highest sample in the window
     * @param sampleCount number of raw samples behind the aggregate
     */
    public record Point(Instant timestamp, double average, double minimum, double maximum, long sampleCount) {

        public double average() {
            return average;
        }

        public double min() {
            return minimum;
        }

        public double max() {
            return maximum;
        }
    }
}