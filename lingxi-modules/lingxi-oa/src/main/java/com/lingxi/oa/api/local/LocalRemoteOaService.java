package com.lingxi.oa.api.local;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.oa.api.RemoteLeaveQuotaService;
import com.lingxi.oa.api.RemoteOaService;
import com.lingxi.oa.domain.OaHolidayConfig;
import com.lingxi.oa.domain.OaLeaveQuota;
import com.lingxi.oa.service.IOaLeaveQuotaService;
import com.lingxi.oa.service.IOaWorkflowBizService;
import com.lingxi.oa.util.HolidayUtil;
import com.lingxi.system.api.domain.SysUser;
import java.time.LocalDate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 单体模式下的本地 OA 服务调用。
 */
@Service
@RequiredArgsConstructor
public class LocalRemoteOaService implements RemoteOaService, RemoteLeaveQuotaService {

    private final IOaWorkflowBizService workflowBizService;
    private final IOaLeaveQuotaService leaveQuotaService;
    private final HolidayUtil holidayUtil;

    @Override
    public R<List<Map<String, Object>>> getPendingTasks(Long userId, String source) {
        return R.ok(workflowBizService.queryPendingTasks(userId));
    }

    @Override
    public R<Map<String, Object>> getLeaveBalance(Long userId, String source) {
        return R.ok(workflowBizService.queryLeaveBalance(userId));
    }

    @Override
    public R<List<Map<String, Object>>> getExpenseStatus(Long userId, String source) {
        return R.ok(workflowBizService.queryExpenseStatus(userId));
    }

    @Override
    public R<List<Map<String, Object>>> getTimeoutWarning(Long userId, String source) {
        return R.ok(workflowBizService.queryTimeoutWarning(userId));
    }

    @Override
    public R<Map<String, Object>> calculateLeaveDuration(String startTime, String endTime, String source) {
        Date start = parseDate(startTime);
        Date end = parseDate(endTime);
        if (start == null || end == null || !start.before(end)) {
            throw new ServiceException("开始时间必须早于结束时间");
        }
        long workHours = holidayUtil.calculateWorkHours(start, end);
        List<OaHolidayConfig> restDays = holidayUtil.getRestDaysInRange(start, end);
        Map<String, Object> result = new HashMap<>();
        result.put("workHours", workHours);
        result.put("workDays", workHours / 8.0);
        result.put("restDays", restDays);
        return R.ok(result);
    }

    @Override
    public R<Boolean> disableUserQuotas(Long userId, String source) {
        OaLeaveQuota query = new OaLeaveQuota();
        query.setUserId(userId);
        List<OaLeaveQuota> quotas = leaveQuotaService.selectOaLeaveQuotaList(query);
        for (OaLeaveQuota quota : quotas) {
            quota.setStatus("1");
            leaveQuotaService.updateOaLeaveQuota(quota);
        }
        return R.ok(true);
    }

    @Override
    public R<Boolean> generateAnnualQuota(Integer year, SysUser user, String source) {
        leaveQuotaService.generateAnnualQuota(year == null ? LocalDate.now().getYear() : year, user);
        return R.ok(true);
    }

    private Date parseDate(String value) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value);
        } catch (ParseException e) {
            throw new ServiceException("时间格式必须为 yyyy-MM-dd HH:mm:ss");
        }
    }
}
