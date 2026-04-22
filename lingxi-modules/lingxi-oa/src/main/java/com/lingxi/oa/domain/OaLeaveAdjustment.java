package com.lingxi.oa.domain;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * P2: 假期额度调整记录表 oa_leave_adjustment
 * 
 * @author cloud
 */
public class OaLeaveAdjustment
{
    private static final long serialVersionUID = 1L;

    /** 调整ID */
    private Long adjustmentId;

    /** 额度ID */
    @NotNull(message = "额度ID不能为空")
    private Long quotaId;

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 用户姓名 */
    private String userName;

    /** 假期类型 */
    @NotBlank(message = "假期类型不能为空")
    private String leaveType;

    /** 年度 */
    @NotNull(message = "年度不能为空")
    private Integer year;

    /** 调整天数(正数增加,负数减少) */
    @NotNull(message = "调整天数不能为空")
    private BigDecimal adjustDays;

    /** 调整前额度 */
    private BigDecimal beforeDays;

    /** 调整后额度 */
    private BigDecimal afterDays;

    /** 调整原因 */
    @NotBlank(message = "调整原因不能为空")
    private String reason;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 创建时间 */
    private Date createTime;

    public Long getAdjustmentId()
    {
        return adjustmentId;
    }

    public void setAdjustmentId(Long adjustmentId)
    {
        this.adjustmentId = adjustmentId;
    }

    public Long getQuotaId()
    {
        return quotaId;
    }

    public void setQuotaId(Long quotaId)
    {
        this.quotaId = quotaId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getLeaveType()
    {
        return leaveType;
    }

    public void setLeaveType(String leaveType)
    {
        this.leaveType = leaveType;
    }

    public Integer getYear()
    {
        return year;
    }

    public void setYear(Integer year)
    {
        this.year = year;
    }

    public BigDecimal getAdjustDays()
    {
        return adjustDays;
    }

    public void setAdjustDays(BigDecimal adjustDays)
    {
        this.adjustDays = adjustDays;
    }

    public BigDecimal getBeforeDays()
    {
        return beforeDays;
    }

    public void setBeforeDays(BigDecimal beforeDays)
    {
        this.beforeDays = beforeDays;
    }

    public BigDecimal getAfterDays()
    {
        return afterDays;
    }

    public void setAfterDays(BigDecimal afterDays)
    {
        this.afterDays = afterDays;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("adjustmentId", getAdjustmentId())
            .append("quotaId", getQuotaId())
            .append("userId", getUserId())
            .append("leaveType", getLeaveType())
            .append("year", getYear())
            .append("adjustDays", getAdjustDays())
            .append("beforeDays", getBeforeDays())
            .append("afterDays", getAfterDays())
            .append("reason", getReason())
            .append("operatorId", getOperatorId())
            .append("operatorName", getOperatorName())
            .append("createTime", getCreateTime())
            .toString();
    }
}
