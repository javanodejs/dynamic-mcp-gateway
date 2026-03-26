package com.xeno.mcpg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xeno.mcpg.entity.McpServiceEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for MCP service entities.
 */
@Mapper
public interface McpServiceMapper extends BaseMapper<McpServiceEntity> {
}