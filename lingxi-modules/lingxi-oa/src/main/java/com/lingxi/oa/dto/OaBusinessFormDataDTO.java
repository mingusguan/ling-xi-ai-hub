package com.lingxi.oa.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * OA业务表单数据DTO
 * 用于AI审批意见生成时传递业务表单数据
 *
 * @author lingxi
 */
@Data
public class OaBusinessFormDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务ID（请假ID或报销ID） */
    private Long businessId;

    /** 申请人用户名 */
    private Long applicantUserId;

    /** 申请人姓名 */
    private String applicantName;

    /** 流程实例ID */
    private String processInstanceId;

    /** 审批状态 */
    private String approvalStatus;

    // ========== 请假相关字段 ==========

    /** 请假类型 */
    private String leaveType;

    /** 开始时间 */
    private Date startDate;

    /** 结束时间 */
    private Date endDate;

    /** 请假天数 */
    private Integer leaveDays;

    /** 请假原因 */
    private String reason;

    // ========== 报销相关字段 ==========

    /** 报销类型 */
    private String expenseType;

    /** 报销金额 */
    private BigDecimal amount;
}
