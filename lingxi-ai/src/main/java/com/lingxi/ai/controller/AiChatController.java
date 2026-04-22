package com.lingxi.ai.controller;

import com.lingxi.ai.domain.AiChatMessage;
import com.lingxi.ai.domain.AiChatSession;
import com.lingxi.ai.service.IAiChatService;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI聊天控制器
 *
 * @author lingxi
 */
@RestController
@RequestMapping("/ai/chat")
public class AiChatController {

    @Autowired
    private IAiChatService chatService;

    /**
     * 获取用户会话列表
     */
    @GetMapping("/sessions")
    public AjaxResult getUserSessions() {
        List<AiChatSession> sessions = chatService.getUserSessions(SecurityUtils.getUserId());
        return AjaxResult.success(sessions);
    }

    /**
     * 创建新会话
     */
    @PostMapping("/session")
    public AjaxResult createSession() {
        AiChatSession session = chatService.createSession(
            SecurityUtils.getUserId(),
            SecurityUtils.getUsername()
        );
        return AjaxResult.success(session);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public AjaxResult deleteSession(@PathVariable Long sessionId) {
        return chatService.deleteSession(sessionId, SecurityUtils.getUserId());
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/messages/{sessionId}")
    public AjaxResult getSessionMessages(@PathVariable Long sessionId) {
        List<AiChatMessage> messages = chatService.getSessionMessages(sessionId);
        return AjaxResult.success(messages);
    }

    /**
     * 发送消息到AI助手
     */
    @PostMapping("/send")
    public AjaxResult sendMessage(@RequestBody Map<String, Object> params) {
        Long sessionId = Long.valueOf(params.get("sessionId").toString());
        String message = params.get("message").toString();
        return chatService.sendMessage(sessionId, SecurityUtils.getUserId(), message);
    }
}
