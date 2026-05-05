package com.lingxi.ai.mcp.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * MCP 工具编排绑定请求，描述某个助手或场景需要复用的工具。
 */
@Data
public class McpToolBindingRequest {

    /** 编排绑定主键，新增时为空。 */
    private Long bindingId;

    /** 助手或业务场景编码。 */
    @NotBlank(message = "助手编码不能为空")
    @Size(max = 64, message = "助手编码不能超过64个字符")
    private String agentCode;

    /** 工具注册主键。 */
    @NotNull(message = "工具ID不能为空")
    private Long toolId;

    /** 是否启用绑定，1 是，0 否。 */
    private String enabled;

    /** 编排配置 JSON。 */
    @Size(max = 2000, message = "编排配置不能超过2000个字符")
    private String configJson;

    public Long getBindingId() {
        return bindingId;
    }

    public void setBindingId(Long bindingId) {
        this.bindingId = bindingId;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }
}
