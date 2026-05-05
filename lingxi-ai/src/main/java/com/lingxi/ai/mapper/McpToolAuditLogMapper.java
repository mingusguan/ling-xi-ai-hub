package com.lingxi.ai.mapper;

import com.lingxi.ai.mcp.domain.McpToolAuditLog;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MCP 工具调用审计日志 Mapper。
 */
public interface McpToolAuditLogMapper {

    /**
     * 查询工具调用审计日志列表。
     *
     * @param toolName 工具名称
     * @param status 调用状态
     * @param tenantId 租户 ID
     * @return 审计日志列表
     */
    List<McpToolAuditLog> selectAuditLogList(@Param("toolName") String toolName, @Param("status") String status,
            @Param("tenantId") String tenantId);

    /**
     * 新增工具调用审计日志。
     *
     * @param auditLog 审计日志
     * @return 影响行数
     */
    int insertAuditLog(McpToolAuditLog auditLog);

    /**
     * 统计今日调用次数。
     *
     * @param status 调用状态，空值表示统计全部
     * @return 今日调用次数
     */
    Long countTodayCalls(@Param("status") String status);

    /**
     * 统计今日平均调用耗时。
     *
     * @return 今日平均耗时，单位毫秒
     */
    Long selectTodayAverageCostMillis();
}
