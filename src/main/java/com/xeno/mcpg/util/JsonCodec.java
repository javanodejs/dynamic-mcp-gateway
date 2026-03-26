package com.xeno.mcpg.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON serialization and deserialization utility.
 */
@Component
public class JsonCodec {

    private final ObjectMapper objectMapper;

    public JsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serialize an object to JSON string.
     *
     * @param value the object to serialize
     * @return JSON string, or null if value is null
     */
    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON serialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parse a JSON string to a Map.
     *
     * @param json the JSON string
     * @return parsed map, or empty map if json is null/blank
     */
    public Map<String, String> toStringMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid headers JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Parse a JSON string to a typed list.
     *
     * @param json          the JSON string
     * @param typeReference the type reference for the list element type
     * @return parsed list, or empty list if json is null/blank
     */
    public <T> List<T> toList(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid list JSON: " + e.getMessage(), e);
        }
    }
}