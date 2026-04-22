package com.lingxi.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.lingxi.common.core.web.domain.BaseEntity;

/**
 * 子系统表 sys_subsystem
 * 
 * @author cloud
 */
public class SysSubsystem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 子系统ID */
    private Long subsystemId;

    /** 子系统编码 */
    private String subsystemCode;

    /** 子系统名称 */
    private String subsystemName;

    /** 显示顺序 */
    private Integer orderNum;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getSubsystemId()
    {
        return subsystemId;
    }

    public void setSubsystemId(Long subsystemId)
    {
        this.subsystemId = subsystemId;
    }

    @NotBlank(message = "子系统编码不能为空")
    @Size(min = 0, max = 50, message = "子系统编码长度不能超过50个字符")
    public String getSubsystemCode()
    {
        return subsystemCode;
    }

    public void setSubsystemCode(String subsystemCode)
    {
        this.subsystemCode = subsystemCode;
    }

    @NotBlank(message = "子系统名称不能为空")
    @Size(min = 0, max = 100, message = "子系统名称长度不能超过100个字符")
    public String getSubsystemName()
    {
        return subsystemName;
    }

    public void setSubsystemName(String subsystemName)
    {
        this.subsystemName = subsystemName;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
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
            .append("subsystemId", getSubsystemId())
            .append("subsystemCode", getSubsystemCode())
            .append("subsystemName", getSubsystemName())
            .append("orderNum", getOrderNum())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
