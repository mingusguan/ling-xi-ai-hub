package com.lingxi.oa.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * AI审批意见建议实体
 *
 * @author lingxi
 */
public class OaAiOpinionSuggest {

    /** 建议ID */
    private Long suggestId;
    
    /** 流程实例ID */
    private String processInstanceId;
    
    /** 任务ID */
    private String taskId;
    
    /** 业务类型（leave:请假,expense:报销） */
    private String businessType;
    
    /** 业务ID */
    private Long businessId;
    
    /** 审批人ID */
    private Long approverId;
    
    /** 申请人姓名 */
    private String applicantName;
    
    /** 表单数据（JSON格式） */
    private String formData;
    
    /** AI建议内容 */
    private String aiSuggestion;
    
    /** 风险等级（low/normal/high） */
    private String riskLevel;
    
    /** 风险点（JSON数组） */
    private String riskPoints;
    
    /** 相似案例（JSON数组） */
    private String similarCases;
    
    /** AI推荐意见模板（JSON数组） */
    private String templates;
    
    /** 是否使用（0:未使用,1:已使用） */
    private String used;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getSuggestId() {
        return suggestId;
    }

    public void setSuggestId(Long suggestId) {
        this.suggestId = suggestId;
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

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
    }

    public String getAiSuggestion() {
        return aiSuggestion;
    }

    public void setAiSuggestion(String aiSuggestion) {
        this.aiSuggestion = aiSuggestion;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRiskPoints() {
        return riskPoints;
    }

    public void setRiskPoints(String riskPoints) {
        this.riskPoints = riskPoints;
    }

    public String getSimilarCases() {
        return similarCases;
    }

    public void setSimilarCases(String similarCases) {
        this.similarCases = similarCases;
    }

    public String getTemplates() {
        return templates;
    }

    public void setTemplates(String templates) {
        this.templates = templates;
    }

    public String getUsed() {
        return used;
    }

    public void setUsed(String used) {
        this.used = used;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("suggestId", getSuggestId())
                .append("processInstanceId", getProcessInstanceId())
                .append("taskId", getTaskId())
                .append("businessType", getBusinessType())
                .append("businessId", getBusinessId())
                .append("approverId", getApproverId())
                .append("applicantName", getApplicantName())
                .append("formData", getFormData())
                .append("aiSuggestion", getAiSuggestion())
                .append("riskLevel", getRiskLevel())
                .append("riskPoints", getRiskPoints())
                .append("similarCases", getSimilarCases())
                .append("templates", getTemplates())
                .append("used", getUsed())
                .append("createTime", getCreateTime())
                .toString();
    }
}
