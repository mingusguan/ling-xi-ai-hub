package com.lingxi.oa.service;

import com.lingxi.oa.domain.OaExpense;
import com.lingxi.oa.domain.OaLeave;
import com.lingxi.oa.domain.OaProcessTemplate;
import com.lingxi.oa.domain.dto.OaApprovalAction;
import com.lingxi.oa.domain.dto.OaTemplateMeta;
import com.lingxi.oa.dto.DashboardSummaryDTO;
import com.lingxi.oa.dto.OaLeaveDTO;
import com.lingxi.oa.dto.TaskCompleteResultDTO;

import java.util.List;

/**
 * OA 工作流业务服务接口
 *
 * @author lingxi
 */
public interface IOaWorkflowBizService {

    /**
     * 工作台概览
     */
    DashboardSummaryDTO dashboardSummary();

    /**
     * 查询请假列表（包含自己提交的和需要自己审核的）- 待处理
     *
     * @param approvalStatus 审批状态
     * @return 请假列表
     */
    List<OaLeaveDTO> listLeaves(String approvalStatus);

    /**
     * 查询我发起的已结束申请 + 我审批过的数据 - 已处理
     *
     * @return 请假列表
     */
    List<OaLeaveDTO> listProcessedLeaves();

    /**
     * 保存请假申请
     *
     * @param leave 请假实体
     * @return 请假ID
     */
    Long saveLeave(OaLeave leave);

    /**
     * 根据ID查询请假详情
     *
     * @param leaveId 请假ID
     * @return 请假实体
     */
    OaLeave getLeaveById(Long leaveId);

    /**
     * 查询报销列表（包含自己提交的和需要自己审核的）- 待处理
     *
     * @param approvalStatus 审批状态
     * @return 报销列表
     */
    List<OaExpense> listExpenses(String approvalStatus);

    /**
     * 查询我审批过的报销历史 - 已处理
     *
     * @return 报销列表
     */
    List<OaExpense> listProcessedExpenses();

    /**
     * 保存报销申请
     *
     * @param expense 报销实体
     * @return 报销ID
     */
    Long saveExpense(OaExpense expense);

    /**
     * 根据ID查询报销详情
     *
     * @param expenseId 报销ID
     * @return 报销实体
     */
    OaExpense getExpenseById(Long expenseId);

    /**
     * 查询流程模板列表
     *
     * @param businessType 业务类型
     * @return 模板列表
     */
    List<OaProcessTemplate> listTemplates(String businessType);

    /**
     * 根据ID查询流程模板
     *
     * @param templateId 模板ID
     * @return 模板实体
     */
    OaProcessTemplate getTemplateById(Long templateId);

    /**
     * 获取激活的流程模板元数据
     *
     * @param businessType 业务类型
     * @return 模板元数据
     */
    OaTemplateMeta getActiveTemplateMeta(String businessType);

    /**
     * 保存流程模板
     *
     * @param template 模板实体
     * @return 模板ID
     */
    Long saveTemplate(OaProcessTemplate template);

    /**
     * 审批请假
     *
     * @param action 审批动作
     * @return 任务完成结果
     */
    TaskCompleteResultDTO approveLeave(OaApprovalAction action);

    /**
     * 审批报销
     *
     * @param action 审批动作
     * @return 任务完成结果
     */
    TaskCompleteResultDTO approveExpense(OaApprovalAction action);

    /**
     * 查询审批记录
     *
     * @param businessKey 业务键
     * @param processInstanceId 流程实例ID
     * @return 审批记录列表
     */
    List<?> listApprovalRecords(String businessKey, String processInstanceId);

    // ==================== AI聊天助手相关接口 ====================

    /**
     * 查询用户待办任务
     *
     * @param userId 用户ID
     * @return 待办任务列表
     */
    List<java.util.Map<String, Object>> queryPendingTasks(Long userId);

    /**
     * 查询用户假期余额
     *
     * @param userId 用户ID
     * @return 假期余额
     */
    java.util.Map<String, Object> queryLeaveBalance(Long userId);

    /**
     * 查询用户报销状态
     *
     * @param userId 用户ID
     * @return 报销列表
     */
    List<java.util.Map<String, Object>> queryExpenseStatus(Long userId);

    /**
     * 查询超时预警
     *
     * @param userId 用户ID
     * @return 预警列表
     */
    List<java.util.Map<String, Object>> queryTimeoutWarning(Long userId);

    /**
     * 取消请假申请
     *
     * @param businessKey 业务key
     * @param processInstanceId 流程实例ID
     */
    void cancelLeaveApplication(String businessKey, String processInstanceId);

    /**
     * 取消报销申请
     *
     * @param businessKey 业务key
     * @param processInstanceId 流程实例ID
     */
    void cancelExpenseApplication(String businessKey, String processInstanceId);
}
