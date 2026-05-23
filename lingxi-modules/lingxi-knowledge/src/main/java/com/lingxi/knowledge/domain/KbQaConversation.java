package com.lingxi.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 知识库问答明细。
 */
@Data
@TableName("kb_qa_conversation")
public class KbQaConversation {

    /** 问答主键。 */
    @TableId(type = IdType.AUTO)
    private Long conversationId;

    /** 会话ID。 */
    private Long sessionId;

    /** 用户ID。 */
    private Long userId;

    /** 部门ID。 */
    private Long deptId;

    /** 用户问题。 */
    private String question;

    /** AI答案。 */
    private String answer;

    /** 引用片段JSON。 */
    private String sourceChunks;

    /** 最高命中分。 */
    private Double topScore;

    /** 可信度：HIGH、MEDIUM、LOW。 */
    private String confidenceLevel;

    /** 是否无答案：1 是，0 否。 */
    private String noAnswer;

    /** 反馈：HELPFUL、UNHELPFUL。 */
    private String feedback;

    /** 反馈备注。 */
    private String feedbackRemark;

    /** 创建时间。 */
    private Date createTime;
}
