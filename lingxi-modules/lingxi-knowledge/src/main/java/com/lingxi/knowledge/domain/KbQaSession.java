package com.lingxi.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 知识库问答会话。
 */
@Data
@TableName("kb_qa_session")
public class KbQaSession {

    /** 会话主键。 */
    @TableId(type = IdType.AUTO)
    private Long sessionId;

    /** 用户ID。 */
    private Long userId;

    /** 部门ID。 */
    private Long deptId;

    /** 会话标题。 */
    private String title;

    /** 消息数量。 */
    private Integer messageCount;

    /** 最后消息时间。 */
    private Date lastMessageTime;

    /** 创建时间。 */
    private Date createTime;
}
