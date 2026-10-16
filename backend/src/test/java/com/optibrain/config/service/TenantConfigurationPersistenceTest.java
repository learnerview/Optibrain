package com.optibrain.config.service;

import com.optibrain.config.model.TenantConfiguration;
import com.optibrain.config.repository.TenantConfigurationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A tuning threshold an operator changes must survive the next read; the previous
 * implementation rebuilt hardcoded defaults on every call and dropped the update. This is
 * a real round trip through the repository rather than a mock, because the defect was that
 * nothing was ever stored.
 */
@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(TenantConfigurationService.class)
class TenantConfigurationPersistenceTest {

    @Autowired
    private TenantConfigurationService service;

    @Autowired
    private TenantConfigurationRepository repository;

    @Test
    @DisplayName("an updated threshold survives a reload")
    void updatedThresholdIsPersisted() {
        TenantConfiguration defaults = service.getConfiguration("acme");
        assertThat(defaults.getEbsUnusedDaysThreshold()).isEqualTo(30);

        defaults.setEbsUnusedDaysThreshold(14);
        defaults.setScalingStrategy("AGGRESSIVE");
        service.updateConfiguration("acme", defaults);

        TenantConfiguration reloaded = service.getConfiguration("acme");
        assertThat(reloaded.getEbsUnusedDaysThreshold()).isEqualTo(14);
        assertThat(reloaded.getScalingStrategy()).isEqualTo("AGGRESSIVE");
    }

    @Test
    @DisplayName("updating one tenant does not change another tenant's configuration")
    void otherTenantsKeepTheirOwnConfiguration() {
        TenantConfiguration config = service.getConfiguration("acme");
        config.setSnapshotOldDaysThreshold(3);
        service.updateConfiguration("acme", config);

        assertThat(service.getConfiguration("acme").getSnapshotOldDaysThreshold()).isEqualTo(3);
        assertThat(service.getConfiguration("beta").getSnapshotOldDaysThreshold()).isEqualTo(7);
    }

    @Test
    @DisplayName("a configuration naming another tenant is refused")
    void mismatchedTenantIsRefused() {
        TenantConfiguration config = service.getConfiguration("acme");

        assertThatThrownBy(() -> service.updateConfiguration("beta", config))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(repository.findById("beta")).isEmpty();
    }
}
