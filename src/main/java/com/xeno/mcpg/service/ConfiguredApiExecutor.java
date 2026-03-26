package com.xeno.mcpg.service;

import java.util.Map;

/**
 * Executor for configured API tool calls.
 */
public interface ConfiguredApiExecutor {

    /**
     * Execute an API call based on the tool configuration.
     *
     * @param binding            the tool binding configuration
     * @param runtimeInput       the runtime input parameters
     * @param passthroughHeaders headers to passthrough from the MCP client
     * @return the execution result as a string
     */
    String execute(McpCatalogService.CatalogToolBinding binding,
                   Map<String, Object> runtimeInput,
                   Map<String, String> passthroughHeaders);
}