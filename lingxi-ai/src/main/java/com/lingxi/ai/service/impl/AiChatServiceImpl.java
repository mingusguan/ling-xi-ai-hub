package com.lingxi.ai.service.impl;

import com.lingxi.ai.agent.XiaolingAgent;
import com.lingxi.ai.domain.AiChatMessage;
import com.lingxi.ai.domain.AiChatSession;
import com.lingxi.ai.mapper.AiChatMessageMapper;
import com.lingxi.ai.mapper.AiChatSessionMapper;
import com.lingxi.ai.service.IAiChatService;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.domain.SysUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI聊天服务实现
 *
 * @author lingxi
 */
@Service
public class AiChatServiceImpl implements IAiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    @Autowired
    private AiChatSessionMapper sessionMapper;

    @Autowired
    private AiChatMessageMapper messageMapper;

    @Autowired
    private XiaolingAgent xiaolingAgent;

    @Override
    public List<AiChatSession> getUserSessions(Long userId) {
        return sessionMapper.selectByUserId(userId);
    }

    @Override
    public AiChatSession createSession(Long userId, String userName) {
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setUserName(userName);
        session.setTitle("新对话");
        session.setIsBookmarked("0");
        session.setLastMessageTime(new Date());
        session.setCreateTime(new Date());
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public AjaxResult deleteSession(Long sessionId, Long userId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return AjaxResult.error("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            return AjaxResult.error("无权限删除");
        }
        messageMapper.deleteBySessionId(sessionId);
        sessionMapper.deleteById(sessionId);
        return AjaxResult.success();
    }

    @Override
    public List<AiChatMessage> getSessionMessages(Long sessionId) {
        return messageMapper.selectBySessionId(sessionId);
    }

    @Override
    public AjaxResult sendMessage(Long sessionId, Long userId, String message) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return AjaxResult.error("会话不存在或无权限");
        }

        // 保存用户消息
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole("user");
        userMessage.setContent(message);
        userMessage.setCreateTime(new Date());
        messageMapper.insert(userMessage);

        // 使用Agent处理（智能调用工具）
        SysUser user = SecurityUtils.getLoginUser().getSysUser();
        Long deptId = user.getDeptId();
        String aiResponse = xiaolingAgent.chat(message, deptId, userId);

        // 保存AI回复
        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setRole("assistant");
        aiMessage.setContent(aiResponse);
        aiMessage.setCreateTime(new Date());
        messageMapper.insert(aiMessage);

        // 更新会话标题
        if ("新对话".equals(session.getTitle())) {
            session.setTitle(message.length() > 20 ? message.substring(0, 20) + "..." : message);
            session.setLastMessageTime(new Date());
            sessionMapper.update(session);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userMessage", userMessage);
        result.put("aiMessage", aiMessage);
        
        return AjaxResult.success(result);
    }
}
