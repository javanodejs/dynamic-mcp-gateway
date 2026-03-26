package com.xeno.mcpg.controller.admin;

import com.xeno.mcpg.common.BaseResponse;
import com.xeno.mcpg.config.McpMultiEndpointRegistry;
import com.xeno.mcpg.dto.admin.CreateMcpServiceRequest;
import com.xeno.mcpg.dto.admin.UpdateMcpServiceRequest;
import com.xeno.mcpg.mcp.DynamicMcpToolCallbackProvider;
import com.xeno.mcpg.service.McpCatalogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin controller for MCP service management.
 */
@RestController
@RequestMapping("/admin/mcp/services")
public class McpServiceAdminController {

    private final McpCatalogService catalogService;
    private final DynamicMcpToolCallbackProvider callbackProvider;
    private final McpMultiEndpointRegistry mcpMultiEndpointRegistry;

    public McpServiceAdminController(McpCatalogService catalogService,
                                     DynamicMcpToolCallbackProvider callbackProvider,
                                     McpMultiEndpointRegistry mcpMultiEndpointRegistry) {
        this.catalogService = catalogService;
        this.callbackProvider = callbackProvider;
        this.mcpMultiEndpointRegistry = mcpMultiEndpointRegistry;
    }

    /**
     * List all MCP services.
     */
    @GetMapping
    public BaseResponse<Object> list() {
        return BaseResponse.ok(catalogService.listServices());
    }

    /**
     * Get a specific MCP service.
     */
    @GetMapping("/{id}")
    public BaseResponse<Object> get(@PathVariable("id") Long id) {
        return BaseResponse.ok(catalogService.getService(id));
    }

    /**
     * Create a new MCP service.
     */
    @PostMapping
    public BaseResponse<Object> create(@Valid @RequestBody CreateMcpServiceRequest request) {
        Map<String, Object> data = catalogService.createService(request);
        mcpMultiEndpointRegistry.reload();
        return BaseResponse.ok(data);
    }

    /**
     * Update an existing MCP service.
     */
    @PutMapping("/{id}")
    public BaseResponse<Object> update(@PathVariable("id") Long id,
                                      @Valid @RequestBody UpdateMcpServiceRequest request) {
        Map<String, Object> data = catalogService.updateService(id, request);
        mcpMultiEndpointRegistry.reload();
        return BaseResponse.ok(data);
    }

    /**
     * Update the enabled status of a service.
     */
    @PatchMapping("/{id}/status")
    public BaseResponse<Object> updateStatus(@PathVariable("id") Long id,
                                            @RequestParam("enabled") boolean enabled) {
        catalogService.updateServiceStatus(id, enabled);
        mcpMultiEndpointRegistry.reload();
        return BaseResponse.ok();
    }

    /**
     * Delete an MCP service.
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Object> delete(@PathVariable("id") Long id) {
        catalogService.deleteService(id);
        mcpMultiEndpointRegistry.reload();
        return BaseResponse.ok();
    }

    /**
     * List all service connection information.
     */
    @GetMapping("/connections")
    public BaseResponse<Object> connections() {
        return BaseResponse.ok(catalogService.listServiceConnections());
    }

    /**
     * Get service configuration by service code.
     */
    @GetMapping("/code/{serviceCode}/config")
    public BaseResponse<Object> serviceConfig(@PathVariable("serviceCode") String serviceCode) {
        return BaseResponse.ok(catalogService.buildServiceConfig(serviceCode));
    }

    /**
     * Reload all MCP tools and endpoints.
     */
    @PostMapping("/reload")
    public BaseResponse<Object> reloadTools() {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("tools", callbackProvider.status());
        response.put("endpoints", mcpMultiEndpointRegistry.reload());
        return BaseResponse.ok(response);
    }
}