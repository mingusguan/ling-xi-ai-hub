package com.lingxi.ai.mcp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

/**
 * MCP 工具授权实体，记录审批通过后某个用户、部门或租户获得的工具使用权。
 */
@TableName("ai_mcp_tool_grant")
public class McpToolGrant {

    /** 授权主键。 */
    @TableId(type = IdType.AUTO)
    private Long grantId;

    /** 工具注册主键。 */
    private Long toolId;

    /** MCP 工具名称。 */
    private String toolName;

    /** 授权范围类型：USER、DEPT、TENANT。 */
    private String grantType;

    /** 授权目标 ID，按授权范围分别表示用户 ID、部门 ID 或租户 ID。 */
    private String grantTargetId;

    /** 授权目标名称，用于管理端展示。 */
    private String grantTargetName;

    /** 来源申请主键。 */
    private Long applicationId;

    /** 授权状态：ACTIVE、REVOKED。 */
    private String status;

    /** 授权过期时间，为空表示不过期。 */
    private Date expireTime;

    /** 创建人账号。 */
    private String createBy;

    /** 创建时间。 */
    private Date createTime;

    /** 更新人账号。 */
    private String updateBy;

    /** 更新时间。 */
    private Date updateTime;

    public Long getGrantId() {
        return grantId;
    }

    public void setGrantId(Long grantId) {
        this.grantId = grantId;
    }

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

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getGrantTargetId() {
        return grantTargetId;
    }

    public void setGrantTargetId(String grantTargetId) {
        this.grantTargetId = grantTargetId;
    }

    public String getGrantTargetName() {
        return grantTargetName;
    }

    public void setGrantTargetName(String grantTargetName) {
        this.grantTargetName = grantTargetName;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
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
