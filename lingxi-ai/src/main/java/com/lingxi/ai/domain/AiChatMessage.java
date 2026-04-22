package com.lingxi.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * AI聊天消息实体
 *
 * @author lingxi
 */
@Data
@TableName("ai_chat_message")
public class AiChatMessage {

    /** 消息ID */
    @TableId(type = IdType.AUTO)
    private Long messageId;

    /** 会话ID */
    private Long sessionId;

    /** 角色（user/assistant） */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间 */
    private Date createTime;
}
