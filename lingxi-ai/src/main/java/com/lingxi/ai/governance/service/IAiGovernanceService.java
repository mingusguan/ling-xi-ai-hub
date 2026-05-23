package com.lingxi.ai.governance.service;

import com.lingxi.ai.governance.domain.AiGovernanceLog;
import com.lingxi.ai.governance.domain.vo.AiGovernanceStatsVO;
import java.util.List;
import java.util.Map;

/**
 * AI治理审计服务。
 */
public interface IAiGovernanceService {

    /**
     * 记录成功调用。
     */
    void recordSuccess(String sceneType, String sceneName, String requestSummary, String responseSummary,
            long costMillis);

    /**
     * 记录失败调用。
     */
    void recordFailure(String sceneType, String sceneName, String requestSummary, long costMillis, Exception e);

    /**
     * 记录外部模块传入的审计日志。
     */
    void recordExternal(Map<String, Object> payload);

    /**
     * 查询审计日志列表。
     */
    List<AiGovernanceLog> listLogs(String sceneType, String sceneName, String status, String riskLevel,
            String sensitiveHit);

    /**
     * 查询治理概览统计。
     */
    AiGovernanceStatsVO getStats();
}
