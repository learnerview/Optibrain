package com.optibrain.policy.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

/**
 * Persists {@link Policy#getRules()} as JSON in a single {@code TEXT} column.
 *
 * <p>Rules are an arbitrary {@code Map<String, Object>} (scalars, nested maps, lists),
 * which has no natural JPA column. The converter is a plain {@code AttributeConverter}
 * rather than a bean because JPA instantiates converters itself; it never goes through
 * Spring's container, so injection is unavailable and a statically held {@code ObjectMapper}
 * is the safe choice.
 */
@Converter
public class JsonStringMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize policy rules to JSON", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize policy rules from JSON", e);
        }
    }
}