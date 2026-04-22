package com.lingxi.oa.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 流程启动结果DTO
 */
@Data
public class ProcessStartResultDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 启动状态 */
    private String status;
    
    /** 流程实例ID */
    private String processInstanceId;
    
    /** 业务Key */
    private String businessKey;
}
