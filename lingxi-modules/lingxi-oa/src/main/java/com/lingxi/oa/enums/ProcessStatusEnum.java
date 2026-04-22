package com.lingxi.oa.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程实例状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProcessStatusEnum {
    
    STARTED("started", "已启动"),
    PROCESSING("processing", "审批中"),
    APPROVED("approved", "已通过"),
    REJECTED("rejected", "已驳回"),
    TASK_NOT_FOUND("task_not_found", "任务未找到");
    
    private final String code;
    private final String desc;
    
    public static ProcessStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ProcessStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
