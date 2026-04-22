package com.lingxi.oa.domain;

import com.lingxi.common.core.annotation.Excel;
import com.lingxi.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 流程超时配置实体
 *
 * @author lingxi
 */
public class OaProcessTimeoutConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private Long configId;

    /** 流程定义KEY */
    @Excel(name = "流程定义KEY")
    private String processDefinitionKey;

    /** 节点定义KEY */
    @Excel(name = "节点定义KEY")
    private String taskDefinitionKey;

    /** 超时时间(小时) */
    @Excel(name = "超时时间(小时)")
    private Integer timeoutHours;

    /** 一级预警时间(小时) */
    @Excel(name = "一级预警时间(小时)")
    private Integer warning1Hours;

    /** 二级预警时间(小时) */
    @Excel(name = "二级预警时间(小时)")
    private Integer warning2Hours;

    /** 提醒方式 */
    @Excel(name = "提醒方式")
    private String reminderType;

    /** 是否启用（0:禁用,1:启用） */
    private String enabled;

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public Long getConfigId() {
        return configId;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTimeoutHours(Integer timeoutHours) {
        this.timeoutHours = timeoutHours;
    }

    public Integer getTimeoutHours() {
        return timeoutHours;
    }

    public void setWarning1Hours(Integer warning1Hours) {
        this.warning1Hours = warning1Hours;
    }

    public Integer getWarning1Hours() {
        return warning1Hours;
    }

    public void setWarning2Hours(Integer warning2Hours) {
        this.warning2Hours = warning2Hours;
    }

    public Integer getWarning2Hours() {
        return warning2Hours;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("configId", getConfigId())
                .append("processDefinitionKey", getProcessDefinitionKey())
                .append("taskDefinitionKey", getTaskDefinitionKey())
                .append("timeoutHours", getTimeoutHours())
                .append("warning1Hours", getWarning1Hours())
                .append("warning2Hours", getWarning2Hours())
                .append("reminderType", getReminderType())
                .append("enabled", getEnabled())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
