package com.lingxi.oa.dto;

public class TaskCompleteResultDTO {

    private boolean success;
    private String status;
    private String message;
    private String processInstanceId;
    private String taskId;
    private String businessKey;
    private String nextTaskId;
    private String nextTaskName;
    private String nextAssignee;
    private Boolean processFinished;

    public TaskCompleteResultDTO() {
    }

    public TaskCompleteResultDTO(boolean success) {
        this.success = success;
    }

    public static TaskCompleteResultDTO success() {
        return new TaskCompleteResultDTO(true);
    }

    public static TaskCompleteResultDTO success(String message) {
        TaskCompleteResultDTO dto = new TaskCompleteResultDTO(true);
        dto.setMessage(message);
        return dto;
    }

    public static TaskCompleteResultDTO fail(String message) {
        TaskCompleteResultDTO dto = new TaskCompleteResultDTO(false);
        dto.setMessage(message);
        return dto;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getNextTaskId() {
        return nextTaskId;
    }

    public void setNextTaskId(String nextTaskId) {
        this.nextTaskId = nextTaskId;
    }

    public String getNextTaskName() {
        return nextTaskName;
    }

    public void setNextTaskName(String nextTaskName) {
        this.nextTaskName = nextTaskName;
    }

    public String getNextAssignee() {
        return nextAssignee;
    }

    public void setNextAssignee(String nextAssignee) {
        this.nextAssignee = nextAssignee;
    }

    public Boolean getProcessFinished() {
        return processFinished;
    }

    public void setProcessFinished(Boolean processFinished) {
        this.processFinished = processFinished;
    }
}
