package com.lingxi.knowledge.mapper;

import com.lingxi.knowledge.domain.KbQaSession;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 知识库问答会话 Mapper。
 */
public interface KbQaSessionMapper {

    /**
     * 查询用户会话列表。
     */
    List<KbQaSession> selectSessionsByUserId(@Param("userId") Long userId);

    /**
     * 按主键查询会话。
     */
    KbQaSession selectSessionById(@Param("sessionId") Long sessionId);

    /**
     * 新增会话。
     */
    int insertSession(KbQaSession session);

    /**
     * 更新会话统计。
     */
    int updateSessionSummary(KbQaSession session);

    /**
     * 删除会话。
     */
    int deleteSessionById(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
