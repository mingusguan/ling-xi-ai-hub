package com.lingxi.oa.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.oa.config.OaWorkflowProperties;
import com.lingxi.oa.domain.OaProcessTemplate;
import com.lingxi.oa.domain.OaLeave;
import com.lingxi.oa.domain.OaExpense;
import com.lingxi.oa.dto.DeployResultDTO;
import com.lingxi.oa.dto.HistoricActivityDTO;
import com.lingxi.oa.dto.ProcessStartResultDTO;
import com.lingxi.oa.dto.TaskCompleteResultDTO;
import com.lingxi.oa.dto.TodoTaskDTO;
import com.lingxi.oa.enums.DeployStatusEnum;
import com.lingxi.oa.enums.ProcessStatusEnum;
import com.lingxi.oa.enums.ApprovalStatusEnum;
import com.lingxi.oa.mapper.OaProcessTemplateMapper;
import com.lingxi.oa.mapper.OaLeaveMapper;
import com.lingxi.oa.mapper.OaExpenseMapper;
import com.lingxi.oa.service.IOaWorkflowService;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.RemoteUserService;
import com.lingxi.system.api.domain.SysRole;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OaWorkflowServiceImpl implements IOaWorkflowService {

    private final OaWorkflowProperties workflowProperties;
    private final OaProcessTemplateMapper templateMapper;
    private final OaLeaveMapper leaveMapper;
    private final OaExpenseMapper expenseMapper;
    private final ObjectProvider<RepositoryService> repositoryServiceProvider;
    private final ObjectProvider<TaskService> taskServiceProvider;
    private final ObjectProvider<RuntimeService> runtimeServiceProvider;
    private final ObjectProvider<HistoryService> historyServiceProvider;
    private final RemoteUserService remoteUserService; // 远程用户服务
    @Override
    public DeployResultDTO deployTemplate(Long templateId) {
        DeployResultDTO result = new DeployResultDTO();
        result.setTemplateId(templateId);
        result.setEnabled(workflowProperties.isEnabled());

        // 1) 从模板表加载流程模板配置。
        OaProcessTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            result.setStatus(DeployStatusEnum.NOT_FOUND.getCode());
            return result;
        }

        // 2) 若工作流引擎未启用或服务不可用，则跳过部署。
        RepositoryService repositoryService = repositoryServiceProvider.getIfAvailable();
        if (!workflowProperties.isEnabled() || repositoryService == null) {
            result.setStatus(DeployStatusEnum.SKIPPED.getCode());
            return result;
        }

        // 3) 构建部署对象，部署名优先使用模板名称。
        var deploymentBuilder = repositoryService.createDeployment()
                .name(StringUtils.defaultIfBlank(template.getTemplateName(), "lingxi-oa-template"));

        String expectedProcessKey = StringUtils.defaultIfBlank(
                template.getProcessDefinitionKey(),
                "leave".equalsIgnoreCase(template.getBusinessType()) ? "oa_leave_approval"
                        : ("expense".equalsIgnoreCase(template.getBusinessType()) ? "oa_expense_approval" : null)
        );
        String fallbackResourcePath = null;

        // 4) 选择部署资源：
        //    - processContent 有值：使用数据库中的 BPMN XML 进行部署；
        //    - processContent 为空：按业务类型回退到单个 classpath 流程文件。
        if (StringUtils.isNotBlank(template.getProcessContent())) {
            // 预处理：同步 lx: 自定义属性到 flowable: 原生属性
            String processedXml = syncLxAttributesToFlowable(template.getProcessContent());
            String resourceName = StringUtils.defaultIfBlank(template.getProcessDefinitionKey(), "oa-process") + ".bpmn20.xml";
            deploymentBuilder.addString(resourceName, processedXml);
        } else if ("leave".equalsIgnoreCase(template.getBusinessType())) {
            fallbackResourcePath = "processes/leave-approval.bpmn20.xml";
            deploymentBuilder.addClasspathResource(fallbackResourcePath);
        } else if ("expense".equalsIgnoreCase(template.getBusinessType())) {
            fallbackResourcePath = "processes/expense-approval.bpmn20.xml";
            deploymentBuilder.addClasspathResource(fallbackResourcePath);
        } else {
            deploymentBuilder
                    .addClasspathResource("processes/leave-approval.bpmn20.xml")
                    .addClasspathResource("processes/expense-approval.bpmn20.xml");
        }

        // 5) 执行部署，并按预期 key 精确选择流程定义，避免多流程时选错。
        Deployment deployment = deploymentBuilder.deploy();
        ProcessDefinition processDefinition = null;
        if (StringUtils.isNotBlank(expectedProcessKey)) {
            processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .processDefinitionKey(expectedProcessKey)
                    .latestVersion()
                    .singleResult();
        }
        if (processDefinition == null) {
            List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .latestVersion()
                    .list();
            processDefinition = processDefinitions.stream()
                    .max(Comparator.comparingInt(ProcessDefinition::getVersion))
                    .orElse(null);
        }

        // 5.1) 若模板未保存 XML，但本次是用 classpath 进行部署，则回填 processContent，
        //      让后续模板编辑页可直接回显流程图。
        if (StringUtils.isBlank(template.getProcessContent())) {
            String resourceToRead = fallbackResourcePath;
            if (StringUtils.isBlank(resourceToRead) && processDefinition != null) {
                resourceToRead = resolveFallbackResourcePathByProcessKey(processDefinition.getKey());
            }
            if (StringUtils.isNotBlank(resourceToRead)) {
                String xml = readClasspathResourceAsUtf8(resourceToRead);
                if (StringUtils.isNotBlank(xml)) {
                    template.setProcessContent(xml);
                }
            }
        }

        // 6) 将部署状态与流程定义信息回写模板表。
        template.setDeployStatus("deployed");
        if (processDefinition != null) {
            template.setProcessDefinitionKey(processDefinition.getKey());
            template.setProcessDefinitionName(processDefinition.getName());
            template.setProcessVersion("v" + processDefinition.getVersion());
        }
        templateMapper.updateById(template);

        // 7) 返回部署结果摘要给前端。
        result.setStatus(DeployStatusEnum.DEPLOYED.getCode());
        result.setDeploymentId(deployment.getId());
        result.setProcessDefinitionKey(template.getProcessDefinitionKey());
        result.setProcessVersion(template.getProcessVersion());
        return result;
    }

    private String resolveFallbackResourcePathByProcessKey(String processDefinitionKey) {
        if (StringUtils.isBlank(processDefinitionKey)) {
            return null;
        }
        if ("oa_leave_approval".equals(processDefinitionKey)) {
            return "processes/leave-approval.bpmn20.xml";
        }
        if ("oa_expense_approval".equals(processDefinitionKey)) {
            return "processes/expense-approval.bpmn20.xml";
        }
        return null;
    }

    private String readClasspathResourceAsUtf8(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                return null;
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public List<TodoTaskDTO> listTodoTasks(String assignee) {
        TaskService taskService = taskServiceProvider.getIfAvailable();
        RuntimeService runtimeService = runtimeServiceProvider.getIfAvailable();
        RepositoryService repositoryService = repositoryServiceProvider.getIfAvailable();
        if (!workflowProperties.isEnabled() || taskService == null) {
            return List.of();
        }
        
        // 获取当前用户的部门ID，用于后续过滤
        Long currentDeptId = 0L;
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null && loginUser.getSysUser() != null) {
                currentDeptId = loginUser.getSysUser().getDeptId();
            }
        } catch (Exception ignored) {
            log.error("获取当前用户部门ID失败", ignored);
        }
        
        Map<String, Task> todoTaskMap = new LinkedHashMap<>();

        // 1) 已直接指派给当前用户的任务。
        var query = taskService.createTaskQuery().active();
        if (assignee != null && !assignee.isBlank()) {
            query.taskAssignee(assignee);
        }
        Long finalCurrentDeptId = currentDeptId;
        query.list().forEach(task -> {
            // 对于个人任务，也需要检查部门匹配
            if (isTaskDeptMatched(task, runtimeService, repositoryService, finalCurrentDeptId)) {
                todoTaskMap.put(task.getId(), task);
            }
        });

        // 2) 角色候选任务：根据登录用户角色（roleKey + roleName）查询候选组任务。
        List<String> candidateGroups = resolveUserCandidateGroups();
        if (!candidateGroups.isEmpty()) {
            Long finalCurrentDeptId1 = currentDeptId;
            taskService.createTaskQuery()
                    .active()
                    .taskUnassigned()
                    .taskCandidateGroupIn(candidateGroups)
                    .list()
                    .forEach(task -> {
                        // 对于角色候选任务，必须检查部门匹配
                        if (isTaskDeptMatched(task, runtimeService, repositoryService, finalCurrentDeptId1)) {
                            todoTaskMap.putIfAbsent(task.getId(), task);
                        }
                    });
        }

        // 批量查询用户昵称，避免循环RPC调用
        Set<Long> assigneeIds = todoTaskMap.values().stream()
                .map(Task::getAssignee)
                .filter(StringUtils::isNotBlank)
                .map(id -> {
                    try {
                        return Long.parseLong(id);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> userNameMap = batchGetUserNames(assigneeIds);

        final Map<Long, String> finalUserNameMap = userNameMap;
        return todoTaskMap.values().stream()
                .map(task -> convertTask(task, runtimeService, repositoryService, finalUserNameMap))
                .collect(Collectors.toList());
    }

    /**
     * 检查任务的部门是否与当前用户部门匹配
     */
    private boolean isTaskDeptMatched(Task task, RuntimeService runtimeService, RepositoryService repositoryService, Long currentDeptId) {
        // 如果没有部门信息，默认不限制（兼容旧数据）
        if (currentDeptId == null || runtimeService == null) {
            return true;
        }
        
        try {
            // 1. 先检查节点配置，看是否需要部门过滤
            Boolean needDeptFilter = getNodeNeedDeptFilter(task, repositoryService);
            if (needDeptFilter != null && !needDeptFilter) {
                // 节点配置为不需要部门过滤，直接返回true
                return true;
            }
            
            // 2. 从流程变量中获取 deptId
            Object deptIdVar = runtimeService.getVariable(task.getExecutionId(), "deptId");
            if (deptIdVar == null) {
                // 没有 deptId 变量，兼容旧数据，不做限制
                return true;
            }
            
            Long taskDeptId = null;
            if (deptIdVar instanceof Long) {
                taskDeptId = (Long) deptIdVar;
            } else if (deptIdVar instanceof String) {
                try {
                    taskDeptId = Long.parseLong((String) deptIdVar);
                } catch (NumberFormatException ignored) {
                }
            }
            
            // 如果任务没有部门信息，不做限制
            if (taskDeptId == null) {
                return true;
            }
            
            // 3. 比较部门ID是否相同
            return currentDeptId.equals(taskDeptId);
        } catch (Exception e) {
            // 发生异常时，为了不影响业务流程，默认返回true
            return true;
        }
    }

    /**
     * 获取节点的部门过滤配置
     */
    private Boolean getNodeNeedDeptFilter(Task task, RepositoryService repositoryService) {
        if (repositoryService == null || StringUtils.isBlank(task.getProcessDefinitionId())) {
            return null;
        }
        
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            if (processDefinition == null) {
                return null;
            }
            
            OaProcessTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<OaProcessTemplate>()
                    .eq(OaProcessTemplate::getProcessDefinitionKey, processDefinition.getKey())
                    .orderByDesc(OaProcessTemplate::getUpdateTime)
                    .orderByDesc(OaProcessTemplate::getCreateTime)
                    .last("limit 1"));
            
            if (template == null || StringUtils.isBlank(template.getProcessContent())) {
                return null;
            }
            
            Map<String, Object> nodeConfig = extractNodeConfig(template.getProcessContent(), task.getTaskDefinitionKey());
            Object needDeptFilter = nodeConfig.get("needDeptFilter");
            
            if (needDeptFilter instanceof Boolean) {
                return (Boolean) needDeptFilter;
            }
            
            return null;
        } catch (Exception e) {
            // 发生异常时返回null，不做限制
            return null;
        }
    }

    @Override
    public ProcessStartResultDTO startProcess(String processDefinitionKey, String businessKey, String starter, Map<String, Object> variables) {
        ProcessStartResultDTO result = new ProcessStartResultDTO();
        RuntimeService runtimeService = runtimeServiceProvider.getIfAvailable();
        if (!workflowProperties.isEnabled() || runtimeService == null) {
            result.setStatus(ProcessStatusEnum.STARTED.getCode());
            result.setProcessInstanceId(businessKey + "-MOCK");
            return result;
        }
        Map<String, Object> payload = new HashMap<>();
        if (variables != null) {
            payload.putAll(variables);
        }
        payload.put("starter", starter);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, payload);
        result.setStatus(ProcessStatusEnum.STARTED.getCode());
        result.setProcessInstanceId(instance.getProcessInstanceId());
        result.setBusinessKey(businessKey);
        return result;
    }

    @Override
    public TaskCompleteResultDTO completeTask(String taskId, String processInstanceId, String userId, String comment, boolean approved, Map<String, Object> variables) {
        TaskCompleteResultDTO result = new TaskCompleteResultDTO();
        TaskService taskService = taskServiceProvider.getIfAvailable();
        RuntimeService runtimeService = runtimeServiceProvider.getIfAvailable();
        if (!workflowProperties.isEnabled() || taskService == null) {
            result.setStatus(approved ? ProcessStatusEnum.APPROVED.getCode() : ProcessStatusEnum.REJECTED.getCode());
            result.setTaskId(taskId);
            result.setProcessFinished(true);
            return result;
        }
        String currentTaskId = taskId;
        if (currentTaskId == null || currentTaskId.isBlank()) {
            Task currentTask = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .singleResult();
            if (currentTask != null) {
                currentTaskId = currentTask.getId();
            }
        }
        if (StringUtils.isBlank(currentTaskId)) {
            result.setStatus(ProcessStatusEnum.TASK_NOT_FOUND.getCode());
            result.setTaskId(taskId);
            result.setProcessFinished(false);
            return result;
        }
        // 候选任务允许先签收再办理，避免"未指派任务直接审批"场景不一致。
        Task latestTask = taskService.createTaskQuery().taskId(currentTaskId).singleResult();
        if (latestTask != null && StringUtils.isBlank(latestTask.getAssignee()) && StringUtils.isNotBlank(userId)) {
            try {
                taskService.claim(currentTaskId, userId);
            } catch (Exception ignored) {
                // 若已被他人签收或当前用户不在候选范围，保持后续逻辑原样处理。
            }
        }
        Map<String, Object> payload = new HashMap<>();
        if (variables != null) {
            payload.putAll(variables);
        }
        payload.put("approved", approved);
        if (comment != null && !comment.isBlank() && currentTaskId != null) {
            taskService.addComment(currentTaskId, processInstanceId, SecurityUtils.getUsername() + ": " + comment);
        }
        taskService.complete(currentTaskId, payload);
        boolean processFinished = runtimeService == null
                || StringUtils.isBlank(processInstanceId)
                || runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult() == null;
        result.setStatus(processFinished ? (approved ? ProcessStatusEnum.APPROVED.getCode() : ProcessStatusEnum.REJECTED.getCode()) 
                : ProcessStatusEnum.PROCESSING.getCode());
        result.setTaskId(currentTaskId);
        result.setProcessFinished(processFinished);
        return result;
    }

    private List<String> resolveUserCandidateGroups() {
        Set<String> groups = new LinkedHashSet<>();
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null) {
                return List.of();
            }
            if (loginUser.getRoles() != null) {
                loginUser.getRoles().stream()
                        .filter(StringUtils::isNotBlank)
                        .forEach(groups::add);
            }
            if (loginUser.getSysUser() != null && loginUser.getSysUser().getRoles() != null) {
                for (SysRole role : loginUser.getSysUser().getRoles()) {
                    if (role == null) {
                        continue;
                    }
                    if (StringUtils.isNotBlank(role.getRoleKey())) {
                        groups.add(role.getRoleKey());
                    }
                    if (StringUtils.isNotBlank(role.getRoleName())) {
                        groups.add(role.getRoleName());
                    }
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return new ArrayList<>(groups);
    }

    @Override
    public List<HistoricActivityDTO> listHistory(String processInstanceId) {
        HistoryService historyService = historyServiceProvider.getIfAvailable();
        if (!workflowProperties.isEnabled() || historyService == null || processInstanceId == null || processInstanceId.isBlank()) {
            return List.of();
        }
        
        // 1. 查询所有历史活动
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();
        
        // 2. 过滤有活动名称的，并收集所有 assignee ID
        List<HistoricActivityInstance> filteredActivities = activities.stream()
                .filter(item -> StringUtils.isNotBlank(item.getActivityName()))
                .collect(Collectors.toList());
        
        Set<Long> userIds = filteredActivities.stream()
                .map(HistoricActivityInstance::getAssignee)
                .filter(StringUtils::isNotBlank)
                .map(id -> {
                    try {
                        return Long.parseLong(id);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 3. 批量查询用户信息
        Map<Long, String> userNameMap = batchGetUserNames(userIds);
        
        // 4. 转换为DTO
        final Map<Long, String> finalUserNameMap = userNameMap;
        List<HistoricActivityDTO> historyList = filteredActivities.stream()
                .map(item -> convertHistoricActivity(item, finalUserNameMap))
                .collect(Collectors.toList());
        
        // 5. 为 endEvent 补充最后审批人
        enrichEndEventWithLastApprover(historyList, filteredActivities, finalUserNameMap);
        
        return historyList;
    }

    private TodoTaskDTO convertTask(Task task, RuntimeService runtimeService, RepositoryService repositoryService,Map<Long, String> userNameMap) {
        TodoTaskDTO dto = new TodoTaskDTO();
        dto.setTaskId(task.getId());
        dto.setTaskName(task.getName());
        dto.setAssignee(StringUtils.isNotBlank(task.getAssignee()) ? userNameMap.getOrDefault(Long.parseLong(task.getAssignee()), "") : "");
        dto.setProcessInstanceId(task.getProcessInstanceId());
        dto.setProcessDefinitionId(task.getProcessDefinitionId());
        dto.setTaskDefinitionKey(task.getTaskDefinitionKey());
        dto.setExecutionId(task.getExecutionId());
        dto.setCreateTime(task.getCreateTime());
        dto.setDescription(task.getDescription());
        if (runtimeService != null) {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (processInstance != null) {
                dto.setBusinessKey(processInstance.getBusinessKey());
            } else {
                Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
                if (execution != null) {
                    dto.setBusinessKey(execution.getProcessInstanceId());
                }
            }
            try {
                dto.setVariables(runtimeService.getVariables(task.getExecutionId()));
            } catch (Exception ignored) {
                dto.setVariables(Map.of());
            }
        }
        if (repositoryService != null && StringUtils.isNotBlank(task.getProcessDefinitionId())) {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            if (processDefinition != null) {
                dto.setProcessDefinitionKey(processDefinition.getKey());
                OaProcessTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<OaProcessTemplate>()
                        .eq(OaProcessTemplate::getProcessDefinitionKey, processDefinition.getKey())
                        .orderByDesc(OaProcessTemplate::getUpdateTime)
                        .orderByDesc(OaProcessTemplate::getCreateTime)
                        .last("limit 1"));
                if (template != null) {
                    dto.setTemplateName(template.getTemplateName());
                    dto.setNodeConfig(extractNodeConfig(template.getProcessContent(), task.getTaskDefinitionKey()));
                }
            }
        }
        
        // 查询业务数据的审批状态
        if (StringUtils.isNotBlank(dto.getBusinessKey())) {
            String approvalStatus = getApprovalStatusByBusinessKey(dto.getBusinessKey());
            dto.setApprovalStatus(approvalStatus);
        }
        
        return dto;
    }

    private Map<String, Object> extractNodeConfig(String processContent, String taskDefinitionKey) {
        if (StringUtils.isBlank(processContent) || StringUtils.isBlank(taskDefinitionKey)) {
            return Map.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(processContent)));
            Element processElement = (Element) document.getElementsByTagNameNS("*", "process").item(0);
            if (processElement == null) {
                return Map.of();
            }
            Map<String, Object> config = new HashMap<>();
            config.put("processFormFields", parseJsonArray(processElement.getAttribute("lx:formFields")));
            NodeList userTasks = document.getElementsByTagNameNS("*", "userTask");
            for (int i = 0; i < userTasks.getLength(); i++) {
                Element userTask = (Element) userTasks.item(i);
                if (!taskDefinitionKey.equals(userTask.getAttribute("id"))) {
                    continue;
                }
                config.put("approverType", userTask.getAttribute("lx:approverType"));
                config.put("approverValue", userTask.getAttribute("lx:approverValue"));
                String formKey = userTask.getAttribute("flowable:formKey");
                if (StringUtils.isBlank(formKey)) {
                    formKey = userTask.getAttribute("lx:formKey");
                }
                config.put("formKey", formKey);
                config.put("formFields", parseJsonArray(userTask.getAttribute("lx:formFields")));
                config.put("candidateGroups", userTask.getAttribute("flowable:candidateGroups"));
                config.put("assigneeExpr", userTask.getAttribute("flowable:assignee"));
                // 提取是否需要部门过滤配置
                String needDeptFilter = userTask.getAttribute("lx:needDeptFilter");
                config.put("needDeptFilter", StringUtils.isNotBlank(needDeptFilter) ? Boolean.parseBoolean(needDeptFilter) : false);
                return config;
            }
            return config;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private List<?> parseJsonArray(String value) {
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        try {
            return JSON.parseArray(value);
        } catch (Exception ex) {
            return List.of();
        }
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
            // 批量查询用户信息 - 只需1次RPC调用
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

    /**
     * 根据 businessKey 获取业务审批状态
     */
    private String getApprovalStatusByBusinessKey(String businessKey) {
        if (StringUtils.isBlank(businessKey)) {
            return null;
        }
        
        try {
            if (businessKey.startsWith("leave:")) {
                Long leaveId = Long.parseLong(businessKey.substring("leave:".length()));
                OaLeave leave = leaveMapper.selectById(leaveId);
                return leave != null ? leave.getApprovalStatus() : null;
            } else if (businessKey.startsWith("expense:")) {
                Long expenseId = Long.parseLong(businessKey.substring("expense:".length()));
                OaExpense expense = expenseMapper.selectById(expenseId);
                return expense != null ? expense.getApprovalStatus() : null;
            }
        } catch (Exception e) {
            log.error("获取业务审批状态失败: businessKey={}", businessKey, e);
        }
        
        return null;
    }

    private HistoricActivityDTO convertHistoricActivity(HistoricActivityInstance item, Map<Long, String> userNameMap) {
        HistoricActivityDTO dto = new HistoricActivityDTO();
        dto.setActivityId(item.getActivityId());
        dto.setActivityName(item.getActivityName());
        dto.setActivityType(item.getActivityType());
        dto.setAssignee(item.getAssignee());
        
        // 设置审批人姓名
        if (StringUtils.isNotBlank(item.getAssignee())) {
            try {
                Long userId = Long.parseLong(item.getAssignee());
                dto.setAssigneeName(userNameMap.getOrDefault(userId, ""));
            } catch (NumberFormatException e) {
                // 如果不是数字ID，保留原值
                dto.setAssigneeName(item.getAssignee());
            }
        } else if ("userTask".equals(item.getActivityType())) {
            // 候选任务：从流程定义中获取 candidateGroups
            dto.setAssigneeName(getCandidateGroupName(item.getProcessDefinitionId(), item.getActivityId()));
        }
        
        dto.setStartTime(item.getStartTime());
        dto.setEndTime(item.getEndTime());
        dto.setDurationInMillis(item.getDurationInMillis());
        // 根据endTime判断节点是否已完成
        dto.setCompleted(item.getEndTime() != null);
        return dto;
    }

    @Override
    public void cleanupProcessDefinitions(String processDefinitionKey) {
        RepositoryService repositoryService = repositoryServiceProvider.getIfAvailable();
        RuntimeService runtimeService = runtimeServiceProvider.getIfAvailable();
        HistoryService historyService = historyServiceProvider.getIfAvailable();
        
        if (!workflowProperties.isEnabled() || repositoryService == null || StringUtils.isBlank(processDefinitionKey)) {
            return;
        }
        
        try {
            // 1. 先删除所有相关的流程实例（包括运行中和已完成的）
            if (runtimeService != null) {
                List<ProcessInstance> runningInstances = runtimeService.createProcessInstanceQuery()
                        .processDefinitionKey(processDefinitionKey)
                        .list();
                
                if (runningInstances != null && !runningInstances.isEmpty()) {
                    for (ProcessInstance instance : runningInstances) {
                        try {
                            // 删除运行中的流程实例
                            runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), "清理流程定义");
                        } catch (Exception e) {
                            // 继续处理其他实例
                        }
                    }
                }
            }
            
            // 2. 删除历史流程实例
            if (historyService != null) {
                List<HistoricProcessInstance> historicInstances = historyService.createHistoricProcessInstanceQuery()
                        .processDefinitionKey(processDefinitionKey)
                        .list();
                
                if (historicInstances != null && !historicInstances.isEmpty()) {
                    for (HistoricProcessInstance instance : historicInstances) {
                        try {
                            historyService.deleteHistoricProcessInstance(instance.getId());
                        } catch (Exception e) {
                            // 继续处理其他实例
                        }
                    }
                }
            }
            
            // 3. 查询该流程定义key的所有版本
            List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .list();
            
            if (definitions == null || definitions.isEmpty()) {
                return;
            }
            
            // 4. 删除所有版本的流程定义（包括部署文件）
            for (ProcessDefinition definition : definitions) {
                String deploymentId = definition.getDeploymentId();
                if (StringUtils.isNotBlank(deploymentId)) {
                    try {
                        // cascade=true: 同时删除相关的流程实例和历史数据
                        repositoryService.deleteDeployment(deploymentId, true);
                    } catch (Exception e) {
                        // 继续处理其他部署
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("清理流程定义失败: " + e.getMessage(), e);
        }
    }

    /**
     * 同步 lx: 自定义属性到 flowable: 原生属性
     * 在保存/部署流程模板前预处理 BPMN XML，确保 Flowable 引擎能正确识别权限配置
     *
     * @param bpmnXml 原始 BPMN XML
     * @return 处理后的 BPMN XML
     */
    public String syncLxAttributesToFlowable(String bpmnXml) {
        if (StringUtils.isBlank(bpmnXml)) {
            return bpmnXml;
        }

        try {
            // 0. 预处理：移除错误的 flowable_1 命名空间声明
            String cleanedXml = bpmnXml.replaceAll("xmlns:flowable_1=\"[^\"]*\"", "")
                                       .replaceAll("flowable_1:", "flowable:");

            // 1. 解析 XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // 禁用外部实体，防止 XXE 攻击
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            var builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new java.io.StringReader(cleanedXml)));

            // 2. 遍历所有 userTask 节点
            NodeList userTasks = document.getElementsByTagName("bpmn:userTask");
            boolean modified = false;

            for (int i = 0; i < userTasks.getLength(); i++) {
                Element userTask = (Element) userTasks.item(i);

                String approverType = userTask.getAttribute("lx:approverType");
                String approverValue = userTask.getAttribute("lx:approverValue");

                if (StringUtils.isBlank(approverType) || StringUtils.isBlank(approverValue)) {
                    continue;
                }

                // 3. 根据 approverType 同步到不同的 flowable 原生属性
                // 使用完整的命名空间 URI，避免生成 flowable_1 等额外前缀
                String flowableNs = "http://flowable.org/bpmn";
                
                switch (approverType.toLowerCase()) {
                    case "role":
                        // 角色类型 -> flowable:candidateGroups
                        userTask.setAttributeNS(flowableNs, "flowable:candidateGroups", approverValue);
                        modified = true;
                        log.debug("同步角色配置: taskId={}, role={}", userTask.getAttribute("id"), approverValue);
                        break;

                    case "user":
                        // 指定用户类型 -> flowable:candidateUsers
                        userTask.setAttributeNS(flowableNs, "flowable:candidateUsers", approverValue);
                        modified = true;
                        log.debug("同步用户配置: taskId={}, userId={}", userTask.getAttribute("id"), approverValue);
                        break;

                    case "initiator":
                        // 发起人类型 -> flowable:assignee (使用申请人ID)
                        userTask.setAttributeNS(flowableNs, "flowable:assignee", "${applicantUserId}");
                        modified = true;
                        log.debug("同步发起人配置: taskId={}", userTask.getAttribute("id"));
                        break;

                    default:
                        log.warn("未知的审批人类型: {}, taskId={}", approverType, userTask.getAttribute("id"));
                        break;
                }
            }

            // 4. 如果有修改，将 Document 转回 XML 字符串
            if (modified) {
                var transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
                transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                var transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");

                var source = new javax.xml.transform.dom.DOMSource(document);
                var writer = new java.io.StringWriter();
                var result = new javax.xml.transform.stream.StreamResult(writer);
                transformer.transform(source, result);

                String processedXml = writer.toString();
                log.info("BPMN XML 预处理完成，已同步 lx: 属性到 flowable: 原生属性");
                return processedXml;
            }

            return bpmnXml;

        } catch (Exception e) {
            log.error("BPMN XML 预处理失败，使用原始 XML", e);
            // 发生异常时返回原始 XML，不影响业务流程
            return bpmnXml;
        }
    }

    /**
     * 为 endEvent 补充最后审批人信息
     */
    private void enrichEndEventWithLastApprover(List<HistoricActivityDTO> historyList, 
                                                  List<HistoricActivityInstance> activities,
                                                  Map<Long, String> userNameMap) {
        if (historyList == null || historyList.isEmpty()) {
            return;
        }
        
        // 查找最后一个完成的 userTask
        HistoricActivityInstance lastUserTask = activities.stream()
                .filter(item -> "userTask".equals(item.getActivityType()))
                .filter(item -> item.getEndTime() != null) // 已完成
                .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime())) // 按结束时间倒序
                .findFirst()
                .orElse(null);
        
        if (lastUserTask == null) {
            return;
        }
        
        // 获取最后审批人信息
        String lastAssignee = lastUserTask.getAssignee();
        String lastAssigneeName = null;
        
        if (StringUtils.isNotBlank(lastAssignee)) {
            try {
                Long userId = Long.parseLong(lastAssignee);
                lastAssigneeName = userNameMap.getOrDefault(userId, lastAssignee);
            } catch (NumberFormatException e) {
                lastAssigneeName = lastAssignee;
            }
        }
        
        // 为所有 endEvent 设置最后审批人
        for (HistoricActivityDTO dto : historyList) {
            if ("endEvent".equals(dto.getActivityType())) {
                if (StringUtils.isBlank(dto.getAssigneeName())) {
                    dto.setAssignee(lastAssignee);
                    dto.setAssigneeName(lastAssigneeName);
                }
            }
        }
    }

    /**
     * 获取候选组的角色名称
     */
    private String getCandidateGroupName(String processDefinitionId, String activityId) {
        if (StringUtils.isBlank(processDefinitionId) || StringUtils.isBlank(activityId)) {
            return "待审批";
        }
        
        try {
            RepositoryService repositoryService = repositoryServiceProvider.getIfAvailable();
            if (repositoryService == null) {
                return "待审批";
            }
            
            // 获取 BPMN 模型
            org.flowable.bpmn.model.BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                return "待审批";
            }
            
            // 找到对应的用户任务节点
            org.flowable.bpmn.model.FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement(activityId);
            if (flowElement instanceof org.flowable.bpmn.model.UserTask) {
                org.flowable.bpmn.model.UserTask userTask = (org.flowable.bpmn.model.UserTask) flowElement;
                List<String> candidateGroups = userTask.getCandidateGroups();
                
                if (candidateGroups != null && !candidateGroups.isEmpty()) {
                    // 取第一个角色代码转换为中文名称
                    return convertRoleToName(candidateGroups.get(0));
                }
            }
        } catch (Exception e) {
            log.error("获取候选组名称失败: processDefinitionId={}, activityId={}", processDefinitionId, activityId, e);
        }
        
        return "待审批";
    }
    
    /**
     * 将角色代码转换为中文名称
     */
    private String convertRoleToName(String roleCode) {
        if (StringUtils.isBlank(roleCode)) {
            return "待审批";
        }
        
        // 根据实际的角色配置进行转换
        switch (roleCode.toLowerCase()) {
            case "manager":
                return "部门负责人";
            case "general_manager":
                return "总经理";
            case "hr":
                return "HR";
            case "finance":
                return "财务";
            case "administration":
                return "行政";
            default:
                return roleCode; // 默认返回原值
        }
    }

    /**
     * 检查流程实例是否还在运行
     */
    public boolean isProcessInstanceRunning(String processInstanceId) {
        if (StringUtils.isBlank(processInstanceId)) {
            return false;
        }
        
        RuntimeService runtimeService = runtimeServiceProvider.getIfAvailable();
        if (runtimeService == null) {
            return false;
        }
        
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            return instance != null;
        } catch (Exception e) {
            log.error("检查流程实例状态失败: processInstanceId={}", processInstanceId, e);
            return false;
        }
    }

    /**
     * 更新流程变量
     */
    public void updateProcessVariables(String processInstanceId, Map<String, Object> variables) {
        if (StringUtils.isBlank(processInstanceId) || variables == null || variables.isEmpty()) {
            return;
        }
        
        RuntimeService runtimeService = runtimeServiceProvider.getIfAvailable();
        if (runtimeService == null) {
            return;
        }
        
        try {
            runtimeService.setVariables(processInstanceId, variables);
            log.info("更新流程变量成功: processInstanceId={}", processInstanceId);
        } catch (Exception e) {
            log.error("更新流程变量失败: processInstanceId={}", processInstanceId, e);
        }
    }

    /**
     * 获取 RepositoryService
     */
    public RepositoryService getRepositoryService() {
        return repositoryServiceProvider.getIfAvailable();
    }

    /**
     * 获取 TaskService
     */
    public TaskService getTaskService() {
        return taskServiceProvider.getIfAvailable();
    }

    /**
     * 获取 RuntimeService
     */
    public RuntimeService getRuntimeService() {
        return runtimeServiceProvider.getIfAvailable();
    }
}
