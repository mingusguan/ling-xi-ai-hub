package com.lingxi.oa.api.factory;

import com.lingxi.common.core.domain.R;
import com.lingxi.oa.api.RemoteOaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * OA服务降级处理
 *
 * @author lingxi
 */
@Component
public class RemoteOaFallbackFactory implements FallbackFactory<RemoteOaService> {
    
    private static final Logger log = LoggerFactory.getLogger(RemoteOaFallbackFactory.class);

    @Override
    public RemoteOaService create(Throwable throwable) {
        log.error("OA服务调用失败:{}", throwable.getMessage());
        return new RemoteOaService() {
            @Override
            public R<List<Map<String, Object>>> getPendingTasks(Long userId, String source) {
                log.warn("查询待办任务降级");
                return R.ok(new ArrayList<>());
            }

            @Override
            public R<Map<String, Object>> getLeaveBalance(Long userId, String source) {
                log.warn("查询假期余额降级");
                return R.ok(new HashMap<>());
            }

            @Override
            public R<List<Map<String, Object>>> getExpenseStatus(Long userId, String source) {
                log.warn("查询报销状态降级");
                return R.ok(new ArrayList<>());
            }

            @Override
            public R<List<Map<String, Object>>> getTimeoutWarning(Long userId, String source) {
                log.warn("查询超时预警降级");
                return R.ok(new ArrayList<>());
            }

            @Override
            public R<Map<String, Object>> calculateLeaveDuration(String startTime, String endTime, String source) {
                return R.ok(new HashMap<>());
            }
        };
    }
}
