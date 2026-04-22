package com.lingxi.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程模板实体
 *
 * @author lingxi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "oa_process_template", excludeProperty = {"searchValue", "params"})
public class OaProcessTemplate extends OaBaseEntity {

    /** 模板ID */
    @TableId(type = IdType.AUTO)
    private Long templateId;

    /** 模板名称 */
    private String templateName;

    /** 业务类型（leave:请假,expense:报销） */
    private String businessType;

    /** 流程定义Key */
    private String processDefinitionKey;

    /** 流程定义名称 */
    private String processDefinitionName;

    /** 流程版本 */
    private String processVersion;

    /** 部署状态（draft:草稿,deployed:已部署） */
    private String deployStatus;

    /** 表单路由 */
    private String formRoute;

    /** 流程内容（BPMN XML） */
    private String processContent;

    /** 描述 */
    private String description;
}
