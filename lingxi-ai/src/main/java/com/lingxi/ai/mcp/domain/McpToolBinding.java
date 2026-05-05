package com.lingxi.ai.mcp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * MCP 工具编排绑定实体，用于描述某个助手或业务场景可复用哪些工具。
 */
@Data
@TableName("ai_mcp_tool_binding")
public class McpToolBinding {

    /** 编排绑定主键。 */
    @TableId(type = IdType.AUTO)
    private Long bindingId;

    /** 助手或业务场景编码，例如 xiaolinger。 */
    private String agentCode;

    /** 工具注册主键。 */
    private Long toolId;

    /** MCP 工具名称。 */
    private String toolName;

    /** 绑定启用状态，1 表示启用，0 表示停用。 */
    private String enabled;

    /** 编排参数 JSON，用于保存工具别名、提示词约束或参数映射。 */
    private String configJson;

    /** 创建者账号。 */
    private String createBy;

    /** 创建时间。 */
    private Date createTime;

    /** 更新者账号。 */
    private String updateBy;

    /** 更新时间。 */
    private Date updateTime;

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

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
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
