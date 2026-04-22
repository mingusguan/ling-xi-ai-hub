package com.lingxi.oa.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 流程部署结果DTO
 */
@Data
public class DeployResultDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 模板ID */
    private Long templateId;
    
    /** 工作流引擎是否启用 */
    private Boolean enabled;
    
    /** 部署状态 */
    private String status;
    
    /** 部署ID */
    private String deploymentId;
    
    /** 流程定义Key */
    private String processDefinitionKey;
    
    /** 流程版本 */
    private String processVersion;
}
