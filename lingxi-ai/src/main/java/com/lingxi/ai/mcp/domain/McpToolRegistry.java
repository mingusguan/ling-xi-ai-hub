package com.lingxi.ai.mcp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * MCP 工具注册表实体，承载工具市场中的发现、发布、版本和隔离配置。
 */
@Data
@TableName("ai_mcp_tool_registry")
public class McpToolRegistry {

    /** 工具注册主键。 */
    @TableId(type = IdType.AUTO)
    private Long toolId;

    /** MCP 工具真实名称，需要和 MCP Server 暴露的 tool name 保持一致。 */
    private String toolName;

    /** 工具在市场中展示的中文名称。 */
    private String displayName;

    /** 工具能力说明，用于助手选择工具和前端市场展示。 */
    private String description;

    /** 工具所属 MCP Server 名称。 */
    private String serverName;

    /** MCP Server 基础地址。 */
    private String serverUrl;

    /** MCP Server SSE 连接端点。 */
    private String sseEndpoint;

    /** 协议类型，例如 MCP_SSE。 */
    private String protocol;

    /** 工具分类，例如知识库、OA、流程、数据查询。 */
    private String category;

    /** 当前发布版本号。 */
    private String version;

    /** 工具负责团队。 */
    private String ownerTeam;

    /** 工具负责人用户 ID。 */
    private Long ownerUserId;

    /** 调用工具需要具备的权限标识。 */
    private String requiredPermission;

    /** 允许访问的租户 ID 列表，多个值用英文逗号分隔，空值表示不限制。 */
    private String tenantIds;

    /** 允许访问的部门 ID 列表，多个值用英文逗号分隔，空值表示不限制。 */
    private String deptIds;

    /** 工具状态：DRAFT、PUBLISHED、DISABLED、DEPRECATED。 */
    private String status;

    /** 是否启用调用审计，1 表示启用，0 表示关闭。 */
    private String auditEnabled;

    /** 是否在工具市场展示，1 表示展示，0 表示隐藏。 */
    private String marketplaceVisible;

    /** 是否需要申请授权后才能调用，1 表示需要，0 表示不需要。 */
    private String approvalRequired;

    /** 输入参数 Schema，通常保存 JSON Schema 或示例 JSON。 */
    private String inputSchema;

    /** 输出结果 Schema，通常保存 JSON Schema 或示例 JSON。 */
    private String outputSchema;

    /** 示例用法，帮助业务方理解调用场景。 */
    private String exampleUsage;

    /** 工具标签，多个标签用英文逗号分隔。 */
    private String tags;

    /** 当前版本发布说明。 */
    private String releaseNotes;

    /** 创建者账号。 */
    private String createBy;

    /** 创建时间。 */
    private Date createTime;

    /** 更新者账号。 */
    private String updateBy;

    /** 更新时间。 */
    private Date updateTime;

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
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

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getSseEndpoint() {
        return sseEndpoint;
    }

    public void setSseEndpoint(String sseEndpoint) {
        this.sseEndpoint = sseEndpoint;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    public String getTenantIds() {
        return tenantIds;
    }

    public void setTenantIds(String tenantIds) {
        this.tenantIds = tenantIds;
    }

    public String getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(String deptIds) {
        this.deptIds = deptIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuditEnabled() {
        return auditEnabled;
    }

    public void setAuditEnabled(String auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    public String getMarketplaceVisible() {
        return marketplaceVisible;
    }

    public void setMarketplaceVisible(String marketplaceVisible) {
        this.marketplaceVisible = marketplaceVisible;
    }

    public String getApprovalRequired() {
        return approvalRequired;
    }

    public void setApprovalRequired(String approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }

    public String getExampleUsage() {
        return exampleUsage;
    }

    public void setExampleUsage(String exampleUsage) {
        this.exampleUsage = exampleUsage;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public void setReleaseNotes(String releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
