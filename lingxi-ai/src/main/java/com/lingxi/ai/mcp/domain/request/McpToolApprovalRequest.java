package com.lingxi.ai.mcp.domain.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * MCP 工具申请审批请求，承载审批动作和审批意见。
 */
@Data
public class McpToolApprovalRequest {

    /** 申请主键。 */
    @NotNull(message = "申请ID不能为空")
    private Long applicationId;

    /** 审批意见。 */
    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String approvalComment;

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getApprovalComment() {
        return approvalComment;
    }

    public void setApprovalComment(String approvalComment) {
        this.approvalComment = approvalComment;
    }
}
