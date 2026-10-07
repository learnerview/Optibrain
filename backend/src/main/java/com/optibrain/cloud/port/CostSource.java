package com.optibrain.cloud.port;

import com.optibrain.cloud.model.CostReport;

/**
 * Read access to billing data.
 */
public interface CostSource {

    /** Aggregate spend for the query period, grouped by service, region and day. */
    CostReport costReport(CostQuery query);

    /**
     * Cost currently attributed to a single resource, per month.
     *
     * <p>Returns {@code null} when the provider cannot attribute cost at resource
     * granularity, which is common - callers must handle it rather than treating it as
     * zero.
     */
    default Double monthlyCostOf(String resourceId) {
        return null;
    }
}