package com.lingxi.knowledge.domain.vo;

import com.lingxi.knowledge.domain.dto.KnowledgeSearchResult;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 知识问答响应。
 */
@Data
public class KbQaChatResponse {

    /** 会话ID。 */
    private Long sessionId;

    /** 问答ID。 */
    private Long conversationId;

    /** 答案。 */
    private String answer;

    /** 引用来源JSON，兼容旧前端字段。 */
    private String source;

    /** 引用片段。 */
    private List<KnowledgeSearchResult> sources = new ArrayList<>();

    /** 可信度。 */
    private String confidenceLevel;

    /** 是否无答案。 */
    private String noAnswer;
}
