package com.lingxi.oa.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节假日类型枚举
 */
@Getter
@AllArgsConstructor
public enum HolidayTypeEnum {

    /**
     * 法定休息日
     */
    LEGAL_HOLIDAY(0, "法定休息日"),

    /**
     * 固定休息日（周末）
     */
    FIXED_REST(1, "固定休息日"),

    /**
     * 工作日
     */
    WORKDAY(2, "工作日");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static HolidayTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (HolidayTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否是休息日
     */
    public boolean isRestDay() {
        return this == LEGAL_HOLIDAY || this == FIXED_REST;
    }

    /**
     * 判断是否是工作日
     */
    public boolean isWorkDay() {
        return this == WORKDAY;
    }
}
