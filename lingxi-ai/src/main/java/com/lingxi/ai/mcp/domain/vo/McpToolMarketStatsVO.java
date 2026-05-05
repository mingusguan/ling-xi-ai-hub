package com.lingxi.ai.mcp.domain.vo;

import lombok.Data;

/**
 * MCP 工具市场统计视图，面向前端展示发布、申请和调用概况。
 */
@Data
public class McpToolMarketStatsVO {

    /** 已注册工具数量。 */
    private Long totalTools;

    /** 已发布工具数量。 */
    private Long publishedTools;

    /** 待审批申请数量。 */
    private Long pendingApplications;

    /** 今日调用次数。 */
    private Long todayCalls;

    /** 今日失败次数。 */
    private Long todayFailures;

    /** 今日平均调用耗时，单位毫秒。 */
    private Long averageCostMillis;

    public Long getTotalTools() {
        return totalTools;
    }

    public void setTotalTools(Long totalTools) {
        this.totalTools = totalTools;
    }

    public Long getPublishedTools() {
        return publishedTools;
    }

    public void setPublishedTools(Long publishedTools) {
        this.publishedTools = publishedTools;
    }

    public Long getPendingApplications() {
        return pendingApplications;
    }

    public void setPendingApplications(Long pendingApplications) {
        this.pendingApplications = pendingApplications;
    }

    public Long getTodayCalls() {
        return todayCalls;
    }

    public void setTodayCalls(Long todayCalls) {
        this.todayCalls = todayCalls;
    }

    public Long getTodayFailures() {
        return todayFailures;
    }

    public void setTodayFailures(Long todayFailures) {
        this.todayFailures = todayFailures;
    }

    public Long getAverageCostMillis() {
        return averageCostMillis;
    }

    public void setAverageCostMillis(Long averageCostMillis) {
        this.averageCostMillis = averageCostMillis;
    }
}
