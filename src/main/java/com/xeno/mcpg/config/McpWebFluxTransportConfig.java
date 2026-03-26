package com.xeno.mcpg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.RequestPredicates;

/**
 * MCP WebFlux transport configuration.
 */
@Configuration
public class McpWebFluxTransportConfig {

    /**
     * Create the router function for multi-endpoint MCP routing.
     *
     * @param registry the MCP multi-endpoint registry
     * @return the router function
     */
    @Bean
    public RouterFunction<?> multiMcpRouterFunction(McpMultiEndpointRegistry registry) {
        return RouterFunctions.route()
                .route(RequestPredicates.path("/mcp/{serviceCode}"), registry::route)
                .route(RequestPredicates.path("/mcp/{serviceCode}/**"), registry::route)
                .build();
    }
}