package com.lingxi.ai.agent.tools;

import com.alibaba.fastjson2.JSON;
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

    private final Map<String, McpSyncClient> toolClientCache = new ConcurrentHashMap<>();

    public McpToolInvoker(List<McpSyncClient> mcpClients) {
        this.mcpClients = mcpClients;
    }

    public String call(String toolName, Map<String, Object> arguments, String failureMessage) {
        if (mcpClients == null || mcpClients.isEmpty()) {
            return "MCP client is not configured.";
        }

        try {
            McpSyncClient client = findClient(toolName);
            CallToolResult result = client.callTool(new CallToolRequest(toolName, arguments));
            return formatResult(result);
        } catch (Exception e) {
            log.warn("Call MCP tool failed, toolName={}, arguments={}", toolName, arguments, e);
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
        throw new IllegalStateException("No MCP server exposes tool: " + toolName);
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
            return "MCP tool returned no result.";
        }
        if (Boolean.TRUE.equals(result.isError())) {
            return "MCP tool returned an error: " + JSON.toJSONString(result.content());
        }
        if (result.content() == null || result.content().isEmpty()) {
            return "MCP tool returned an empty result.";
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
