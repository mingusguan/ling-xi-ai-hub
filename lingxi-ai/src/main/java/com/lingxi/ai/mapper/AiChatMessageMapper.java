package com.lingxi.ai.mapper;

import com.lingxi.ai.domain.AiChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI聊天消息Mapper
 *
 * @author lingxi
 */
public interface AiChatMessageMapper {

    /**
     * 查询会话消息列表
     */
    List<AiChatMessage> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 插入消息
     */
    int insert(AiChatMessage message);

    /**
     * 删除会话的所有消息
     */
    int deleteBySessionId(@Param("sessionId") Long sessionId);
}
