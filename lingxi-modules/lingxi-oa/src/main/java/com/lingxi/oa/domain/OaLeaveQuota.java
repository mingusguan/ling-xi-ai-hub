package com.lingxi.oa.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.lingxi.common.core.web.domain.BaseEntity;

/**
 * P0: 假期额度表 oa_leave_quota
 * 
 * @author cloud
 */
public class OaLeaveQuota extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 额度ID */
    private Long quotaId;

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 假期类型(annual-年假,sick-病假,personal-事假,marriage-婚假,maternity-产假) */
    @NotBlank(message = "假期类型不能为空")
    private String leaveType;

    /** 年度 */
    @NotNull(message = "年度不能为空")
    private Integer year;

    /** 总额度(天) */
    private BigDecimal totalDays;

    /** 已使用天数 */
    private BigDecimal usedDays;

    /** 剩余天数 */
    private BigDecimal remainingDays;

    /** 结转天数(从上一年) */
    private BigDecimal carryOverDays;

    /** 状态(0正常 1停用) */
    private String status;

    /** 用户姓名(非数据库字段,用于查询展示) */
    private String userName;

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

    public BigDecimal getTotalDays()
    {
        return totalDays;
    }

    public void setTotalDays(BigDecimal totalDays)
    {
        this.totalDays = totalDays;
    }

    public BigDecimal getUsedDays()
    {
        return usedDays;
    }

    public void setUsedDays(BigDecimal usedDays)
    {
        this.usedDays = usedDays;
    }

    public BigDecimal getRemainingDays()
    {
        return remainingDays;
    }

    public void setRemainingDays(BigDecimal remainingDays)
    {
        this.remainingDays = remainingDays;
    }

    public BigDecimal getCarryOverDays()
    {
        return carryOverDays;
    }

    public void setCarryOverDays(BigDecimal carryOverDays)
    {
        this.carryOverDays = carryOverDays;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("quotaId", getQuotaId())
            .append("userId", getUserId())
            .append("leaveType", getLeaveType())
            .append("year", getYear())
            .append("totalDays", getTotalDays())
            .append("usedDays", getUsedDays())
            .append("remainingDays", getRemainingDays())
            .append("carryOverDays", getCarryOverDays())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
