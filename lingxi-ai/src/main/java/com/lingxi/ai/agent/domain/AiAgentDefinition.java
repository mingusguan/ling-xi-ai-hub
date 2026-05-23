package com.lingxi.ai.agent.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 企业智能体定义，描述一个可配置业务助手的角色、提示词和发布状态。
 */
@Data
@TableName("ai_agent_definition")
public class AiAgentDefinition {

    /** 智能体主键。 */
    @TableId(type = IdType.AUTO)
    private Long agentId;

    /** 智能体编码，作为编排和MCP工具绑定的业务标识。 */
    private String agentCode;

    /** 智能体展示名称。 */
    private String agentName;

    /** 适用业务场景，如 HR、FINANCE、IT。 */
    private String businessScene;

    /** 智能体职责说明。 */
    private String description;

    /** 系统提示词。 */
    private String systemPrompt;

    /** 回答约束和安全边界。 */
    private String guardrails;

    /** 状态：DRAFT、PUBLISHED、DISABLED。 */
    private String status;

    /** 所属团队。 */
    private String ownerTeam;

    /** 是否开启治理审计：1 是，0 否。 */
    private String auditEnabled;

    /** 创建人。 */
    private String createBy;

    /** 创建时间。 */
    private Date createTime;

    /** 更新人。 */
    private String updateBy;

    /** 更新时间。 */
    private Date updateTime;
}
