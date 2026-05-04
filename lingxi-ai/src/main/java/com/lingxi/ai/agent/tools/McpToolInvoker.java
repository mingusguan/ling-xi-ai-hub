package com.lingxi.ai.agent.tools;

import com.alibaba.fastjson2.JSON;
import com.lingxi.ai.mcp.config.McpPlatformProperties.ToolDefinition;
import com.lingxi.ai.mcp.service.McpPlatformService;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class McpToolInvoker {

    private static final Logger log = LoggerFactory.getLogger(McpToolInvoker.class);

    private final List<McpSyncClient> mcpClients;

    private final McpPlatformService platformService;

    private final Map<String, McpSyncClient> toolClientCache = new ConcurrentHashMap<>();

    public McpToolInvoker(List<McpSyncClient> mcpClients, McpPlatformService platformService) {
        this.mcpClients = mcpClients;
        this.platformService = platformService;
    }

    public String call(String toolName, Map<String, Object> arguments, String failureMessage) {
        if (mcpClients == null || mcpClients.isEmpty()) {
            return "MCP 客户端未配置。";
        }

        ToolDefinition definition = null;
        long startTime = System.currentTimeMillis();
        try {
            definition = platformService.beforeCall(toolName, arguments);
            McpSyncClient client = findClient(toolName);
            CallToolResult result = client.callTool(new CallToolRequest(toolName, arguments));
            platformService.auditSuccess(definition, toolName, System.currentTimeMillis() - startTime);
            return formatResult(result);
        } catch (Exception e) {
            log.warn("Call MCP tool failed, toolName={}, arguments={}", toolName, arguments, e);
            platformService.auditFailure(definition, toolName, System.currentTimeMillis() - startTime, e);
            return failureMessage;
        }
    }

    private McpSyncClient findClient(String toolName) {
        McpSyncClient cachedClient = toolClientCache.get(toolName);
        if (cachedClient != null) {
            return cachedClient;
        }

        for (McpSyncClient client : mcpClients) {
            if (hasTool(client, toolName)) {
                toolClientCache.put(toolName, client);
                return client;
            }
        }
        throw new IllegalStateException("没有 MCP Server 暴露该工具：" + toolName);
    }

    private boolean hasTool(McpSyncClient client, String toolName) {
        McpSchema.ListToolsResult result = client.listTools();
        if (result == null || result.tools() == null) {
            return false;
        }
        return result.tools().stream().anyMatch(tool -> toolName.equals(tool.name()));
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
