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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * MCP 动态客户端注册表，负责从工具市场数据库配置创建和复用 MCP Client。
 *
 * <p>该服务是小灵儿运行时调用 MCP 的连接入口，连接地址、SSE 端点和工具归属全部来自
 * ai_mcp_tool_registry，避免继续依赖 Spring 配置文件中的静态 MCP Server 列表。</p>
 */
@Slf4j
@Service
public class McpDynamicClientRegistry {

    private static final String DEFAULT_SSE_ENDPOINT = "/mcp/sse";

    private static final Set<String> MONOLITH_MCP_SERVERS = Set.of("knowledge", "oa");

    private static final Set<Integer> LEGACY_MICROSERVICE_PORTS = Set.of(9800, 9810);

    private final McpToolRegistryMapper registryMapper;

    private final McpPlatformProperties properties;

    private final Environment environment;

    private final Map<String, McpSyncClient> serverClientCache = new ConcurrentHashMap<>();

    public McpDynamicClientRegistry(McpToolRegistryMapper registryMapper, McpPlatformProperties properties,
            Environment environment) {
        this.registryMapper = registryMapper;
        this.properties = properties;
        this.environment = environment;
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
        String serverUrl = resolveServerUrl(registry);
        if (StringUtils.isBlank(serverUrl)) {
            throw new ServiceException("MCP 工具未配置服务地址：" + toolName);
        }
        validateServerUrl(serverUrl, toolName);
    }

    private McpSyncClient createClient(McpToolRegistry registry) {
        String serverUrl = resolveServerUrl(registry);
        String sseEndpoint = StringUtils.defaultIfEmpty(registry.getSseEndpoint(), DEFAULT_SSE_ENDPOINT);
        log.info("Initializing MCP client, toolName={}, serverName={}, serverUrl={}, sseEndpoint={}, timeout={}",
                registry.getToolName(), registry.getServerName(), serverUrl, sseEndpoint,
                properties.getDefaultTimeout());
        try {
            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(serverUrl)
                    .sseEndpoint(sseEndpoint)
                    .build();
            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(properties.getDefaultTimeout())
                    .clientInfo(new McpSchema.Implementation("lingxi-ai-dynamic-mcp-client", "1.0.0"))
                    .build();
            client.initialize();
            log.info("MCP client initialized, toolName={}, serverName={}, serverUrl={}, sseEndpoint={}",
                    registry.getToolName(), registry.getServerName(), serverUrl, sseEndpoint);
            return client;
        } catch (Exception e) {
            log.warn("MCP client initialization failed, toolName={}, serverName={}, serverUrl={}, sseEndpoint={}, error={}",
                    registry.getToolName(), registry.getServerName(), serverUrl, sseEndpoint, e.getMessage(), e);
            throw new ServiceException("MCP Server 连接失败：toolName=" + registry.getToolName()
                    + "，serverName=" + registry.getServerName()
                    + "，serverUrl=" + serverUrl
                    + "，sseEndpoint=" + sseEndpoint
                    + "，error=" + e.getMessage());
        }
    }

    private void assertServerExposesTool(McpSyncClient client, String toolName) {
        McpSchema.ListToolsResult result = client.listTools();
        if (result == null || result.tools() == null
                || result.tools().stream().noneMatch(tool -> toolName.equals(tool.name()))) {
            throw new ServiceException("MCP Server 未暴露该工具：" + toolName);
        }
    }

    private String buildCacheKey(McpToolRegistry registry) {
        return registry.getServerName() + "|" + resolveServerUrl(registry) + "|"
                + StringUtils.defaultIfEmpty(registry.getSseEndpoint(), DEFAULT_SSE_ENDPOINT);
    }

    private String resolveServerUrl(McpToolRegistry registry) {
        String serverUrl = registry.getServerUrl();
        if (!isMonolithMcpServer(registry.getServerName())) {
            return serverUrl;
        }
        if (StringUtils.isBlank(serverUrl) || isLegacyMicroserviceUrl(serverUrl)) {
            String resolvedUrl = currentMonolithServerUrl();
            if (!resolvedUrl.equals(serverUrl)) {
                log.info("Resolved monolith MCP server URL, toolName={}, serverName={}, originalUrl={}, resolvedUrl={}",
                        registry.getToolName(), registry.getServerName(), serverUrl, resolvedUrl);
            }
            return resolvedUrl;
        }
        return serverUrl;
    }

    private boolean isMonolithMcpServer(String serverName) {
        return serverName != null && MONOLITH_MCP_SERVERS.contains(serverName);
    }

    private boolean isLegacyMicroserviceUrl(String serverUrl) {
        try {
            URI uri = new URI(serverUrl);
            String host = uri.getHost();
            return ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host))
                    && LEGACY_MICROSERVICE_PORTS.contains(uri.getPort());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private String currentMonolithServerUrl() {
        String port = environment.getProperty("server.port", "8080");
        return "http://127.0.0.1:" + port;
    }

    private void validateServerUrl(String serverUrl, String toolName) {
        try {
            URI uri = new URI(serverUrl);
            if (StringUtils.isBlank(uri.getScheme()) || StringUtils.isBlank(uri.getHost())) {
                throw new ServiceException("MCP 工具服务地址格式不正确：" + toolName + "，serverUrl=" + serverUrl);
            }
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new ServiceException("MCP 工具服务地址仅支持 HTTP/HTTPS：" + toolName + "，serverUrl=" + serverUrl);
            }
        } catch (URISyntaxException e) {
            throw new ServiceException("MCP 工具服务地址格式不正确：" + toolName + "，serverUrl=" + serverUrl);
        }
    }

    private void closeQuietly(McpSyncClient client) {
        try {
            client.close();
        } catch (Exception ignored) {
            // Refresh must not fail because an old MCP connection is already closed.
        }
    }
}
