package com.xeno.mcpg.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request DTO for creating a new MCP service.
 */
public record CreateMcpServiceRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_-]+$") @Size(max = 64) String serviceCode,
        @NotBlank @Size(max = 128) String serviceName,
        @Size(max = 1000) String serviceDesc,
        @Size(max = 32) String protocol,
        Boolean enabled,
        Map<String, String> requestHeaders
) {
}