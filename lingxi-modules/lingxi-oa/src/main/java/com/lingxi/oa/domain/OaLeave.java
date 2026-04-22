package com.lingxi.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 请假申请实体
 *
 * @author lingxi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "oa_leave", excludeProperty = {"searchValue", "params"})
public class OaLeave extends OaBaseEntity {

    /** 请假ID */
    @TableId(type = IdType.AUTO)
    private Long leaveId;

    /** 请假单号 */
    private String leaveNo;

    /** 请假类型（casual:事假,annual:年假,sick:病假,other:其他） */
    private String leaveType;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /** 请假时长（小时） */
    private BigDecimal leaveHours;

    /** 请假原因 */
    private String leaveReason;

    /** 审批状态（pending:待审批,approved:已通过,rejected:已驳回） */
    private String approvalStatus;

    /** 申请人用户ID */
    private Long applicantUserId;

    /** 申请人姓名（非数据库字段，仅用于查询） */
    @TableField(exist = false)
    private String applicantUserName;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 流程实例ID */
    private String processInstanceId;

    /** 最后审批人姓名（非数据库字段，仅用于查询） */
    @TableField(exist = false)
    private String lastApproverName;

    /** 最后审批意见（非数据库字段，仅用于查询） */
    @TableField(exist = false)
    private String lastApproverOpinion;

    /** 是否超额(0-否,1-是) */
    private Integer isOverQuota;

    /** 超额天数 */
    private java.math.BigDecimal overQuotaDays;
}
