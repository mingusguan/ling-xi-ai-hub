package com.lingxi.ai.mcp.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * MCP 工具授权申请请求，承载申请人希望为本人、部门或租户开通工具使用权的业务用途。
 */
public class McpToolApplyRequest {

    /** 申请使用的工具主键。 */
    @NotNull(message = "工具ID不能为空")
    private Long toolId;

    /** 授权范围类型：USER、DEPT、TENANT。 */
    @NotBlank(message = "授权范围不能为空")
    private String grantType;

    /** 申请用途说明。 */
    @Size(max = 500, message = "申请用途不能超过500个字符")
    private String purpose;

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
