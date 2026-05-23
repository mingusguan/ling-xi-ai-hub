package com.lingxi.ai.agent.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 智能体编排步骤保存请求。
 */
@Data
public class AiAgentStepSaveRequest {

    /** 步骤主键，新增时为空。 */
    private Long stepId;

    /** 智能体编码。 */
    @NotBlank(message = "智能体编码不能为空")
    private String agentCode;

    /** 步骤序号。 */
    @NotNull(message = "步骤序号不能为空")
    private Integer stepOrder;

    /** 步骤名称。 */
    @NotBlank(message = "步骤名称不能为空")
    private String stepName;

    /** 步骤类型。 */
    @NotBlank(message = "步骤类型不能为空")
    private String stepType;

    /** MCP工具名称。 */
    private String toolName;

    /** 步骤说明。 */
    private String instruction;

    /** 配置JSON。 */
    private String configJson;

    /** 失败策略。 */
    private String failurePolicy;

    /** 是否启用。 */
    private String enabled;
}
