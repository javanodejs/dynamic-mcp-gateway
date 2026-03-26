package com.xeno.mcpg.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xeno.mcpg.service.ConfiguredApiExecutor;
import com.xeno.mcpg.service.McpCatalogService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of {@link ConfiguredApiExecutor}.
 */
@Service
public class ConfiguredApiExecutorImpl implements ConfiguredApiExecutor {

    private final WebClient webClient;

    public ConfiguredApiExecutorImpl(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Override
    public String execute(McpCatalogService.CatalogToolBinding binding,
                          Map<String, Object> runtimeInput,
                          Map<String, String> passthroughHeaders) {
        Map<String, Object> requestData = runtimeInput == null ? new LinkedHashMap<>() : new LinkedHashMap<>(runtimeInput);
        return callApi(binding.httpMethod(), binding.apiUrl(), mergeHeaders(binding.headers(), passthroughHeaders), requestData)
                .map(this::formatResponse)
                .onErrorResume(e -> Mono.just("API call failed: " + e.getMessage()))
                .block(Duration.ofSeconds(30));
    }

    private Mono<JsonNode> callApi(String method,
                                   String apiUrl,
                                   Map<String, String> headers,
                                   Map<String, Object> bodyOrQuery) {
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        String finalUrl = buildFinalUrl(apiUrl, httpMethod, bodyOrQuery);
        WebClient.RequestBodySpec req = webClient.method(httpMethod).uri(finalUrl);

        if (headers != null) {
            headers.forEach(req::header);
        }
        if (isBodyMethod(httpMethod)) {
            req.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        }

        if (!isBodyMethod(httpMethod)) {
            return req.retrieve().bodyToMono(JsonNode.class);
        }
        Map<String, Object> body = bodyOrQuery == null ? Map.of() : bodyOrQuery;
        return req.bodyValue(body).retrieve().bodyToMono(JsonNode.class);
    }

    private String buildFinalUrl(String apiUrl, HttpMethod method, Map<String, Object> bodyOrQuery) {
        if (method != HttpMethod.GET && method != HttpMethod.DELETE) {
            return apiUrl;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl);
        bodyOrQuery.forEach((k, v) -> {
            if (v != null) {
                builder.queryParam(k, String.valueOf(v));
            }
        });
        return builder.toUriString();
    }

    private boolean isBodyMethod(HttpMethod method) {
        return method == HttpMethod.POST
                || method == HttpMethod.PUT
                || method == HttpMethod.PATCH;
    }

    private String formatResponse(JsonNode result) {
        if (result == null || result.isNull()) {
            return "API call successful";
        }
        JsonNode codeNode = result.get("code");
        if (codeNode != null && codeNode.canConvertToInt() && codeNode.asInt() != 0) {
            JsonNode msgNode = result.get("msg");
            return msgNode == null ? "API call failed" : msgNode.asText();
        }
        return "API call successful\n```json\n" + result.toPrettyString() + "\n```";
    }

    private Map<String, String> mergeHeaders(Map<String, String> configuredHeaders,
                                             Map<String, String> passthroughHeaders) {
        Map<String, String> merged = new LinkedHashMap<>();
        Map<String, String> keyIndex = new LinkedHashMap<>();

        // 1) Passthrough headers from MCP client connection
        if (passthroughHeaders != null) {
            passthroughHeaders.forEach((k, v) -> putHeaderIgnoreCase(merged, keyIndex, k, v));
        }

        // 2) Tool configured headers take precedence (case-insensitive)
        if (configuredHeaders != null) {
            configuredHeaders.forEach((k, v) -> putHeaderIgnoreCase(merged, keyIndex, k, v));
        }

        return merged;
    }

    private void putHeaderIgnoreCase(Map<String, String> target,
                                     Map<String, String> keyIndex,
                                     String key,
                                     String value) {
        if (key == null || value == null) {
            return;
        }
        String lower = key.toLowerCase();
        if (shouldSkipHeader(lower)) {
            return;
        }

        String existedKey = keyIndex.get(lower);
        if (existedKey != null) {
            target.remove(existedKey);
        }
        target.put(key, value);
        keyIndex.put(lower, key);
    }

    private boolean shouldSkipHeader(String lowerHeader) {
        return "host".equals(lowerHeader)
                || "content-length".equals(lowerHeader)
                || "connection".equals(lowerHeader)
                || lowerHeader.startsWith("mcp-");
    }
}