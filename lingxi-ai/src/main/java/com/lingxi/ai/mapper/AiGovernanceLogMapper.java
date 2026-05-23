package com.lingxi.ai.mapper;

import com.lingxi.ai.governance.domain.AiGovernanceLog;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * AI治理审计日志 Mapper。
 */
public interface AiGovernanceLogMapper {

    /**
     * 查询AI治理审计日志列表。
     */
    List<AiGovernanceLog> selectGovernanceLogList(@Param("sceneType") String sceneType,
            @Param("sceneName") String sceneName,
            @Param("status") String status,
            @Param("riskLevel") String riskLevel,
            @Param("sensitiveHit") String sensitiveHit);

    /**
     * 新增AI治理审计日志。
     */
    int insertGovernanceLog(AiGovernanceLog log);

    /**
     * 统计今日AI调用次数。
     */
    Long countTodayCalls(@Param("status") String status);

    /**
     * 统计今日敏感命中次数。
     */
    Long countTodaySensitiveHits();

    /**
     * 统计今日高风险次数。
     */
    Long countTodayHighRisks();

    /**
     * 统计今日平均耗时。
     */
    Long selectTodayAverageCostMillis();

    /**
     * 统计累计调用次数。
     */
    Long countTotalCalls();
}
