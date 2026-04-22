package com.lingxi.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * AI聊天会话实体
 *
 * @author lingxi
 */
@Data
@TableName("ai_chat_session")
public class AiChatSession {

    /** 会话ID */
    @TableId(type = IdType.AUTO)
    private Long sessionId;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String userName;

    /** 会话标题 */
    private String title;

    /** 是否收藏（0否 1是） */
    private String isBookmarked;

    /** 最后消息时间 */
    private Date lastMessageTime;

    /** 创建时间 */
    private Date createTime;
}
