package com.lingxi.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lingxi.common.core.utils.StringUtils;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 机器人会话中的工具调用明细。
 */
@Data
@TableName("ai_chat_tool_call")
public class AiChatToolCall {

    @TableId(type = IdType.AUTO)
    private Long callId;

    private Long sessionId;

    private Long userMessageId;

    private Long assistantMessageId;

    private Long userId;

    private String username;

    private Long deptId;

    private String tenantId;

    private String agentCode;

    private String toolName;

    private String queryText;

    private String requestPayload;

    private String resultSummary;

    private String assistantAnswer;

    private String sourceChunks;

    private BigDecimal topScore;

    private String confidenceLevel;

    private String noAnswer;

    private String status;

    private Long costMillis;

    private String errorMessage;

    private String feedback;

    private String feedbackRemark;

    private Date createTime;

    private Date updateTime;

    public Long getConversationId() {
        return callId;
    }

    public String getQuestion() {
        return queryText;
    }

    public String getAnswer() {
        return StringUtils.isNotEmpty(assistantAnswer) ? assistantAnswer : resultSummary;
    }
}
