package com.lingxi.ai.service;

import com.lingxi.ai.agent.tools.AiChatToolCallContext.Context;
import com.lingxi.ai.domain.AiChatToolCall;
import com.lingxi.ai.domain.dto.AiKnowledgeFeedbackRequest;
import com.lingxi.ai.domain.vo.AiKnowledgeOperationStatsVO;
import java.util.List;
import java.util.Map;

/**
 * 机器人知识库工具调用运营服务。
 */
public interface IAiChatToolCallService {

    void recordKnowledgeCall(Context context, String query, Map<String, Object> arguments, String resultText,
            long costMillis, String status, String errorMessage);

    void updateAssistantAnswer(Long sessionId, Long userMessageId, Long assistantMessageId, String assistantAnswer);

    List<AiChatToolCall> listKnowledgeOperations(String keyword, String noAnswer, String feedback,
            String confidenceLevel);

    int feedback(AiKnowledgeFeedbackRequest request);

    AiKnowledgeOperationStatsVO getKnowledgeOperationStats();
}
