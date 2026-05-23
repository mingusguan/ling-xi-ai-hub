package com.lingxi.knowledge.service;

import com.lingxi.knowledge.domain.KbQaConversation;
import com.lingxi.knowledge.domain.KbQaSession;
import com.lingxi.knowledge.domain.dto.KbQaChatRequest;
import com.lingxi.knowledge.domain.dto.KbQaFeedbackRequest;
import com.lingxi.knowledge.domain.vo.KbOperationStatsVO;
import com.lingxi.knowledge.domain.vo.KbQaChatResponse;
import java.util.List;

/**
 * 知识库问答与运营服务。
 */
public interface IKbQaService {

    /**
     * 知识库问答。
     */
    KbQaChatResponse chat(KbQaChatRequest request);

    /**
     * 查询会话列表。
     */
    List<KbQaSession> listSessions(Long userId);

    /**
     * 创建会话。
     */
    KbQaSession createSession(Long userId, Long deptId);

    /**
     * 删除会话。
     */
    int deleteSession(Long sessionId, Long userId);

    /**
     * 查询会话问答记录。
     */
    List<KbQaConversation> listConversations(Long sessionId, Long userId);

    /**
     * 查询运营日志。
     */
    List<KbQaConversation> listOperations(String keyword, String noAnswer, String feedback, String confidenceLevel);

    /**
     * 保存反馈。
     */
    int feedback(KbQaFeedbackRequest request);

    /**
     * 查询知识运营统计。
     */
    KbOperationStatsVO getOperationStats();
}
