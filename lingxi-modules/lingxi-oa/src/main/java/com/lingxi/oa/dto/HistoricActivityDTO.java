package com.lingxi.oa.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程历史活动DTO
 */
@Data
public class HistoricActivityDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 活动ID */
    private String activityId;
    
    /** 活动名称 */
    private String activityName;
    
    /** 活动类型 */
    private String activityType;
    
    /** 办理人 */
    private String assignee;
    
    /** 办理人姓名 */
    private String assigneeName;
    
    /** 开始时间 */
    private Date startTime;
    
    /** 结束时间 */
    private Date endTime;
    
    /** 持续时间（毫秒） */
    private Long durationInMillis;
    
    /** 是否已完成 */
    private Boolean completed;
}
