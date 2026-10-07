package com.optibrain.cloud.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Protection is the one control standing between a mistake and irreversible data loss,
 * so it is pinned here rather than only being exercised through a running provider.
 */
class ProtectionPolicyTest {

    @Test
    @DisplayName("the documented tag with value true protects a resource")
    void protectsWithCanonicalTag() {
        assertThat(ProtectionPolicy.isProtected(Map.of(ProtectionPolicy.TAG, "true")))
                .isTrue();
    }

    @Test
    @DisplayName("value matching ignores case")
    void ignoresCase() {
        assertThat(ProtectionPolicy.isProtected(Map.of(ProtectionPolicy.TAG, "TRUE"))).isTrue();
        assertThat(ProtectionPolicy.isProtected(Map.of(ProtectionPolicy.TAG, "True"))).isTrue();
    }

    @Test
    @DisplayName("any other value does not protect, so a typo cannot silently lock a resource")
    void otherValuesDoNotProtect() {
        assertThat(ProtectionPolicy.isProtected(Map.of(ProtectionPolicy.TAG, "false"))).isFalse();
        assertThat(ProtectionPolicy.isProtected(Map.of(ProtectionPolicy.TAG, "yes"))).isFalse();
        assertThat(ProtectionPolicy.isProtected(Map.of(ProtectionPolicy.TAG, "1"))).isFalse();
        assertThat(ProtectionPolicy.isProtected(Map.of(ProtectionPolicy.TAG, ""))).isFalse();
    }

    @Test
    @DisplayName("an unrelated tag set does not protect")
    void unrelatedTagsDoNotProtect() {
        assertThat(ProtectionPolicy.isProtected(Map.of("Environment", "prod"))).isFalse();
        assertThat(ProtectionPolicy.isProtected(Map.of())).isFalse();
    }

    @Test
    @DisplayName("absent tags are not protection, and do not throw")
    void nullTagsDoNotProtect() {
        assertThat(ProtectionPolicy.isProtected(null)).isFalse();
    }

    @Test
    @DisplayName("a near-miss tag name is not honoured")
    void tagNameIsExact() {
        Map<String, String> tags = new HashMap<>();
        tags.put("Optibrain:Protected", "true");
        tags.put("optibrain:protected ", "true");
        assertThat(ProtectionPolicy.isProtected(tags)).isFalse();
    }
}