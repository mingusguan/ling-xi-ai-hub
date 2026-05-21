package com.lingxi.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * AI 聊天发送请求。
 */
public class AiChatSendRequest
{
    /**
     * 会话 ID。
     */
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    /**
     * 用户输入内容。
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    public Long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
