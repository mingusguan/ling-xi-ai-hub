package com.lingxi.oa.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 请假申请DTO（带权限标识）
 */
@Data
public class OaLeaveDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 请假ID */
    private Long leaveId;
    
    /** 请假单号 */
    private String leaveNo;
    
    /** 请假类型 */
    private String leaveType;
    
    /** 开始时间 */
    private Date startTime;
    
    /** 结束时间 */
    private Date endTime;
    
    /** 请假时长(小时) */
    private BigDecimal leaveHours;
    
    /** 请假事由 */
    private String leaveReason;
    
    /** 审批状态 */
    private String approvalStatus;
    
    /** 申请人用户ID */
    private Long applicantUserId;
    
    /** 申请人姓名 */
    private String applicantUserName;
    
    /** 部门ID */
    private Long deptId;
    
    /** 部门名称 */
    private String deptName;
    
    /** 流程实例ID */
    private String processInstanceId;
    
    /** 创建人 */
    private String createBy;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新人 */
    private String updateBy;
    
    /** 更新时间 */
    private Date updateTime;
    
    /** 是否有审批权限（true:可以审批, false:只能查看） */
    private Boolean canApprove;
    
    /** 是否可以重新提交（true:已驳回可重新编辑提交） */
    private Boolean canResubmit;
    
    /** 当前节点名称 */
    private String currentNodeName;

    /** 是否超额(0-否,1-是) */
    private Integer isOverQuota;

    /** 超额天数 */
    private java.math.BigDecimal overQuotaDays;
}
