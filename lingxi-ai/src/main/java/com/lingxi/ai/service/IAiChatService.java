package com.lingxi.ai.service;

import com.lingxi.ai.domain.AiChatMessage;
import com.lingxi.ai.domain.AiChatSession;
import com.lingxi.common.core.web.domain.AjaxResult;

import java.util.List;

/**
 * AI聊天服务接口
 *
 * @author lingxi
 */
public interface IAiChatService {

    /**
     * 获取用户会话列表
     */
    List<AiChatSession> getUserSessions(Long userId);

    /**
     * 创建新会话
     */
    AiChatSession createSession(Long userId, String userName);

    /**
     * 删除会话
     */
    AjaxResult deleteSession(Long sessionId, Long userId);

    /**
     * 获取会话消息历史
     */
    List<AiChatMessage> getSessionMessages(Long sessionId);

    /**
     * 发送消息到AI助手
     */
    AjaxResult sendMessage(Long sessionId, Long userId, String message);

}
