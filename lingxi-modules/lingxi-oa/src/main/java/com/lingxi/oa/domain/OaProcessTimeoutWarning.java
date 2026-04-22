package com.lingxi.oa.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lingxi.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 流程超时预警实体
 *
 * @author lingxi
 */
public class OaProcessTimeoutWarning extends BaseEntity {

    /** 预警ID */
    private Long warningId;
    
    /** 流程实例ID */
    private String processInstanceId;
    
    /** 任务ID */
    private String taskId;
    
    /** 流程定义Key */
    private String processDefinitionKey;
    
    /** 任务定义Key */
    private String taskDefinitionKey;
    
    /** 任务名称 */
    private String taskName;
    
    /** 审批人 */
    private String assignee;
    
    /** 申请人 */
    private String applicant;
    
    /** 预警级别（1:一级,2:二级,3:已超时） */
    private Integer warningLevel;
    
    /** 超时时长（小时） */
    private Integer timeoutHours;
    
    /** 已耗时（小时） */
    private BigDecimal durationHours;
    
    /** 状态（0:未处理,1:已处理） */
    private String status;
    
    /** 提醒是否发送（0:未发送,1:已发送） */
    private String reminderSent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date warningTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reminderTime;

    private String processed;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processedTime;

    public Long getWarningId() {
        return warningId;
    }

    public void setWarningId(Long warningId) {
        this.warningId = warningId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public Integer getWarningLevel() {
        return warningLevel;
    }

    public void setWarningLevel(Integer warningLevel) {
        this.warningLevel = warningLevel;
    }

    public Integer getTimeoutHours() {
        return timeoutHours;
    }

    public void setTimeoutHours(Integer timeoutHours) {
        this.timeoutHours = timeoutHours;
    }

    public BigDecimal getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(BigDecimal durationHours) {
        this.durationHours = durationHours;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(String reminderSent) {
        this.reminderSent = reminderSent;
    }

    public Date getWarningTime() {
        return warningTime;
    }

    public void setWarningTime(Date warningTime) {
        this.warningTime = warningTime;
    }

    public Date getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(Date reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getProcessed() {
        return processed;
    }

    public void setProcessed(String processed) {
        this.processed = processed;
    }

    public Date getProcessedTime() {
        return processedTime;
    }

    public void setProcessedTime(Date processedTime) {
        this.processedTime = processedTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("warningId", getWarningId())
                .append("processInstanceId", getProcessInstanceId())
                .append("taskId", getTaskId())
                .append("processDefinitionKey", getProcessDefinitionKey())
                .append("taskDefinitionKey", getTaskDefinitionKey())
                .append("taskName", getTaskName())
                .append("assignee", getAssignee())
                .append("applicant", getApplicant())
                .append("warningLevel", getWarningLevel())
                .append("timeoutHours", getTimeoutHours())
                .append("durationHours", getDurationHours())
                .append("status", getStatus())
                .append("reminderSent", getReminderSent())
                .append("warningTime", getWarningTime())
                .append("reminderTime", getReminderTime())
                .append("processed", getProcessed())
                .append("processedTime", getProcessedTime())
                .toString();
    }
}
