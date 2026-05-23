package com.lingxi.knowledge.service.impl;

import com.alibaba.fastjson2.JSON;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.knowledge.api.AiGovernanceClient;
import com.lingxi.knowledge.domain.KbDocument;
import com.lingxi.knowledge.domain.KbQaConversation;
import com.lingxi.knowledge.domain.KbQaSession;
import com.lingxi.knowledge.domain.dto.KbQaChatRequest;
import com.lingxi.knowledge.domain.dto.KbQaFeedbackRequest;
import com.lingxi.knowledge.domain.dto.KnowledgeSearchResponse;
import com.lingxi.knowledge.domain.dto.KnowledgeSearchResult;
import com.lingxi.knowledge.domain.vo.KbOperationStatsVO;
import com.lingxi.knowledge.domain.vo.KbQaChatResponse;
import com.lingxi.knowledge.mapper.KbDocumentMapper;
import com.lingxi.knowledge.mapper.KbQaConversationMapper;
import com.lingxi.knowledge.mapper.KbQaSessionMapper;
import com.lingxi.knowledge.service.IKbDocumentService;
import com.lingxi.knowledge.service.IKbQaService;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识库问答与运营服务实现。
 */
@Slf4j
@Service
public class KbQaServiceImpl implements IKbQaService {

    private static final String DEFAULT_SESSION_TITLE = "新知识问答";

    private static final double HIGH_CONFIDENCE_SCORE = 0.78D;

    private static final double MEDIUM_CONFIDENCE_SCORE = 0.55D;

    private final IKbDocumentService documentService;

    private final KbQaSessionMapper sessionMapper;

    private final KbQaConversationMapper conversationMapper;

    private final KbDocumentMapper documentMapper;

    private final ChatLanguageModel chatLanguageModel;

    private final AiGovernanceClient governanceClient;

    public KbQaServiceImpl(IKbDocumentService documentService, KbQaSessionMapper sessionMapper,
            KbQaConversationMapper conversationMapper, KbDocumentMapper documentMapper,
            ChatLanguageModel chatLanguageModel, AiGovernanceClient governanceClient) {
        this.documentService = documentService;
        this.sessionMapper = sessionMapper;
        this.conversationMapper = conversationMapper;
        this.documentMapper = documentMapper;
        this.chatLanguageModel = chatLanguageModel;
        this.governanceClient = governanceClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbQaChatResponse chat(KbQaChatRequest request) {
        long startTime = System.currentTimeMillis();
        Long userId = resolveUserId(request.getUserId());
        Long deptId = resolveDeptId(request.getDeptId());
        KbQaSession session = resolveSession(request.getSessionId(), userId, deptId);

        try {
            KnowledgeSearchResponse searchResponse = documentService.searchKnowledge(request.getQuestion(), deptId, 5,
                    0.0D);
            List<KnowledgeSearchResult> sources = searchResponse.results();
            double topScore = resolveTopScore(sources);
            String confidenceLevel = resolveConfidence(topScore);
            boolean noAnswer = sources.isEmpty() || topScore < MEDIUM_CONFIDENCE_SCORE;
            String answer = noAnswer
                    ? "当前知识库没有找到足够可靠的依据，建议补充相关制度、流程或业务文档后再查询。"
                    : generateAnswer(request.getQuestion(), sources);

            KbQaConversation conversation = saveConversation(session, userId, deptId, request.getQuestion(), answer,
                    sources, topScore, confidenceLevel, noAnswer);
            updateSessionAfterChat(session, request.getQuestion());
            recordGovernance("SUCCESS", request.getQuestion(), answer, System.currentTimeMillis() - startTime, null);

            KbQaChatResponse response = new KbQaChatResponse();
            response.setSessionId(session.getSessionId());
            response.setConversationId(conversation.getConversationId());
            response.setAnswer(answer);
            response.setSource(JSON.toJSONString(sources));
            response.setSources(sources);
            response.setConfidenceLevel(confidenceLevel);
            response.setNoAnswer(noAnswer ? "1" : "0");
            return response;
        } catch (Exception e) {
            recordGovernance("FAILURE", request.getQuestion(), null, System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }

    @Override
    public List<KbQaSession> listSessions(Long userId) {
        return sessionMapper.selectSessionsByUserId(resolveUserId(userId));
    }

    @Override
    public KbQaSession createSession(Long userId, Long deptId) {
        KbQaSession session = new KbQaSession();
        session.setUserId(resolveUserId(userId));
        session.setDeptId(resolveDeptId(deptId));
        session.setTitle(DEFAULT_SESSION_TITLE);
        session.setMessageCount(0);
        session.setLastMessageTime(new Date());
        session.setCreateTime(new Date());
        sessionMapper.insertSession(session);
        return session;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSession(Long sessionId, Long userId) {
        KbQaSession session = requireOwnedSession(sessionId, resolveUserId(userId));
        conversationMapper.deleteBySessionId(session.getSessionId());
        return sessionMapper.deleteSessionById(session.getSessionId(), session.getUserId());
    }

    @Override
    public List<KbQaConversation> listConversations(Long sessionId, Long userId) {
        requireOwnedSession(sessionId, resolveUserId(userId));
        return conversationMapper.selectBySessionId(sessionId);
    }

    @Override
    public List<KbQaConversation> listOperations(String keyword, String noAnswer, String feedback,
            String confidenceLevel) {
        return conversationMapper.selectOperationList(keyword, noAnswer, feedback, confidenceLevel);
    }

    @Override
    public int feedback(KbQaFeedbackRequest request) {
        KbQaConversation conversation = new KbQaConversation();
        conversation.setConversationId(request.getConversationId());
        conversation.setFeedback(request.getFeedback());
        conversation.setFeedbackRemark(request.getFeedbackRemark());
        return conversationMapper.updateFeedback(conversation);
    }

    @Override
    public KbOperationStatsVO getOperationStats() {
        KbOperationStatsVO stats = new KbOperationStatsVO();
        stats.setTotalDocuments(nvl(documentMapper.countDocumentsByStatus(null)));
        stats.setEmbeddedDocuments(nvl(documentMapper.countDocumentsByStatus(2)));
        stats.setTodayQuestions(nvl(conversationMapper.countTodayQuestions()));
        stats.setTodayNoAnswer(nvl(conversationMapper.countTodayNoAnswer()));
        stats.setTodayNegativeFeedback(nvl(conversationMapper.countTodayNegativeFeedback()));
        stats.setAverageScore(conversationMapper.selectAverageScore());
        return stats;
    }

    private KbQaSession resolveSession(Long sessionId, Long userId, Long deptId) {
        if (sessionId == null) {
            return createSession(userId, deptId);
        }
        return requireOwnedSession(sessionId, userId);
    }

    private KbQaSession requireOwnedSession(Long sessionId, Long userId) {
        KbQaSession session = sessionMapper.selectSessionById(sessionId);
        if (session == null) {
            throw new ServiceException("知识问答会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new ServiceException("无权访问该知识问答会话");
        }
        return session;
    }

    private String generateAnswer(String question, List<KnowledgeSearchResult> sources) {
        StringBuilder prompt = new StringBuilder(4096);
        prompt.append("你是企业知识库问答助手，只能依据给定引用回答。")
                .append("如果引用不足，必须说明无法确认。回答要简洁、专业，并列出来源编号。\n");
        prompt.append("【问题】\n").append(question).append('\n');
        prompt.append("【引用】\n");
        for (int i = 0; i < sources.size(); i++) {
            KnowledgeSearchResult source = sources.get(i);
            prompt.append("[").append(i + 1).append("] 文档ID:")
                    .append(source.documentId()).append(" 片段ID:")
                    .append(source.chunkId()).append(" 相似度:")
                    .append(source.score()).append('\n')
                    .append(source.content()).append('\n');
        }
        return chatLanguageModel.generate(prompt.toString());
    }

    private KbQaConversation saveConversation(KbQaSession session, Long userId, Long deptId, String question,
            String answer, List<KnowledgeSearchResult> sources, double topScore, String confidenceLevel,
            boolean noAnswer) {
        KbQaConversation conversation = new KbQaConversation();
        conversation.setSessionId(session.getSessionId());
        conversation.setUserId(userId);
        conversation.setDeptId(deptId);
        conversation.setQuestion(question);
        conversation.setAnswer(answer);
        conversation.setSourceChunks(JSON.toJSONString(sources));
        conversation.setTopScore(topScore);
        conversation.setConfidenceLevel(confidenceLevel);
        conversation.setNoAnswer(noAnswer ? "1" : "0");
        conversation.setCreateTime(new Date());
        conversationMapper.insertConversation(conversation);
        return conversation;
    }

    private void updateSessionAfterChat(KbQaSession session, String question) {
        KbQaSession update = new KbQaSession();
        update.setSessionId(session.getSessionId());
        update.setTitle(DEFAULT_SESSION_TITLE.equals(session.getTitle()) ? buildTitle(question) : session.getTitle());
        update.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
        update.setLastMessageTime(new Date());
        sessionMapper.updateSessionSummary(update);
    }

    private String buildTitle(String question) {
        if (StringUtils.isBlank(question)) {
            return DEFAULT_SESSION_TITLE;
        }
        return question.length() > 20 ? question.substring(0, 20) + "..." : question;
    }

    private double resolveTopScore(List<KnowledgeSearchResult> sources) {
        if (sources == null || sources.isEmpty()) {
            return 0D;
        }
        Double score = sources.get(0).score();
        return score == null ? 0D : score;
    }

    private String resolveConfidence(double topScore) {
        if (topScore >= HIGH_CONFIDENCE_SCORE) {
            return "HIGH";
        }
        if (topScore >= MEDIUM_CONFIDENCE_SCORE) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private void recordGovernance(String status, String question, String answer, long costMillis, Exception e) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("sceneType", "KNOWLEDGE_QA");
            payload.put("sceneName", "knowledge_qa");
            payload.put("requestSummary", question);
            payload.put("responseSummary", answer);
            payload.put("status", status);
            payload.put("costMillis", costMillis);
            payload.put("errorMessage", e == null ? null : e.getMessage());
            governanceClient.record(payload);
        } catch (Exception ex) {
            log.warn("Record knowledge QA governance log failed: {}", ex.getMessage());
        }
    }

    private Long resolveUserId(Long requestUserId) {
        Long currentUserId = SecurityUtils.getUserId();
        return currentUserId == null ? requestUserId : currentUserId;
    }

    private Long resolveDeptId(Long requestDeptId) {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null) {
                SysUser sysUser = loginUser.getSysUser();
                if (sysUser != null && sysUser.getDeptId() != null) {
                    return sysUser.getDeptId();
                }
            }
        } catch (Exception ignored) {
            // 兼容内部调用和无登录上下文的远程调用。
        }
        return requestDeptId;
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
