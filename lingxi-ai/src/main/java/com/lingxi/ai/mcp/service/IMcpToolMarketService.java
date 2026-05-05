package com.lingxi.ai.mcp.service;

import com.lingxi.ai.mcp.domain.McpToolApplication;
import com.lingxi.ai.mcp.domain.McpToolAuditLog;
import com.lingxi.ai.mcp.domain.McpToolBinding;
import com.lingxi.ai.mcp.domain.McpToolRegistry;
import com.lingxi.ai.mcp.domain.request.McpToolApplyRequest;
import com.lingxi.ai.mcp.domain.request.McpToolApprovalRequest;
import com.lingxi.ai.mcp.domain.request.McpToolBindingRequest;
import com.lingxi.ai.mcp.domain.request.McpToolQueryRequest;
import com.lingxi.ai.mcp.domain.request.McpToolSaveRequest;
import com.lingxi.ai.mcp.domain.vo.McpGrantScopeOptionVO;
import com.lingxi.ai.mcp.domain.vo.McpToolMarketStatsVO;
import java.util.List;

/**
 * MCP 工具市场服务，承载工具发现、发布、申请审批、审计监控和编排复用能力。
 */
public interface IMcpToolMarketService {

    /**
     * 查询工具市场注册列表。
     *
     * @param query 查询条件
     * @return 工具注册列表
     */
    List<McpToolRegistry> listTools(McpToolQueryRequest query);

    /**
     * 查询工具注册详情。
     *
     * @param toolId 工具注册主键
     * @return 工具注册详情
     */
    McpToolRegistry getTool(Long toolId);

    /**
     * 保存工具注册信息，新增时默认进入草稿状态。
     *
     * @param request 保存请求
     * @return 影响行数
     */
    int saveTool(McpToolSaveRequest request);

    /**
     * 发布工具，使其进入市场可发现状态。
     *
     * @param toolId 工具注册主键
     * @return 影响行数
     */
    int publishTool(Long toolId);

    /**
     * 停用工具，阻止后续使用。
     *
     * @param toolId 工具注册主键
     * @return 影响行数
     */
    int disableTool(Long toolId);

    /**
     * 废弃工具，用于版本治理中的旧版本退出。
     *
     * @param toolId 工具注册主键
     * @return 影响行数
     */
    int deprecateTool(Long toolId);

    /**
     * 提交工具权限申请。
     *
     * @param request 申请请求
     * @return 影响行数
     */
    int applyTool(McpToolApplyRequest request);

    /**
     * 查询当前登录用户可申请的工具授权范围。
     *
     * @return 授权范围选项
     */
    List<McpGrantScopeOptionVO> listGrantScopeOptions();

    /**
     * 查询工具权限申请列表。
     *
     * @param toolName 工具名称
     * @param status 申请状态
     * @return 权限申请列表
     */
    List<McpToolApplication> listApplications(String toolName, String status);

    /**
     * 通过工具权限申请。
     *
     * @param request 审批请求
     * @return 影响行数
     */
    int approveApplication(McpToolApprovalRequest request);

    /**
     * 拒绝工具权限申请。
     *
     * @param request 审批请求
     * @return 影响行数
     */
    int rejectApplication(McpToolApprovalRequest request);

    /**
     * 查询工具调用审计日志。
     *
     * @param toolName 工具名称
     * @param status 调用状态
     * @param tenantId 租户 ID
     * @return 调用审计日志
     */
    List<McpToolAuditLog> listAuditLogs(String toolName, String status, String tenantId);

    /**
     * 查询工具市场监控统计。
     *
     * @return 市场统计视图
     */
    McpToolMarketStatsVO getStats();

    /**
     * 查询某个助手或业务场景的工具编排绑定。
     *
     * @param agentCode 助手或业务场景编码
     * @return 编排绑定列表
     */
    List<McpToolBinding> listBindings(String agentCode);

    /**
     * 保存工具编排绑定。
     *
     * @param request 编排绑定请求
     * @return 影响行数
     */
    int saveBinding(McpToolBindingRequest request);

    /**
     * 删除工具编排绑定。
     *
     * @param bindingId 编排绑定主键
     * @return 影响行数
     */
    int deleteBinding(Long bindingId);

    /**
     * 判断指定助手或业务场景是否启用了某个 MCP 工具。
     *
     * @param agentCode 助手或业务场景编码
     * @param toolName MCP 工具名称
     * @return true 表示允许调用
     */
    boolean isToolEnabledForAgent(String agentCode, String toolName);

    /**
     * 判断当前登录上下文是否已获得指定 MCP 工具的使用授权。
     *
     * @param toolName MCP 工具名称
     * @return true 表示允许调用
     */
    boolean hasGrantForCurrentUser(String toolName);

    /**
     * 判断指定 MCP 工具是否需要申请授权。
     *
     * @param toolName MCP 工具名称
     * @return true 表示需要检查授权申请审批结果
     */
    boolean isApprovalRequired(String toolName);

    /**
     * 刷新运行时 MCP Client 连接缓存，使工具市场中的服务地址和 SSE 端点配置立即生效。
     */
    void refreshClientRegistry();
}
