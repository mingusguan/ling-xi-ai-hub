package com.lingxi.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 报销申请实体
 *
 * @author lingxi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "oa_expense", excludeProperty = {"searchValue", "params"})
public class OaExpense extends OaBaseEntity {

    /** 报销ID */
    @TableId(type = IdType.AUTO)
    private Long expenseId;

    /** 报销单号 */
    private String expenseNo;

    /** 报销类型（travel:差旅,office:办公,project:项目,other:其他） */
    private String expenseType;

    /** 报销金额 */
    private BigDecimal amount;

    /** 报销日期 */
    private String expenseDate;

    /** 报销原因 */
    private String expenseReason;

    /** 发票附件URL */
    private String invoiceUrl;

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
    
    /** 当前节点名称（非数据库字段，仅用于展示） */
    @TableField(exist = false)
    private String currentNodeName;
    
    /** 是否有审批权限（非数据库字段，true:可以审批, false:只能查看） */
    @TableField(exist = false)
    private Boolean canApprove;

    /** 是否可以重新提交（非数据库字段，true:已驳回可重新编辑提交） */
    @TableField(exist = false)
    private Boolean canResubmit;

    /** 最后审批人姓名（非数据库字段，仅用于查询） */
    @TableField(exist = false)
    private String lastApproverName;

    /** 最后审批意见（非数据库字段，仅用于查询） */
    @TableField(exist = false)
    private String lastApproverOpinion;
}
