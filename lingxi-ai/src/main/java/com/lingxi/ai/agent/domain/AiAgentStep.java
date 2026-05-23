package com.lingxi.ai.agent.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 智能体编排步骤，用于描述业务助手执行时的工具、人工审批和输出约束。
 */
@Data
@TableName("ai_agent_step")
public class AiAgentStep {

    /** 步骤主键。 */
    @TableId(type = IdType.AUTO)
    private Long stepId;

    /** 所属智能体编码。 */
    private String agentCode;

    /** 步骤序号。 */
    private Integer stepOrder;

    /** 步骤名称。 */
    private String stepName;

    /** 步骤类型：PROMPT、TOOL、APPROVAL、OUTPUT。 */
    private String stepType;

    /** 绑定的MCP工具名称，步骤类型为TOOL时使用。 */
    private String toolName;

    /** 步骤提示词或执行说明。 */
    private String instruction;

    /** 步骤配置JSON。 */
    private String configJson;

    /** 失败处理策略：STOP、CONTINUE、MANUAL_REVIEW。 */
    private String failurePolicy;

    /** 是否启用：1 是，0 否。 */
    private String enabled;

    /** 创建人。 */
    private String createBy;

    /** 创建时间。 */
    private Date createTime;

    /** 更新人。 */
    private String updateBy;

    /** 更新时间。 */
    private Date updateTime;
}
