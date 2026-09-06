package com.lingxi.ai.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 机器人知识库调用反馈请求。
 */
@Data
public class AiKnowledgeFeedbackRequest {

    /**
     * 前端沿用知识运营列表的 conversationId 字段，这里实际对应 ai_chat_tool_call.call_id。
     */
    @NotNull(message = "调用记录ID不能为空")
    private Long conversationId;

    private String feedback;

    private String feedbackRemark;
}
