package com.xeno.mcpg.dto.admin;

import com.xeno.mcpg.dto.ApiToolParameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new MCP tool.
 */
public record CreateMcpToolRequest(
        @NotBlank @Size(max = 128) String toolCode,
        @NotBlank @Size(max = 128) String toolName,
        @Size(max = 1000) String toolDesc,
        @Size(max = 16) String httpMethod,
        @NotBlank @Size(max = 2000) String apiUrl,
        List<ApiToolParameter> parameters,
        Map<String, String> requestHeaders,
        Boolean enabled
) {
}