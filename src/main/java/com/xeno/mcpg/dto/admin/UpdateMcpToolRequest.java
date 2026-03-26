package com.xeno.mcpg.dto.admin;

import com.xeno.mcpg.dto.ApiToolParameter;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating an existing MCP tool.
 */
public record UpdateMcpToolRequest(
        @Size(max = 128) String toolCode,
        @Size(max = 128) String toolName,
        @Size(max = 1000) String toolDesc,
        @Size(max = 16) String httpMethod,
        @Size(max = 2000) String apiUrl,
        List<ApiToolParameter> parameters,
        Map<String, String> requestHeaders,
        Boolean enabled
) {
}