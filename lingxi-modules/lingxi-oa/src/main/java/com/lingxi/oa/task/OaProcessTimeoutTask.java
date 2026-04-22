
package com.lingxi.oa.task;

import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.oa.domain.OaProcessTimeoutConfig;
import com.lingxi.oa.domain.OaProcessTimeoutWarning;
import com.lingxi.oa.domain.OaProcessTemplate;
import com.lingxi.oa.mapper.OaProcessTimeoutConfigMapper;
import com.lingxi.oa.mapper.OaProcessTimeoutWarningMapper;
import com.lingxi.oa.mapper.OaProcessTemplateMapper;
import com.lingxi.system.api.RemoteUserService;
import com.lingxi.system.api.RemoteMessageService;
import com.lingxi.system.api.model.LoginUser;
import com.lingxi.system.api.domain.SysUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.TaskService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.idm.api.User;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OaProcessTimeoutTask {
    private static final Logger log = LoggerFactory.getLogger(OaProcessTimeoutTask.class);
    
    // 常量定义
    private static final long MILLIS_PER_HOUR = 3600000L;
    private static final String CANDIDATE_TYPE = "candidate";
    private static final String REMINDER_TYPE_TIMEOUT = "timeout";
    private static final String CHANNEL_SYSTEM = "system";
    private static final String STATUS_UNREAD = "0";
    private static final String PROCESSED_UNREAD = "0";
    private static final String STATUS_SENT = "1";
    private static final String CREATE_BY_SYSTEM = "system";
    private static final String BUSINESS_KEY_SEPARATOR = ":";

    @Autowired
    private TaskService taskService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private OaProcessTimeoutConfigMapper timeoutConfigMapper;

    @Autowired
    private OaProcessTimeoutWarningMapper timeoutWarningMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private RemoteMessageService remoteMessageService;

    @Autowired
    private OaProcessTemplateMapper templateMapper;

//    @Scheduled(cron = "0 0 * * * ?")
    @Scheduled(cron = "0 * * * * ?")
    public void checkProcessTimeout() {
        log.info("开始执行OA流程超时检测任务");
        
        // 用户信息缓存，避免重复查询
        Map<Long, SysUser> userCache = new HashMap<>();
        
        List<Task> activeTasks = taskService.createTaskQuery().active().list();
        List<OaProcessTimeoutConfig> configList = timeoutConfigMapper.selectEnabledList();
        
        // 构建配置映射
        Map<String, OaProcessTimeoutConfig> configMap = buildConfigMap(configList);

        // 处理每个活跃任务
        for (Task task : activeTasks) {
            try {
                processSingleTask(task, configMap, userCache);
            } catch (Exception e) {
                log.error("处理任务 {} 失败: {}", task.getName(), e.getMessage(), e);
            }
        }
        
        log.info("OA流程超时检测任务执行完成");
    }

    /**
     * 构建配置映射
     */
    private Map<String, OaProcessTimeoutConfig> buildConfigMap(List<OaProcessTimeoutConfig> configList) {
        Map<String, OaProcessTimeoutConfig> configMap = new HashMap<>();
        for (OaProcessTimeoutConfig config : configList) {
            String key = config.getProcessDefinitionKey() + "_" + config.getTaskDefinitionKey();
            configMap.put(key, config);
        }
        return configMap;
    }

    /**
     * 处理单个任务
     */
    private void processSingleTask(Task task, Map<String, OaProcessTimeoutConfig> configMap, Map<Long, SysUser> userCache) {
        // 1. 查找配置
        OaProcessTimeoutConfig config = findConfig(task, configMap);
        if (config == null) {
            return;
        }

        // 2. 计算预警等级
        long durationMs = System.currentTimeMillis() - task.getCreateTime().getTime();
        BigDecimal durationHours = new BigDecimal(durationMs).divide(new BigDecimal(MILLIS_PER_HOUR), 2, BigDecimal.ROUND_HALF_UP);
        int warningLevel = calculateWarningLevel(durationHours, config);
        
        if (warningLevel == 0) {
            return;
        }

        // 3. 收集候选人
        List<Long> assigneeList = collectCandidateUsers(task, userCache);
        if (assigneeList.isEmpty()) {
            log.warn("任务 {} 没有明确的审批人或候选人，跳过", task.getName());
            return;
        }

        // 4. 保存或更新预警记录
        String assignees = assigneeList.stream().map(String::valueOf).collect(Collectors.joining(","));
        OaProcessTimeoutWarning existing = timeoutWarningMapper.selectByTaskId(task.getId());
        boolean shouldUpdate = existing == null || existing.getWarningLevel() < warningLevel;
        
        if (shouldUpdate) {
            existing = saveOrUpdateWarning(task, config, assignees, durationHours, warningLevel, existing);
        }

        // 5. 发送提醒
        boolean shouldSendReminder = existing == null || STATUS_UNREAD.equals(existing.getReminderSent());
        if (shouldUpdate && shouldSendReminder) {
            sendReminders(task, assigneeList, warningLevel, durationHours, userCache);
            
            // 更新提醒发送状态
            existing.setReminderSent(STATUS_SENT);
            existing.setReminderTime(new Date());
            timeoutWarningMapper.updateOaProcessTimeoutWarning(existing);
        }
    }

    /**
     * 查找任务配置
     */
    private OaProcessTimeoutConfig findConfig(Task task, Map<String, OaProcessTimeoutConfig> configMap) {
        String processKey = task.getProcessDefinitionId().split(BUSINESS_KEY_SEPARATOR)[0];
        String configKey = processKey + "_" + task.getTaskDefinitionKey();
        OaProcessTimeoutConfig config = configMap.get(configKey);
        
        if (config == null) {
            config = configMap.get("default_" + task.getTaskDefinitionKey());
        }
        
        return config;
    }

    /**
     * 计算预警等级
     */
    private int calculateWarningLevel(BigDecimal durationHours, OaProcessTimeoutConfig config) {
        if (durationHours.compareTo(new BigDecimal(config.getTimeoutHours())) >= 0) {
            return 3; // 已超时
        }
        if (durationHours.compareTo(new BigDecimal(config.getWarning2Hours())) >= 0) {
            return 2; // 二级预警
        }
        if (durationHours.compareTo(new BigDecimal(config.getWarning1Hours())) >= 0) {
            return 1; // 一级预警
        }
        return 0; // 未达预警时间
    }

    /**
     * 收集候选人列表
     */
    private List<Long> collectCandidateUsers(Task task, Map<Long, SysUser> userCache) {
        List<Long> assigneeList = new ArrayList<>();
        String directAssignee = task.getAssignee();
        
        if (StringUtils.isNotBlank(directAssignee)) {
            // 已分配的任务，只有一个审批人
            assigneeList.add(Long.parseLong(directAssignee));
        } else {
            // 角色审批，获取所有候选用户
            Set<Long> userIdSet = collectCandidateUsernames(task, userCache);
            assigneeList.addAll(userIdSet);
        }
        
        return assigneeList;
    }

    /**
     * 收集候选用户名（去重）
     */
    private Set<Long> collectCandidateUsernames(Task task, Map<Long, SysUser> userCache) {
        Set<Long> userIdSet = new HashSet<>();
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        
        // 检查节点是否需要部门过滤
        Boolean needDeptFilter = getNodeNeedDeptFilter(task);
        Long deptId = getTaskDeptId(task, needDeptFilter);
        
        for (IdentityLink link : identityLinks) {
            if (!CANDIDATE_TYPE.equals(link.getType())) {
                continue;
            }
            
            // 情况1：直接指定了候选用户
            if (StringUtils.isNotBlank(link.getUserId())) {
                handleDirectCandidateUser(Long.parseLong(link.getUserId()), needDeptFilter, deptId, userIdSet, userCache);
            }
            // 情况2：指定了候选角色/组
            else if (StringUtils.isNotBlank(link.getGroupId())) {
                handleRoleCandidateUsers(link.getGroupId(), needDeptFilter, deptId, userIdSet, userCache);
            }
        }
        
        return userIdSet;
    }

    /**
     * 获取任务的部门ID
     */
    private Long getTaskDeptId(Task task, Boolean needDeptFilter) {
        if (needDeptFilter == null || !needDeptFilter) {
            return null;
        }
        
        Object deptIdVar = runtimeService.getVariable(task.getExecutionId(), "deptId");
        if (deptIdVar instanceof Long) {
            return (Long) deptIdVar;
        } else if (deptIdVar instanceof String) {
            try {
                return Long.parseLong((String) deptIdVar);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /**
     * 处理直接指定的候选用户
     */
    private void handleDirectCandidateUser(Long userId, Boolean needDeptFilter, Long deptId,
                                           Set<Long> userIdSet, Map<Long, SysUser> userCache) {
        if (needDeptFilter != null && needDeptFilter && deptId != null) {
            // 需要部门过滤，验证用户部门
            try {
                R<SysUser> userResult = remoteUserService.getUserById(userId, SecurityConstants.INNER);
                if (userResult != null && userResult.getData() != null) {
                    Long userDeptId = userResult.getData().getDeptId();
                    if (userDeptId != null && userDeptId.equals(deptId)) {
                        userIdSet.add(userId);
                        userCache.put(userId, userResult.getData());
                    }
                }
            } catch (Exception e) {
                log.error("查询用户 {} 信息失败", userId, e);
            }
        } else {
            // 不需要部门过滤，直接添加
            userIdSet.add(userId);
        }
    }

    /**
     * 处理角色候选用户
     */
    private void handleRoleCandidateUsers(String roleKey, Boolean needDeptFilter, Long deptId,
                                          Set<Long> userIdSet, Map<Long, SysUser> userCache) {
        try {
            R<List<SysUser>> result;
            if (needDeptFilter != null && needDeptFilter && deptId != null) {
                result = remoteUserService.listUsersByRoleAndDept(roleKey, deptId, SecurityConstants.INNER);
            } else {
                result = remoteUserService.listUsersByRole(roleKey, SecurityConstants.INNER);
            }
            
            if (result != null && result.getData() != null) {
                for (SysUser user : result.getData()) {
                    if (StringUtils.isNotBlank(user.getUserName())) {
                        userIdSet.add(user.getUserId());
                        userCache.put(user.getUserId(), user);
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询角色 {} 下的用户失败", roleKey, e);
        }
    }

    /**
     * 保存或更新预警记录
     * @return 更新后的预警记录对象
     */
    private OaProcessTimeoutWarning saveOrUpdateWarning(Task task, OaProcessTimeoutConfig config, String assignees,
                                     BigDecimal durationHours, int warningLevel, OaProcessTimeoutWarning existing) {
        if (existing != null) {
            // 更新现有记录
            existing.setWarningLevel(warningLevel);
            existing.setDurationHours(durationHours);
            existing.setAssignee(assignees);
            timeoutWarningMapper.updateOaProcessTimeoutWarning(existing);
            return existing;
        } else {
            // 创建新记录
            String processDefinitionKey = extractProcessDefinitionKey(task.getProcessDefinitionId());
            
            OaProcessTimeoutWarning warning = new OaProcessTimeoutWarning();
            warning.setProcessInstanceId(task.getProcessInstanceId());
            warning.setTaskId(task.getId());
            warning.setProcessDefinitionKey(processDefinitionKey);
            warning.setTaskDefinitionKey(task.getTaskDefinitionKey());
            warning.setTaskName(task.getName());
            warning.setAssignee(assignees);
            warning.setApplicant(getApplicant(task.getProcessInstanceId()));
            warning.setDurationHours(durationHours);
            warning.setTimeoutHours(config.getTimeoutHours());
            warning.setWarningLevel(warningLevel);
            warning.setStatus(STATUS_UNREAD);
            warning.setReminderSent(STATUS_UNREAD);
            warning.setWarningTime(new Date());
            warning.setProcessed(PROCESSED_UNREAD);
            timeoutWarningMapper.insertOaProcessTimeoutWarning(warning);
            
            // 返回插入后的对象(包含生成的warningId)
            return warning;
        }
    }

    /**
     * 提取流程定义Key
     */
    private String extractProcessDefinitionKey(String processDefinitionId) {
        if (processDefinitionId != null && processDefinitionId.contains(BUSINESS_KEY_SEPARATOR)) {
            return processDefinitionId.split(BUSINESS_KEY_SEPARATOR)[0];
        }
        return processDefinitionId;
    }

    /**
     * 发送提醒给所有候选人
     */
    private void sendReminders(Task task, List<Long> assigneeList, int warningLevel,
                               BigDecimal durationHours, Map<Long, SysUser> userCache) {
        for (Long assignee : assigneeList) {
            sendReminderToUser(task, assignee, warningLevel, durationHours, userCache);
        }
    }
    
    /**
     * 向指定用户发送超时提醒
     */
    private void sendReminderToUser(Task task, Long userId, int warningLevel, BigDecimal durationHours, Map<Long, SysUser> userCache) {
        try {
            // 从缓存中获取用户信息，避免重复查询
            SysUser sysUser = userCache.get(userId);
            if (sysUser == null) {
                // 缓存未命中，通过用户名获取用户信息
                R<SysUser> result = remoteUserService.getUserById(userId, SecurityConstants.INNER);
                if (result == null || result.getData() == null) {
                    log.warn("无法获取用户信息: {}", userId);
                    return;
                }
                sysUser = result.getData();
                userCache.put(userId, sysUser);
            }

            // 构建提醒内容
            String levelText = warningLevel == 1 ? "一级" : warningLevel == 2 ? "二级" : "已";
            String title = "【" + levelText + "超时预警】" + task.getName();
            String content = String.format("审批任务【%s】已耗时%s小时,请尽快处理!", 
                    task.getName(), durationHours);

            // 从流程变量中获取业务类型和业务ID
            BusinessInfo businessInfo = extractBusinessInfo(task);

            // 构建消息信息Map
            Map<String, Object> messageInfo = new HashMap<>();
            messageInfo.put("userId", sysUser.getUserId());
            messageInfo.put("userName", sysUser.getUserName());
            messageInfo.put("sourceType", "oa");
            messageInfo.put("messageType", REMINDER_TYPE_TIMEOUT);
            messageInfo.put("title", title);
            messageInfo.put("content", content);
            messageInfo.put("businessType", businessInfo.getType());
            messageInfo.put("businessId", businessInfo.getId());
            messageInfo.put("processInstanceId", task.getProcessInstanceId());
            messageInfo.put("taskId", task.getId());
            messageInfo.put("priority", warningLevel == 3 ? 3 : 2);
            messageInfo.put("channel", CHANNEL_SYSTEM);
            
            R<Boolean> result = remoteMessageService.sendMessage(messageInfo, SecurityConstants.INNER);
            if (result != null && Boolean.TRUE.equals(result.getData())) {
                log.info("发送超时提醒给用户: {}, 任务: {}", sysUser.getUserName(), task.getName());
            } else {
                log.warn("发送超时提醒失败: userId={}, task={}", sysUser.getUserId(), task.getName());
            }
        } catch (Exception e) {
            log.error("发送超时提醒给用户 {} 失败: {}", userId, e.getMessage());
        }
    }

    /**
     * 业务信息内部类
     */
    private static class BusinessInfo {
        private String type;
        private String id;

        public BusinessInfo(String type, String id) {
            this.type = type;
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * 从流程变量中提取业务信息
     */
    private BusinessInfo extractBusinessInfo(Task task) {
        try {
            String businessKey = "";
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (processInstance != null) {
                businessKey = processInstance.getBusinessKey();
            } else {
                Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
                if (execution != null) {
                    businessKey = execution.getProcessInstanceId();
                }
            }
            if (StringUtils.isNotBlank(businessKey) && businessKey.contains(BUSINESS_KEY_SEPARATOR)) {
                String[] parts = businessKey.split(BUSINESS_KEY_SEPARATOR, 2);
                return new BusinessInfo(parts[0], parts.length > 1 ? parts[1] : null);
            }
        } catch (Exception e) {
            log.warn("获取业务信息失败: {}", e.getMessage());
        }
        return new BusinessInfo(null, null);
    }

    private String getApplicant(String processInstanceId) {
        try {
            Map<String, Object> variables = taskService.getVariables(taskService.createTaskQuery()
                    .processInstanceId(processInstanceId).list().get(0).getId());
            if (variables.containsKey("applicantUserId")) {
                return String.valueOf(variables.get("applicantUserId"));
            }
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * 获取节点的部门过滤配置
     */
    private Boolean getNodeNeedDeptFilter(Task task) {
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
            
            // 解析 BPMN XML，提取节点的 needDeptFilter 属性
            Map<String, Object> nodeConfig = extractNodeConfig(template.getProcessContent(), task.getTaskDefinitionKey());
            Object needDeptFilter = nodeConfig.get("needDeptFilter");
            
            if (needDeptFilter instanceof Boolean) {
                return (Boolean) needDeptFilter;
            }
            
            return null;
        } catch (Exception e) {
            log.error("获取节点 {} 的部门过滤配置失败", task.getTaskDefinitionKey(), e);
            return null;
        }
    }

    /**
     * 从 BPMN XML 中提取节点配置
     */
    private Map<String, Object> extractNodeConfig(String bpmnXml, String taskDefinitionKey) {
        Map<String, Object> config = new HashMap<>();
        if (StringUtils.isBlank(bpmnXml) || StringUtils.isBlank(taskDefinitionKey)) {
            return config;
        }
        
        try {
            // 查找对应的 userTask 节点
            int taskStart = bpmnXml.indexOf("id=\"" + taskDefinitionKey + "\"");
            if (taskStart == -1) {
                return config;
            }
            
            // 找到标签的开始位置
            int tagStart = bpmnXml.lastIndexOf('<', taskStart);
            int tagEnd = bpmnXml.indexOf('>', taskStart) + 1;
            
            if (tagStart == -1 || tagEnd == 0) {
                return config;
            }
            
            String taskTag = bpmnXml.substring(tagStart, tagEnd);
            
            // 提取 lx:needDeptFilter 属性
            int filterAttrStart = taskTag.indexOf("lx:needDeptFilter=\"");
            if (filterAttrStart != -1) {
                int valueStart = filterAttrStart + "lx:needDeptFilter=\"".length();
                int valueEnd = taskTag.indexOf('"', valueStart);
                if (valueEnd != -1) {
                    String filterValue = taskTag.substring(valueStart, valueEnd);
                    config.put("needDeptFilter", "true".equalsIgnoreCase(filterValue));
                }
            }
        } catch (Exception e) {
            log.error("解析节点配置失败", e);
        }
        
        return config;
    }
}
