package com.xeno.mcpg.dto.admin;

import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request DTO for updating an existing MCP service.
 */
public record UpdateMcpServiceRequest(
        @Size(max = 128) String serviceName,
        @Size(max = 1000) String serviceDesc,
        @Size(max = 32) String protocol,
        Boolean enabled,
        Map<String, String> requestHeaders
) {
}