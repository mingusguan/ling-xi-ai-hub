package com.lingxi.ai.controller;

import com.lingxi.ai.domain.AiChatMessage;
import com.lingxi.ai.domain.AiChatSession;
import com.lingxi.ai.domain.dto.AiChatSendRequest;
import com.lingxi.ai.service.IAiChatService;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.security.annotation.RequiresLogin;
import com.lingxi.common.security.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI聊天控制器
 *
 * @author lingxi
 */
@Validated
@RestController
@RequestMapping("/ai/chat")
public class AiChatController {

    @Autowired
    private IAiChatService chatService;

    /**
     * 获取用户会话列表
     */
    @RequiresLogin
    @GetMapping("/sessions")
    public AjaxResult getUserSessions() {
        List<AiChatSession> sessions = chatService.getUserSessions(SecurityUtils.getUserId());
        return AjaxResult.success(sessions);
    }

    /**
     * 创建新会话
     */
    @RequiresLogin
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
    @RequiresLogin
    @DeleteMapping("/session/{sessionId}")
    public AjaxResult deleteSession(@PathVariable Long sessionId) {
        return chatService.deleteSession(sessionId, SecurityUtils.getUserId());
    }

    /**
     * 获取会话消息历史
     */
    @RequiresLogin
    @GetMapping("/messages/{sessionId}")
    public AjaxResult getSessionMessages(@PathVariable Long sessionId) {
        List<AiChatMessage> messages = chatService.getSessionMessages(sessionId, SecurityUtils.getUserId());
        return AjaxResult.success(messages);
    }

    /**
     * 发送消息到AI助手
     */
    @RequiresLogin
    @PostMapping("/send")
    public AjaxResult sendMessage(@Valid @RequestBody AiChatSendRequest request) {
        return chatService.sendMessage(request.getSessionId(), SecurityUtils.getUserId(), request.getMessage());
    }
}
