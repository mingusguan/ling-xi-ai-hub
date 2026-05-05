package com.lingxi.ai.mcp.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * MCP 工具发布和编辑请求，承载工具市场注册信息。
 */
@Data
public class McpToolSaveRequest {

    /** 工具注册主键，新增时为空。 */
    private Long toolId;

    /** MCP 工具真实名称，需要和服务端暴露名称一致。 */
    @NotBlank(message = "工具名称不能为空")
    @Size(max = 100, message = "工具名称不能超过100个字符")
    private String toolName;

    /** 工具中文展示名称。 */
    @NotBlank(message = "展示名称不能为空")
    @Size(max = 100, message = "展示名称不能超过100个字符")
    private String displayName;

    /** 工具能力描述。 */
    @Size(max = 1000, message = "工具描述不能超过1000个字符")
    private String description;

    /** MCP Server 名称。 */
    @NotBlank(message = "服务名称不能为空")
    private String serverName;

    /** MCP Server 基础地址。 */
    private String serverUrl;

    /** MCP Server SSE 端点。 */
    private String sseEndpoint;

    /** 工具分类。 */
    private String category;

    /** 工具版本号。 */
    @NotBlank(message = "版本号不能为空")
    private String version;

    /** 负责团队。 */
    private String ownerTeam;

    /** 负责人用户 ID。 */
    private Long ownerUserId;

    /** 调用权限标识。 */
    private String requiredPermission;

    /** 租户 ID 列表，多个值用英文逗号分隔。 */
    private String tenantIds;

    /** 部门 ID 列表，多个值用英文逗号分隔。 */
    private String deptIds;

    /** 工具状态。 */
    private String status;

    /** 是否开启审计，1 是，0 否。 */
    private String auditEnabled;

    /** 是否在市场展示，1 是，0 否。 */
    private String marketplaceVisible;

    /** 是否需要申请授权后才能调用，1 是，0 否。 */
    private String approvalRequired;

    /** 输入参数 Schema。 */
    private String inputSchema;

    /** 输出结果 Schema。 */
    private String outputSchema;

    /** 示例用法。 */
    private String exampleUsage;

    /** 工具标签。 */
    private String tags;

    /** 发布说明。 */
    private String releaseNotes;

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
}
