package com.lingxi.ai.mapper;

import com.lingxi.ai.mcp.domain.McpToolApplication;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MCP 工具权限申请 Mapper。
 */
public interface McpToolApplicationMapper {

    /**
     * 查询工具权限申请列表。
     *
     * @param toolName 工具名称
     * @param status 申请状态
     * @return 权限申请列表
     */
    List<McpToolApplication> selectApplicationList(@Param("toolName") String toolName, @Param("status") String status);

    /**
     * 根据主键查询工具权限申请。
     *
     * @param applicationId 申请主键
     * @return 工具权限申请
     */
    McpToolApplication selectApplicationById(Long applicationId);

    /**
     * 查询用户对指定工具的待审批申请。
     *
     * @param toolId 工具主键
     * @param applicantUserId 申请人用户 ID
     * @return 待审批申请
     */
    McpToolApplication selectPendingByToolAndTarget(@Param("toolId") Long toolId,
            @Param("grantType") String grantType, @Param("grantTargetId") String grantTargetId);

    /**
     * 新增工具权限申请。
     *
     * @param application 工具权限申请
     * @return 影响行数
     */
    int insertApplication(McpToolApplication application);

    /**
     * 更新工具权限申请。
     *
     * @param application 工具权限申请
     * @return 影响行数
     */
    int updateApplication(McpToolApplication application);

    /**
     * 统计指定状态的申请数量。
     *
     * @param status 申请状态
     * @return 申请数量
     */
    Long countApplicationsByStatus(@Param("status") String status);
}
