package com.lingxi.ai.agent.tools;

import com.alibaba.fastjson2.JSON;
import com.lingxi.ai.mcp.config.McpPlatformProperties.ToolDefinition;
import com.lingxi.ai.mcp.constant.McpMarketConstants;
import com.lingxi.ai.mcp.service.IMcpToolMarketService;
import com.lingxi.ai.mcp.service.McpDynamicClientRegistry;
import com.lingxi.ai.mcp.service.McpPlatformService;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 小灵儿 MCP 工具调用器，按工具市场的复用编排、授权结果和连接配置动态调用 MCP Server。
 */
@Component
public class McpToolInvoker {

    private static final Logger log = LoggerFactory.getLogger(McpToolInvoker.class);

    private final McpDynamicClientRegistry clientRegistry;

    private final McpPlatformService platformService;

    private final IMcpToolMarketService toolMarketService;

    public McpToolInvoker(McpDynamicClientRegistry clientRegistry, McpPlatformService platformService,
            IMcpToolMarketService toolMarketService) {
        this.clientRegistry = clientRegistry;
        this.platformService = platformService;
        this.toolMarketService = toolMarketService;
    }

    /**
     * 调用指定 MCP 工具。
     *
     * @param toolName MCP 工具名称
     * @param arguments 工具入参
     * @param failureMessage 失败时返回给助手的降级提示
     * @return MCP 工具返回文本
     */
    public String call(String toolName, Map<String, Object> arguments, String failureMessage) {
        if (!toolMarketService.isToolEnabledForAgent(McpMarketConstants.AGENT_CODE_XIAOLINGER, toolName)) {
            return "该工具未在小灵儿助手的 MCP 工具编排中启用，请先到 MCP 工具市场完成绑定。";
        }
        if (toolMarketService.isApprovalRequired(toolName) && !toolMarketService.hasGrantForCurrentUser(toolName)) {
            return "当前用户、部门或租户尚未获得该 MCP 工具的使用授权，请先在 MCP 工具市场提交授权申请。";
        }

        ToolDefinition definition = null;
        long startTime = System.currentTimeMillis();
        try {
            definition = platformService.beforeCall(toolName, arguments);
            McpSyncClient client = clientRegistry.getClient(toolName);
            CallToolResult result = client.callTool(new CallToolRequest(toolName, arguments));
            platformService.auditSuccess(definition, toolName, System.currentTimeMillis() - startTime);
            return formatResult(result);
        } catch (Exception e) {
            log.warn("Call MCP tool failed, toolName={}, arguments={}", toolName, arguments, e);
            platformService.auditFailure(definition, toolName, System.currentTimeMillis() - startTime, e);
            return failureMessage;
        }
    }

    private String formatResult(CallToolResult result) {
        if (result == null) {
            return "MCP 工具未返回结果。";
        }
        if (Boolean.TRUE.equals(result.isError())) {
            return "MCP 工具返回错误：" + JSON.toJSONString(result.content());
        }
        if (result.content() == null || result.content().isEmpty()) {
            return "MCP 工具返回空结果。";
        }

        StringBuilder builder = new StringBuilder();
        for (Object content : result.content()) {
            String text = extractText(content);
            if (text != null && !text.isBlank()) {
                builder.append(text).append('\n');
            }
        }

        String response = builder.toString().trim();
        return response.isBlank() ? JSON.toJSONString(result.content()) : response;
    }

    private String extractText(Object content) {
        if (content == null) {
            return null;
        }
        for (String methodName : List.of("text", "getText")) {
            try {
                Method method = content.getClass().getMethod(methodName);
                Object value = method.invoke(content);
                return value == null ? null : String.valueOf(value);
            } catch (ReflectiveOperationException ignored) {
                // Try the next known MCP text content accessor.
            }
        }
        return JSON.toJSONString(content);
    }

    public Map<String, Object> arguments() {
        return new LinkedHashMap<>();
    }
}
