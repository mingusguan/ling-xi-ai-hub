package com.lingxi.knowledge.domain.vo;

import lombok.Data;

/**
 * 知识运营统计。
 */
@Data
public class KbOperationStatsVO {

    /** 文档总数。 */
    private Long totalDocuments;

    /** 已入库文档数。 */
    private Long embeddedDocuments;

    /** 今日问答次数。 */
    private Long todayQuestions;

    /** 今日无答案次数。 */
    private Long todayNoAnswer;

    /** 今日负反馈次数。 */
    private Long todayNegativeFeedback;

    /** 平均命中分。 */
    private Double averageScore;
}
