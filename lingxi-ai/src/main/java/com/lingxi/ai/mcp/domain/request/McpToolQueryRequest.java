package com.lingxi.ai.mcp.domain.request;

import lombok.Data;

/**
 * MCP 工具市场查询请求，承载前端筛选条件。
 */
@Data
public class McpToolQueryRequest {

    /** 工具名称，支持按真实名称或展示名称模糊查询。 */
    private String keyword;

    /** MCP Server 名称。 */
    private String serverName;

    /** 工具分类。 */
    private String category;

    /** 工具状态：DRAFT、PUBLISHED、DISABLED、DEPRECATED。 */
    private String status;

    /** 工具所属租户 ID。 */
    private String tenantId;

    /** 工具所属部门 ID。 */
    private Long deptId;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
}
