package com.lingxi.oa.controller;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.common.security.annotation.InnerAuth;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.oa.domain.OaExpense;
import com.lingxi.oa.domain.OaHolidayConfig;
import com.lingxi.oa.domain.OaLeave;
import com.lingxi.oa.domain.OaProcessTemplate;
import com.lingxi.oa.domain.dto.OaApprovalAction;
import com.lingxi.oa.service.IOaWorkflowBizService;
import com.lingxi.oa.service.IOaWorkflowService;
import com.lingxi.oa.util.HolidayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OA工作流控制器
 * 提供请假、报销等业务流程的审批管理功能
 *
 * @author lingxi
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/oa/workflow")
public class OaWorkflowController extends OaBaseController {

    private final IOaWorkflowBizService workflowBizService;
    private final IOaWorkflowService workflowService;
    private final HolidayUtil holidayUtil;

    /**
     * 获取请假申请列表
     *
     * @param approvalStatus 审批状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页的请假申请列表
     */
    @GetMapping("/leave/list")
    public TableDataInfo leaveList(@RequestParam(required = false) String approvalStatus,
                                   @RequestParam(required = false) Integer pageNum,
                                   @RequestParam(required = false) Integer pageSize) {
        return buildPage(workflowBizService.listLeaves(approvalStatus), pageNum, pageSize);
    }

    /**
     * 获取我审批过的请假申请列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页的已审批请假列表
     */
    @GetMapping("/leave/my-approved")
    public TableDataInfo myApprovedLeaves(@RequestParam(required = false) Integer pageNum,
                                          @RequestParam(required = false) Integer pageSize) {
        return buildPage(workflowBizService.listProcessedLeaves(), pageNum, pageSize);
    }

    /**
     * 保存请假申请（新增或更新）
     *
     * @param leave 请假申请信息
     * @return 操作结果
     */
    @PostMapping("/leave")
    public R<?> saveLeave(@RequestBody OaLeave leave) {
        return R.ok(workflowBizService.saveLeave(leave));
    }

    /**
     * 审批请假申请
     *
     * @param action 审批动作（包含任务ID、审批意见、是否通过等）
     * @return 操作结果
     */
    @PostMapping("/leave/approve")
    public R<?> approveLeave(@RequestBody OaApprovalAction action) {
        return R.ok(workflowBizService.approveLeave(action));
    }

    /**
     * 根据ID获取请假详情
     *
     * @param leaveId 请假ID
     * @return 请假详细信息
     */
    @GetMapping("/leave/{leaveId}")
    public R<?> getLeaveDetail(@PathVariable Long leaveId) {
        return R.ok(workflowBizService.getLeaveById(leaveId));
    }

    /**
     * 计算请假工作时长（用于前端实时显示）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 工作时长（小时）和工作天数
     */
    @GetMapping("/leave/calculate-duration")
    public R<?> calculateLeaveDuration(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        try {
            long workHours = holidayUtil.calculateWorkHours(startTime, endTime);
            
            // 获取时间范围内的休息日列表
            List<OaHolidayConfig> restDays = holidayUtil.getRestDaysInRange(startTime, endTime);
            
            Map<String, Object> result = new HashMap<>();
            result.put("workHours", workHours);
            result.put("workDays", workHours / 8.0); // 按每天8小时计算天数
            result.put("restDays", restDays); // 休息日列表
            
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("计算失败: " + e.getMessage());
        }
    }

    /**
     * 获取报销申请列表
     *
     * @param approvalStatus 审批状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页的报销申请列表
     */
    @GetMapping("/expense/list")
    public TableDataInfo expenseList(@RequestParam(required = false) String approvalStatus,
                                     @RequestParam(required = false) Integer pageNum,
                                     @RequestParam(required = false) Integer pageSize) {
        return buildPage(workflowBizService.listExpenses(approvalStatus), pageNum, pageSize);
    }

    /**
     * 获取我审批过的报销申请列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页的已审批报销列表
     */
    @GetMapping("/expense/my-approved")
    public TableDataInfo myApprovedExpenses(@RequestParam(required = false) Integer pageNum,
                                            @RequestParam(required = false) Integer pageSize) {
        return buildPage(workflowBizService.listProcessedExpenses(), pageNum, pageSize);
    }

    /**
     * 保存报销申请（新增或更新）
     *
     * @param expense 报销申请信息
     * @return 操作结果
     */
    @PostMapping("/expense")
    public R<?> saveExpense(@RequestBody OaExpense expense) {
        return R.ok(workflowBizService.saveExpense(expense));
    }

    /**
     * 审批报销申请
     *
     * @param action 审批动作（包含任务ID、审批意见、是否通过等）
     * @return 操作结果
     */
    @PostMapping("/expense/approve")
    public R<?> approveExpense(@RequestBody OaApprovalAction action) {
        return R.ok(workflowBizService.approveExpense(action));
    }

    /**
     * 取消请假申请
     *
     * @param businessKey 业务key
     * @param processInstanceId 流程实例ID
     * @return 操作结果
     */
    @PostMapping("/leave/cancel")
    public R<?> cancelLeave(@RequestParam String businessKey, @RequestParam String processInstanceId) {
        workflowBizService.cancelLeaveApplication(businessKey, processInstanceId);
        return R.ok("取消成功");
    }

    /**
     * 取消报销申请
     *
     * @param businessKey 业务key
     * @param processInstanceId 流程实例ID
     * @return 操作结果
     */
    @PostMapping("/expense/cancel")
    public R<?> cancelExpense(@RequestParam String businessKey, @RequestParam String processInstanceId) {
        workflowBizService.cancelExpenseApplication(businessKey, processInstanceId);
        return R.ok("取消成功");
    }

    /**
     * 根据ID获取报销详情
     *
     * @param expenseId 报销ID
     * @return 报销详细信息
     */
    @GetMapping("/expense/{expenseId}")
    public R<?> getExpenseDetail(@PathVariable Long expenseId) {
        return R.ok(workflowBizService.getExpenseById(expenseId));
    }

    /**
     * 获取流程模板列表
     *
     * @param businessType 业务类型（可选）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页的流程模板列表
     */
    @GetMapping("/template/list")
    public TableDataInfo templateList(@RequestParam(required = false) String businessType,
                                      @RequestParam(required = false) Integer pageNum,
                                      @RequestParam(required = false) Integer pageSize) {
        return buildPage(workflowBizService.listTemplates(businessType), pageNum, pageSize);
    }

    /**
     * 获取流程模板详情
     *
     * @param templateId 模板ID
     * @return 模板详细信息
     */
    @GetMapping("/template/{templateId}")
    public R<?> templateDetail(@PathVariable Long templateId) {
        return R.ok(workflowBizService.getTemplateById(templateId));
    }

    /**
     * 保存流程模板（新增或更新）
     *
     * @param template 流程模板信息
     * @return 操作结果
     */
    @PostMapping("/template")
    public R<?> saveTemplate(@RequestBody OaProcessTemplate template) {
        return R.ok(workflowBizService.saveTemplate(template));
    }

    /**
     * 获取指定业务类型的激活模板元数据
     *
     * @param businessType 业务类型
     * @return 激活的模板信息
     */
    @GetMapping("/template/active")
    public R<?> activeTemplate(@RequestParam String businessType) {
        return R.ok(workflowBizService.getActiveTemplateMeta(businessType));
    }

    /**
     * 部署流程模板
     * 将模板发布为可执行的流程定义
     *
     * @param templateId 模板ID
     * @return 操作结果
     */
    @PostMapping("/template/{templateId}/deploy")
    public R<?> deployTemplate(@PathVariable Long templateId) {
        return R.ok(workflowService.deployTemplate(templateId));
    }

    /**
     * 清理流程定义
     * 删除指定流程定义key的所有历史版本
     *
     * @param processDefinitionKey 流程定义key
     * @return 操作结果
     */
    @DeleteMapping("/process/cleanup")
    public R<?> cleanupProcessDefinitions(@RequestParam String processDefinitionKey) {
        workflowService.cleanupProcessDefinitions(processDefinitionKey);
        return R.ok();
    }

    /**
     * 获取待办任务列表
     *
     * @param assignee 任务办理人（可选，默认为当前登录用户）
     * @return 待办任务列表
     */
    @GetMapping("/task/todo")
    public R<?> todoTasks(@RequestParam(required = false) String assignee) {
        String actualAssignee = (assignee == null || assignee.isBlank()) ? String.valueOf(SecurityUtils.getUserId()) : assignee;
        return R.ok(workflowService.listTodoTasks(actualAssignee));
    }

    /**
     * 获取流程实例的历史记录
     *
     * @param processInstanceId 流程实例ID
     * @return 流程历史记录
     */
    @GetMapping("/history")
    public R<?> history(@RequestParam String processInstanceId) {
        return R.ok(workflowService.listHistory(processInstanceId));
    }

    /**
     * 获取审批记录列表
     *
     * @param businessKey 业务单据key（可选）
     * @param processInstanceId 流程实例ID（可选）
     * @return 审批记录列表
     */
    @GetMapping("/record/list")
    public R<?> recordList(@RequestParam(required = false) String businessKey,
                           @RequestParam(required = false) String processInstanceId) {
        return R.ok(workflowBizService.listApprovalRecords(businessKey, processInstanceId));
    }

    // ==================== 内部调用接口（供ai模块使用） ====================

    /**
     * 查询用户待办任务（内部调用）
     */
    @InnerAuth
    @GetMapping("/pending/{userId}")
    public R<List<Map<String, Object>>> getPendingTasks(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> result = workflowBizService.queryPendingTasks(userId);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("查询待办任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户假期余额（内部调用）
     */
    @InnerAuth
    @GetMapping("/leave/balance/{userId}")
    public R<Map<String, Object>> getLeaveBalance(@PathVariable Long userId) {
        try {
            Map<String, Object> data = workflowBizService.queryLeaveBalance(userId);
            return R.ok(data);
        } catch (Exception e) {
            return R.fail("查询假期余额失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户报销状态（内部调用）
     */
    @InnerAuth
    @GetMapping("/expense/status/{userId}")
    public R<List<Map<String, Object>>> getExpenseStatus(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> result = workflowBizService.queryExpenseStatus(userId);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("查询报销状态失败: " + e.getMessage());
        }
    }

    /**
     * 查询超时预警（内部调用）
     */
    @InnerAuth
    @GetMapping("/timeout/warning/{userId}")
    public R<List<Map<String, Object>>> getTimeoutWarning(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> warnings = workflowBizService.queryTimeoutWarning(userId);
            return R.ok(warnings);
        } catch (Exception e) {
            return R.fail("查询超时预警失败: " + e.getMessage());
        }
    }

    /**
     * 查询当前用户的超时预警列表
     */
    @GetMapping("/timeout/warning/current")
    public R<List<Map<String, Object>>> getCurrentUserTimeoutWarning() {
        try {
            Long userId = SecurityUtils.getUserId();
            List<Map<String, Object>> warnings = workflowBizService.queryTimeoutWarning(userId);
            return R.ok(warnings);
        } catch (Exception e) {
            return R.fail("查询超时预警失败: " + e.getMessage());
        }
    }
}
