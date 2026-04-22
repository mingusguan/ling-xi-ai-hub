package com.lingxi.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批记录实体
 *
 * @author lingxi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "oa_approval_record", excludeProperty = {"searchValue", "params"})
public class OaApprovalRecord extends OaBaseEntity {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long recordId;

    /** 业务类型（leave:请假,expense:报销） */
    private String businessType;

    /** 业务Key（格式：businessType:businessId） */
    private String businessKey;

    /** 流程实例ID */
    private String processInstanceId;

    /** 任务ID */
    private String taskId;

    /** 操作类型（参考 ApprovalActionEnum） */
    private String actionType;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名（非数据库字段，用于展示） */
    @TableField(exist = false)
    private String approverName;

    /** 审批意见 */
    private String commentText;
}
