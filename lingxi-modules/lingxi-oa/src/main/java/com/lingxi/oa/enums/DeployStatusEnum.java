package com.lingxi.oa.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程部署状态枚举
 */
@Getter
@AllArgsConstructor
public enum DeployStatusEnum {
    
    NOT_FOUND("not_found", "未找到模板"),
    SKIPPED("skipped", "跳过部署"),
    DEPLOYED("deployed", "已部署"),
    DRAFT("draft", "草稿");
    
    private final String code;
    private final String desc;
    
    public static DeployStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DeployStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
