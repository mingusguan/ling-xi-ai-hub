 package com.lingxi.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.oa.domain.*;
import com.lingxi.oa.domain.dto.OaApprovalAction;
import com.lingxi.oa.domain.dto.OaTemplateField;
import com.lingxi.oa.domain.dto.OaTemplateMeta;
import com.lingxi.oa.dto.DashboardSummaryDTO;
import com.lingxi.oa.dto.OaLeaveDTO;
import com.lingxi.oa.dto.ProcessStartResultDTO;
import com.lingxi.oa.dto.TaskCompleteResultDTO;
import com.lingxi.oa.dto.TodoTaskDTO;
import com.lingxi.oa.enums.ApprovalActionEnum;
import com.lingxi.oa.enums.ApprovalStatusEnum;
import com.lingxi.oa.mapper.OaExpenseMapper;
import com.lingxi.oa.mapper.OaLeaveMapper;
import com.lingxi.oa.mapper.OaProcessTemplateMapper;
import com.lingxi.oa.mapper.OaProcessTimeoutWarningMapper;
import com.lingxi.oa.service.IOaWorkflowBizService;
import com.lingxi.oa.service.IOaWorkflowService;
import com.lingxi.oa.service.IOaApprovalRecordService;
import com.lingxi.oa.service.IOaLeaveQuotaService;
import com.lingxi.oa.util.HolidayUtil;
import com.lingxi.system.api.RemoteFileService;
import com.lingxi.system.api.RemoteUserService;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
    public class OaWorkflowBizServiceImpl implements IOaWorkflowBizService {

    private final OaLeaveMapper leaveMapper;
    private final OaExpenseMapper expenseMapper;
    private final OaProcessTemplateMapper templateMapper;
    private final IOaWorkflowService workflowService;
    private final IOaApprovalRecordService approvalRecordService;
    private final IOaLeaveQuotaService leaveQuotaService; // P0: 注入假期额度Service
    private final OaProcessTimeoutWarningMapper timeoutWarningMapper; // 超时预警Mapper
    protected final ObjectProvider<HistoryService> historyServiceProvider;
    protected final ObjectProvider<RuntimeService> runtimeServiceProvider; // RuntimeService
    private final RemoteUserService remoteUserService; // 远程用户服务
    
    @Resource
    private RemoteFileService remoteFileService;

    @Resource
    private HolidayUtil holidayUtil; // 休息日工具类

    @Override
    public DashboardSummaryDTO dashboardSummary() {
        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        String currentUsername = SecurityUtils.getUsername();

        // 待审批统计
        summary.setLeavePendingCount(leaveMapper.selectCount(new LambdaQueryWrapper<OaLeave>().eq(OaLeave::getApprovalStatus, ApprovalStatusEnum.PENDING.getCode())));
        summary.setExpensePendingCount(expenseMapper.selectCount(new LambdaQueryWrapper<OaExpense>().eq(OaExpense::getApprovalStatus, ApprovalStatusEnum.PENDING.getCode())));
        summary.setTemplateCount(templateMapper.selectCount(null));
        summary.setTodoWorkflowCount(workflowService.listTodoTasks(String.valueOf(SecurityUtils.getUserId())).size());

        // 今日已审批数：查询今天完成的审批任务
        HistoryService historyService = historyServiceProvider.getIfAvailable();
        int approvedTodayCount = 0;
        if (historyService != null) {
            try {
                // 获取今天的开始时间
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                Date todayStart = calendar.getTime();

                // 查询今天完成的任务
                List<HistoricTaskInstance> finishedTasks = historyService.createHistoricTaskInstanceQuery()
                        .taskAssignee(currentUsername)
                        .finished()
                        .taskCompletedAfter(todayStart)
                        .list();

                approvedTodayCount = finishedTasks != null ? finishedTasks.size() : 0;
            } catch (Exception e) {
                log.error("查询今日已审批数失败", e);
            }
        }
        summary.setApprovedTodayCount(approvedTodayCount);

        // 超时预警数：从 oa_process_timeout_warning 表查询当前用户未处理的预警
        int timeoutWarningCount = 0;
        try {
            Long userId = SecurityUtils.getUserId();
            List<OaProcessTimeoutWarning> warnings = timeoutWarningMapper.selectUnprocessedByAssignee(String.valueOf(userId));
            timeoutWarningCount = warnings != null ? warnings.size() : 0;
        } catch (Exception e) {
            log.error("查询超时预警数失败", e);
        }
        summary.setTimeoutWarningCount(timeoutWarningCount);

        summary.setWelcomeText("OA workbench is ready");
        return summary;
    }

    @Override
    public List<OaLeaveDTO> listLeaves(String approvalStatus) {
        Long currentUserId = SecurityUtils.getUserId();
        
        // 1. 查询自己提交的请假申请（包括 pending 和 rejected）
        List<OaLeave> myLeaves = leaveMapper.selectList(new LambdaQueryWrapper<OaLeave>()
                .eq(OaLeave::getApplicantUserId, currentUserId)
                .in(OaLeave::getApprovalStatus, "pending", "rejected")  // 包含待审批和已驳回
                .eq(StringUtils.isNotBlank(approvalStatus), OaLeave::getApprovalStatus, approvalStatus)
                .orderByDesc(OaLeave::getCreateTime));
        
        // 2. 查询需要我审核的请假申请（从工作流待办任务中获取，包含个人任务和角色候选任务）
        List<TodoTaskDTO> todoTasks = workflowService.listTodoTasks(String.valueOf(SecurityUtils.getUserId()));
        List<String> leaveProcessInstanceIds = todoTasks.stream()
                .filter(task -> task.getBusinessKey() != null && task.getBusinessKey().startsWith("leave:"))
                .map(TodoTaskDTO::getProcessInstanceId)
                .distinct()
                .collect(Collectors.toList());
        
        List<OaLeave> needApprovalLeaves = new ArrayList<>();
        if (!leaveProcessInstanceIds.isEmpty()) {
            needApprovalLeaves = leaveMapper.selectList(new LambdaQueryWrapper<OaLeave>()
                    .in(OaLeave::getProcessInstanceId, leaveProcessInstanceIds)
                    .eq(StringUtils.isNotBlank(approvalStatus), OaLeave::getApprovalStatus, approvalStatus)
                    .orderByDesc(OaLeave::getCreateTime));
        }
        
        // 3. 合并列表并设置权限标识（使用批量转换优化性能）
        Map<Long, OaLeaveDTO> leaveMap = new LinkedHashMap<>();
        
        // 添加自己提交的（无审批权限，但如果是已驳回状态，可以重新编辑）
        List<OaLeaveDTO> myLeaveDTOs = batchConvertToLeaveDTOs(myLeaves, false);
        for (OaLeaveDTO dto : myLeaveDTOs) {
            // 如果是已驳回状态，标记为可以重新编辑
            if ("rejected".equals(dto.getApprovalStatus())) {
                dto.setCanApprove(false); // 不能审批
                dto.setCanResubmit(true); // 可以重新提交
            }
            leaveMap.put(dto.getLeaveId(), dto);
        }
        
        // 添加需要审批的（有审批权限，覆盖之前的canApprove标识）
        List<OaLeaveDTO> needApprovalDTOs = batchConvertToLeaveDTOs(needApprovalLeaves, true);
        for (OaLeaveDTO dto : needApprovalDTOs) {
            leaveMap.put(dto.getLeaveId(), dto); // 如果已存在，会更新为可审批
        }
        
        return new ArrayList<>(leaveMap.values());
    }

    /**
     * 批量转换请假DTO，优化用户信息查询
     */
    private List<OaLeaveDTO> batchConvertToLeaveDTOs(List<OaLeave> leaves, Boolean canApprove) {
        if (leaves == null || leaves.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 收集所有用户ID
        Set<Long> userIds = leaves.stream()
                .map(OaLeave::getApplicantUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 批量查询用户信息
        Map<Long, String> userNameMap = batchGetUserNames(userIds);
        
        // 转换为DTO
        List<OaLeaveDTO> result = new ArrayList<>();
        for (OaLeave leave : leaves) {
            OaLeaveDTO dto = new OaLeaveDTO();
            dto.setLeaveId(leave.getLeaveId());
            dto.setLeaveNo(leave.getLeaveNo());
            dto.setLeaveType(leave.getLeaveType());
            dto.setStartTime(leave.getStartTime());
            dto.setEndTime(leave.getEndTime());
            dto.setLeaveHours(leave.getLeaveHours());
            dto.setLeaveReason(leave.getLeaveReason());
            dto.setApprovalStatus(leave.getApprovalStatus());
            dto.setApplicantUserId(leave.getApplicantUserId());
            dto.setApplicantUserName(userNameMap.getOrDefault(leave.getApplicantUserId(), ""));
            dto.setDeptId(leave.getDeptId());
            dto.setDeptName(leave.getDeptName());
            dto.setProcessInstanceId(leave.getProcessInstanceId());
            dto.setCreateBy(leave.getCreateBy());
            dto.setCreateTime(leave.getCreateTime());
            dto.setUpdateBy(leave.getUpdateBy());
            dto.setUpdateTime(leave.getUpdateTime());
            dto.setCanApprove(canApprove);
            
            // 获取当前节点名称
            if (StringUtils.isNotBlank(leave.getProcessInstanceId())) {
                dto.setCurrentNodeName(getCurrentTaskName(leave.getProcessInstanceId()));
            }
            
            result.add(dto);
        }
        
        return result;
    }

    /**
     * 批量查询用户姓名
     */
    private Map<Long, String> batchGetUserNames(Set<Long> userIds) {
        Map<Long, String> userNameMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return userNameMap;
        }
        
        try {
            // 批量查询用户信息
            R<List<SysUser>> result = remoteUserService.getUserByIds(new ArrayList<>(userIds), SecurityConstants.INNER);
            if (result != null && result.getData() != null) {
                for (SysUser user : result.getData()) {
                    userNameMap.put(user.getUserId(), user.getNickName());
                }
            }
        } catch (Exception e) {
            log.error("批量查询用户信息失败", e);
        }
        
        return userNameMap;
    }

    @Override
    public List<OaLeaveDTO> listProcessedLeaves() {
        Long currentUserId = SecurityUtils.getUserId();
        HistoryService historyService = historyServiceProvider.getIfAvailable();
        
        // 1. 查询自己提交的已结束的请假申请（approved 或 rejected）
        List<OaLeave> myEndedLeaves = leaveMapper.selectList(new LambdaQueryWrapper<OaLeave>()
                .eq(OaLeave::getApplicantUserId, currentUserId)
                .in(OaLeave::getApprovalStatus, ApprovalStatusEnum.APPROVED.getCode(), ApprovalStatusEnum.REJECTED.getCode())
                .orderByDesc(OaLeave::getUpdateTime));
        
        // 2. 查询我审批过的请假记录
        Set<Long> approvedLeaveIds = new HashSet<>();
        if (historyService != null) {
            try {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                        .taskAssignee(String.valueOf(currentUserId))
                        .finished()
                        .list();
                
                if (historicTasks != null && !historicTasks.isEmpty()) {
                    Set<String> processInstanceIds = historicTasks.stream()
                            .map(HistoricTaskInstance::getProcessInstanceId)
                            .collect(Collectors.toSet());
                    
                    List<OaLeave> approvedLeaves = leaveMapper.selectList(new LambdaQueryWrapper<OaLeave>()
                            .in(OaLeave::getProcessInstanceId, processInstanceIds)
                            .isNotNull(OaLeave::getProcessInstanceId));
                    
                    for (OaLeave leave : approvedLeaves) {
                        approvedLeaveIds.add(leave.getLeaveId());
                    }
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }
        
        // 3. 合并列表（使用Map去重，使用批量转换优化性能）
        Map<Long, OaLeaveDTO> leaveMap = new LinkedHashMap<>();
        
        // 添加我发起的已结束申请
        List<OaLeaveDTO> myEndedDTOs = batchConvertProcessedLeaveDTOs(myEndedLeaves, false);
        for (OaLeaveDTO dto : myEndedDTOs) {
            leaveMap.put(dto.getLeaveId(), dto);
        }
        
        // 添加我审批过的（如果已经在我发起的列表中，更新canApprove标识）
        for (Long leaveId : approvedLeaveIds) {
            OaLeave leave = leaveMapper.selectById(leaveId);
            if (leave != null) {
                List<OaLeave> singleLeaveList = Collections.singletonList(leave);
                List<OaLeaveDTO> dtos = batchConvertProcessedLeaveDTOs(singleLeaveList, true);
                if (!dtos.isEmpty()) {
                    leaveMap.put(leaveId, dtos.get(0)); // 覆盖，标记为可查看详情
                }
            }
        }
        
        return new ArrayList<>(leaveMap.values());
    }

    /**
     * 批量转换已处理请假DTO，优化用户信息查询
     */
    private List<OaLeaveDTO> batchConvertProcessedLeaveDTOs(List<OaLeave> leaves, Boolean canApprove) {
        if (leaves == null || leaves.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 收集所有用户ID
        Set<Long> userIds = leaves.stream()
                .map(OaLeave::getApplicantUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 批量查询用户信息
        Map<Long, String> userNameMap = batchGetUserNames(userIds);
        
        // 转换为DTO
        List<OaLeaveDTO> result = new ArrayList<>();
        for (OaLeave leave : leaves) {
            OaLeaveDTO dto = new OaLeaveDTO();
            dto.setLeaveId(leave.getLeaveId());
            dto.setLeaveNo(leave.getLeaveNo());
            dto.setLeaveType(leave.getLeaveType());
            dto.setStartTime(leave.getStartTime());
            dto.setEndTime(leave.getEndTime());
            dto.setLeaveHours(leave.getLeaveHours());
            dto.setLeaveReason(leave.getLeaveReason());
            dto.setApprovalStatus(leave.getApprovalStatus());
            dto.setApplicantUserId(leave.getApplicantUserId());
            dto.setApplicantUserName(userNameMap.getOrDefault(leave.getApplicantUserId(), ""));
            dto.setDeptId(leave.getDeptId());
            dto.setDeptName(leave.getDeptName());
            dto.setProcessInstanceId(leave.getProcessInstanceId());
            dto.setCreateBy(leave.getCreateBy());
            dto.setCreateTime(leave.getCreateTime());
            dto.setUpdateBy(leave.getUpdateBy());
            dto.setUpdateTime(leave.getUpdateTime());
            dto.setCanApprove(canApprove);
            
            // 获取当前节点名称
            if (StringUtils.isNotBlank(leave.getProcessInstanceId())) {
                dto.setCurrentNodeName(getCurrentTaskName(leave.getProcessInstanceId()));
            }
            
            result.add(dto);
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveLeave(OaLeave leave) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        fillApplicantInfo(loginUser, leave);
        if (StringUtils.isBlank(leave.getLeaveNo())) {
            leave.setLeaveNo("LV" + System.currentTimeMillis());
        }
        populateLeaveDerivedValues(leave);
        if (StringUtils.isBlank(leave.getApprovalStatus())) {
            leave.setApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
        }

        // P0: 校验假期余额并标记超额
        checkAndMarkOverQuota(leave);

        if (leave.getLeaveId() == null) {
            // 新增：启动新流程
            leave.setCreateBy(SecurityUtils.getUsername());
            leave.setCreateTime(new Date());
            leaveMapper.insert(leave);
            
            ProcessStartResultDTO workflowResult = workflowService.startProcess(
                    resolveProcessDefinitionKey("leave", "oa_leave_approval"),
                    "leave:" + leave.getLeaveId(),
                    String.valueOf(SecurityUtils.getUserId()),
                    buildLeaveWorkflowVariables(leave)
            );
            leave.setProcessInstanceId(workflowResult.getProcessInstanceId());
            leaveMapper.updateById(leave);
        } else {
            // 编辑：检查是否需要重启流程
            leave.setUpdateBy(SecurityUtils.getUsername());
            leave.setUpdateTime(new Date());
            
            // 如果是已驳回状态，需要检查流程是否结束
            if (ApprovalStatusEnum.REJECTED.getCode().equals(leave.getApprovalStatus()) && StringUtils.isNotBlank(leave.getProcessInstanceId())) {
                // 检查流程实例是否还在运行
                boolean isProcessRunning = isProcessInstanceRunning(leave.getProcessInstanceId());
                if (!isProcessRunning) {
                    // 流程已结束，需要重新启动
                    leave.setApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
                    leaveMapper.updateById(leave);

                    ProcessStartResultDTO workflowResult = workflowService.startProcess(
                            resolveProcessDefinitionKey("leave", "oa_leave_approval"),
                            "leave:" + leave.getLeaveId(),
                            String.valueOf(SecurityUtils.getUserId()),
                            buildLeaveWorkflowVariables(leave)
                    );
                    leave.setProcessInstanceId(workflowResult.getProcessInstanceId());
                    leaveMapper.updateById(leave);
                } else {
                    // 流程还在运行，更新业务数据并自动流转到下一节点
                    leave.setApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
                    leaveMapper.updateById(leave);

                    // 更新流程变量
                    updateProcessVariables(leave.getProcessInstanceId(), buildLeaveWorkflowVariables(leave));

                    // 自动完成当前待办任务，让流程流转到下一节点
                    autoCompleteCurrentTask(leave.getProcessInstanceId(), "申请人重新提交");
                }
            } else {
                // 其他状态，正常更新
                leaveMapper.updateById(leave);
            }
        }
        saveApprovalRecord("leave", "leave:" + leave.getLeaveId(), leave.getProcessInstanceId(), null, ApprovalActionEnum.SUBMIT.getCode(), "提交请假申请");
        return leave.getLeaveId();
    }

    @Override
    public OaLeave getLeaveById(Long leaveId) {
        if (leaveId == null) {
            return null;
        }
        OaLeave leave = leaveMapper.selectById(leaveId);
        if (leave != null && leave.getApplicantUserId() != null) {
            // 补充申请人姓名
            leave.setApplicantUserName(getUserNameById(leave.getApplicantUserId()));
        }
        return leave;
    }

    @Override
    public List<OaExpense> listExpenses(String approvalStatus) {
        Long currentUserId = SecurityUtils.getUserId();
        
        // 1. 查询自己提交的报销申请（包括 pending 和 rejected）
        List<OaExpense> myExpenses = expenseMapper.selectList(new LambdaQueryWrapper<OaExpense>()
                .eq(OaExpense::getApplicantUserId, currentUserId)
                .in(OaExpense::getApprovalStatus, "pending", "rejected")  // 包含待审批和已驳回
                .eq(StringUtils.isNotBlank(approvalStatus), OaExpense::getApprovalStatus, approvalStatus)
                .orderByDesc(OaExpense::getCreateTime));
        
        // 2. 查询需要我审核的报销申请（从工作流待办任务中获取，包含个人任务和角色候选任务）
        List<TodoTaskDTO> todoTasks = workflowService.listTodoTasks(String.valueOf(SecurityUtils.getUserId()));
        List<String> expenseProcessInstanceIds = todoTasks.stream()
                .filter(task -> task.getBusinessKey() != null && task.getBusinessKey().startsWith("expense:"))
                .map(TodoTaskDTO::getProcessInstanceId)
                .distinct()
                .collect(Collectors.toList());
        
        List<OaExpense> needApprovalExpenses = new ArrayList<>();
        if (!expenseProcessInstanceIds.isEmpty()) {
            needApprovalExpenses = expenseMapper.selectList(new LambdaQueryWrapper<OaExpense>()
                    .in(OaExpense::getProcessInstanceId, expenseProcessInstanceIds)
                    .eq(StringUtils.isNotBlank(approvalStatus), OaExpense::getApprovalStatus, approvalStatus)
                    .orderByDesc(OaExpense::getCreateTime));
        }
        
        // 3. 合并列表并设置权限标识和当前节点名称
        Map<Long, OaExpense> expenseMap = new LinkedHashMap<>();
        
        // 收集所有用户ID，批量查询用户名
        Set<Long> userIds = new HashSet<>();
        myExpenses.forEach(e -> {
            if (e.getApplicantUserId() != null) {
                userIds.add(e.getApplicantUserId());
            }
        });
        needApprovalExpenses.forEach(e -> {
            if (e.getApplicantUserId() != null) {
                userIds.add(e.getApplicantUserId());
            }
        });
        Map<Long, String> userNameMap = batchGetUserNames(userIds);
        
        // 添加自己提交的（无审批权限，但如果是已驳回状态，可以重新编辑）
        for (OaExpense expense : myExpenses) {
            expense.setCanApprove(false);
            // 设置申请人姓名
            if (expense.getApplicantUserId() != null) {
                expense.setApplicantUserName(userNameMap.getOrDefault(expense.getApplicantUserId(), ""));
            }
            // 如果是已驳回状态，标记为可以重新提交
            if (ApprovalStatusEnum.REJECTED.getCode().equals(expense.getApprovalStatus())) {
                expense.setCanResubmit(true);
            }
            if (StringUtils.isNotBlank(expense.getProcessInstanceId())) {
                expense.setCurrentNodeName(getCurrentTaskName(expense.getProcessInstanceId()));
            }
            expenseMap.put(expense.getExpenseId(), expense);
        }
        
        // 添加需要审批的（有审批权限，覆盖之前的canApprove标识）
        for (OaExpense expense : needApprovalExpenses) {
            expense.setCanApprove(true);
            // 设置申请人姓名
            if (expense.getApplicantUserId() != null) {
                expense.setApplicantUserName(userNameMap.getOrDefault(expense.getApplicantUserId(), ""));
            }
            if (StringUtils.isNotBlank(expense.getProcessInstanceId())) {
                expense.setCurrentNodeName(getCurrentTaskName(expense.getProcessInstanceId()));
            }
            expenseMap.put(expense.getExpenseId(), expense); // 如果已存在，会更新为可审批
        }
        
        return new ArrayList<>(expenseMap.values());
    }

    @Override
    public List<OaExpense> listProcessedExpenses() {
        Long currentUserId = SecurityUtils.getUserId();
        HistoryService historyService = historyServiceProvider.getIfAvailable();
        
        // 1. 查询自己提交的已结束的报销申请（approved 或 rejected）
        List<OaExpense> myEndedExpenses = expenseMapper.selectList(new LambdaQueryWrapper<OaExpense>()
                .eq(OaExpense::getApplicantUserId, currentUserId)
                .in(OaExpense::getApprovalStatus, "approved", "rejected")
                .orderByDesc(OaExpense::getUpdateTime));
        
        // 2. 查询我审批过的报销记录
        Set<Long> approvedExpenseIds = new HashSet<>();
        if (historyService != null) {
            try {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                        .taskAssignee(String.valueOf(currentUserId))
                        .finished()
                        .list();
                
                if (historicTasks != null && !historicTasks.isEmpty()) {
                    Set<String> processInstanceIds = historicTasks.stream()
                            .map(HistoricTaskInstance::getProcessInstanceId)
                            .collect(Collectors.toSet());
                    
                    List<OaExpense> approvedExpenses = expenseMapper.selectList(new LambdaQueryWrapper<OaExpense>()
                            .in(OaExpense::getProcessInstanceId, processInstanceIds)
                            .isNotNull(OaExpense::getProcessInstanceId));
                    
                    for (OaExpense expense : approvedExpenses) {
                        approvedExpenseIds.add(expense.getExpenseId());
                    }
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }
        
        // 3. 合并列表（使用Map去重）
        Map<Long, OaExpense> expenseMap = new LinkedHashMap<>();
        
        // 收集所有用户ID，批量查询用户名
        Set<Long> userIds = new HashSet<>();
        myEndedExpenses.forEach(e -> {
            if (e.getApplicantUserId() != null) {
                userIds.add(e.getApplicantUserId());
            }
        });
        approvedExpenseIds.forEach(id -> {
            OaExpense e = expenseMapper.selectById(id);
            if (e != null && e.getApplicantUserId() != null) {
                userIds.add(e.getApplicantUserId());
            }
        });
        Map<Long, String> userNameMap = batchGetUserNames(userIds);
        
        // 添加我发起的已结束申请
        for (OaExpense expense : myEndedExpenses) {
            expense.setCanApprove(false);
            // 设置申请人姓名
            if (expense.getApplicantUserId() != null) {
                expense.setApplicantUserName(userNameMap.getOrDefault(expense.getApplicantUserId(), ""));
            }
            if (StringUtils.isNotBlank(expense.getProcessInstanceId())) {
                expense.setCurrentNodeName(getCurrentTaskName(expense.getProcessInstanceId()));
            }
            expenseMap.put(expense.getExpenseId(), expense);
        }
        
        // 添加我审批过的
        for (Long expenseId : approvedExpenseIds) {
            OaExpense expense = expenseMapper.selectById(expenseId);
            if (expense != null) {
                expense.setCanApprove(true);
                // 设置申请人姓名
                if (expense.getApplicantUserId() != null) {
                    expense.setApplicantUserName(userNameMap.getOrDefault(expense.getApplicantUserId(), ""));
                }
                if (StringUtils.isNotBlank(expense.getProcessInstanceId())) {
                    expense.setCurrentNodeName(getCurrentTaskName(expense.getProcessInstanceId()));
                }
                expenseMap.put(expenseId, expense);
            }
        }
        
        return new ArrayList<>(expenseMap.values());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveExpense(OaExpense expense) {
        expense.setInvoiceUrl(normalizeFileUrl(expense.getInvoiceUrl()));
        LoginUser loginUser = SecurityUtils.getLoginUser();
        fillApplicantInfo(loginUser, expense);
        if (StringUtils.isBlank(expense.getExpenseNo())) {
            expense.setExpenseNo("EX" + System.currentTimeMillis());
        }
        if (StringUtils.isBlank(expense.getApprovalStatus())) {
            expense.setApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
        }
        if (expense.getExpenseId() == null) {
            expense.setCreateBy(SecurityUtils.getUsername());
            expense.setCreateTime(new Date());
            expenseMapper.insert(expense);
        } else {
            expense.setUpdateBy(SecurityUtils.getUsername());
            expense.setUpdateTime(new Date());
            
            // 如果是已驳回状态，需要检查流程是否结束
            if (ApprovalStatusEnum.REJECTED.getCode().equals(expense.getApprovalStatus()) && StringUtils.isNotBlank(expense.getProcessInstanceId())) {
                // 检查流程实例是否还在运行
                boolean isProcessRunning = isProcessInstanceRunning(expense.getProcessInstanceId());
                if (!isProcessRunning) {
                    // 流程已结束，需要重新启动
                    expense.setApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
                    expenseMapper.updateById(expense);

                    ProcessStartResultDTO workflowResult = workflowService.startProcess(
                            resolveProcessDefinitionKey("expense", "oa_expense_approval"),
                            "expense:" + expense.getExpenseId(),
                            String.valueOf(SecurityUtils.getUserId()),
                            buildExpenseWorkflowVariables(expense)
                    );
                    expense.setProcessInstanceId(workflowResult.getProcessInstanceId());
                    expenseMapper.updateById(expense);
                } else {
                    // 流程还在运行，更新业务数据并自动流转到下一节点
                    expense.setApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
                    expenseMapper.updateById(expense);

                    // 更新流程变量
                    updateProcessVariables(expense.getProcessInstanceId(), buildExpenseWorkflowVariables(expense));

                    // 自动完成当前待办任务，让流程流转到下一节点
                    autoCompleteCurrentTask(expense.getProcessInstanceId(), "申请人重新提交");
                }
            } else {
                // 其他状态，正常更新
                expenseMapper.updateById(expense);
            }
        }
        if (StringUtils.isBlank(expense.getProcessInstanceId())) {
            ProcessStartResultDTO workflowResult = workflowService.startProcess(
                    resolveProcessDefinitionKey("expense", "oa_expense_approval"),
                    "expense:" + expense.getExpenseId(),
                    String.valueOf(SecurityUtils.getUserId()),
                    buildExpenseWorkflowVariables(expense)
            );
            expense.setProcessInstanceId(workflowResult.getProcessInstanceId());
            expenseMapper.updateById(expense);
        }
        saveApprovalRecord("expense", "expense:" + expense.getExpenseId(), expense.getProcessInstanceId(), null, ApprovalActionEnum.SUBMIT.getCode(), "提交报销申请");
        return expense.getExpenseId();
    }

    @Override
    public OaExpense getExpenseById(Long expenseId) {
        if (expenseId == null) {
            return null;
        }
        OaExpense expense = expenseMapper.selectById(expenseId);
        if (expense != null && expense.getApplicantUserId() != null) {
            // 补充申请人姓名
            expense.setApplicantUserName(getUserNameById(expense.getApplicantUserId()));
        }
        return expense;
    }

    @Override
    public List<OaProcessTemplate> listTemplates(String businessType) {
        List<OaProcessTemplate> templates = templateMapper.selectList(new LambdaQueryWrapper<OaProcessTemplate>()
                .eq(StringUtils.isNotBlank(businessType), OaProcessTemplate::getBusinessType, businessType)
                .orderByDesc(OaProcessTemplate::getCreateTime));
        // 列表接口不返回完整 XML，避免大字段影响传输；仅保留"是否已绘制"的标记给前端展示。
        templates.forEach(item -> item.setProcessContent(StringUtils.isNotBlank(item.getProcessContent()) ? "1" : ""));
        return templates;
    }

    @Override
    public OaProcessTemplate getTemplateById(Long templateId) {
        if (templateId == null) {
            return null;
        }
        return templateMapper.selectById(templateId);
    }

    @Override
    public OaTemplateMeta getActiveTemplateMeta(String businessType) {
        OaProcessTemplate template = findPreferredTemplate(businessType);
        if (template == null) {
            return null;
        }
        OaTemplateMeta meta = new OaTemplateMeta();
        meta.setTemplateId(template.getTemplateId());
        meta.setBusinessType(template.getBusinessType());
        meta.setTemplateName(template.getTemplateName());
        meta.setProcessDefinitionKey(template.getProcessDefinitionKey());
        meta.setProcessDefinitionName(template.getProcessDefinitionName());
        meta.setProcessVersion(template.getProcessVersion());
        meta.setFormRoute(template.getFormRoute());
        meta.setFormFields(parseTemplateFields(template.getProcessContent()));
        return meta;
    }

    @Override
    public Long saveTemplate(OaProcessTemplate template) {
        if (StringUtils.isBlank(template.getDeployStatus())) {
            template.setDeployStatus("draft");
        }

        // 预处理：同步 lx: 自定义属性到 flowable: 原生属性（调用公共方法）
        if (StringUtils.isNotBlank(template.getProcessContent())) {
            try {
                String processedXml = workflowService.syncLxAttributesToFlowable(template.getProcessContent());
                template.setProcessContent(processedXml);
            } catch (Exception e) {
                log.error("BPMN XML 预处理失败，使用原始 XML", e);
            }
        }

        if (template.getTemplateId() == null) {
            template.setCreateBy(SecurityUtils.getUsername());
            template.setCreateTime(new Date());
            templateMapper.insert(template);
        } else {
            template.setUpdateBy(SecurityUtils.getUsername());
            template.setUpdateTime(new Date());
            templateMapper.updateById(template);
        }
        return template.getTemplateId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TaskCompleteResultDTO approveLeave(OaApprovalAction action) {
        try {
            boolean approved = !ApprovalActionEnum.REJECT.getCode().equalsIgnoreCase(action.getAction());
            
            // 1. 准备流程变量
            Map<String, Object> workflowVariables = new HashMap<>();
            workflowVariables.put("approved", approved);
            if (action.getVariables() != null) {
                workflowVariables.putAll(action.getVariables());
                syncEditableLeaveFields(action.getBusinessKey(), action.getVariables());
            }
            enrichLeaveWorkflowVariables(action.getBusinessKey(), workflowVariables);
            
            // 2. 判断是否是最后一个审批节点（必须在 completeTask 之前）
            boolean isLastNode = isLastUserTask(action.getProcessInstanceId(), action.getTaskId());
            log.info("审批请假: taskId={}, isLastNode={}, approved={}", action.getTaskId(), isLastNode, approved);
            
            // 3. 更新业务状态(Spring事务内)
            String newStatus;
            if (!approved) {
                // 驳回：直接设置为 rejected
                newStatus = ApprovalStatusEnum.REJECTED.getCode();
            } else if (isLastNode) {
                // 最后一个节点通过：设置为 approved
                newStatus = ApprovalStatusEnum.APPROVED.getCode();
            } else {
                // 中间节点通过：保持 pending
                newStatus = ApprovalStatusEnum.PENDING.getCode();
            }
            syncLeaveStatus(action.getBusinessKey(), newStatus);

            // 4. 保存审批记录(Spring事务内)
            saveApprovalRecord("leave", action.getBusinessKey(), action.getProcessInstanceId(), 
                    action.getTaskId(), approved ? ApprovalActionEnum.APPROVE.getCode() : ApprovalActionEnum.REJECT.getCode(), action.getComment());
            // 5. 更新超时预警状态为已处理
            updateTimeoutWarningProcessed(action.getTaskId());
            // 6. 最后完成Flowable任务(独立事务,即使失败也不影响业务数据)
            TaskCompleteResultDTO result = workflowService.completeTask(
                    action.getTaskId(),
                    action.getProcessInstanceId(),
                    String.valueOf(SecurityUtils.getUserId()),
                    action.getComment(),
                    approved,
                    workflowVariables
            );
            return result;
        } catch (Exception e) {
            log.error("审批请假失败: businessKey={}, error={}", action.getBusinessKey(), e.getMessage(), e);
            throw e; // 抛出异常触发事务回滚
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TaskCompleteResultDTO approveExpense(OaApprovalAction action) {
        try {
            boolean approved = !ApprovalActionEnum.REJECT.getCode().equalsIgnoreCase(action.getAction());
            
            // 1. 准备流程变量
            Map<String, Object> workflowVariables = new HashMap<>();
            workflowVariables.put("approved", approved);
            if (action.getVariables() != null) {
                workflowVariables.putAll(action.getVariables());
                syncEditableExpenseFields(action.getBusinessKey(), action.getVariables());
            }
            enrichExpenseWorkflowVariables(action.getBusinessKey(), workflowVariables);
            
            // 2. 判断是否是最后一个审批节点
            boolean isLastNode = isLastUserTask(action.getProcessInstanceId(), action.getTaskId());
            log.info("审批报销: taskId={}, isLastNode={}, approved={}", action.getTaskId(), isLastNode, approved);
            
            // 3. 更新业务状态(Spring事务内)
            String newStatus;
            if (!approved) {
                // 驳回：直接设置为 rejected
                newStatus = ApprovalStatusEnum.REJECTED.getCode();
            } else if (isLastNode) {
                // 最后一个节点通过：设置为 approved
                newStatus = ApprovalStatusEnum.APPROVED.getCode();
            } else {
                // 中间节点通过：保持 pending
                newStatus = ApprovalStatusEnum.PENDING.getCode();
            }
            syncExpenseStatus(action.getBusinessKey(), newStatus);
            
            // 4. 保存审批记录(Spring事务内)
            saveApprovalRecord("expense", action.getBusinessKey(), action.getProcessInstanceId(), 
                    action.getTaskId(), approved ? ApprovalActionEnum.APPROVE.getCode() : ApprovalActionEnum.REJECT.getCode(), action.getComment());

            // 5. 最后完成Flowable任务(独立事务,即使失败也不影响业务数据)
            TaskCompleteResultDTO result = workflowService.completeTask(
                    action.getTaskId(),
                    action.getProcessInstanceId(),
                    String.valueOf(SecurityUtils.getUserId()),
                    action.getComment(),
                    approved,
                    workflowVariables
            );

            // 6. 更新超时预警状态为已处理
            updateTimeoutWarningProcessed(action.getTaskId());

            return result;
        } catch (Exception e) {
            log.error("审批报销失败: businessKey={}, error={}", action.getBusinessKey(), e.getMessage(), e);
            throw e; // 抛出异常触发事务回滚
        }
    }

    @Override
    public List<?> listApprovalRecords(String businessKey, String processInstanceId) {
        if (StringUtils.isNotBlank(businessKey)) {
            return approvalRecordService.listByBusinessKey(businessKey);
        }
        return approvalRecordService.listByProcessInstanceId(processInstanceId);
    }

    /**
     * 同步请假单状态
     *
     * @param businessKey     业务标识
     * @param approvalStatus  审批结果
     */
    private void syncLeaveStatus(String businessKey, String approvalStatus) {
        if (StringUtils.isBlank(businessKey) || !businessKey.startsWith("leave:")) {
            return;
        }
        Long leaveId = Long.parseLong(businessKey.substring("leave:".length()));
        OaLeave leave = leaveMapper.selectById(leaveId);
        if (leave == null) {
            return;
        }

        String oldStatus = leave.getApprovalStatus();
        leave.setApprovalStatus(approvalStatus);
        leave.setUpdateBy(SecurityUtils.getUsername());
        leave.setUpdateTime(new Date());
        leaveMapper.updateById(leave);

        // P0: 根据审批结果处理假期额度
        try {
            // 从待审批变为已通过 -> 扣减额度
            if (ApprovalStatusEnum.PENDING.getCode().equals(oldStatus) && ApprovalStatusEnum.APPROVED.getCode().equals(approvalStatus)) {
                Integer year = leave.getStartTime() != null ?
                    java.time.LocalDate.ofInstant(leave.getStartTime().toInstant(), java.time.ZoneId.systemDefault()).getYear()
                    : java.time.LocalDate.now().getYear();

                leaveQuotaService.deductLeaveQuota(
                    leave.getApplicantUserId(),
                    leave.getLeaveType(),
                    year,
                    leave.getLeaveHours().divide(new BigDecimal("8"), 1, java.math.RoundingMode.HALF_UP) // 转换为天
                );
            }
            // 从已通过变为已驳回/撤销 -> 恢复额度
            else if (ApprovalStatusEnum.APPROVED.getCode().equals(oldStatus) && (ApprovalStatusEnum.REJECTED.getCode().equals(approvalStatus) || "cancelled".equals(approvalStatus))) {
                Integer year = leave.getStartTime() != null ?
                    java.time.LocalDate.ofInstant(leave.getStartTime().toInstant(), java.time.ZoneId.systemDefault()).getYear()
                    : java.time.LocalDate.now().getYear();

                leaveQuotaService.restoreLeaveQuota(
                        leave.getApplicantUserId(),
                        leave.getLeaveType(),
                        year,
                        leave.getLeaveHours().divide(new BigDecimal("8"), 1, java.math.RoundingMode.HALF_UP)
                );
            }
        } catch (Exception e) {
            // 记录日志但不影响主流程
            log.error("处理假期额度失败: leaveId={}, error={}", leaveId, e.getMessage(), e);
        }
    }

    private void syncEditableLeaveFields(String businessKey, Map<String, Object> variables) {
        if (StringUtils.isBlank(businessKey) || !businessKey.startsWith("leave:") || variables == null || variables.isEmpty()) {
            return;
        }
        Long leaveId = Long.parseLong(businessKey.substring("leave:".length()));
        OaLeave leave = leaveMapper.selectById(leaveId);
        if (leave == null) {
            return;
        }
        boolean changed = false;
        if (variables.containsKey("leaveType")) {
            leave.setLeaveType(toStringValue(variables.get("leaveType")));
            changed = true;
        }
        if (variables.containsKey("startTime")) {
            Date value = parseDateValue(variables.get("startTime"));
            if (value != null) {
                leave.setStartTime(value);
                changed = true;
            }
        }
        if (variables.containsKey("endTime")) {
            Date value = parseDateValue(variables.get("endTime"));
            if (value != null) {
                leave.setEndTime(value);
                changed = true;
            }
        }
        if (variables.containsKey("leaveReason")) {
            leave.setLeaveReason(toStringValue(variables.get("leaveReason")));
            changed = true;
        }
        if (variables.containsKey("leaveHours")) {
            BigDecimal value = parseDecimalValue(variables.get("leaveHours"));
            if (value != null) {
                leave.setLeaveHours(value);
                changed = true;
            }
        }
        BigDecimal oldLeaveHours = leave.getLeaveHours();
        populateLeaveDerivedValues(leave);
        if (oldLeaveHours == null ? leave.getLeaveHours() != null : !oldLeaveHours.equals(leave.getLeaveHours())) {
            changed = true;
        }
        if (changed) {
            leave.setUpdateBy(SecurityUtils.getUsername());
            leave.setUpdateTime(new Date());
            leaveMapper.updateById(leave);
        }
    }

    private void syncExpenseStatus(String businessKey, String approvalStatus) {
        if (StringUtils.isBlank(businessKey) || !businessKey.startsWith("expense:")) {
            return;
        }
        Long expenseId = Long.parseLong(businessKey.substring("expense:".length()));
        OaExpense expense = expenseMapper.selectById(expenseId);
        if (expense == null) {
            return;
        }
        expense.setApprovalStatus(approvalStatus);
        expense.setUpdateBy(SecurityUtils.getUsername());
        expense.setUpdateTime(new Date());
        expenseMapper.updateById(expense);
    }

    private void syncEditableExpenseFields(String businessKey, Map<String, Object> variables) {
        if (StringUtils.isBlank(businessKey) || !businessKey.startsWith("expense:") || variables == null || variables.isEmpty()) {
            return;
        }
        Long expenseId = Long.parseLong(businessKey.substring("expense:".length()));
        OaExpense expense = expenseMapper.selectById(expenseId);
        if (expense == null) {
            return;
        }
        boolean changed = false;
        if (variables.containsKey("expenseType")) {
            expense.setExpenseType(toStringValue(variables.get("expenseType")));
            changed = true;
        }
        if (variables.containsKey("amount")) {
            BigDecimal value = parseDecimalValue(variables.get("amount"));
            if (value != null) {
                expense.setAmount(value);
                changed = true;
            }
        }
        if (variables.containsKey("expenseDate")) {
            expense.setExpenseDate(toStringValue(variables.get("expenseDate")));
            changed = true;
        }
        if (variables.containsKey("expenseReason")) {
            expense.setExpenseReason(toStringValue(variables.get("expenseReason")));
            changed = true;
        }
        if (changed) {
            expense.setUpdateBy(SecurityUtils.getUsername());
            expense.setUpdateTime(new Date());
            expenseMapper.updateById(expense);
        }
    }

    private void fillApplicantInfo(LoginUser loginUser, OaLeave leave) {
        if (loginUser == null || loginUser.getSysUser() == null) {
            return;
        }
        leave.setApplicantUserId(loginUser.getUserid());
        leave.setDeptId(loginUser.getSysUser().getDeptId());
        leave.setDeptName(loginUser.getSysUser().getDept() == null ? null : loginUser.getSysUser().getDept().getDeptName());
    }

    private void fillApplicantInfo(LoginUser loginUser, OaExpense expense) {
        if (loginUser == null || loginUser.getSysUser() == null) {
            return;
        }
        expense.setApplicantUserId(loginUser.getUserid());
        expense.setDeptId(loginUser.getSysUser().getDeptId());
        expense.setDeptName(loginUser.getSysUser().getDept() == null ? null : loginUser.getSysUser().getDept().getDeptName());
    }

    private void saveApprovalRecord(String businessType, String businessKey, String processInstanceId, String taskId, String actionType, String commentText) {
        OaApprovalRecord record = new OaApprovalRecord();
        record.setBusinessType(businessType);
        record.setBusinessKey(businessKey);
        record.setProcessInstanceId(processInstanceId);
        record.setTaskId(taskId);
        record.setActionType(actionType);
        record.setApproverId(SecurityUtils.getUserId());
        record.setCommentText(commentText);
        record.setCreateBy(SecurityUtils.getUsername());
        approvalRecordService.saveRecord(record);
    }

    private String resolveProcessDefinitionKey(String businessType, String fallbackKey) {
        OaProcessTemplate template = findPreferredTemplate(businessType);
        if (template == null || StringUtils.isBlank(template.getProcessDefinitionKey())) {
            return fallbackKey;
        }
        return template.getProcessDefinitionKey();
    }

    private OaProcessTemplate findPreferredTemplate(String businessType) {
        OaProcessTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<OaProcessTemplate>()
                .eq(OaProcessTemplate::getBusinessType, businessType)
                .eq(OaProcessTemplate::getDeployStatus, "deployed")
                .orderByDesc(OaProcessTemplate::getUpdateTime)
                .orderByDesc(OaProcessTemplate::getCreateTime)
                .last("limit 1"));
        if (template != null) {
            return template;
        }
        return templateMapper.selectOne(new LambdaQueryWrapper<OaProcessTemplate>()
                .eq(OaProcessTemplate::getBusinessType, businessType)
                .orderByDesc(OaProcessTemplate::getUpdateTime)
                .orderByDesc(OaProcessTemplate::getCreateTime)
                .last("limit 1"));
    }

    private List<OaTemplateField> parseTemplateFields(String processContent) {
        if (StringUtils.isBlank(processContent)) {
            return List.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(processContent)));
            Element processElement = (Element) document.getElementsByTagNameNS("*", "process").item(0);
            if (processElement == null) {
                return List.of();
            }
            String formFields = processElement.getAttribute("lx:formFields");
            if (StringUtils.isBlank(formFields)) {
                return List.of();
            }
            return com.alibaba.fastjson2.JSON.parseArray(formFields, OaTemplateField.class);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Map<String, Object> extractWorkflowVariables(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        Object dynamicForm = params.get("dynamicForm");
        if (!(dynamicForm instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> variables = new HashMap<>();
        map.forEach((key, value) -> variables.put(String.valueOf(key), value));
        return variables;
    }

    private Map<String, Object> buildLeaveWorkflowVariables(OaLeave leave) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessType", "leave");
        variables.put("leaveId", leave.getLeaveId());
        variables.put("leaveNo", leave.getLeaveNo());
        variables.put("leaveType", leave.getLeaveType());
        variables.put("startTime", leave.getStartTime());
        variables.put("endTime", leave.getEndTime());
        variables.put("leaveHours", leave.getLeaveHours());
        variables.put("leaveDays", calculateLeaveDays(leave.getStartTime(), leave.getEndTime(), leave.getLeaveHours()));
        variables.put("leaveReason", leave.getLeaveReason());
        variables.put("applicantUserId", leave.getApplicantUserId());
        variables.put("deptId", leave.getDeptId());
        variables.putAll(extractWorkflowVariables(leave.getParams()));
        return variables;
    }

    private Map<String, Object> buildExpenseWorkflowVariables(OaExpense expense) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessType", "expense");
        variables.put("expenseId", expense.getExpenseId());
        variables.put("expenseNo", expense.getExpenseNo());
        variables.put("expenseType", expense.getExpenseType());
        variables.put("amount", expense.getAmount());
        variables.put("expenseDate", expense.getExpenseDate());
        variables.put("expenseReason", expense.getExpenseReason());
        variables.put("applicantUserId", expense.getApplicantUserId());
        variables.put("deptId", expense.getDeptId());
        variables.putAll(extractWorkflowVariables(expense.getParams()));
        return variables;
    }

    private void enrichLeaveWorkflowVariables(String businessKey, Map<String, Object> workflowVariables) {
        if (StringUtils.isBlank(businessKey) || !businessKey.startsWith("leave:")) {
            return;
        }
        Long leaveId = Long.parseLong(businessKey.substring("leave:".length()));
        OaLeave leave = leaveMapper.selectById(leaveId);
        if (leave == null) {
            return;
        }
        workflowVariables.putAll(buildLeaveWorkflowVariables(leave));
    }

    private void enrichExpenseWorkflowVariables(String businessKey, Map<String, Object> workflowVariables) {
        if (StringUtils.isBlank(businessKey) || !businessKey.startsWith("expense:")) {
            return;
        }
        Long expenseId = Long.parseLong(businessKey.substring("expense:".length()));
        OaExpense expense = expenseMapper.selectById(expenseId);
        if (expense == null) {
            return;
        }
        workflowVariables.putAll(buildExpenseWorkflowVariables(expense));
    }

    private String resolveApprovalStatus(TaskCompleteResultDTO result, boolean approved) {
        if (result != null && Boolean.TRUE.equals(result.getProcessFinished())) {
            return approved ? "approved" : "rejected";
        }
        return "pending";
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal parseDecimalValue(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Date parseDateValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date date) {
            return date;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        for (String pattern : List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd")) {
            try {
                return new SimpleDateFormat(pattern).parse(text);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private void populateLeaveDerivedValues(OaLeave leave) {
        if (leave == null) {
            return;
        }
        // 使用休息日工具计算工作日小时数
        long workHours = holidayUtil.calculateWorkHours(leave.getStartTime(), leave.getEndTime());
        leave.setLeaveHours(BigDecimal.valueOf(workHours));
    }

    private BigDecimal calculateLeaveDays(Date startTime, Date endTime, BigDecimal leaveHours) {
        if (startTime == null || endTime == null) {
            return null;
        }

        java.time.LocalDateTime start = startTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
        java.time.LocalDateTime end = endTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();

        java.time.LocalDate startDate = start.toLocalDate();
        java.time.LocalDate endDate = end.toLocalDate();

        // 计算工作日天数（排除周末）
        long workDays = 0;
        java.time.LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            int dayOfWeek = currentDate.getDayOfWeek().getValue();
            if (dayOfWeek != 6 && dayOfWeek != 7) {
                workDays++;
            }
            currentDate = currentDate.plusDays(1);
        }

        if (workDays == 1) {
            long diffHours = java.time.temporal.ChronoUnit.HOURS.between(start, end);
            return diffHours < 4 ? new BigDecimal("0.5") : BigDecimal.ONE;
        }

        return BigDecimal.valueOf(workDays);
    }

    /**
     * 转换为DTO并设置权限标识
     */
    private OaLeaveDTO convertToLeaveDTO(OaLeave leave, Boolean canApprove) {
        OaLeaveDTO dto = new OaLeaveDTO();
        dto.setLeaveId(leave.getLeaveId());
        dto.setLeaveNo(leave.getLeaveNo());
        dto.setLeaveType(leave.getLeaveType());
        dto.setStartTime(leave.getStartTime());
        dto.setEndTime(leave.getEndTime());
        dto.setLeaveHours(leave.getLeaveHours());
        dto.setLeaveReason(leave.getLeaveReason());
        dto.setApprovalStatus(leave.getApprovalStatus());
        dto.setApplicantUserId(leave.getApplicantUserId());
        dto.setApplicantUserName(getUserNameById(leave.getApplicantUserId()));
        dto.setDeptId(leave.getDeptId());
        dto.setDeptName(leave.getDeptName());
        dto.setProcessInstanceId(leave.getProcessInstanceId());
        dto.setCreateBy(leave.getCreateBy());
        dto.setCreateTime(leave.getCreateTime());
        dto.setUpdateBy(leave.getUpdateBy());
        dto.setUpdateTime(leave.getUpdateTime());
        dto.setCanApprove(canApprove);
        
        // 获取当前节点名称
        if (StringUtils.isNotBlank(leave.getProcessInstanceId())) {
            dto.setCurrentNodeName(getCurrentTaskName(leave.getProcessInstanceId()));
        }
        
        return dto;
    }
    
    /**
     * 根据用户ID获取用户名
     */
    private String getUserNameById(Long userId) {
        if (userId == null) {
            return "";
        }
        try {
            R<SysUser> result = remoteUserService.getUserById(userId, SecurityConstants.INNER);
            if (result != null && result.getData() != null) {
                return result.getData().getNickName();
            }
        } catch (Exception e) {
            log.error("查询用户 {} 信息失败", userId, e);
        }
        return "";
    }

    /**
     * 获取流程实例的当前任务名称
     */
    private String getCurrentTaskName(String processInstanceId) {
        HistoryService historyService = historyServiceProvider.getIfAvailable();
        if (historyService == null || StringUtils.isBlank(processInstanceId)) {
            return null;
        }
        
        try {
            // 查询未结束的活动（endTime为null表示正在进行中）
            List<org.flowable.engine.history.HistoricActivityInstance> activities = 
                historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .unfinished()
                    .orderByHistoricActivityInstanceStartTime()
                    .asc()
                    .list();
            
            if (activities != null && !activities.isEmpty()) {
                // 返回第一个未完成的活动名称
                org.flowable.engine.history.HistoricActivityInstance activity = activities.get(0);
                return activity.getActivityName();
            }
        } catch (Exception e) {
            // 忽略异常
        }
        
        return null;
    }

    // ==================== AI聊天助手相关接口实现 ====================

    @Override
    public List<Map<String, Object>> queryPendingTasks(Long userId) {
        try {
            List<TodoTaskDTO> tasks = workflowService.listTodoTasks(userId.toString());
            List<Map<String, Object>> result = new ArrayList<>();
            for (TodoTaskDTO task : tasks) {
                Map<String, Object> item = new HashMap<>();
                item.put("taskId", task.getTaskId());
                item.put("taskName", task.getTaskName() != null ? task.getTaskName() : "未知任务");
                item.put("processInstanceId", task.getProcessInstanceId());
                item.put("createTime", task.getCreateTime());
                result.add(item);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("查询待办任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * P0: 查询用户假期余额(动态计算)
     */
    @Override
    public Map<String, Object> queryLeaveBalance(Long userId) {
        return leaveQuotaService.queryLeaveBalance(userId, null);
    }

    @Override
    public List<Map<String, Object>> queryExpenseStatus(Long userId) {
        try {
            // 查询该用户最近的5条报销记录
            List<OaExpense> expenses = expenseMapper.selectList(new LambdaQueryWrapper<OaExpense>()
                    .eq(OaExpense::getApplicantUserId, userId)
                    .orderByDesc(OaExpense::getCreateTime)
                    .last("LIMIT 5"));

            List<Map<String, Object>> result = new ArrayList<>();
            for (OaExpense expense : expenses) {
                Map<String, Object> item = new HashMap<>();
                item.put("expenseNo", expense.getExpenseNo());
                item.put("expenseType", expense.getExpenseType());
                item.put("amount", expense.getAmount());
                item.put("approvalStatus", expense.getApprovalStatus());
                item.put("createTime", expense.getCreateTime());
                result.add(item);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("查询报销状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> queryTimeoutWarning(Long userId) {
        try {
            // 从 oa_process_timeout_warning 表查询当前用户未处理的预警
            List<OaProcessTimeoutWarning> warnings = timeoutWarningMapper.selectUnprocessedByAssignee(String.valueOf(userId));

            List<Map<String, Object>> result = new ArrayList<>();
            for (OaProcessTimeoutWarning warning : warnings) {
                Map<String, Object> item = new HashMap<>();
                item.put("warningId", warning.getWarningId());
                item.put("taskId", warning.getTaskId());
                item.put("taskName", warning.getTaskName());
                item.put("createTime", warning.getCreateTime());
                item.put("overdueHours", warning.getDurationHours() != null ? warning.getDurationHours().longValue() : 0);
                item.put("durationHours", warning.getDurationHours());
                item.put("warningLevel", warning.getWarningLevel());
                item.put("applicant", warning.getApplicant() != null ? warning.getApplicant() : "未知");

                // 解析业务类型和ID
                String businessType = "";
                String businessId = "";
                if (StringUtils.isNotBlank(warning.getProcessInstanceId())) {
                    // 通过流程实例ID反查业务类型
                    try {
                        // 先尝试请假
                        OaLeave leave = leaveMapper.selectOne(new LambdaQueryWrapper<OaLeave>()
                                .eq(OaLeave::getProcessInstanceId, warning.getProcessInstanceId()));
                        if (leave != null) {
                            businessType = "leave";
                            businessId = String.valueOf(leave.getLeaveId());
                        } else {
                            // 再尝试报销
                            OaExpense expense = expenseMapper.selectOne(new LambdaQueryWrapper<OaExpense>()
                                    .eq(OaExpense::getProcessInstanceId, warning.getProcessInstanceId()));
                            if (expense != null) {
                                businessType = "expense";
                                businessId = String.valueOf(expense.getExpenseId());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("查询业务信息失败: {}", warning.getProcessInstanceId());
                    }
                }

                item.put("businessType", businessType);
                item.put("businessId", businessId);
                item.put("processInstanceId", warning.getProcessInstanceId());
                item.put("taskDefinitionKey", warning.getTaskDefinitionKey());
                result.add(item);
            }
            return result;
        } catch (Exception e) {
            log.error("查询超时预警失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * P0: 校验假期余额并标记超额
     */
    private void checkAndMarkOverQuota(OaLeave leave) {
        try {
            // 计算请假天数（与前端保持一致逻辑：按日历天数）
            BigDecimal leaveDays = calculateLeaveDays(leave.getStartTime(), leave.getEndTime(), leave.getLeaveHours());
            if (leaveDays == null) {
                leaveDays = BigDecimal.ZERO;
            }

            if (leaveDays.compareTo(BigDecimal.ZERO) <= 0) {
                leave.setIsOverQuota(0);
                leave.setOverQuotaDays(BigDecimal.ZERO);
                return;
            }

            // 查询当前假期余额
            Integer year = leave.getStartTime() != null ?
                java.time.LocalDate.ofInstant(leave.getStartTime().toInstant(), java.time.ZoneId.systemDefault()).getYear()
                : java.time.LocalDate.now().getYear();

            Map<String, Object> balanceInfo = leaveQuotaService.queryLeaveBalance(leave.getApplicantUserId(), year);

            // 获取对应假期类型的余额
            BigDecimal remainingDays = BigDecimal.ZERO;
            if (balanceInfo != null && balanceInfo.containsKey("quotas")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> quotas = (List<Map<String, Object>>) balanceInfo.get("quotas");
                for (Map<String, Object> quota : quotas) {
                    if (leave.getLeaveType().equals(quota.get("leaveType"))) {
                        Object remaining = quota.get("remainingDays");
                        if (remaining != null) {
                            remainingDays = new BigDecimal(remaining.toString());
                        }
                        break;
                    }
                }
            }

            // 判断是否超额
            if (leaveDays.compareTo(remainingDays) > 0) {
                leave.setIsOverQuota(1);
                leave.setOverQuotaDays(leaveDays.subtract(remainingDays));
            } else {
                leave.setIsOverQuota(0);
                leave.setOverQuotaDays(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.error("校验假期余额失败", e);
            // 异常时默认不标记超额，避免影响正常提交
            leave.setIsOverQuota(0);
            leave.setOverQuotaDays(BigDecimal.ZERO);
        }
    }

    /**
     * 更新超时预警状态为已处理
     */
    private void updateTimeoutWarningProcessed(String taskId) {
        try {
            OaProcessTimeoutWarning warning = timeoutWarningMapper.selectByTaskId(taskId);
            if (warning != null && "0".equals(warning.getProcessed())) {
                warning.setProcessed("1");
                warning.setProcessedTime(new Date());
                timeoutWarningMapper.updateOaProcessTimeoutWarning(warning);
                log.info("更新超时预警为已处理: taskId={}", taskId);
            }
        } catch (Exception e) {
            log.error("更新超时预警状态失败: taskId={}", taskId, e);
        }
    }

    /**
     * 检查流程实例是否还在运行
     */
    private boolean isProcessInstanceRunning(String processInstanceId) {
        if (StringUtils.isBlank(processInstanceId)) {
            return false;
        }
        
        try {
            // 调用 workflowService 的公共方法检查流程状态
            return workflowService.isProcessInstanceRunning(processInstanceId);
        } catch (Exception e) {
            log.error("检查流程实例状态失败: processInstanceId={}", processInstanceId, e);
            return false;
        }
    }

    /**
     * 更新流程变量
     */
    private void updateProcessVariables(String processInstanceId, Map<String, Object> variables) {
        if (StringUtils.isBlank(processInstanceId) || variables == null || variables.isEmpty()) {
            return;
        }
        
        try {
            // 调用 workflowService 的公共方法更新变量
            workflowService.updateProcessVariables(processInstanceId, variables);
            log.info("更新流程变量成功: processInstanceId={}", processInstanceId);
        } catch (Exception e) {
            log.error("更新流程变量失败: processInstanceId={}", processInstanceId, e);
        }
    }

    /**
     * 自动完成当前待办任务，让流程流转到下一节点
     */
    private void autoCompleteCurrentTask(String processInstanceId, String comment) {
        if (StringUtils.isBlank(processInstanceId)) {
            return;
        }
        
        try {
            // 通过 workflowService 获取 TaskService
            TaskService taskService = ((OaWorkflowServiceImpl) workflowService).getTaskService();
            if (taskService == null) {
                return;
            }
            
            // 查询当前待办任务
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .list();
            
            if (tasks != null && !tasks.isEmpty()) {
                // 遍历所有待办任务，自动完成
                for (Task task : tasks) {
                    try {
                        // 如果是候选任务，先签收
                        if (StringUtils.isBlank(task.getAssignee())) {
                            taskService.claim(task.getId(), String.valueOf(SecurityUtils.getUserId()));
                        }
                        
                        // 添加评论
                        if (StringUtils.isNotBlank(comment)) {
                            taskService.addComment(task.getId(), processInstanceId, comment);
                        }
                        
                        // 完成任务，让流程流转
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("approved", true); // 设置为通过，继续流转
                        variables.put("action", ApprovalActionEnum.RESUBMIT.getCode()); // 指定走重新提交分支
                        taskService.complete(task.getId(), variables);
                        
                        log.info("自动完成任务成功: taskId={}, processInstanceId={}", task.getId(), processInstanceId);
                    } catch (Exception e) {
                        log.error("自动完成任务失败: taskId={}", task.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("自动完成当前任务失败: processInstanceId={}", processInstanceId, e);
        }
    }

    /**
     * 判断当前任务是否是最后一个用户任务
     */
    private boolean isLastUserTask(String processInstanceId, String currentTaskId) {
        HistoryService historyService = historyServiceProvider.getIfAvailable();
        if (historyService == null || StringUtils.isBlank(processInstanceId) || StringUtils.isBlank(currentTaskId)) {
            return false;
        }
        
        try {
            TaskService taskService = ((OaWorkflowServiceImpl) workflowService).getTaskService();
            if (taskService == null) {
                return false;
            }
            
            // 先查询当前任务，获取 taskDefinitionKey
            Task currentTask = taskService.createTaskQuery()
                    .taskId(currentTaskId)
                    .singleResult();
            
            if (currentTask == null) {
                log.warn("任务不存在: taskId={}", currentTaskId);
                return false;
            }
            
            String taskDefinitionKey = currentTask.getTaskDefinitionKey();
            log.debug("判断最后节点: taskId={}, taskDefinitionKey={}", currentTaskId, taskDefinitionKey);
            
            RepositoryService repositoryService = workflowService.getRepositoryService();
            if (repositoryService == null) {
                return false;
            }
            
            // 获取流程定义
            org.flowable.engine.history.HistoricProcessInstance historicProcessInstance = 
                historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (historicProcessInstance == null) {
                return false;
            }
            
            org.flowable.engine.repository.ProcessDefinition processDefinition = repositoryService
                    .createProcessDefinitionQuery()
                    .processDefinitionId(historicProcessInstance.getProcessDefinitionId())
                    .singleResult();
            
            if (processDefinition == null) {
                return false;
            }
            
            // 获取 BPMN 模型
            org.flowable.bpmn.model.BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            
            // 找到当前任务节点（使用 taskDefinitionKey）
            org.flowable.bpmn.model.FlowElement currentElement = bpmnModel.getMainProcess().getFlowElement(taskDefinitionKey);
            if (currentElement == null) {
                log.warn("BPMN模型中找不到节点: taskDefinitionKey={}", taskDefinitionKey);
                return false;
            }
            
            // 获取流程变量，用于条件判断
            RuntimeService runtimeService = ((OaWorkflowServiceImpl) workflowService).getRuntimeService();
            Map<String, Object> processVariables = null;
            if (runtimeService != null) {
                try {
                    processVariables = runtimeService.getVariables(processInstanceId);
                } catch (Exception e) {
                    log.warn("获取流程变量失败", e);
                }
            }
            
            // 从当前节点开始，沿着顺序流往后找，看是否还有用户任务
            boolean hasNext = hasNextUserTask(bpmnModel.getMainProcess(), currentElement, new HashSet<>(), processVariables);
            log.info("判断结果: taskDefinitionKey={}, hasNextUserTask={}, isLastNode={}", 
                    taskDefinitionKey, hasNext, !hasNext);
            
            return !hasNext;
            
        } catch (Exception e) {
            log.error("判断是否最后一个任务失败: processInstanceId={}, taskId={}", processInstanceId, currentTaskId, e);
            return false;
        }
    }

    /**
     * 递归检查当前节点之后是否还有用户任务(只检查通过路径)
     */
    private boolean hasNextUserTask(org.flowable.bpmn.model.Process process, 
                                     org.flowable.bpmn.model.FlowElement currentElement,
                                     Set<String> visitedElements) {
        return hasNextUserTask(process, currentElement, visitedElements, null);
    }

    /**
     * 递归检查当前节点之后是否还有用户任务(只检查通过路径)
     * @param processVariables 流程变量，用于条件判断
     */
    private boolean hasNextUserTask(org.flowable.bpmn.model.Process process, 
                                     org.flowable.bpmn.model.FlowElement currentElement,
                                     Set<String> visitedElements,
                                     Map<String, Object> processVariables) {
        // 防止循环
        if (visitedElements.contains(currentElement.getId())) {
            return false;
        }
        visitedElements.add(currentElement.getId());
        
        // 获取当前节点的流出顺序流
        List<org.flowable.bpmn.model.SequenceFlow> outgoingFlows = null;
        if (currentElement instanceof org.flowable.bpmn.model.FlowNode) {
            outgoingFlows = ((org.flowable.bpmn.model.FlowNode) currentElement).getOutgoingFlows();
        }
        
        if (outgoingFlows == null || outgoingFlows.isEmpty()) {
            return false;
        }
        
        // 遍历所有流出路径
        for (org.flowable.bpmn.model.SequenceFlow flow : outgoingFlows) {
            String targetRef = flow.getTargetRef();
            if (StringUtils.isBlank(targetRef)) {
                continue;
            }
            
            // 检查是否是驳回路径：如果条件表达式包含 reject/false，跳过
            String conditionExpression = flow.getConditionExpression();
            if (StringUtils.isNotBlank(conditionExpression)) {
                // 驳回路径通常条件为 ${approved == false} 或 ${!approved}
                if (conditionExpression.contains("reject") || 
                    conditionExpression.contains("== false") ||
                    conditionExpression.contains("!approved")) {
                    log.debug("跳过驳回路径: {} -> {}", currentElement.getId(), targetRef);
                    continue;
                }
                // 金额条件：根据实际金额判断是否跳过
                if (conditionExpression.contains("amount") && processVariables != null) {
                    Object amountObj = processVariables.get("amount");
                    if (amountObj != null) {
                        try {
                            java.math.BigDecimal actualAmount = new java.math.BigDecimal(amountObj.toString());
                            // 解析条件表达式中的阈值，如 ${amount > 5000}
                            if (conditionExpression.contains(">")) {
                                String thresholdStr = conditionExpression.replaceAll(".*>\\s*", "").replaceAll("}.*", "").trim();
                                java.math.BigDecimal threshold = new java.math.BigDecimal(thresholdStr);
                                if (actualAmount.compareTo(threshold) <= 0) {
                                    log.info("跳过金额条件路径(实际{}<=阈值{}): {} -> {}", actualAmount, threshold, currentElement.getId(), targetRef);
                                    continue;
                                }
                            } else if (conditionExpression.contains("<=")) {
                                String thresholdStr = conditionExpression.replaceAll(".*<=\\s*", "").replaceAll("}.*", "").trim();
                                java.math.BigDecimal threshold = new java.math.BigDecimal(thresholdStr);
                                if (actualAmount.compareTo(threshold) > 0) {
                                    log.info("跳过金额条件路径(实际{}>阈值{}): {} -> {}", actualAmount, threshold, currentElement.getId(), targetRef);
                                    continue;
                                }
                            }
                        } catch (Exception e) {
                            log.warn("解析金额条件失败: {}", conditionExpression, e);
                        }
                    }
                }
            }
            
            org.flowable.bpmn.model.FlowElement targetElement = process.getFlowElement(targetRef);
            if (targetElement == null) {
                continue;
            }
            
            // 如果目标是结束事件，说明这条路径结束了
            if (targetElement instanceof org.flowable.bpmn.model.EndEvent) {
                continue;
            }
            
            // 如果目标是用户任务，说明后面还有任务
            if (targetElement instanceof org.flowable.bpmn.model.UserTask) {
                log.info("找到后续用户任务: {}", targetElement.getName());
                return true;
            }
            
            // 如果是网关或其他节点，继续递归查找
            if (hasNextUserTask(process, targetElement, visitedElements, processVariables)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLeaveApplication(String businessKey, String processInstanceId) {
        if (StringUtils.isBlank(businessKey) || !businessKey.startsWith("leave:")) {
            throw new IllegalArgumentException("无效的业务key");
        }
        
        Long leaveId = Long.parseLong(businessKey.substring("leave:".length()));
        OaLeave leave = leaveMapper.selectById(leaveId);
        if (leave == null) {
            throw new IllegalArgumentException("请假申请不存在");
        }
        
        // 检查权限：只有申请人可以取消
        if (!leave.getApplicantUserId().equals(SecurityUtils.getUserId())) {
            throw new RuntimeException("只有申请人可以取消申请");
        }
        
        // 检查状态：只有已驳回的申请才能取消
        if (!ApprovalStatusEnum.REJECTED.getCode().equals(leave.getApprovalStatus())) {
            throw new RuntimeException("只有已驳回的申请才能取消");
        }
        
        try {
            // 完成当前任务，流向"已取消"结束事件
            TaskService taskService = ((OaWorkflowServiceImpl) workflowService).getTaskService();
            if (taskService != null && StringUtils.isNotBlank(processInstanceId)) {
                List<Task> tasks = taskService.createTaskQuery()
                        .processInstanceId(processInstanceId)
                        .list();
                
                if (tasks != null && !tasks.isEmpty()) {
                    for (Task task : tasks) {
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("action", ApprovalActionEnum.CANCEL.getCode());
                        taskService.complete(task.getId(), variables);
                        log.info("取消申请完成任务: taskId={}", task.getId());
                    }
                }
            }
            
            // 更新业务状态为已取消
            leave.setApprovalStatus("cancelled");
            leave.setUpdateBy(SecurityUtils.getUsername());
            leave.setUpdateTime(new Date());
            leaveMapper.updateById(leave);
            
            // 保存取消记录
            saveApprovalRecord("leave", businessKey, processInstanceId, null, 
                    ApprovalActionEnum.CANCEL.getCode(), "申请人取消申请");
            
            log.info("取消请假申请成功: leaveId={}, processInstanceId={}", leaveId, processInstanceId);
        } catch (Exception e) {
            log.error("取消请假申请失败: leaveId={}", leaveId, e);
            throw new RuntimeException("取消申请失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpenseApplication(String businessKey, String processInstanceId) {
        if (StringUtils.isBlank(businessKey) || !businessKey.startsWith("expense:")) {
            throw new IllegalArgumentException("无效的业务key");
        }
        
        Long expenseId = Long.parseLong(businessKey.substring("expense:".length()));
        OaExpense expense = expenseMapper.selectById(expenseId);
        if (expense == null) {
            throw new IllegalArgumentException("报销申请不存在");
        }
        
        // 检查权限：只有申请人可以取消
        if (!expense.getApplicantUserId().equals(SecurityUtils.getUserId())) {
            throw new RuntimeException("只有申请人可以取消申请");
        }
        
        // 检查状态：只有已驳回的申请才能取消
        if (!ApprovalStatusEnum.REJECTED.getCode().equals(expense.getApprovalStatus())) {
            throw new RuntimeException("只有已驳回的申请才能取消");
        }
        
        try {
            // 完成当前任务，流向"已取消"结束事件
            TaskService taskService = ((OaWorkflowServiceImpl) workflowService).getTaskService();
            if (taskService != null && StringUtils.isNotBlank(processInstanceId)) {
                List<Task> tasks = taskService.createTaskQuery()
                        .processInstanceId(processInstanceId)
                        .list();
                
                if (tasks != null && !tasks.isEmpty()) {
                    for (Task task : tasks) {
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("action", ApprovalActionEnum.CANCEL.getCode());
                        taskService.complete(task.getId(), variables);
                        log.info("取消申请完成任务: taskId={}", task.getId());
                    }
                }
            }
            
            // 更新业务状态为已取消
            expense.setApprovalStatus("cancelled");
            expense.setUpdateBy(SecurityUtils.getUsername());
            expense.setUpdateTime(new Date());
            expenseMapper.updateById(expense);
            
            // 保存取消记录
            saveApprovalRecord("expense", businessKey, processInstanceId, null, 
                    ApprovalActionEnum.CANCEL.getCode(), "申请人取消申请");
            
            log.info("取消报销申请成功: expenseId={}, processInstanceId={}", expenseId, processInstanceId);
        } catch (Exception e) {
            log.error("取消报销申请失败: expenseId={}", expenseId, e);
            throw new RuntimeException("取消申请失败: " + e.getMessage());
        }
    }

    private String normalizeFileUrl(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return fileUrl;
        }
        R<String> result = remoteFileService.normalize(fileUrl);
        if (R.isError(result)) {
            throw new RuntimeException(StringUtils.defaultString(result.getMsg(), "文件地址规范化失败"));
        }
        return result.getData();
    }
}
