package com.optibrain.config;

import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Creates the default tenant on first start.
 *
 * <p>Replaces the removed {@code DataSeeder}, which inserted fabricated audit rows,
 * spot actions and orphan resources ("i-023ab-sample", "S3_UNENCRYPTED_BUCKET"). That
 * data was indistinguishable from real findings and has no legitimate source.
 *
 * <p>The only row created here is the tenant itself, and only when the table is empty,
 * so it is idempotent and never overwrites operator changes. Every other record in the
 * database is produced by the product doing real work.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantBootstrap implements ApplicationRunner {

    private final TenantRepository tenants;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        seedIfEmpty();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onReady() {
        // Belt and braces: ApplicationRunner can be skipped by certain test contexts.
        seedIfEmpty();
    }

    private void seedIfEmpty() {
        if (tenants.count() > 0) {
            return;
        }
        Tenant tenant = Tenant.builder()
                .id("default-tenant")
                .name("Default Tenant")
                .active(true)
                .cloudProvider("AWS")
                // Manual mode with approval required: a fresh install must not be able to
                // mutate infrastructure without a human deciding it should.
                .mode("MANUAL")
                .autonomousMode(false)
                .requireApprovalForChanges(true)
                .plan("BASIC")
                .monthlyBudgetLimit(0.0)
                .createdAt(Instant.now())
                .build();
        tenants.save(tenant);
        log.info("Created default tenant '{}'", tenant.getId());
    }
}