package com.lingxi.oa.mcp;

import com.lingxi.oa.domain.OaHolidayConfig;
import com.lingxi.oa.service.IOaWorkflowBizService;
import com.lingxi.oa.util.HolidayUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OaMcpTools {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final IOaWorkflowBizService workflowBizService;

    private final HolidayUtil holidayUtil;

    public OaMcpTools(IOaWorkflowBizService workflowBizService, HolidayUtil holidayUtil) {
        this.workflowBizService = workflowBizService;
        this.holidayUtil = holidayUtil;
    }

    @Tool(name = "query_pending_tasks", description = "查询用户待审批的 OA 任务列表。")
    public List<Map<String, Object>> queryPendingTasks(
            @ToolParam(description = "用户 ID。") Long userId) {
        return workflowBizService.queryPendingTasks(userId);
    }

    @Tool(name = "query_leave_balance", description = "查询用户假期余额，包括年假、调休假等。")
    public Map<String, Object> queryLeaveBalance(
            @ToolParam(description = "用户 ID。") Long userId) {
        return workflowBizService.queryLeaveBalance(userId);
    }

    @Tool(name = "query_expense_status", description = "查询用户近期 OA 报销记录及审批状态。")
    public List<Map<String, Object>> queryExpenseStatus(
            @ToolParam(description = "用户 ID。") Long userId) {
        return workflowBizService.queryExpenseStatus(userId);
    }

    @Tool(name = "query_timeout_warning", description = "查询用户超时未审批的预警任务。")
    public List<Map<String, Object>> queryTimeoutWarning(
            @ToolParam(description = "用户 ID。") Long userId) {
        return workflowBizService.queryTimeoutWarning(userId);
    }

    @Tool(name = "calculate_leave_duration", description = "按工作时间计算请假时长，自动排除周末和已配置节假日。")
    public Map<String, Object> calculateLeaveDuration(
            @ToolParam(description = "开始时间，格式为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss。") String startDate,
            @ToolParam(description = "结束时间，格式为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss。") String endDate) {
        Date startTime = toDate(startDate, LocalTime.of(9, 0));
        Date endTime = toDate(endDate, LocalTime.of(18, 0));
        long workHours = holidayUtil.calculateWorkHours(startTime, endTime);
        List<OaHolidayConfig> restDays = holidayUtil.getRestDaysInRange(startTime, endTime);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workHours", workHours);
        result.put("workDays", workHours / 8.0);
        result.put("restDays", restDays);
        return result;
    }

    private Date toDate(String value, LocalTime defaultTime) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("日期参数不能为空。");
        }

        LocalDateTime dateTime = parseDateTime(value.trim(), defaultTime);
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime parseDateTime(String value, LocalTime defaultTime) {
        try {
            if (value.length() == 10) {
                return LocalDate.parse(value, DATE_FORMATTER).atTime(defaultTime);
            }
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式必须为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss。", e);
        }
    }
}
