package com.xeno.mcpg.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xeno.mcpg.dto.ApiToolParameter;
import com.xeno.mcpg.service.ConfiguredApiExecutor;
import com.xeno.mcpg.service.McpCatalogService;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dynamic MCP tool callback provider.
 * <p>
 * Provides tool callbacks based on the current catalog configuration with automatic refresh.
 */
@Component
@Slf4j
public class DynamicMcpToolCallbackProvider implements ToolCallbackProvider {

    /**
     * Context key for service code in MCP transport context.
     */
    public static final String CTX_SERVICE_CODE_KEY = "mcp.gateway.serviceCode";

    /**
     * Context key for request headers in MCP transport context.
     */
    public static final String CTX_REQUEST_HEADERS_KEY = "mcp.gateway.requestHeaders";

    private static final long REFRESH_INTERVAL_MS = 10_000L;

    private final McpCatalogService catalogService;
    private final ConfiguredApiExecutor apiExecutor;
    private final ObjectMapper objectMapper;
    private final AtomicLong lastRefreshAt = new AtomicLong(0L);

    private volatile String cachedSignature = "";
    private volatile ToolCallback[] cachedCallbacks = new ToolCallback[0];
    private volatile Map<String, ToolCallback[]> cachedCallbacksByService = new HashMap<>();

    public DynamicMcpToolCallbackProvider(McpCatalogService catalogService,
                                          ConfiguredApiExecutor apiExecutor,
                                          ObjectMapper objectMapper) {
        this.catalogService = catalogService;
        this.apiExecutor = apiExecutor;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        refreshIfNeeded();
        return cachedCallbacks;
    }

    /**
     * Get tool callbacks for a specific service.
     *
     * @param serviceCode the service code
     * @return array of tool callbacks for the service
     */
    public ToolCallback[] getToolCallbacksForService(String serviceCode) {
        refreshIfNeeded();
        if (serviceCode == null || serviceCode.isBlank()) {
            return new ToolCallback[0];
        }
        return cachedCallbacksByService.getOrDefault(serviceCode, new ToolCallback[0]);
    }

    /**
     * Force reload the tool callbacks.
     */
    public synchronized void forceReload() {
        rebuildCallbacks();
        lastRefreshAt.set(System.currentTimeMillis());
    }

    /**
     * Get the current status of the tool callback provider.
     *
     * @return status map
     */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("lastRefreshAt", lastRefreshAt.get());
        status.put("toolCount", cachedCallbacks.length);
        status.put("serviceCount", cachedCallbacksByService.size());
        status.put("catalogSignature", cachedSignature);
        return status;
    }

    private void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        long last = lastRefreshAt.get();
        if (now - last < REFRESH_INTERVAL_MS) {
            return;
        }
        synchronized (this) {
            if (now - lastRefreshAt.get() < REFRESH_INTERVAL_MS) {
                return;
            }
            rebuildCallbacks();
            lastRefreshAt.set(now);
        }
    }

    private void rebuildCallbacks() {
        McpCatalogService.CatalogSnapshot snapshot = catalogService.loadEnabledCatalog();
        if (snapshot.signature().equals(cachedSignature)) {
            return;
        }
        List<ToolCallback> callbacks = new ArrayList<>();
        Map<String, List<ToolCallback>> serviceGrouped = new LinkedHashMap<>();
        for (McpCatalogService.CatalogToolBinding binding : snapshot.bindings()) {
            ToolCallback callback = buildToolCallback(binding);
            callbacks.add(callback);
            serviceGrouped.computeIfAbsent(binding.serviceCode(), k -> new ArrayList<>()).add(callback);
        }
        cachedCallbacks = callbacks.toArray(new ToolCallback[0]);
        Map<String, ToolCallback[]> groupedArr = new HashMap<>();
        serviceGrouped.forEach((serviceCode, serviceCallbacks) ->
                groupedArr.put(serviceCode, serviceCallbacks.toArray(new ToolCallback[0])));
        cachedCallbacksByService = groupedArr;
        cachedSignature = snapshot.signature();
    }

    private ToolCallback buildToolCallback(McpCatalogService.CatalogToolBinding binding) {
        String toolName = normalizeToolName(binding.toolCode());
        return FunctionToolCallback.<Map<String, Object>, String>builder(toolName, (input, toolContext) -> execute(binding, input, toolContext))
                .description(buildDescription(binding))
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {})
                .inputSchema(buildInputSchema(binding.parameters()))
                .build();
    }

    private String execute(McpCatalogService.CatalogToolBinding binding,
                           Map<String, Object> input,
                           ToolContext toolContext) {
        String contextServiceCode = readServiceCodeFromContext(toolContext);
        if (contextServiceCode != null && !contextServiceCode.isBlank()
                && !contextServiceCode.equalsIgnoreCase(binding.serviceCode())) {
            return "Current service connection is [" + contextServiceCode + "], cannot invoke tool from [" + binding.serviceCode() + "]";
        }
        return apiExecutor.execute(binding, input, readRequestHeadersFromContext(toolContext));
    }

    private String readServiceCodeFromContext(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Object exchangeObj = toolContext.getContext().get("exchange");
        if (exchangeObj instanceof McpSyncServerExchange syncExchange) {
            return toStringValue(syncExchange.transportContext().get(CTX_SERVICE_CODE_KEY));
        }
        if (exchangeObj instanceof McpTransportContext transportContext) {
            return toStringValue(transportContext.get(CTX_SERVICE_CODE_KEY));
        }
        return null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readRequestHeadersFromContext(ToolContext toolContext) {
        if (toolContext == null) {
            return Map.of();
        }
        Object exchangeObj = toolContext.getContext().get("exchange");
        if (exchangeObj instanceof McpSyncServerExchange syncExchange) {
            return toStringMap(syncExchange.transportContext().get(CTX_REQUEST_HEADERS_KEY));
        }
        if (exchangeObj instanceof McpTransportContext transportContext) {
            return toStringMap(transportContext.get(CTX_REQUEST_HEADERS_KEY));
        }
        return Map.of();
    }

    private Map<String, String> toStringMap(Object raw) {
        if (!(raw instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        rawMap.forEach((k, v) -> {
            String value = normalizeHeaderValue(v);
            if (k != null && value != null && !value.isBlank()) {
                result.put(String.valueOf(k), value);
            }
        });
        return result;
    }

    private String normalizeHeaderValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Collection<?> coll) {
            if (coll.isEmpty()) {
                return null;
            }
            Object first = coll.iterator().next();
            return first == null ? null : String.valueOf(first);
        }
        if (value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            if (arr.length == 0 || arr[0] == null) {
                return null;
            }
            return String.valueOf(arr[0]);
        }
        return String.valueOf(value);
    }

    private String normalizeToolName(String toolCode) {
        return toolCode.replace(".", "_").replace("-", "_").replace("/", "_");
    }

    private String buildDescription(McpCatalogService.CatalogToolBinding binding) {
        return binding.toolName() + " - " + safe(binding.toolDesc());
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private String buildInputSchema(List<ApiToolParameter> parameters) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        if (parameters != null) {
            for (ApiToolParameter parameter : parameters) {
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("type", toJsonSchemaType(parameter.type()));
                field.put("description", parameter.description() == null ? "" : parameter.description());
                if (parameter.defaultValue() != null) {
                    field.put("default", parameter.defaultValue());
                }
                properties.put(parameter.name(), field);
                if (parameter.required()) {
                    required.add(parameter.name());
                }
            }
        }

        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", true);

        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
        }
    }

    private String toJsonSchemaType(String javaType) {
        if (javaType == null) {
            return "string";
        }
        return switch (javaType.toLowerCase()) {
            case "integer", "int", "long" -> "integer";
            case "number", "double", "float", "decimal" -> "number";
            case "boolean", "bool" -> "boolean";
            case "object", "map", "json" -> "object";
            case "array", "list" -> "array";
            default -> "string";
        };
    }
}