package com.lingxi.ai.governance.domain.vo;

import lombok.Data;

/**
 * AI治理中心概览统计。
 */
@Data
public class AiGovernanceStatsVO {

    /** 今日AI调用次数。 */
    private Long todayCalls;

    /** 今日失败次数。 */
    private Long todayFailures;

    /** 今日敏感命中次数。 */
    private Long todaySensitiveHits;

    /** 今日高风险次数。 */
    private Long todayHighRisks;

    /** 今日MCP工具调用次数。 */
    private Long todayToolCalls;

    /** 今日平均耗时，单位毫秒。 */
    private Long averageCostMillis;

    /** 累计调用次数。 */
    private Long totalCalls;
}
