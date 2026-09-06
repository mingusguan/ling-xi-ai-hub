package com.lingxi.ai.agent.tools;

import com.lingxi.ai.service.IAiChatToolCallService;
import dev.langchain4j.agent.tool.Tool;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class XiaolingMcpTool {

    private static final int DEFAULT_MAX_RESULTS = 5;

    private static final double DEFAULT_MIN_SCORE = 0.7D;

    private final McpToolInvoker toolInvoker;

    private final IAiChatToolCallService toolCallService;

    public XiaolingMcpTool(McpToolInvoker toolInvoker, IAiChatToolCallService toolCallService) {
        this.toolInvoker = toolInvoker;
        this.toolCallService = toolCallService;
    }

    @Tool("通过知识库 MCP Server 检索企业知识库内容。适合回答制度、流程、文档资料等企业知识问题。")
    public String searchKnowledge(String query, Long deptId) {
        if (query == null || query.isBlank()) {
            return "请提供需要检索的知识库问题。";
        }
        Map<String, Object> arguments = toolInvoker.arguments();
        arguments.put("query", query);
        arguments.put("deptId", deptId);
        arguments.put("maxResults", DEFAULT_MAX_RESULTS);
        arguments.put("minScore", DEFAULT_MIN_SCORE);
        String failureMessage = "知识库查询失败，请稍后重试。";
        long startTime = System.currentTimeMillis();
        String result = toolInvoker.call("search_knowledge", arguments, failureMessage);
        String status = failureMessage.equals(result) ? "FAILURE" : "SUCCESS";
        // 小灵儿的知识库运营统计只记录知识库工具调用，不污染普通聊天消息。
        toolCallService.recordKnowledgeCall(AiChatToolCallContext.get(), query, arguments, result,
                System.currentTimeMillis() - startTime, status, "FAILURE".equals(status) ? result : null);
        return result;
    }

    @Tool("通过 OA MCP Server 查询用户待审批任务列表。")
    public String queryPendingTasks(Long userId) {
        Map<String, Object> arguments = toolInvoker.arguments();
        arguments.put("userId", userId);
        return toolInvoker.call("query_pending_tasks", arguments, "查询待审批任务失败，请稍后重试。");
    }

    @Tool("通过 OA MCP Server 查询用户假期余额，包括年假、调休假等。")
    public String queryLeaveBalance(Long userId) {
        Map<String, Object> arguments = toolInvoker.arguments();
        arguments.put("userId", userId);
        return toolInvoker.call("query_leave_balance", arguments, "查询假期余额失败，请稍后重试。");
    }

    @Tool("通过 OA MCP Server 查询用户近期报销记录和审批状态。")
    public String queryExpenseStatus(Long userId) {
        Map<String, Object> arguments = toolInvoker.arguments();
        arguments.put("userId", userId);
        return toolInvoker.call("query_expense_status", arguments, "查询报销状态失败，请稍后重试。");
    }

    @Tool("通过 OA MCP Server 查询用户超时未审批任务预警。")
    public String queryTimeoutWarning(Long userId) {
        Map<String, Object> arguments = toolInvoker.arguments();
        arguments.put("userId", userId);
        return toolInvoker.call("query_timeout_warning", arguments, "查询超时预警失败，请稍后重试。");
    }

    @Tool("通过 OA MCP Server 计算请假工作时长。日期格式支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss。")
    public String calculateLeaveDuration(String startDate, String endDate) {
        Map<String, Object> arguments = toolInvoker.arguments();
        arguments.put("startDate", startDate);
        arguments.put("endDate", endDate);
        return toolInvoker.call("calculate_leave_duration", arguments, "计算请假时长失败，请稍后重试。");
    }
}
