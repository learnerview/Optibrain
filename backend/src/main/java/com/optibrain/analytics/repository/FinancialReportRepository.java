package com.optibrain.analytics.repository;

import com.optibrain.analytics.model.FinancialReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, UUID> {
    List<FinancialReport> findByTenantId(String tenantId);
    List<FinancialReport> findByTenantIdAndPeriod(String tenantId, String period);
}
