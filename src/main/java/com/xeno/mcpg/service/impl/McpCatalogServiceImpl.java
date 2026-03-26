package com.xeno.mcpg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xeno.mcpg.dto.ApiToolParameter;
import com.xeno.mcpg.dto.admin.CreateMcpServiceRequest;
import com.xeno.mcpg.dto.admin.CreateMcpToolRequest;
import com.xeno.mcpg.dto.admin.UpdateMcpServiceRequest;
import com.xeno.mcpg.dto.admin.UpdateMcpToolRequest;
import com.xeno.mcpg.entity.McpServiceEntity;
import com.xeno.mcpg.entity.McpServiceToolBindingEntity;
import com.xeno.mcpg.entity.McpToolEntity;
import com.xeno.mcpg.mapper.McpServiceMapper;
import com.xeno.mcpg.mapper.McpServiceToolBindingMapper;
import com.xeno.mcpg.mapper.McpToolMapper;
import com.xeno.mcpg.service.McpCatalogService;
import com.xeno.mcpg.util.JsonCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link McpCatalogService}.
 */
@Service
public class McpCatalogServiceImpl implements McpCatalogService {

    private final McpServiceMapper serviceMapper;
    private final McpToolMapper toolMapper;
    private final McpServiceToolBindingMapper bindingMapper;
    private final JsonCodec jsonCodec;

    @Value("${app.public-base-url:http://127.0.0.1:8080}")
    private String publicBaseUrl;

    public McpCatalogServiceImpl(McpServiceMapper serviceMapper,
                                 McpToolMapper toolMapper,
                                 McpServiceToolBindingMapper bindingMapper,
                                 JsonCodec jsonCodec) {
        this.serviceMapper = serviceMapper;
        this.toolMapper = toolMapper;
        this.bindingMapper = bindingMapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public List<Map<String, Object>> listServices() {
        List<McpServiceToolBindingEntity> tools = bindingMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, Long> toolCountMap = new LinkedHashMap<>();
        for (McpServiceToolBindingEntity tool : tools) {
            toolCountMap.put(tool.getServiceId(), toolCountMap.getOrDefault(tool.getServiceId(), 0L) + 1);
        }
        return serviceMapper.selectList(new LambdaQueryWrapper<McpServiceEntity>().orderByAsc(McpServiceEntity::getId))
                .stream()
                .map(service -> toServiceDto(service, toolCountMap.getOrDefault(service.getId(), 0L)))
                .toList();
    }

    @Override
    public Map<String, Object> getService(Long serviceId) {
        McpServiceEntity service = getServiceEntity(serviceId);
        long toolCount = bindingMapper.selectCount(
                new LambdaQueryWrapper<McpServiceToolBindingEntity>()
                        .eq(McpServiceToolBindingEntity::getServiceId, serviceId)
        );
        return toServiceDto(service, toolCount);
    }

    @Override
    @Transactional
    public Map<String, Object> createService(CreateMcpServiceRequest request) {
        Long exists = serviceMapper.selectCount(
                new LambdaQueryWrapper<McpServiceEntity>()
                        .eq(McpServiceEntity::getServiceCode, request.serviceCode())
        );
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "serviceCode already exists");
        }
        McpServiceEntity service = new McpServiceEntity();
        service.setServiceCode(request.serviceCode());
        service.setServiceName(request.serviceName());
        service.setServiceDesc(request.serviceDesc());
        service.setProtocol(normalizeProtocol(request.protocol()));
        service.setEnabled(request.enabled() == null || request.enabled());
        service.setRequestHeaders(jsonCodec.toJson(defaultMap(request.requestHeaders())));
        serviceMapper.insert(service);
        return toServiceDto(service, 0L);
    }

    @Override
    @Transactional
    public Map<String, Object> updateService(Long serviceId, UpdateMcpServiceRequest request) {
        McpServiceEntity service = getServiceEntity(serviceId);
        if (request.serviceName() != null) {
            service.setServiceName(request.serviceName());
        }
        if (request.serviceDesc() != null) {
            service.setServiceDesc(request.serviceDesc());
        }
        if (request.protocol() != null) {
            service.setProtocol(normalizeProtocol(request.protocol()));
        }
        if (request.enabled() != null) {
            service.setEnabled(request.enabled());
        }
        if (request.requestHeaders() != null) {
            service.setRequestHeaders(jsonCodec.toJson(defaultMap(request.requestHeaders())));
        }
        serviceMapper.updateById(service);
        long toolCount = bindingMapper.selectCount(
                new LambdaQueryWrapper<McpServiceToolBindingEntity>()
                        .eq(McpServiceToolBindingEntity::getServiceId, serviceId)
        );
        return toServiceDto(service, toolCount);
    }

    @Override
    @Transactional
    public void updateServiceStatus(Long serviceId, boolean enabled) {
        McpServiceEntity service = getServiceEntity(serviceId);
        service.setEnabled(enabled);
        serviceMapper.updateById(service);
    }

    @Override
    @Transactional
    public void deleteService(Long serviceId) {
        getServiceEntity(serviceId);
        bindingMapper.delete(new LambdaQueryWrapper<McpServiceToolBindingEntity>()
                .eq(McpServiceToolBindingEntity::getServiceId, serviceId));
        serviceMapper.deleteById(serviceId);
    }

    @Override
    public List<Map<String, Object>> listTools(Long serviceId) {
        getServiceEntity(serviceId);
        Map<Long, McpToolEntity> toolMap = loadToolMap();
        return bindingMapper.selectList(
                        new LambdaQueryWrapper<McpServiceToolBindingEntity>()
                                .eq(McpServiceToolBindingEntity::getServiceId, serviceId)
                                .orderByAsc(McpServiceToolBindingEntity::getId))
                .stream()
                .map(binding -> toBindingDto(binding, toolMap.get(binding.getToolId())))
                .toList();
    }

    @Override
    public Map<String, Object> getTool(Long toolId) {
        McpServiceToolBindingEntity binding = getBindingEntity(toolId);
        McpToolEntity tool = getToolEntity(binding.getToolId());
        return toBindingDto(binding, tool);
    }

    @Override
    @Transactional
    public Map<String, Object> createTool(Long serviceId, CreateMcpToolRequest request) {
        getServiceEntity(serviceId);
        McpToolEntity tool = toolMapper.selectOne(
                new LambdaQueryWrapper<McpToolEntity>()
                        .eq(McpToolEntity::getToolCode, request.toolCode())
                        .last("LIMIT 1")
        );
        if (tool == null) {
            tool = new McpToolEntity();
            tool.setToolCode(request.toolCode());
        }
        tool.setToolName(request.toolName());
        tool.setToolDesc(request.toolDesc());
        tool.setHttpMethod(normalizeMethod(request.httpMethod()));
        tool.setApiUrl(request.apiUrl());
        tool.setParametersJson(jsonCodec.toJson(defaultList(request.parameters())));
        tool.setRequestHeaders(jsonCodec.toJson(defaultMap(request.requestHeaders())));
        tool.setEnabled(request.enabled() == null || request.enabled());
        if (tool.getId() == null) {
            toolMapper.insert(tool);
        } else {
            toolMapper.updateById(tool);
        }
        McpToolEntity savedTool = tool;

        McpServiceToolBindingEntity binding = bindingMapper.selectOne(
                new LambdaQueryWrapper<McpServiceToolBindingEntity>()
                        .eq(McpServiceToolBindingEntity::getServiceId, serviceId)
                        .eq(McpServiceToolBindingEntity::getToolId, savedTool.getId())
                        .last("LIMIT 1")
        );
        if (binding == null) {
            binding = new McpServiceToolBindingEntity();
        }
        binding.setServiceId(serviceId);
        binding.setToolId(savedTool.getId());
        if (binding.getId() == null) {
            bindingMapper.insert(binding);
        } else {
            bindingMapper.updateById(binding);
        }
        McpServiceToolBindingEntity savedBinding = binding;
        return toBindingDto(savedBinding, savedTool);
    }

    @Override
    @Transactional
    public Map<String, Object> updateTool(Long toolId, UpdateMcpToolRequest request) {
        McpServiceToolBindingEntity binding = getBindingEntity(toolId);
        McpToolEntity tool = getToolEntity(binding.getToolId());
        if (request.toolCode() != null) {
            tool.setToolCode(request.toolCode());
        }
        if (request.toolName() != null) {
            tool.setToolName(request.toolName());
        }
        if (request.toolDesc() != null) {
            tool.setToolDesc(request.toolDesc());
        }
        if (request.httpMethod() != null) {
            tool.setHttpMethod(normalizeMethod(request.httpMethod()));
        }
        if (request.apiUrl() != null) {
            tool.setApiUrl(request.apiUrl());
        }
        if (request.parameters() != null) {
            tool.setParametersJson(jsonCodec.toJson(defaultList(request.parameters())));
        }
        if (request.requestHeaders() != null) {
            tool.setRequestHeaders(jsonCodec.toJson(defaultMap(request.requestHeaders())));
        }
        if (request.enabled() != null) {
            tool.setEnabled(request.enabled());
        }
        toolMapper.updateById(tool);
        bindingMapper.updateById(binding);
        return toBindingDto(binding, tool);
    }

    @Override
    @Transactional
    public void updateToolStatus(Long toolId, boolean enabled) {
        McpServiceToolBindingEntity binding = getBindingEntity(toolId);
        McpToolEntity tool = getToolEntity(binding.getToolId());
        tool.setEnabled(enabled);
        toolMapper.updateById(tool);
    }

    @Override
    @Transactional
    public void deleteTool(Long toolId) {
        McpServiceToolBindingEntity binding = getBindingEntity(toolId);
        bindingMapper.deleteById(binding.getId());
    }

    @Override
    public Map<String, Object> buildServiceConfig(String serviceCode) {
        McpServiceEntity service = serviceMapper.selectOne(
                new LambdaQueryWrapper<McpServiceEntity>()
                        .eq(McpServiceEntity::getServiceCode, serviceCode)
                        .last("LIMIT 1")
        );
        if (service == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "service not found");
        }
        Map<Long, McpToolEntity> toolMap = loadToolMap();
        List<Map<String, Object>> tools = bindingMapper.selectList(
                        new LambdaQueryWrapper<McpServiceToolBindingEntity>()
                                .eq(McpServiceToolBindingEntity::getServiceId, service.getId())
                                .orderByAsc(McpServiceToolBindingEntity::getId))
                .stream()
                .map(binding -> {
                    McpToolEntity tool = toolMap.get(binding.getToolId());
                    return Map.<String, Object>of(
                            "bindingId", binding.getId(),
                            "toolCode", tool == null ? "" : tool.getToolCode(),
                            "toolName", tool == null ? "" : tool.getToolName(),
                            "enabled", tool != null && Boolean.TRUE.equals(tool.getEnabled()),
                            "httpMethod", tool == null ? "" : tool.getHttpMethod(),
                            "apiUrl", tool == null ? "" : tool.getApiUrl()
                    );
                })
                .toList();

        Map<String, Object> mcpJson = new LinkedHashMap<>();
        mcpJson.put("serviceCode", service.getServiceCode());
        mcpJson.put("serviceName", service.getServiceName());
        mcpJson.put("description", Optional.ofNullable(service.getServiceDesc()).orElse(""));
        mcpJson.put("protocol", service.getProtocol());
        mcpJson.put("status", Boolean.TRUE.equals(service.getEnabled()) ? "ENABLED" : "DISABLED");
        mcpJson.put("createdAt", service.getCreatedAt());
        mcpJson.put("requestHeaders", jsonCodec.toStringMap(service.getRequestHeaders()));
        mcpJson.put("mcpUrl", publicBaseUrl + "/mcp/" + service.getServiceCode());
        mcpJson.put("tools", tools);
        return mcpJson;
    }

    @Override
    public List<Map<String, Object>> listServiceConnections() {
        return serviceMapper.selectList(new LambdaQueryWrapper<McpServiceEntity>().orderByAsc(McpServiceEntity::getId))
                .stream()
                .map(service -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("serviceCode", service.getServiceCode());
                    item.put("serviceName", service.getServiceName());
                    item.put("protocol", service.getProtocol());
                    item.put("status", Boolean.TRUE.equals(service.getEnabled()) ? "ENABLED" : "DISABLED");
                    item.put("mcpUrl", publicBaseUrl + "/mcp/" + service.getServiceCode());
                    item.put("requestHeaders", jsonCodec.toStringMap(service.getRequestHeaders()));
                    return item;
                }).toList();
    }

    @Override
    public CatalogSnapshot loadEnabledCatalog() {
        List<McpServiceEntity> services = serviceMapper.selectList(
                new LambdaQueryWrapper<McpServiceEntity>()
                        .eq(McpServiceEntity::getEnabled, true)
                        .orderByAsc(McpServiceEntity::getId)
        );
        List<CatalogToolBinding> bindings = new ArrayList<>();
        LocalDateTime latestUpdatedAt = LocalDateTime.MIN;
        Map<Long, McpToolEntity> toolMap = loadEnabledToolMap();

        for (McpServiceEntity service : services) {
            latestUpdatedAt = maxTime(latestUpdatedAt, service.getUpdatedAt());
            Map<String, String> serviceHeaders = jsonCodec.toStringMap(service.getRequestHeaders());
            List<McpServiceToolBindingEntity> serviceBindings = bindingMapper.selectList(
                    new LambdaQueryWrapper<McpServiceToolBindingEntity>()
                            .eq(McpServiceToolBindingEntity::getServiceId, service.getId())
                            .orderByAsc(McpServiceToolBindingEntity::getId)
            );
            for (McpServiceToolBindingEntity binding : serviceBindings) {
                McpToolEntity tool = toolMap.get(binding.getToolId());
                if (tool == null || !Boolean.TRUE.equals(tool.getEnabled())) {
                    continue;
                }
                latestUpdatedAt = maxTime(latestUpdatedAt, tool.getUpdatedAt());
                latestUpdatedAt = maxTime(latestUpdatedAt, binding.getUpdatedAt());
                bindings.add(new CatalogToolBinding(
                        service.getServiceCode(),
                        service.getServiceName(),
                        service.getProtocol(),
                        binding.getId(),
                        tool.getToolCode(),
                        tool.getToolName(),
                        tool.getToolDesc(),
                        normalizeMethod(tool.getHttpMethod()),
                        tool.getApiUrl(),
                        jsonCodec.toList(tool.getParametersJson(), new TypeReference<List<ApiToolParameter>>() {
                        }),
                        mergeHeaders(serviceHeaders, jsonCodec.toStringMap(tool.getRequestHeaders()))
                ));
            }
        }
        String signature = services.size() + ":" + bindings.size() + ":" + latestUpdatedAt;
        return new CatalogSnapshot(bindings, signature);
    }

    private Map<String, String> mergeHeaders(Map<String, String> serviceHeaders, Map<String, String> toolHeaders) {
        Map<String, String> merged = new LinkedHashMap<>(defaultMap(serviceHeaders));
        merged.putAll(defaultMap(toolHeaders));
        return merged;
    }

    private LocalDateTime maxTime(LocalDateTime t1, LocalDateTime t2) {
        if (t1 == null) {
            return t2;
        }
        if (t2 == null) {
            return t1;
        }
        return t1.isAfter(t2) ? t1 : t2;
    }

    private String normalizeProtocol(String protocol) {
        if (protocol == null || protocol.isBlank()) {
            return "STATELESS";
        }
        return protocol.trim().toUpperCase();
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            return "POST";
        }
        return method.trim().toUpperCase();
    }

    private McpServiceEntity getServiceEntity(Long serviceId) {
        McpServiceEntity entity = serviceMapper.selectById(serviceId);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "service not found");
        }
        return entity;
    }

    private McpToolEntity getToolEntity(Long toolId) {
        McpToolEntity entity = toolMapper.selectById(toolId);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "tool not found");
        }
        return entity;
    }

    private McpServiceToolBindingEntity getBindingEntity(Long bindingId) {
        McpServiceToolBindingEntity entity = bindingMapper.selectById(bindingId);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "binding not found");
        }
        return entity;
    }

    private Map<Long, McpToolEntity> loadToolMap() {
        Map<Long, McpToolEntity> map = new HashMap<>();
        for (McpToolEntity tool : toolMapper.selectList(new LambdaQueryWrapper<>())) {
            map.put(tool.getId(), tool);
        }
        return map;
    }

    private Map<Long, McpToolEntity> loadEnabledToolMap() {
        Map<Long, McpToolEntity> map = new HashMap<>();
        for (McpToolEntity tool : toolMapper.selectList(
                new LambdaQueryWrapper<McpToolEntity>()
                        .eq(McpToolEntity::getEnabled, true)
                        .orderByAsc(McpToolEntity::getId))) {
            map.put(tool.getId(), tool);
        }
        return map;
    }

    private Map<String, Object> toServiceDto(McpServiceEntity entity, long toolCount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", entity.getId());
        item.put("serviceCode", entity.getServiceCode());
        item.put("serviceName", entity.getServiceName());
        item.put("serviceDesc", entity.getServiceDesc());
        item.put("protocol", entity.getProtocol());
        item.put("status", Boolean.TRUE.equals(entity.getEnabled()) ? "ENABLED" : "DISABLED");
        item.put("createdAt", entity.getCreatedAt());
        item.put("updatedAt", entity.getUpdatedAt());
        item.put("requestHeaders", jsonCodec.toStringMap(entity.getRequestHeaders()));
        item.put("toolCount", toolCount);
        item.put("mcpUrl", publicBaseUrl + "/mcp/" + entity.getServiceCode());
        return item;
    }

    private Map<String, Object> toBindingDto(McpServiceToolBindingEntity binding, McpToolEntity tool) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", binding.getId());
        item.put("serviceId", binding.getServiceId());
        item.put("toolId", binding.getToolId());
        item.put("toolCode", tool == null ? null : tool.getToolCode());
        item.put("toolName", tool == null ? null : tool.getToolName());
        item.put("toolDesc", tool == null ? null : tool.getToolDesc());
        item.put("httpMethod", tool == null ? null : tool.getHttpMethod());
        item.put("apiUrl", tool == null ? null : tool.getApiUrl());
        item.put("status", tool != null && Boolean.TRUE.equals(tool.getEnabled()) ? "ENABLED" : "DISABLED");
        item.put("requestHeaders", jsonCodec.toStringMap(tool == null ? null : tool.getRequestHeaders()));
        item.put("parameters", jsonCodec.toList(tool == null ? null : tool.getParametersJson(), new TypeReference<List<ApiToolParameter>>() {
        }));
        item.put("createdAt", binding.getCreatedAt());
        item.put("updatedAt", binding.getUpdatedAt());
        return item;
    }

    private <T> Map<String, T> defaultMap(Map<String, T> source) {
        return source == null ? new LinkedHashMap<>() : source;
    }

    private <T> List<T> defaultList(List<T> source) {
        return source == null ? List.of() : source;
    }
}