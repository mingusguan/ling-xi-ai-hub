package com.lingxi.ai.mcp.domain;

import java.util.List;

/**
 * MCP 工具市场展示对象。
 *
 * <p>该对象只面向管理端或工具市场列表输出，隐藏底层 MCP Client 实例和调用细节。</p>
 */
public class McpToolMarketplaceItem {

    /** MCP 工具名称，和 Server 暴露的 tool name 一致。 */
    private String name;

    /** 工具展示名称。 */
    private String displayName;

    /** 工具能力说明。 */
    private String description;

    /** 工具归属的 MCP Server 名称。 */
    private String serverName;

    /** 工具版本号。 */
    private String version;

    /** 工具分类，例如 knowledge、oa。 */
    private String category;

    /** 工具发布或维护团队。 */
    private String ownerTeam;

    /** 调用该工具需要具备的权限码。 */
    private String requiredPermission;

    /** 工具是否启用。 */
    private boolean enabled;

    /** 工具是否开启审计。 */
    private boolean auditEnabled;

    /** 允许访问该工具的租户集合；为空表示不做租户限制。 */
    private List<String> tenantIds;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    public List<String> getTenantIds() {
        return tenantIds;
    }

    public void setTenantIds(List<String> tenantIds) {
        this.tenantIds = tenantIds;
    }
}
