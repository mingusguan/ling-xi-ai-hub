package com.lingxi.ai.mapper;

import com.lingxi.ai.mcp.domain.McpToolBinding;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MCP 工具编排绑定 Mapper。
 */
public interface McpToolBindingMapper {

    /**
     * 查询助手或业务场景的工具绑定列表。
     *
     * @param agentCode 助手或业务场景编码
     * @return 工具绑定列表
     */
    List<McpToolBinding> selectBindingList(@Param("agentCode") String agentCode);

    /**
     * 根据主键查询工具绑定。
     *
     * @param bindingId 绑定主键
     * @return 工具绑定
     */
    McpToolBinding selectBindingById(Long bindingId);

    /**
     * 查询指定助手和工具的绑定关系。
     *
     * @param agentCode 助手或业务场景编码
     * @param toolId 工具主键
     * @return 工具绑定
     */
    McpToolBinding selectBindingByAgentAndTool(@Param("agentCode") String agentCode, @Param("toolId") Long toolId);

    /**
     * 查询指定助手和工具名称是否存在启用状态的绑定关系。
     *
     * @param agentCode 助手或业务场景编码
     * @param toolName MCP 工具名称
     * @return 启用绑定数量
     */
    int countEnabledBindingByAgentAndToolName(@Param("agentCode") String agentCode, @Param("toolName") String toolName);

    /**
     * 新增工具绑定。
     *
     * @param binding 工具绑定
     * @return 影响行数
     */
    int insertBinding(McpToolBinding binding);

    /**
     * 更新工具绑定。
     *
     * @param binding 工具绑定
     * @return 影响行数
     */
    int updateBinding(McpToolBinding binding);

    /**
     * 删除工具绑定。
     *
     * @param bindingId 绑定主键
     * @return 影响行数
     */
    int deleteBindingById(Long bindingId);
}
