package com.lingxi.ai.mcp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * MCP 工具调用审计日志，记录工具、用户、租户、耗时和失败原因。
 */
@Data
@TableName("ai_mcp_tool_audit_log")
public class McpToolAuditLog {

    /** 审计日志主键。 */
    @TableId(type = IdType.AUTO)
    private Long logId;

    /** MCP 工具名称。 */
    private String toolName;

    /** MCP Server 名称。 */
    private String serverName;

    /** 调用时工具版本。 */
    private String version;

    /** 调用用户 ID。 */
    private Long userId;

    /** 调用用户账号。 */
    private String username;

    /** 调用用户部门 ID。 */
    private Long deptId;

    /** 调用租户 ID。 */
    private String tenantId;

    /** 调用状态：SUCCESS 或 FAILURE。 */
    private String status;

    /** 调用耗时，单位毫秒。 */
    private Long costMillis;

    /** 失败原因，成功时为空。 */
    private String errorMessage;

    /** 调用发生时间。 */
    private Date createTime;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCostMillis() {
        return costMillis;
    }

    public void setCostMillis(Long costMillis) {
        this.costMillis = costMillis;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
