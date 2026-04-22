package com.lingxi.oa.api;

import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.constant.ServiceNameConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.oa.api.factory.RemoteOaFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

/**
 * OA远程服务
 *
 * @author lingxi
 */
@FeignClient(contextId = "remoteOaService", value = ServiceNameConstants.OA_SERVICE, fallbackFactory = RemoteOaFallbackFactory.class)
public interface RemoteOaService {

    /**
     * 查询用户待办任务
     */
    @GetMapping("/oa/workflow/pending/{userId}")
    R<List<Map<String, Object>>> getPendingTasks(@PathVariable("userId") Long userId,@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 查询用户假期余额
     */
    @GetMapping("/oa/workflow/leave/balance/{userId}")
    R<Map<String, Object>> getLeaveBalance(@PathVariable("userId") Long userId,@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 查询用户报销状态
     */
    @GetMapping("/oa/workflow/expense/status/{userId}")
    R<List<Map<String, Object>>> getExpenseStatus(@PathVariable("userId") Long userId,@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 查询超时预警
     */
    @GetMapping("/oa/workflow/timeout/warning/{userId}")
    R<List<Map<String, Object>>> getTimeoutWarning(@PathVariable("userId") Long userId,@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 计算请假工作时长（排除周末和节假日）
     *
     * @param startTime 开始时间（格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime 结束时间（格式：yyyy-MM-dd HH:mm:ss）
     * @return 工作时长信息 {workHours: 工作小时数, workDays: 工作天数}
     */
    @GetMapping("/oa/workflow/leave/calculate-duration")
    R<Map<String, Object>> calculateLeaveDuration(
            @org.springframework.web.bind.annotation.RequestParam("startTime") String startTime,
            @org.springframework.web.bind.annotation.RequestParam("endTime") String endTime,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
