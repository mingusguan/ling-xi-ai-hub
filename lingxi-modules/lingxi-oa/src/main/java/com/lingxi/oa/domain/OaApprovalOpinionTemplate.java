package com.lingxi.oa.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 审批意见模板实体
 *
 * @author lingxi
 */
public class OaApprovalOpinionTemplate {

    /** 模板ID */
    private Long templateId;
    
    /** 业务类型（leave:请假,expense:报销） */
    private String businessType;
    
    /** 模板内容 */
    private String templateContent;
    
    /** 风险等级（low/normal/high） */
    private String riskLevel;
    
    /** 是否启用（0:禁用,1:启用） */
    private String enabled;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("templateId", getTemplateId())
                .append("businessType", getBusinessType())
                .append("templateContent", getTemplateContent())
                .append("riskLevel", getRiskLevel())
                .append("enabled", getEnabled())
                .toString();
    }
}
