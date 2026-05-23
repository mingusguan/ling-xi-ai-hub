package com.lingxi.knowledge.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识问答反馈请求。
 */
@Data
public class KbQaFeedbackRequest {

    /** 问答主键。 */
    @NotNull(message = "问答ID不能为空")
    private Long conversationId;

    /** 反馈：HELPFUL、UNHELPFUL。 */
    @NotBlank(message = "反馈结果不能为空")
    private String feedback;

    /** 反馈备注。 */
    private String feedbackRemark;
}
