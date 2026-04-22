package com.lingxi.oa.service;

import com.lingxi.oa.dto.*;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;

import java.util.List;
import java.util.Map;

public interface IOaWorkflowService {

    /**
     * 部署流程模板
     */
    DeployResultDTO deployTemplate(Long templateId);

    /**
     * 查询待办任务列表
     */
    List<TodoTaskDTO> listTodoTasks(String assignee);

    /**
     * 启动流程实例
     */
    ProcessStartResultDTO startProcess(String processDefinitionKey, String businessKey, String starter, Map<String, Object> variables);

    /**
     * 完成任务
     */
    TaskCompleteResultDTO completeTask(String taskId, String processInstanceId, String userId, String comment, boolean approved, Map<String, Object> variables);

    /**
     * 查询流程历史
     */
    List<HistoricActivityDTO> listHistory(String processInstanceId);

    /**
     * 清理指定流程定义的所有版本（用于重新部署前清理旧流程）
     * @param processDefinitionKey 流程定义key，如 oa_leave_approval
     */
    void cleanupProcessDefinitions(String processDefinitionKey);

    /**
     * 获取 RepositoryService
     */
    RepositoryService getRepositoryService();

    /**
     * 检查流程实例是否还在运行
     */
    boolean isProcessInstanceRunning(String processInstanceId);

    /**
     * 更新流程变量
     */
    void updateProcessVariables(String processInstanceId, Map<String, Object> variables);

    /**
     * 同步 lx: 自定义属性到 flowable: 原生属性
     */
    String syncLxAttributesToFlowable(String bpmnXml);

    /**
     * 获取 TaskService
     */
    TaskService getTaskService();
}
