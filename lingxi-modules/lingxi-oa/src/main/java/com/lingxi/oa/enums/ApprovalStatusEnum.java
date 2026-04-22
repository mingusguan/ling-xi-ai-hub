package com.lingxi.oa.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批状态枚举
 */
@Getter
@AllArgsConstructor
public enum ApprovalStatusEnum {
    
    /** 审批中 */
    PENDING("pending", "审批中"),
    
    /** 已通过 */
    APPROVED("approved", "已通过"),
    
    /** 已驳回 */
    REJECTED("rejected", "已驳回");
    
    private final String code;
    private final String desc;
    
    public static ApprovalStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ApprovalStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
