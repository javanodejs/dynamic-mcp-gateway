package com.xeno.mcpg.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health probe controller for service monitoring.
 */
@RestController
public class HealthProbeController {

    /**
     * Health check endpoint.
     *
     * @return health status
     */
    @GetMapping("/health")
    public Map<String, Object> healthz() {
        return Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        );
    }
}