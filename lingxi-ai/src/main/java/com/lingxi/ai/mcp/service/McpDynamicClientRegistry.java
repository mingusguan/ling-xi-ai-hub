package com.lingxi.ai.mcp.service;

import com.lingxi.ai.mapper.McpToolRegistryMapper;
import com.lingxi.ai.mcp.config.McpPlatformProperties;
import com.lingxi.ai.mcp.constant.McpMarketConstants;
import com.lingxi.ai.mcp.domain.McpToolRegistry;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.utils.StringUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * MCP 动态客户端注册表，负责从工具市场数据库配置创建和复用 MCP Client。
 *
 * <p>该服务是小灵儿运行时调用 MCP 的连接入口，连接地址、SSE 端点和工具归属全部来自
 * ai_mcp_tool_registry，避免继续依赖 Spring 配置文件中的静态 MCP Server 列表。</p>
 */
@Service
public class McpDynamicClientRegistry {

    private static final String DEFAULT_SSE_ENDPOINT = "/mcp/sse";

    private final McpToolRegistryMapper registryMapper;

    private final McpPlatformProperties properties;

    private final Map<String, McpSyncClient> serverClientCache = new ConcurrentHashMap<>();

    public McpDynamicClientRegistry(McpToolRegistryMapper registryMapper, McpPlatformProperties properties) {
        this.registryMapper = registryMapper;
        this.properties = properties;
    }

    /**
     * 按工具名称获取可调用的 MCP Client。
     *
     * @param toolName MCP 工具名称
     * @return 已初始化的 MCP 同步客户端
     */
    public McpSyncClient getClient(String toolName) {
        McpToolRegistry registry = registryMapper.selectRegistryByName(toolName);
        validateRegistry(registry, toolName);
        McpSyncClient client = serverClientCache.computeIfAbsent(buildCacheKey(registry), key -> createClient(registry));
        assertServerExposesTool(client, toolName);
        return client;
    }

    /**
     * 清空运行时 MCP Client 缓存，后续调用将按数据库最新配置重新建连。
     */
    public void refresh() {
        serverClientCache.values().forEach(this::closeQuietly);
        serverClientCache.clear();
    }

    @PreDestroy
    public void destroy() {
        refresh();
    }

    private void validateRegistry(McpToolRegistry registry, String toolName) {
        if (registry == null) {
            throw new ServiceException("MCP 工具未注册：" + toolName);
        }
        if (!McpMarketConstants.TOOL_STATUS_PUBLISHED.equals(registry.getStatus())) {
            throw new ServiceException("MCP 工具未发布：" + toolName);
        }
        if (StringUtils.isBlank(registry.getServerName())) {
            throw new ServiceException("MCP 工具未配置服务名称：" + toolName);
        }
        if (StringUtils.isBlank(registry.getServerUrl())) {
            throw new ServiceException("MCP 工具未配置服务地址：" + toolName);
        }
    }

    private McpSyncClient createClient(McpToolRegistry registry) {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(registry.getServerUrl())
                .sseEndpoint(StringUtils.defaultIfEmpty(registry.getSseEndpoint(), DEFAULT_SSE_ENDPOINT))
                .build();
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(properties.getDefaultTimeout())
                .clientInfo(new McpSchema.Implementation("lingxi-ai-dynamic-mcp-client", "1.0.0"))
                .build();
        client.initialize();
        return client;
    }

    private void assertServerExposesTool(McpSyncClient client, String toolName) {
        McpSchema.ListToolsResult result = client.listTools();
        if (result == null || result.tools() == null
                || result.tools().stream().noneMatch(tool -> toolName.equals(tool.name()))) {
            throw new ServiceException("MCP Server 未暴露该工具：" + toolName);
        }
    }

    private String buildCacheKey(McpToolRegistry registry) {
        return registry.getServerName() + "|" + registry.getServerUrl() + "|"
                + StringUtils.defaultIfEmpty(registry.getSseEndpoint(), DEFAULT_SSE_ENDPOINT);
    }

    private void closeQuietly(McpSyncClient client) {
        try {
            client.close();
        } catch (Exception ignored) {
            // Refresh must not fail because an old MCP connection is already closed.
        }
    }
}
