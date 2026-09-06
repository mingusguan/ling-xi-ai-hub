package com.lingxi.ai.mapper;

import com.lingxi.ai.domain.AiChatToolCall;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 机器人工具调用明细 Mapper。
 */
public interface AiChatToolCallMapper {

    int insert(AiChatToolCall call);

    int updateAssistantAnswer(@Param("sessionId") Long sessionId,
            @Param("userMessageId") Long userMessageId,
            @Param("assistantMessageId") Long assistantMessageId,
            @Param("assistantAnswer") String assistantAnswer);

    List<AiChatToolCall> selectKnowledgeOperationList(@Param("keyword") String keyword,
            @Param("noAnswer") String noAnswer,
            @Param("feedback") String feedback,
            @Param("confidenceLevel") String confidenceLevel);

    int updateFeedback(@Param("callId") Long callId,
            @Param("feedback") String feedback,
            @Param("feedbackRemark") String feedbackRemark);

    Long countKnowledgeDocumentsByStatus(@Param("status") Integer status);

    Long countTodayKnowledgeCalls();

    Long countTodayKnowledgeNoAnswer();

    Long countTodayKnowledgeNegativeFeedback();

    Double selectKnowledgeAverageScore();
}
