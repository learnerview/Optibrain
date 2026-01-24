package com.optibrain.report.service;

import java.util.Map;

/**
 * Service interface for Reports functionality
 */
public interface ReportService {
    
    /**
     * Get monthly report summary
     * @return Monthly report data
     */
    Map<String, Object> getMonthlyReport();
}
