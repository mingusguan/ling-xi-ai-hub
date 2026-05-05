package com.lingxi.ai.mapper;

import com.lingxi.ai.mcp.domain.McpToolRegistry;
import com.lingxi.ai.mcp.domain.request.McpToolQueryRequest;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MCP 工具注册表 Mapper。
 */
public interface McpToolRegistryMapper {

    /**
     * 查询工具市场注册列表。
     *
     * @param query 查询条件
     * @return 工具注册列表
     */
    List<McpToolRegistry> selectRegistryList(McpToolQueryRequest query);

    /**
     * 根据主键查询工具注册信息。
     *
     * @param toolId 工具注册主键
     * @return 工具注册信息
     */
    McpToolRegistry selectRegistryById(Long toolId);

    /**
     * 根据工具名称查询注册信息。
     *
     * @param toolName MCP 工具名称
     * @return 工具注册信息
     */
    McpToolRegistry selectRegistryByName(String toolName);

    /**
     * 新增工具注册信息。
     *
     * @param registry 工具注册信息
     * @return 影响行数
     */
    int insertRegistry(McpToolRegistry registry);

    /**
     * 更新工具注册信息。
     *
     * @param registry 工具注册信息
     * @return 影响行数
     */
    int updateRegistry(McpToolRegistry registry);

    /**
     * 更新工具状态。
     *
     * @param toolId 工具注册主键
     * @param status 目标状态
     * @param updateBy 更新者账号
     * @return 影响行数
     */
    int updateRegistryStatus(@Param("toolId") Long toolId, @Param("status") String status,
            @Param("updateBy") String updateBy);

    /**
     * 统计指定状态的工具数量。
     *
     * @param status 工具状态，空值表示统计全部
     * @return 工具数量
     */
    Long countToolsByStatus(@Param("status") String status);
}
