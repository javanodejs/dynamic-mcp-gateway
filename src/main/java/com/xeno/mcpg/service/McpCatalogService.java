package com.xeno.mcpg.service;

import com.xeno.mcpg.dto.ApiToolParameter;
import com.xeno.mcpg.dto.admin.CreateMcpServiceRequest;
import com.xeno.mcpg.dto.admin.CreateMcpToolRequest;
import com.xeno.mcpg.dto.admin.UpdateMcpServiceRequest;
import com.xeno.mcpg.dto.admin.UpdateMcpToolRequest;

import java.util.List;
import java.util.Map;

/**
 * Service interface for MCP catalog management.
 * <p>
 * Provides CRUD operations for MCP services and tools, plus catalog snapshot loading.
 */
public interface McpCatalogService {

    /**
     * List all MCP services.
     *
     * @return list of service details
     */
    List<Map<String, Object>> listServices();

    /**
     * Get a specific service by ID.
     *
     * @param serviceId the service ID
     * @return service details
     */
    Map<String, Object> getService(Long serviceId);

    /**
     * Create a new MCP service.
     *
     * @param request the creation request
     * @return created service details
     */
    Map<String, Object> createService(CreateMcpServiceRequest request);

    /**
     * Update an existing MCP service.
     *
     * @param serviceId the service ID
     * @param request   the update request
     * @return updated service details
     */
    Map<String, Object> updateService(Long serviceId, UpdateMcpServiceRequest request);

    /**
     * Update the enabled status of a service.
     *
     * @param serviceId the service ID
     * @param enabled   whether the service should be enabled
     */
    void updateServiceStatus(Long serviceId, boolean enabled);

    /**
     * Delete a service and its tool bindings.
     *
     * @param serviceId the service ID
     */
    void deleteService(Long serviceId);

    /**
     * List all tools bound to a service.
     *
     * @param serviceId the service ID
     * @return list of tool details
     */
    List<Map<String, Object>> listTools(Long serviceId);

    /**
     * Get a specific tool binding by ID.
     *
     * @param toolId the tool binding ID
     * @return tool binding details
     */
    Map<String, Object> getTool(Long toolId);

    /**
     * Create a new tool and bind it to a service.
     *
     * @param serviceId the service ID
     * @param request   the creation request
     * @return created tool binding details
     */
    Map<String, Object> createTool(Long serviceId, CreateMcpToolRequest request);

    /**
     * Update an existing tool.
     *
     * @param toolId  the tool binding ID
     * @param request the update request
     * @return updated tool binding details
     */
    Map<String, Object> updateTool(Long toolId, UpdateMcpToolRequest request);

    /**
     * Update the enabled status of a tool.
     *
     * @param toolId  the tool binding ID
     * @param enabled whether the tool should be enabled
     */
    void updateToolStatus(Long toolId, boolean enabled);

    /**
     * Delete a tool binding.
     *
     * @param toolId the tool binding ID
     */
    void deleteTool(Long toolId);

    /**
     * Build a service configuration for export.
     *
     * @param serviceCode the service code
     * @return service configuration JSON
     */
    Map<String, Object> buildServiceConfig(String serviceCode);

    /**
     * List all service connection information.
     *
     * @return list of connection details
     */
    List<Map<String, Object>> listServiceConnections();

    /**
     * Load the current catalog snapshot of enabled services and tools.
     *
     * @return the catalog snapshot
     */
    CatalogSnapshot loadEnabledCatalog();

    /**
     * Snapshot of the current catalog state.
     *
     * @param bindings  the list of tool bindings
     * @param signature a signature for change detection
     */
    record CatalogSnapshot(List<CatalogToolBinding> bindings, String signature) {
    }

    /**
     * Represents a tool binding in the catalog.
     *
     * @param serviceCode the service code
     * @param serviceName the service name
     * @param protocol    the protocol type
     * @param toolId      the tool binding ID
     * @param toolCode    the tool code
     * @param toolName    the tool name
     * @param toolDesc    the tool description
     * @param httpMethod  the HTTP method
     * @param apiUrl      the API URL
     * @param parameters  the tool parameters
     * @param headers     the merged request headers
     */
    record CatalogToolBinding(
            String serviceCode,
            String serviceName,
            String protocol,
            Long toolId,
            String toolCode,
            String toolName,
            String toolDesc,
            String httpMethod,
            String apiUrl,
            List<ApiToolParameter> parameters,
            Map<String, String> headers
    ) {
    }
}