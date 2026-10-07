package com.optibrain.policy.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Policy rules are persisted as a JSON column; the conversion is a plain JPA
 * {@code AttributeConverter} with no Spring proxy, so the exact round trip is pinned here
 * rather than assumed to work through a real session.
 */
class JsonStringMapConverterTest {

    private final JsonStringMapConverter converter = new JsonStringMapConverter();

    @Test
    @DisplayName("rules survive a database round trip unchanged")
    void roundTripsRuleMap() {
        Map<String, Object> rules = Map.of(
                "performanceWeight", 0.5,
                "cpuThreshold", 80.0,
                "maxScaleUpPercentage", 50,
                "autoOptimizationEnabled", true,
                "region", "us-east-1");

        String json = converter.convertToDatabaseColumn(rules);
        Map<String, Object> restored = converter.convertToEntityAttribute(json);

        assertThat(restored).isEqualTo(rules);
    }

    @Test
    @DisplayName("a nested rule survives the round trip")
    void roundTripsNestedRules() {
        Map<String, Object> rules = Map.of("limits", Map.of("minInstances", 1, "maxInstances", 20));

        assertThat(converter.convertToEntityAttribute(
                converter.convertToDatabaseColumn(rules))).isEqualTo(rules);
    }

    @Test
    @DisplayName("null column values become an empty rule map, not null")
    void nullColumnBecomesEmptyInsteadOfNull() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }

    @Test
    @DisplayName("null rules are persisted as null")
    void nullRulesArePersistedAsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }
}