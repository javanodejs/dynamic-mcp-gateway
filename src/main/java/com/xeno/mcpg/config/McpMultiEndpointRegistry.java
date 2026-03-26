package com.xeno.mcpg.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xeno.mcpg.entity.McpServiceEntity;
import com.xeno.mcpg.mapper.McpServiceMapper;
import com.xeno.mcpg.mcp.DynamicMcpToolCallbackProvider;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebFluxStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registry for managing multiple MCP service endpoints.
 * <p>
 * Each MCP service is exposed as a separate endpoint at /mcp/{serviceCode}.
 */
@Component
public class McpMultiEndpointRegistry {

    private final ObjectMapper objectMapper;
    private final McpServiceMapper serviceMapper;
    private final DynamicMcpToolCallbackProvider dynamicTools;

    @Value("${spring.ai.mcp.server.name:mcp-gateway}")
    private String serverName;

    @Value("${spring.ai.mcp.server.version:1.0.0}")
    private String serverVersion;

    @Value("${spring.ai.mcp.server.streamable-http.keep-alive-interval:30s}")
    private Duration keepAliveInterval;

    @Value("${spring.ai.mcp.server.streamable-http.disallow-delete:true}")
    private boolean disallowDelete;

    private final AtomicReference<Map<String, ServiceMcpInstance>> instancesRef = new AtomicReference<>(Map.of());

    public McpMultiEndpointRegistry(ObjectMapper objectMapper,
                                    McpServiceMapper serviceMapper,
                                    DynamicMcpToolCallbackProvider dynamicTools) {
        this.objectMapper = objectMapper;
        this.serviceMapper = serviceMapper;
        this.dynamicTools = dynamicTools;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * Reload all MCP service endpoints.
     *
     * @return status map
     */
    public synchronized Map<String, Object> reload() {
        dynamicTools.forceReload();
        var services = serviceMapper.selectList(
                new LambdaQueryWrapper<McpServiceEntity>()
                        .eq(McpServiceEntity::getEnabled, true)
                        .orderByAsc(McpServiceEntity::getId)
        );

        Map<String, ServiceMcpInstance> next = new LinkedHashMap<>();
        for (McpServiceEntity service : services) {
            String serviceCode = service.getServiceCode();
            String endpoint = "/mcp/" + serviceCode;
            var transportProvider = buildTransportProvider(serviceCode, endpoint);
            var toolSpecs = McpToolUtils.toSyncToolSpecifications(dynamicTools.getToolCallbacksForService(serviceCode));
            McpSyncServer mcpSyncServer = McpServer.sync(transportProvider)
                    .serverInfo(new McpSchema.Implementation(serverName + "-" + serviceCode, serverVersion))
                    .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                    .tools(toolSpecs)
                    .build();
            next.put(serviceCode, new ServiceMcpInstance(serviceCode, endpoint, transportProvider, mcpSyncServer));
        }

        Map<String, ServiceMcpInstance> old = instancesRef.getAndSet(next);
        old.values().forEach(this::safeClose);
        return status();
    }

    /**
     * Route an incoming request to the appropriate MCP service.
     *
     * @param request the server request
     * @return the server response
     */
    public Mono<ServerResponse> route(ServerRequest request) {
        String serviceCode = request.pathVariable("serviceCode");
        ServiceMcpInstance instance = instancesRef.get().get(serviceCode);
        if (instance == null) {
            return ServerResponse.notFound().build();
        }
        return instance.transportProvider().getRouterFunction().route(request)
                .flatMap(handler -> {
                    @SuppressWarnings("unchecked")
                    var typedHandler = (org.springframework.web.reactive.function.server.HandlerFunction<ServerResponse>) handler;
                    return typedHandler.handle(request);
                })
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Get the current status of the registry.
     *
     * @return status map
     */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("serviceCount", instancesRef.get().size());
        status.put("serviceCodes", instancesRef.get().keySet());
        return status;
    }

    private WebFluxStreamableServerTransportProvider buildTransportProvider(String serviceCode, String endpoint) {
        return WebFluxStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .messageEndpoint(endpoint)
                .keepAliveInterval(keepAliveInterval)
                .disallowDelete(disallowDelete)
                .contextExtractor(request -> {
                    Map<String, Object> context = new LinkedHashMap<>();
                    context.put(DynamicMcpToolCallbackProvider.CTX_SERVICE_CODE_KEY, serviceCode);
                    context.put(DynamicMcpToolCallbackProvider.CTX_REQUEST_HEADERS_KEY,
                            request.headers().asHttpHeaders().toSingleValueMap());
                    return io.modelcontextprotocol.common.McpTransportContext.create(context);
                })
                .build();
    }

    private void safeClose(ServiceMcpInstance instance) {
        try {
            instance.server().closeGracefully();
        } catch (Exception ignore) {
        }
    }

    /**
     * Represents an MCP service instance with its transport and server.
     *
     * @param serviceCode      the service code
     * @param endpoint         the MCP endpoint path
     * @param transportProvider the transport provider
     * @param server           the MCP server instance
     */
    public record ServiceMcpInstance(
            String serviceCode,
            String endpoint,
            WebFluxStreamableServerTransportProvider transportProvider,
            McpSyncServer server) {
    }
}