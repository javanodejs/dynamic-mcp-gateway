package com.xeno.mcpg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xeno.mcpg.entity.McpToolEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for MCP tool entities.
 */
@Mapper
public interface McpToolMapper extends BaseMapper<McpToolEntity> {
}