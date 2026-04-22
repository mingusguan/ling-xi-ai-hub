package com.lingxi.oa.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.lingxi.common.core.web.domain.BaseEntity;

/**
 * P1: 假期规则配置表 oa_leave_rule
 * 
 * @author cloud
 */
public class OaLeaveRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long ruleId;

    /** 假期类型编码 */
    @NotBlank(message = "假期类型编码不能为空")
    private String leaveType;

    /** 假期类型名称 */
    @NotBlank(message = "假期类型名称不能为空")
    private String leaveName;

    /** 默认天数 */
    @NotNull(message = "默认天数不能为空")
    private BigDecimal defaultDays;

    /** 计算规则(fixed-固定,by_seniority-按工龄,by_level-按职级) */
    private String calculationRule;

    /** 是否可结转(0否 1是) */
    private String canCarryOver;

    /** 结转比例(如0.5表示结转50%) */
    private BigDecimal carryOverRatio;

    /** 最大结转天数 */
    private BigDecimal maxCarryOverDays;

    /** 最小工龄要求(年) */
    private BigDecimal minWorkYears;

    /** 显示顺序 */
    private Integer sortOrder;

    /** 状态(0正常 1停用) */
    private String status;

    public Long getRuleId()
    {
        return ruleId;
    }

    public void setRuleId(Long ruleId)
    {
        this.ruleId = ruleId;
    }

    public String getLeaveType()
    {
        return leaveType;
    }

    public void setLeaveType(String leaveType)
    {
        this.leaveType = leaveType;
    }

    public String getLeaveName()
    {
        return leaveName;
    }

    public void setLeaveName(String leaveName)
    {
        this.leaveName = leaveName;
    }

    public BigDecimal getDefaultDays()
    {
        return defaultDays;
    }

    public void setDefaultDays(BigDecimal defaultDays)
    {
        this.defaultDays = defaultDays;
    }

    public String getCalculationRule()
    {
        return calculationRule;
    }

    public void setCalculationRule(String calculationRule)
    {
        this.calculationRule = calculationRule;
    }

    public String getCanCarryOver()
    {
        return canCarryOver;
    }

    public void setCanCarryOver(String canCarryOver)
    {
        this.canCarryOver = canCarryOver;
    }

    public BigDecimal getCarryOverRatio()
    {
        return carryOverRatio;
    }

    public void setCarryOverRatio(BigDecimal carryOverRatio)
    {
        this.carryOverRatio = carryOverRatio;
    }

    public BigDecimal getMaxCarryOverDays()
    {
        return maxCarryOverDays;
    }

    public void setMaxCarryOverDays(BigDecimal maxCarryOverDays)
    {
        this.maxCarryOverDays = maxCarryOverDays;
    }

    public BigDecimal getMinWorkYears()
    {
        return minWorkYears;
    }

    public void setMinWorkYears(BigDecimal minWorkYears)
    {
        this.minWorkYears = minWorkYears;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("ruleId", getRuleId())
            .append("leaveType", getLeaveType())
            .append("leaveName", getLeaveName())
            .append("defaultDays", getDefaultDays())
            .append("calculationRule", getCalculationRule())
            .append("canCarryOver", getCanCarryOver())
            .append("carryOverRatio", getCarryOverRatio())
            .append("maxCarryOverDays", getMaxCarryOverDays())
            .append("minWorkYears", getMinWorkYears())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
