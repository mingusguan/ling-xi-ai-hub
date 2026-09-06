package com.lingxi.ai.domain.vo;

import lombok.Data;

/**
 * 机器人知识库运营统计视图。
 */
@Data
public class AiKnowledgeOperationStatsVO {

    private Long totalDocuments;

    private Long embeddedDocuments;

    private Long todayQuestions;

    private Long todayNoAnswer;

    private Long todayNegativeFeedback;

    private Double averageScore;
}
