package com.lingxi.ai.mapper;

import com.lingxi.ai.mcp.domain.McpToolGrant;
import org.apache.ibatis.annotations.Param;

/**
 * MCP 工具授权 Mapper。
 */
public interface McpToolGrantMapper {

    /**
     * 新增或更新工具授权。
     *
     * @param grant 工具授权
     * @return 影响行数
     */
    int upsertGrant(McpToolGrant grant);

    /**
     * 查询指定工具和目标是否存在生效授权。
     *
     * @param toolName MCP 工具名称
     * @param grantType 授权范围类型
     * @param grantTargetId 授权目标 ID
     * @return 授权数量
     */
    int countActiveGrant(@Param("toolName") String toolName, @Param("grantType") String grantType,
            @Param("grantTargetId") String grantTargetId);
}
