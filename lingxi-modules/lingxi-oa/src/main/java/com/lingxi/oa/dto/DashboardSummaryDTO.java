package com.lingxi.oa.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 工作台概览DTO
 */
@Data
public class DashboardSummaryDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 公告数量 */
    private Long noticeCount;
    
    /** 待办任务数 */
    private Long taskTodoCount;
    
    /** 进行中任务数 */
    private Long taskProcessingCount;
    
    /** 待审批请假数 */
    private Long leavePendingCount;
    
    /** 待审批报销数 */
    private Long expensePendingCount;
    
    /** 模板数量 */
    private Long templateCount;
    
    /** 待办工作流数 */
    private Integer todoWorkflowCount;
    
    /** 今日已审批数 */
    private Integer approvedTodayCount;
    
    /** 超时预警数 */
    private Integer timeoutWarningCount;
    
    /** 欢迎文本 */
    private String welcomeText;
}
