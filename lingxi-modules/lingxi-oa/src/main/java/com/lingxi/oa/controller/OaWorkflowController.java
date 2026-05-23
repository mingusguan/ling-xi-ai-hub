package com.lingxi.oa.controller;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.common.security.annotation.InnerAuth;
import com.lingxi.common.security.annotation.Logical;
import com.lingxi.common.security.annotation.RequiresPermissions;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.oa.domain.OaExpense;
import com.lingxi.oa.domain.OaHolidayConfig;
import com.lingxi.oa.domain.OaLeave;
import com.lingxi.oa.domain.OaProcessTemplate;
import com.lingxi.oa.domain.dto.OaApprovalAction;
import com.lingxi.oa.domain.dto.OaCancelRequest;
import com.lingxi.oa.service.IOaWorkflowBizService;
import com.lingxi.oa.service.IOaWorkflowService;
import com.lingxi.oa.util.HolidayUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OA 工作流控制器。
 *
 * @author lingxi
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/oa/workflow")
public class OaWorkflowController extends OaBaseController
{
    private final IOaWorkflowBizService workflowBizService;
    private final IOaWorkflowService workflowService;
    private final HolidayUtil holidayUtil;

    @RequiresPermissions("oa:leave:list")
    @GetMapping("/leave/list")
    public TableDataInfo leaveList(@RequestParam(required = false) String approvalStatus,
                                   @RequestParam(required = false) Integer pageNum,
                                   @RequestParam(required = false) Integer pageSize)
    {
        return buildPage(workflowBizService.listLeaves(approvalStatus), pageNum, pageSize);
    }

    @RequiresPermissions("oa:leave:list")
    @GetMapping("/leave/my-approved")
    public TableDataInfo myApprovedLeaves(@RequestParam(required = false) Integer pageNum,
                                          @RequestParam(required = false) Integer pageSize)
    {
        return buildPage(workflowBizService.listProcessedLeaves(), pageNum, pageSize);
    }

    @RequiresPermissions(value = {"oa:leave:add", "oa:leave:edit"}, logical = Logical.OR)
    @PostMapping("/leave")
    public R<?> saveLeave(@RequestBody OaLeave leave)
    {
        return R.ok(workflowBizService.saveLeave(leave));
    }

    @RequiresPermissions("oa:leave:edit")
    @PostMapping("/leave/approve")
    public R<?> approveLeave(@RequestBody OaApprovalAction action)
    {
        return R.ok(workflowBizService.approveLeave(action));
    }

    @RequiresPermissions("oa:leave:list")
    @GetMapping("/leave/{leaveId}")
    public R<?> getLeaveDetail(@PathVariable Long leaveId)
    {
        return R.ok(workflowBizService.getLeaveById(leaveId));
    }

    @RequiresPermissions("oa:leave:list")
    @GetMapping("/leave/calculate-duration")
    public R<?> calculateLeaveDuration(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime)
    {
        if (startTime == null || endTime == null || !startTime.before(endTime)) {
            throw new ServiceException("开始时间必须早于结束时间");
        }

        long workHours = holidayUtil.calculateWorkHours(startTime, endTime);
        List<OaHolidayConfig> restDays = holidayUtil.getRestDaysInRange(startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("workHours", workHours);
        result.put("workDays", workHours / 8.0);
        result.put("restDays", restDays);
        return R.ok(result);
    }

    @RequiresPermissions("oa:expense:list")
    @GetMapping("/expense/list")
    public TableDataInfo expenseList(@RequestParam(required = false) String approvalStatus,
                                     @RequestParam(required = false) Integer pageNum,
                                     @RequestParam(required = false) Integer pageSize)
    {
        return buildPage(workflowBizService.listExpenses(approvalStatus), pageNum, pageSize);
    }

    @RequiresPermissions("oa:expense:list")
    @GetMapping("/expense/my-approved")
    public TableDataInfo myApprovedExpenses(@RequestParam(required = false) Integer pageNum,
                                            @RequestParam(required = false) Integer pageSize)
    {
        return buildPage(workflowBizService.listProcessedExpenses(), pageNum, pageSize);
    }

    @RequiresPermissions(value = {"oa:expense:add", "oa:expense:edit"}, logical = Logical.OR)
    @PostMapping("/expense")
    public R<?> saveExpense(@RequestBody OaExpense expense)
    {
        return R.ok(workflowBizService.saveExpense(expense));
    }

    @RequiresPermissions("oa:expense:edit")
    @PostMapping("/expense/approve")
    public R<?> approveExpense(@RequestBody OaApprovalAction action)
    {
        return R.ok(workflowBizService.approveExpense(action));
    }

    @RequiresPermissions("oa:leave:edit")
    @PostMapping("/leave/cancel")
    public R<?> cancelLeave(@Valid OaCancelRequest request)
    {
        workflowBizService.cancelLeaveApplication(request.getBusinessKey(), request.getProcessInstanceId());
        return R.ok("取消成功");
    }

    @RequiresPermissions("oa:expense:edit")
    @PostMapping("/expense/cancel")
    public R<?> cancelExpense(@Valid OaCancelRequest request)
    {
        workflowBizService.cancelExpenseApplication(request.getBusinessKey(), request.getProcessInstanceId());
        return R.ok("取消成功");
    }

    @RequiresPermissions("oa:expense:list")
    @GetMapping("/expense/{expenseId}")
    public R<?> getExpenseDetail(@PathVariable Long expenseId)
    {
        return R.ok(workflowBizService.getExpenseById(expenseId));
    }

    @RequiresPermissions(value = {"oa:process:list", "oa:leave:rule:list"}, logical = Logical.OR)
    @GetMapping("/template/list")
    public TableDataInfo templateList(@RequestParam(required = false) String businessType,
                                      @RequestParam(required = false) Integer pageNum,
                                      @RequestParam(required = false) Integer pageSize)
    {
        return buildPage(workflowBizService.listTemplates(businessType), pageNum, pageSize);
    }

    @RequiresPermissions(value = {"oa:process:list", "oa:leave:rule:list"}, logical = Logical.OR)
    @GetMapping("/template/{templateId}")
    public R<?> templateDetail(@PathVariable Long templateId)
    {
        return R.ok(workflowBizService.getTemplateById(templateId));
    }

    @RequiresPermissions(value = {"oa:process:list", "oa:leave:add", "oa:leave:edit"}, logical = Logical.OR)
    @PostMapping("/template")
    public R<?> saveTemplate(@RequestBody OaProcessTemplate template)
    {
        return R.ok(workflowBizService.saveTemplate(template));
    }

    @RequiresPermissions(value = {"oa:process:list", "oa:leave:rule:list", "oa:leave:list", "oa:expense:list"}, logical = Logical.OR)
    @GetMapping("/template/active")
    public R<?> activeTemplate(@RequestParam String businessType)
    {
        return R.ok(workflowBizService.getActiveTemplateMeta(businessType));
    }

    @RequiresPermissions(value = {"oa:process:list", "oa:leave:edit"}, logical = Logical.OR)
    @PostMapping("/template/{templateId}/deploy")
    public R<?> deployTemplate(@PathVariable Long templateId)
    {
        return R.ok(workflowService.deployTemplate(templateId));
    }

    @RequiresPermissions(value = {"oa:process:list", "oa:leave:remove"}, logical = Logical.OR)
    @DeleteMapping("/process/cleanup")
    public R<?> cleanupProcessDefinitions(@RequestParam String processDefinitionKey)
    {
        workflowService.cleanupProcessDefinitions(processDefinitionKey);
        return R.ok();
    }

    @RequiresPermissions(value = {"oa:dashboard:view", "oa:leave:list", "oa:expense:list"}, logical = Logical.OR)
    @GetMapping("/task/todo")
    public R<?> todoTasks(@RequestParam(required = false) String assignee)
    {
        String actualAssignee = (assignee == null || assignee.isBlank()) ? String.valueOf(SecurityUtils.getUserId()) : assignee;
        return R.ok(workflowService.listTodoTasks(actualAssignee));
    }

    @RequiresPermissions(value = {"oa:leave:list", "oa:expense:list"}, logical = Logical.OR)
    @GetMapping("/history")
    public R<?> history(@RequestParam String processInstanceId)
    {
        return R.ok(workflowService.listHistory(processInstanceId));
    }

    @RequiresPermissions(value = {"oa:leave:list", "oa:expense:list"}, logical = Logical.OR)
    @GetMapping("/record/list")
    public R<?> recordList(@RequestParam(required = false) String businessKey,
                           @RequestParam(required = false) String processInstanceId)
    {
        return R.ok(workflowBizService.listApprovalRecords(businessKey, processInstanceId));
    }

    @InnerAuth
    @GetMapping("/pending/{userId}")
    public R<List<Map<String, Object>>> getPendingTasks(@PathVariable Long userId)
    {
        return R.ok(workflowBizService.queryPendingTasks(userId));
    }

    @InnerAuth
    @GetMapping("/leave/balance/{userId}")
    public R<Map<String, Object>> getLeaveBalance(@PathVariable Long userId)
    {
        return R.ok(workflowBizService.queryLeaveBalance(userId));
    }

    @InnerAuth
    @GetMapping("/expense/status/{userId}")
    public R<List<Map<String, Object>>> getExpenseStatus(@PathVariable Long userId)
    {
        return R.ok(workflowBizService.queryExpenseStatus(userId));
    }

    @InnerAuth
    @GetMapping("/timeout/warning/{userId}")
    public R<List<Map<String, Object>>> getTimeoutWarning(@PathVariable Long userId)
    {
        return R.ok(workflowBizService.queryTimeoutWarning(userId));
    }

    @RequiresPermissions(value = {"oa:dashboard:view", "oa:leave:list", "oa:expense:list"}, logical = Logical.OR)
    @GetMapping("/timeout/warning/current")
    public R<List<Map<String, Object>>> getCurrentUserTimeoutWarning()
    {
        Long userId = SecurityUtils.getUserId();
        return R.ok(workflowBizService.queryTimeoutWarning(userId));
    }
}
