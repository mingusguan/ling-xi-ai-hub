package com.lingxi.ai.agent.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能体保存请求。
 */
@Data
public class AiAgentSaveRequest {

    /** 智能体主键，新增时为空。 */
    private Long agentId;

    /** 智能体编码。 */
    @NotBlank(message = "智能体编码不能为空")
    private String agentCode;

    /** 智能体名称。 */
    @NotBlank(message = "智能体名称不能为空")
    private String agentName;

    /** 业务场景。 */
    private String businessScene;

    /** 描述。 */
    private String description;

    /** 系统提示词。 */
    private String systemPrompt;

    /** 安全边界。 */
    private String guardrails;

    /** 状态。 */
    private String status;

    /** 所属团队。 */
    private String ownerTeam;

    /** 是否开启审计。 */
    private String auditEnabled;
}
