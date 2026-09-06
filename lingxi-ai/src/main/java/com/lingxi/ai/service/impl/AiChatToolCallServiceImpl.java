package com.lingxi.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lingxi.ai.agent.tools.AiChatToolCallContext.Context;
import com.lingxi.ai.domain.AiChatToolCall;
import com.lingxi.ai.domain.dto.AiKnowledgeFeedbackRequest;
import com.lingxi.ai.domain.vo.AiKnowledgeOperationStatsVO;
import com.lingxi.ai.mapper.AiChatToolCallMapper;
import com.lingxi.ai.service.IAiChatToolCallService;
import com.lingxi.common.core.utils.StringUtils;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 机器人知识库工具调用运营服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatToolCallServiceImpl implements IAiChatToolCallService {

    private static final String KNOWLEDGE_TOOL_NAME = "search_knowledge";

    private static final double HIGH_CONFIDENCE_SCORE = 0.78D;

    private static final double MEDIUM_CONFIDENCE_SCORE = 0.55D;

    private final AiChatToolCallMapper toolCallMapper;

    @Override
    public void recordKnowledgeCall(Context context, String query, Map<String, Object> arguments, String resultText,
            long costMillis, String status, String errorMessage) {
        try {
            AiChatToolCall call = new AiChatToolCall();
            fillContext(call, context);
            call.setToolName(KNOWLEDGE_TOOL_NAME);
            call.setQueryText(query);
            call.setRequestPayload(JSON.toJSONString(arguments));
            call.setResultSummary(buildResultSummary(resultText));
            call.setSourceChunks(extractSourceChunks(resultText));
            call.setTopScore(resolveTopScore(call.getSourceChunks()));
            call.setConfidenceLevel(resolveConfidence(call.getTopScore()));
            call.setNoAnswer(resolveNoAnswer(status, call.getTopScore(), call.getSourceChunks()));
            call.setStatus(status);
            call.setCostMillis(costMillis);
            call.setErrorMessage(errorMessage);
            call.setCreateTime(new Date());
            call.setUpdateTime(new Date());
            toolCallMapper.insert(call);
        } catch (Exception ex) {
            log.warn("Record AI chat knowledge tool call failed, query={}, error={}", query, ex.getMessage());
        }
    }

    @Override
    public void updateAssistantAnswer(Long sessionId, Long userMessageId, Long assistantMessageId,
            String assistantAnswer) {
        if (sessionId == null || userMessageId == null) {
            return;
        }
        toolCallMapper.updateAssistantAnswer(sessionId, userMessageId, assistantMessageId, assistantAnswer);
    }

    @Override
    public List<AiChatToolCall> listKnowledgeOperations(String keyword, String noAnswer, String feedback,
            String confidenceLevel) {
        return toolCallMapper.selectKnowledgeOperationList(keyword, noAnswer, feedback, confidenceLevel);
    }

    @Override
    public int feedback(AiKnowledgeFeedbackRequest request) {
        return toolCallMapper.updateFeedback(request.getConversationId(), request.getFeedback(),
                request.getFeedbackRemark());
    }

    @Override
    public AiKnowledgeOperationStatsVO getKnowledgeOperationStats() {
        AiKnowledgeOperationStatsVO stats = new AiKnowledgeOperationStatsVO();
        stats.setTotalDocuments(nvl(toolCallMapper.countKnowledgeDocumentsByStatus(null)));
        stats.setEmbeddedDocuments(nvl(toolCallMapper.countKnowledgeDocumentsByStatus(2)));
        stats.setTodayQuestions(nvl(toolCallMapper.countTodayKnowledgeCalls()));
        stats.setTodayNoAnswer(nvl(toolCallMapper.countTodayKnowledgeNoAnswer()));
        stats.setTodayNegativeFeedback(nvl(toolCallMapper.countTodayKnowledgeNegativeFeedback()));
        stats.setAverageScore(toolCallMapper.selectKnowledgeAverageScore());
        return stats;
    }

    private void fillContext(AiChatToolCall call, Context context) {
        if (context == null) {
            return;
        }
        call.setSessionId(context.sessionId());
        call.setUserMessageId(context.userMessageId());
        call.setUserId(context.userId());
        call.setUsername(context.username());
        call.setDeptId(context.deptId());
        call.setTenantId(context.tenantId());
        call.setAgentCode(context.agentCode());
    }

    private String extractSourceChunks(String resultText) {
        JSONObject response = parseResponse(resultText);
        if (response == null) {
            return null;
        }
        Object results = response.get("results");
        return results == null ? null : JSON.toJSONString(results);
    }

    private BigDecimal resolveTopScore(String sourceChunks) {
        if (StringUtils.isEmpty(sourceChunks)) {
            return BigDecimal.ZERO;
        }
        JSONArray sources = JSON.parseArray(sourceChunks);
        BigDecimal topScore = BigDecimal.ZERO;
        for (int i = 0; i < sources.size(); i++) {
            JSONObject source = sources.getJSONObject(i);
            BigDecimal score = source.getBigDecimal("score");
            if (score != null && score.compareTo(topScore) > 0) {
                topScore = score;
            }
        }
        return topScore;
    }

    private String resolveConfidence(BigDecimal topScore) {
        double score = topScore == null ? 0D : topScore.doubleValue();
        if (score >= HIGH_CONFIDENCE_SCORE) {
            return "HIGH";
        }
        if (score >= MEDIUM_CONFIDENCE_SCORE) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveNoAnswer(String status, BigDecimal topScore, String sourceChunks) {
        if (!"SUCCESS".equals(status) || StringUtils.isEmpty(sourceChunks)) {
            return "1";
        }
        double score = topScore == null ? 0D : topScore.doubleValue();
        return score < MEDIUM_CONFIDENCE_SCORE ? "1" : "0";
    }

    private String buildResultSummary(String resultText) {
        if (StringUtils.isEmpty(resultText)) {
            return null;
        }
        JSONObject response = parseResponse(resultText);
        if (response == null) {
            return resultText.length() > 1000 ? resultText.substring(0, 1000) : resultText;
        }
        JSONArray results = response.getJSONArray("results");
        if (results == null || results.isEmpty()) {
            return "未检索到知识库内容";
        }
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < results.size() && i < 3; i++) {
            JSONObject source = results.getJSONObject(i);
            if (summary.length() > 0) {
                summary.append('\n');
            }
            summary.append(source.getString("content"));
        }
        String value = summary.toString();
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    private JSONObject parseResponse(String resultText) {
        if (StringUtils.isEmpty(resultText)) {
            return null;
        }
        try {
            return JSON.parseObject(resultText);
        } catch (Exception ignored) {
            return null;
        }
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
