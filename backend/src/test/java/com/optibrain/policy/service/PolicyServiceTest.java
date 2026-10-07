package com.optibrain.policy.service;

import com.optibrain.common.context.TenantContext;
import com.optibrain.policy.model.Policy;
import com.optibrain.policy.repository.PolicyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Policies became durable and tenant-scoped; these tests pin the two properties that
 * make that true: reads never cross a tenant boundary, and a row never gets written
 * without an owner.
 */
class PolicyServiceTest {

    private final PolicyRepository repository = mock(PolicyRepository.class);
    private final PolicyService service = new PolicyService(repository);

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private Policy policy(String id, String tenantId, String type) {
        return Policy.builder()
                .id(id)
                .tenantId(tenantId)
                .name(id)
                .type(type)
                .enabled(true)
                .priority(1)
                .rules(Map.of())
                .build();
    }

    @Test
    @DisplayName("reads outside a tenant context fall back to the built-in default without persisting")
    void tenantlessReadFallsBackToBuiltInDefault() {
        Policy current = service.getCurrentPolicy();

        assertThat(current.getId()).isEqualTo("default");
        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("a tenant with no policies is seeded on first read")
    void tenantWithNoPoliciesIsSeededOnFirstRead() {
        TenantContext.setTenantId("t1");
        when(repository.findFirstByTenantIdAndEnabledTrueOrderByPriorityDesc("t1"))
                .thenReturn(Optional.empty());
        when(repository.existsByTenantId("t1")).thenReturn(false);

        service.getCurrentPolicy();

        ArgumentCaptor<List<Policy>> seeded = ArgumentCaptor.forClass(List.class);
        verify(repository).existsByTenantId("t1");
        verify(repository).saveAll(seeded.capture());
        assertThat(seeded.getValue()).hasSize(3);
        assertThat(seeded.getValue()).allMatch(p -> "t1".equals(p.getTenantId()));
    }

    @Test
    @DisplayName("creating a policy requires a tenant context and stamps it as the owner")
    void createPolicyStampsOwnershipAndIdentity() {
        TenantContext.setTenantId("t1");
        Policy input = Policy.builder()
                .name("Budget Cap")
                .type("cost")
                .enabled(true)
                .priority(2)
                .rules(Map.of("maxMonthlyBudget", 5000.0))
                .build();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createPolicy(input);

        ArgumentCaptor<Policy> saved = ArgumentCaptor.forClass(Policy.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo("t1");
        assertThat(saved.getValue().getId()).isNotBlank();
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("creating a policy without a tenant context is refused, not silently shared")
    void createPolicyRefusesWithoutTenantContext() {
        Policy input = policy("p1", null, "cost");

        assertThatThrownBy(() -> service.createPolicy(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenant context");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("an update cannot move a policy to another tenant")
    void updatePolicyCannotMoveAPolicyToAnotherTenant() {
        TenantContext.setTenantId("t1");
        when(repository.findByIdAndTenantId("p1", "t1"))
                .thenReturn(Optional.of(policy("p1", "t1", "scaling")));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Policy smuggled = policy("p1", "other-tenant", "scaling");

        service.updatePolicy("p1", smuggled);

        ArgumentCaptor<Policy> saved = ArgumentCaptor.forClass(Policy.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo("t1");
    }

    @Test
    @DisplayName("reads are scoped to the current tenant, never global")
    void policiesAreScopedToTheCallingTenant() {
        TenantContext.setTenantId("t1");
        when(repository.findByTenantIdOrderByPriorityDesc("t1"))
                .thenReturn(List.of(policy("a", "t1", "cost")));
        assertThat(service.getAllPolicies()).hasSize(1);

        TenantContext.setTenantId("t2");
        when(repository.findByTenantIdOrderByPriorityDesc("t2")).thenReturn(List.of());
        assertThat(service.getAllPolicies()).isEmpty();

        verify(repository, never()).findAll();
    }
}