package com.lingxi.oa.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批操作类型枚举
 */
@Getter
@AllArgsConstructor
public enum ApprovalActionEnum {
    
    /** 通过 */
    APPROVE("approve", "通过"),
    
    /** 驳回 */
    REJECT("reject", "驳回"),
    
    /** 提交 */
    SUBMIT("submit", "提交"),
    
    /** 撤回 */
    CANCEL("cancel", "撤回"),
    
    /** 重新提交 */
    RESUBMIT("resubmit", "重新提交");
    
    private final String code;
    private final String desc;
    
    public static ApprovalActionEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ApprovalActionEnum action : values()) {
            if (action.getCode().equals(code)) {
                return action;
            }
        }
        return null;
    }
}
