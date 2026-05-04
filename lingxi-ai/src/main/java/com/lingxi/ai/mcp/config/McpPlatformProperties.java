package com.lingxi.ai.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 平台统一配置。
 *
 * <p>承载 MCP Server 注册信息、工具市场元数据、权限码、租户范围和开关配置。
 * 该类只负责配置绑定，不承载调用、权限校验或审计逻辑。</p>
 */
@Component
@ConfigurationProperties(prefix = "lingxi.mcp")
public class McpPlatformProperties {

    /** MCP 平台总开关，关闭后所有受管 MCP 工具不可调用。 */
    private boolean enabled = true;

    /** MCP 工具调用的默认超时时间，供后续统一客户端配置或网关层复用。 */
    private Duration defaultTimeout = Duration.ofSeconds(20);

    /** 已纳入平台治理的 MCP Server 列表。 */
    private List<ServerDefinition> servers = new ArrayList<>();

    /** 已发布到平台的 MCP 工具定义，key 为 MCP tool name。 */
    private Map<String, ToolDefinition> tools = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(Duration defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public List<ServerDefinition> getServers() {
        return servers;
    }

    public void setServers(List<ServerDefinition> servers) {
        this.servers = servers;
    }

    public Map<String, ToolDefinition> getTools() {
        return tools;
    }

    public void setTools(Map<String, ToolDefinition> tools) {
        this.tools = tools;
    }

    public ToolDefinition getTool(String toolName) {
        return tools == null ? null : tools.get(toolName);
    }

    /**
     * 按服务名称查找已注册的 MCP Server。
     *
     * @param serverName MCP Server 配置名称
     * @return 匹配的服务定义；不存在时返回 null
     */
    public ServerDefinition getServer(String serverName) {
        if (servers == null) {
            return null;
        }
        return servers.stream()
                .filter(server -> serverName != null && serverName.equals(server.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * MCP Server 注册信息。
     *
     * <p>用于统一管理服务地址、访问令牌、版本、归属团队和租户可见范围。</p>
     */
    public static class ServerDefinition {

        /** 平台内唯一服务名，需要和 tool.serverName 对齐。 */
        private String name;

        /** 面向管理端展示的服务名称。 */
        private String displayName;

        /** 服务级开关，关闭后该服务下所有工具不可调用。 */
        private boolean enabled = true;

        /** MCP Server 基础地址。 */
        private String url;

        /** MCP Server SSE 连接端点。 */
        private String sseEndpoint = "/mcp/sse";

        /** 访问 MCP Server 的令牌占位，生产环境应从配置中心或环境变量注入。 */
        private String token;

        /** MCP Server 版本号，用于治理、审计和兼容性判断。 */
        private String version = "1.0.0";

        /** 服务归属团队，用于工具市场展示和责任边界划分。 */
        private String ownerTeam;

        /** 允许访问该服务的租户集合；为空表示不做租户限制。 */
        private List<String> tenantIds = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSseEndpoint() {
            return sseEndpoint;
        }

        public void setSseEndpoint(String sseEndpoint) {
            this.sseEndpoint = sseEndpoint;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getOwnerTeam() {
            return ownerTeam;
        }

        public void setOwnerTeam(String ownerTeam) {
            this.ownerTeam = ownerTeam;
        }

        public List<String> getTenantIds() {
            return tenantIds;
        }

        public void setTenantIds(List<String> tenantIds) {
            this.tenantIds = tenantIds;
        }
    }

    /**
     * MCP 工具发布定义。
     *
     * <p>用于描述单个 MCP 工具的权限码、版本、分类、审计策略和工具市场展示信息。</p>
     */
    public static class ToolDefinition {

        /** MCP 工具名称，需要和 Server 暴露的 tool name 一致。 */
        private String name;

        /** 面向管理端和工具市场展示的工具名称。 */
        private String displayName;

        /** 工具能力说明，用于工具市场展示和治理审阅。 */
        private String description;

        /** 工具归属的 MCP Server 名称。 */
        private String serverName;

        /** 工具级开关，关闭后该工具不可调用。 */
        private boolean enabled = true;

        /** 工具版本号，用于兼容性治理和审计追踪。 */
        private String version = "1.0.0";

        /** 工具分类，例如 knowledge、oa。 */
        private String category;

        /** 工具发布或维护团队。 */
        private String ownerTeam;

        /** 调用该工具需要具备的权限码；为空表示不做权限校验。 */
        private String requiredPermission;

        /** 允许调用该工具的租户集合；为空表示不做租户限制。 */
        private List<String> tenantIds = new ArrayList<>();

        /** 是否记录该工具的调用审计日志。 */
        private boolean auditEnabled = true;

        /** 是否在工具市场中展示。 */
        private boolean marketplaceVisible = true;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getOwnerTeam() {
            return ownerTeam;
        }

        public void setOwnerTeam(String ownerTeam) {
            this.ownerTeam = ownerTeam;
        }

        public String getRequiredPermission() {
            return requiredPermission;
        }

        public void setRequiredPermission(String requiredPermission) {
            this.requiredPermission = requiredPermission;
        }

        public List<String> getTenantIds() {
            return tenantIds;
        }

        public void setTenantIds(List<String> tenantIds) {
            this.tenantIds = tenantIds;
        }

        public boolean isAuditEnabled() {
            return auditEnabled;
        }

        public void setAuditEnabled(boolean auditEnabled) {
            this.auditEnabled = auditEnabled;
        }

        public boolean isMarketplaceVisible() {
            return marketplaceVisible;
        }

        public void setMarketplaceVisible(boolean marketplaceVisible) {
            this.marketplaceVisible = marketplaceVisible;
        }
    }
}
