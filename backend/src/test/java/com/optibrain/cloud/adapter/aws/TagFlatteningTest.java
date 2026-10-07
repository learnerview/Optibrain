package com.optibrain.cloud.adapter.aws;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tag flattening sits between the AWS tag list and the protection check, so a mistake here
 * silently disarms the one guard that prevents data loss. These tests pin the conversion
 * and the resulting protection flag independently of any scanner.
 */
class TagFlatteningTest {

    private static Map<String, String> flatten(List<Tag> tags) {
        return tags.stream().collect(java.util.stream.Collectors.toMap(
                Tag::key, t -> t.value() == null ? "" : t.value(),
                (first, ignored) -> first,
                java.util.LinkedHashMap::new));
    }

    @Test
    @DisplayName("the protection tag survives flattening and sets the flag")
    void protectionTagSurvivesFlattening() {
        Map<String, String> tags = flatten(List.of(
                Tag.builder().key("Name").value("baseline").build(),
                Tag.builder().key("optibrain:protected").value("true").build()));

        assertThat(tags).containsEntry("optibrain:protected", "true");
        assertThat(com.optibrain.cloud.policy.ProtectionPolicy.isProtected(tags)).isTrue();
    }

    @Test
    @DisplayName("an ordinary tagged volume is not protected")
    void ordinaryTagsAreNotProtection() {
        Map<String, String> tags = flatten(List.of(
                Tag.builder().key("Name").value("sandbox-i-123").build(),
                Tag.builder().key("Environment").value("dev").build(),
                Tag.builder().key("Owner").value("platform").build()));

        assertThat(com.optibrain.cloud.policy.ProtectionPolicy.isProtected(tags)).isFalse();
    }

    @Test
    @DisplayName("an untagged resource yields an empty map rather than null")
    void untaggedResourceYieldsEmptyMap() {
        assertThat(flatten(List.of())).isEmpty();
    }

    @Test
    @DisplayName("a null tag value becomes empty, not a null that breaks the map contract")
    void nullValueDoesNotBreakFlattening() {
        Map<String, String> tags = flatten(List.of(
                Tag.builder().key("optibrain:protected").build()));

        assertThat(tags).containsEntry("optibrain:protected", "");
        assertThat(com.optibrain.cloud.policy.ProtectionPolicy.isProtected(tags)).isFalse();
    }

    @Test
    @DisplayName("a duplicated key does not throw on duplicate-detection rules")
    void duplicateKeysDoNotThrow() {
        Map<String, String> tags = flatten(List.of(
                Tag.builder().key("Owner").value("a").build(),
                Tag.builder().key("Owner").value("b").build()));

        assertThat(tags).hasSize(1);
    }
}