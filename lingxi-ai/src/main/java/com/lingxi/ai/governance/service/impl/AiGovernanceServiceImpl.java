package com.lingxi.ai.governance.service.impl;

import com.lingxi.ai.governance.domain.AiGovernanceLog;
import com.lingxi.ai.governance.domain.vo.AiGovernanceStatsVO;
import com.lingxi.ai.governance.service.IAiGovernanceService;
import com.lingxi.ai.mapper.AiGovernanceLogMapper;
import com.lingxi.ai.mapper.McpToolAuditLogMapper;
import com.lingxi.ai.mcp.constant.McpMarketConstants;
import com.lingxi.common.core.utils.ServletUtils;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI治理审计服务实现。
 */
@Slf4j
@Service
public class AiGovernanceServiceImpl implements IAiGovernanceService {

    private static final int SUMMARY_LIMIT = 500;

    private static final List<String> SENSITIVE_WORDS = List.of("身份证", "银行卡", "密码", "token", "secret",
            "api_key", "手机号");

    private final AiGovernanceLogMapper governanceLogMapper;

    private final McpToolAuditLogMapper toolAuditLogMapper;

    public AiGovernanceServiceImpl(AiGovernanceLogMapper governanceLogMapper,
            McpToolAuditLogMapper toolAuditLogMapper) {
        this.governanceLogMapper = governanceLogMapper;
        this.toolAuditLogMapper = toolAuditLogMapper;
    }

    @Override
    public void recordSuccess(String sceneType, String sceneName, String requestSummary, String responseSummary,
            long costMillis) {
        AiGovernanceLog log = buildBaseLog(sceneType, sceneName, requestSummary, responseSummary, costMillis,
                McpMarketConstants.CALL_STATUS_SUCCESS, null);
        persistQuietly(log);
    }

    @Override
    public void recordFailure(String sceneType, String sceneName, String requestSummary, long costMillis, Exception e) {
        AiGovernanceLog log = buildBaseLog(sceneType, sceneName, requestSummary, null, costMillis,
                McpMarketConstants.CALL_STATUS_FAILURE, e == null ? null : e.getMessage());
        persistQuietly(log);
    }

    @Override
    public void recordExternal(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        AiGovernanceLog log = buildBaseLog(
                stringValue(payload.get("sceneType")),
                stringValue(payload.get("sceneName")),
                stringValue(payload.get("requestSummary")),
                stringValue(payload.get("responseSummary")),
                longValue(payload.get("costMillis")),
                StringUtils.defaultIfEmpty(stringValue(payload.get("status")), McpMarketConstants.CALL_STATUS_SUCCESS),
                stringValue(payload.get("errorMessage")));
        persistQuietly(log);
    }

    @Override
    public List<AiGovernanceLog> listLogs(String sceneType, String sceneName, String status, String riskLevel,
            String sensitiveHit) {
        return governanceLogMapper.selectGovernanceLogList(sceneType, sceneName, status, riskLevel, sensitiveHit);
    }

    @Override
    public AiGovernanceStatsVO getStats() {
        AiGovernanceStatsVO stats = new AiGovernanceStatsVO();
        stats.setTodayCalls(nvl(governanceLogMapper.countTodayCalls(null)));
        stats.setTodayFailures(nvl(governanceLogMapper.countTodayCalls(McpMarketConstants.CALL_STATUS_FAILURE)));
        stats.setTodaySensitiveHits(nvl(governanceLogMapper.countTodaySensitiveHits()));
        stats.setTodayHighRisks(nvl(governanceLogMapper.countTodayHighRisks()));
        stats.setTodayToolCalls(nvl(toolAuditLogMapper.countTodayCalls(null)));
        stats.setAverageCostMillis(nvl(governanceLogMapper.selectTodayAverageCostMillis()));
        stats.setTotalCalls(nvl(governanceLogMapper.countTotalCalls()));
        return stats;
    }

    private AiGovernanceLog buildBaseLog(String sceneType, String sceneName, String requestSummary,
            String responseSummary, long costMillis, String status, String errorMessage) {
        AiGovernanceLog log = new AiGovernanceLog();
        log.setSceneType(defaultScene(sceneType));
        log.setSceneName(truncate(sceneName));
        log.setRequestSummary(truncate(requestSummary));
        log.setResponseSummary(truncate(responseSummary));
        log.setStatus(status);
        log.setCostMillis(costMillis);
        log.setTokenCount(estimateTokens(requestSummary, responseSummary));
        log.setSensitiveHit(hasSensitiveText(requestSummary, responseSummary, errorMessage) ? "1" : "0");
        log.setRiskLevel(resolveRiskLevel(log));
        log.setErrorMessage(truncate(errorMessage));
        fillCurrentUser(log);
        log.setCreateTime(new Date());
        return log;
    }

    private void fillCurrentUser(AiGovernanceLog log) {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null) {
                log.setUserId(loginUser.getUserid());
                log.setUsername(loginUser.getUsername());
                SysUser sysUser = loginUser.getSysUser();
                if (sysUser != null) {
                    log.setDeptId(sysUser.getDeptId());
                    if (log.getUserId() == null) {
                        log.setUserId(sysUser.getUserId());
                    }
                    if (StringUtils.isBlank(log.getUsername())) {
                        log.setUsername(sysUser.getUserName());
                    }
                }
            }
        } catch (Exception ignored) {
            // 部分异步或内部调用没有登录上下文，审计日志仍然保留场景信息。
        }
        if (ServletUtils.getRequest() != null) {
            log.setTenantId(ServletUtils.getRequest().getHeader("X-Tenant-Id"));
        }
    }

    private void persistQuietly(AiGovernanceLog governanceLog) {
        try {
            governanceLogMapper.insertGovernanceLog(governanceLog);
        } catch (Exception e) {
            log.warn("AI governance log persistence failed, sceneType={}, sceneName={}, error={}",
                    governanceLog.getSceneType(), governanceLog.getSceneName(), e.getMessage());
        }
    }

    private String resolveRiskLevel(AiGovernanceLog log) {
        if (McpMarketConstants.CALL_STATUS_FAILURE.equals(log.getStatus()) || "1".equals(log.getSensitiveHit())) {
            return "HIGH";
        }
        if (log.getCostMillis() != null && log.getCostMillis() > 10000L) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private boolean hasSensitiveText(String... values) {
        for (String value : values) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            String lowerValue = value.toLowerCase();
            for (String word : SENSITIVE_WORDS) {
                if (lowerValue.contains(word.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private int estimateTokens(String requestSummary, String responseSummary) {
        int length = StringUtils.defaultString(requestSummary).length()
                + StringUtils.defaultString(responseSummary).length();
        return Math.max(1, (int) Math.ceil(length / 2.0D));
    }

    private String truncate(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return value.length() > SUMMARY_LIMIT ? value.substring(0, SUMMARY_LIMIT) : value;
    }

    private String defaultScene(String sceneType) {
        return StringUtils.defaultIfEmpty(sceneType, "UNKNOWN");
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
