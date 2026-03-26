package com.xeno.mcpg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.server.autoconfigure.McpServerSseWebFluxAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpServerStatelessWebFluxAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebFluxAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerStatelessAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Dynamic MCP Gateway Application.
 * <p>
 * A multi-tenant MCP (Model Context Protocol) gateway that provides dynamic tool management
 * with runtime configuration capabilities.
 *
 * @author Xeno Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication(exclude = {
        McpServerAutoConfiguration.class,
        McpServerStatelessAutoConfiguration.class,
        McpServerSseWebFluxAutoConfiguration.class,
        McpServerStatelessWebFluxAutoConfiguration.class,
        McpServerStreamableHttpWebFluxAutoConfiguration.class
})
public class DynamicMcpGatewayApplication {

    @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp/{serviceCode}}")
    private String mcpEndpoint;

    public static void main(String[] args) {
        SpringApplication.run(DynamicMcpGatewayApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("Dynamic MCP Gateway started successfully. Endpoint template: {}", mcpEndpoint);
    }
}