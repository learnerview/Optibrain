package com.optibrain.cloud.model;

/**
 * Whether a data series reflects a measured reality or should not be read as evidence.
 *
 * <p>The distinction exists so "no data" never silently degrades into "zero": treating a
 * failed or missing telemetry call as a metric value of zero would classify a healthy
 * resource as idle and invite a destructive recommendation on poor evidence.
 */
public enum DataStatus {

    /** Data was retrieved and reflects the queried window. */
    AVAILABLE,

    /** The source was reached and returned no observations; absence is genuine. */
    EMPTY,

    /** The data could not be retrieved; absence must not be read as zero or idle. */
    UNAVAILABLE;

    public boolean isUsable() {
        return this == AVAILABLE;
    }
}