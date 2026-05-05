package com.lingxi.ai.mcp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

/**
 * MCP 工具授权申请实体，记录用户为本人、部门或租户申请工具使用权的审批过程。
 */
@TableName("ai_mcp_tool_application")
public class McpToolApplication {

    /** 申请主键。 */
    @TableId(type = IdType.AUTO)
    private Long applicationId;

    /** 申请使用的工具主键。 */
    private Long toolId;

    /** 申请使用的工具名称。 */
    private String toolName;

    /** 申请人用户 ID。 */
    private Long applicantUserId;

    /** 申请人账号。 */
    private String applicantName;

    /** 申请人部门 ID。 */
    private Long applicantDeptId;

    /** 申请人租户 ID。 */
    private String tenantId;

    /** 授权范围类型：USER、DEPT、TENANT。 */
    private String grantType;

    /** 授权目标 ID，按授权范围分别表示用户 ID、部门 ID 或租户 ID。 */
    private String grantTargetId;

    /** 授权目标名称，用于审批和审计展示。 */
    private String grantTargetName;

    /** 申请用途说明。 */
    private String purpose;

    /** 申请状态：PENDING、APPROVED、REJECTED。 */
    private String status;

    /** 审批人用户 ID。 */
    private Long approverUserId;

    /** 审批人账号。 */
    private String approverName;

    /** 审批意见。 */
    private String approvalComment;

    /** 审批时间。 */
    private Date approvalTime;

    /** 创建时间。 */
    private Date createTime;

    /** 更新时间。 */
    private Date updateTime;

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
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

    public Long getApplicantUserId() {
        return applicantUserId;
    }

    public void setApplicantUserId(Long applicantUserId) {
        this.applicantUserId = applicantUserId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public Long getApplicantDeptId() {
        return applicantDeptId;
    }

    public void setApplicantDeptId(Long applicantDeptId) {
        this.applicantDeptId = applicantDeptId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getApproverUserId() {
        return approverUserId;
    }

    public void setApproverUserId(Long approverUserId) {
        this.approverUserId = approverUserId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public String getApprovalComment() {
        return approvalComment;
    }

    public void setApprovalComment(String approvalComment) {
        this.approvalComment = approvalComment;
    }

    public Date getApprovalTime() {
        return approvalTime;
    }

    public void setApprovalTime(Date approvalTime) {
        this.approvalTime = approvalTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
