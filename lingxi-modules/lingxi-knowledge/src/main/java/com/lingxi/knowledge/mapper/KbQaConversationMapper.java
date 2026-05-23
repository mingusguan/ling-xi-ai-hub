package com.lingxi.knowledge.mapper;

import com.lingxi.knowledge.domain.KbQaConversation;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 知识库问答明细 Mapper。
 */
public interface KbQaConversationMapper {

    /**
     * 查询会话问答明细。
     */
    List<KbQaConversation> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 查询知识运营日志。
     */
    List<KbQaConversation> selectOperationList(@Param("keyword") String keyword,
            @Param("noAnswer") String noAnswer,
            @Param("feedback") String feedback,
            @Param("confidenceLevel") String confidenceLevel);

    /**
     * 新增问答明细。
     */
    int insertConversation(KbQaConversation conversation);

    /**
     * 删除会话下的问答明细。
     */
    int deleteBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 更新问答反馈。
     */
    int updateFeedback(KbQaConversation conversation);

    /**
     * 统计今日问答次数。
     */
    Long countTodayQuestions();

    /**
     * 统计今日无答案次数。
     */
    Long countTodayNoAnswer();

    /**
     * 统计今日负反馈次数。
     */
    Long countTodayNegativeFeedback();

    /**
     * 统计平均命中分。
     */
    Double selectAverageScore();
}
