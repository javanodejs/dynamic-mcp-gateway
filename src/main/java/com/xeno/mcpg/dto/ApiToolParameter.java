package com.xeno.mcpg.dto;

/**
 * Represents a parameter definition for an API tool.
 *
 * @param name         the parameter name
 * @param required     whether the parameter is required
 * @param type         the parameter type (string, integer, boolean, etc.)
 * @param description  the parameter description
 * @param defaultValue the default value for the parameter
 */
public record ApiToolParameter(
        String name,
        boolean required,
        String type,
        String description,
        Object defaultValue
) {
}