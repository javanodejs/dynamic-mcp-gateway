package com.xeno.mcpg.controller.admin;

import com.xeno.mcpg.common.BaseResponse;
import com.xeno.mcpg.config.McpMultiEndpointRegistry;
import com.xeno.mcpg.dto.admin.CreateMcpToolRequest;
import com.xeno.mcpg.dto.admin.UpdateMcpToolRequest;
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
 * Admin controller for MCP tool management.
 */
@RestController
@RequestMapping("/admin/mcp")
public class McpToolAdminController {

    private final McpCatalogService catalogService;
    private final DynamicMcpToolCallbackProvider callbackProvider;
    private final McpMultiEndpointRegistry mcpMultiEndpointRegistry;

    public McpToolAdminController(McpCatalogService catalogService,
                                  DynamicMcpToolCallbackProvider callbackProvider,
                                  McpMultiEndpointRegistry mcpMultiEndpointRegistry) {
        this.catalogService = catalogService;
        this.callbackProvider = callbackProvider;
        this.mcpMultiEndpointRegistry = mcpMultiEndpointRegistry;
    }

    /**
     * List all tools bound to a service.
     */
    @GetMapping("/services/{serviceId}/tools")
    public BaseResponse<Object> listByService(@PathVariable("serviceId") Long serviceId) {
        return BaseResponse.ok(catalogService.listTools(serviceId));
    }

    /**
     * Create a new tool and bind it to a service.
     */
    @PostMapping("/services/{serviceId}/tools")
    public BaseResponse<Object> create(@PathVariable("serviceId") Long serviceId,
                                      @Valid @RequestBody CreateMcpToolRequest request) {
        Map<String, Object> data = catalogService.createTool(serviceId, request);
        mcpMultiEndpointRegistry.reload();
        return BaseResponse.ok(data);
    }

    /**
     * Get a specific tool binding.
     */
    @GetMapping("/tools/{toolId}")
    public BaseResponse<Object> get(@PathVariable("toolId") Long toolId) {
        return BaseResponse.ok(catalogService.getTool(toolId));
    }

    /**
     * Update an existing tool.
     */
    @PutMapping("/tools/{toolId}")
    public BaseResponse<Object> update(@PathVariable("toolId") Long toolId,
                                      @Valid @RequestBody UpdateMcpToolRequest request) {
        Map<String, Object> data = catalogService.updateTool(toolId, request);
        mcpMultiEndpointRegistry.reload();
        return BaseResponse.ok(data);
    }

    /**
     * Update the enabled status of a tool.
     */
    @PatchMapping("/tools/{toolId}/status")
    public BaseResponse<Object> updateStatus(@PathVariable("toolId") Long toolId,
                                            @RequestParam("enabled") boolean enabled) {
        catalogService.updateToolStatus(toolId, enabled);
        mcpMultiEndpointRegistry.reload();
        return BaseResponse.ok();
    }

    /**
     * Delete a tool binding.
     */
    @DeleteMapping("/tools/{toolId}")
    public BaseResponse<Object> delete(@PathVariable("toolId") Long toolId) {
        catalogService.deleteTool(toolId);
        mcpMultiEndpointRegistry.reload();
        return BaseResponse.ok();
    }

    /**
     * Get the runtime status of dynamic tools.
     */
    @GetMapping("/runtime/status")
    public BaseResponse<Object> runtimeStatus() {
        return BaseResponse.ok(callbackProvider.status());
    }
}