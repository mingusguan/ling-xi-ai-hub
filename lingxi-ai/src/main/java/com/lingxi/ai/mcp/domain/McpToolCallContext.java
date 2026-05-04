package com.lingxi.ai.mcp.domain;

/**
 * MCP 工具调用上下文。
 *
 * <p>用于在权限校验、租户隔离和审计日志中传递当前调用人的身份信息。
 * 当前上下文来自登录态和请求头，不直接参与 MCP 协议传输。</p>
 */
public class McpToolCallContext {

    /** 当前登录用户 ID。 */
    private Long userId;

    /** 当前登录用户名。 */
    private String username;

    /** 当前登录用户所属部门 ID，用于审计和业务可见范围辅助判断。 */
    private Long deptId;

    /** 当前租户标识，默认从请求头 X-Tenant-Id 获取。 */
    private String tenantId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
