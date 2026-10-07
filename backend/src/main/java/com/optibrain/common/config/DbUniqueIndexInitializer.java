package com.optibrain.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Ensures unique indexes that the Hibernate-to-SQLite schema path silently drops.
 *
 * <p>{@code @Column(unique = true)} and {@code @Index(unique = true)} normally become
 * {@code ALTER TABLE ... ADD CONSTRAINT ... UNIQUE}, a statement SQLite does not support;
 * the {@code org.hibernate.community.dialect.SQLiteDialect} logs a warning and skips it.
 * The result in dev and sandbox runs is that {@code app_users.username} and
 * {@code orphaned_resources.resource_id} carry no uniqueness guarantee, which weakens the
 * identity store and the orphan-inventory deduplication.
 *
 * <p>Running after Hibernate has created the tables, this recreates those unique indexes
 * explicitly when the database is SQLite. PostgreSQL (prod) is untouched: it is validated,
 * not created, by the application.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DbUniqueIndexInitializer implements ApplicationRunner {

    private static final String[][] UNIQUE_INDEXES = {
            {"idx_app_users_username", "CREATE UNIQUE INDEX IF NOT EXISTS "
                    + "idx_app_users_username ON app_users (username)"},
            {"uk_orphaned_resources_resource_id", "CREATE UNIQUE INDEX IF NOT EXISTS "
                    + "uk_orphaned_resources_resource_id ON orphaned_resources (resource_id)"},
    };

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("sqlite")) {
                return;
            }
            for (String[] index : UNIQUE_INDEXES) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(index[1]);
                    log.info("Ensured unique index {}", index[0]);
                } catch (Exception e) {
                    log.warn("Could not create unique index {}: {}", index[0], e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Could not locate the database to ensure unique indexes: {}", e.getMessage());
        }
    }
}