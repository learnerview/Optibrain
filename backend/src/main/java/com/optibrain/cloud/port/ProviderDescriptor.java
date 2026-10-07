package com.optibrain.cloud.port;

import com.optibrain.cloud.config.CloudMode;

/**
 * Identity of the execution target behind a provider implementation.
 *
 * <p>Separated from the operational capabilities so that callers which only need to
 * know <em>where</em> data came from (for a response envelope, or an audit record) do
 * not have to depend on the full provider surface.
 */
public interface ProviderDescriptor {

    /** The cloud vendor, for example {@code AWS}. */
    String provider();

    /** Default region for this provider. */
    String region();

    /** Whether this instance talks to a real vendor endpoint or to an in-process sandbox. */
    CloudMode executionTarget();

    /** True when actions on this provider can reach real infrastructure. */
    default boolean isLive() {
        return executionTarget() == CloudMode.AWS;
    }

    /**
     * True when the caller may skip a confirmation step. Demo targets are safe to
     * exercise destructively because nothing outside the JVM is affected.
     */
    default boolean isSandboxed() {
        return !isLive();
    }
}