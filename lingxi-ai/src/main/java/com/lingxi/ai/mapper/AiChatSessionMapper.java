package com.lingxi.ai.mapper;

import com.lingxi.ai.domain.AiChatSession;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI聊天会话Mapper
 *
 * @author lingxi
 */
public interface AiChatSessionMapper {

    /**
     * 查询用户会话列表
     */
    List<AiChatSession> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据ID查询会话
     */
    AiChatSession selectById(@Param("sessionId") Long sessionId);

    /**
     * 插入会话
     */
    int insert(AiChatSession session);

    /**
     * 更新会话
     */
    int update(AiChatSession session);

    /**
     * 删除会话
     */
    int deleteById(@Param("sessionId") Long sessionId);
}
