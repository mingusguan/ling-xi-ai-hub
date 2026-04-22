package com.lingxi.oa.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lingxi.oa.enums.HolidayTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 休息日配置表
 */
@Data
@TableName("oa_holiday_config")
public class OaHolidayConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 日期 */
    private Date holidayDate;

    /** 类型：0-法定休息日，1-固定休息日，2-工作日 */
    private Integer holidayType;

    /** 节日名称 */
    private String holidayName;

    /** 年份 */
    private Integer year;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    // ========== 农历信息 ==========
    /** 农历日期 */
    private String lunarDate;

    /** 农历月份 */
    private String lunarMonth;

    // ========== 宜忌 ==========
    /** 宜（JSON数组） */
    private String yi;

    /** 忌（JSON数组） */
    private String ji;

    // ========== 星座生肖 ==========
    /** 星座 */
    private String xingzuo;

    /** 生肖 */
    private String shengxiao;

    // ========== 五行八字 ==========
    /** 年干支 */
    private String gzNian;

    /** 月干支 */
    private String gzYue;

    /** 日干支 */
    private String gzRi;

    /** 年五行 */
    private String wxNian;

    /** 月五行 */
    private String wxYue;

    /** 日五行 */
    private String wxRi;

    // ========== 其他 ==========
    /** 星期 */
    private String weekDay;

    /** 月相 */
    private String yuexiang;

    /**
     * 获取节假日类型枚举
     */
    public HolidayTypeEnum getHolidayTypeEnum() {
        return HolidayTypeEnum.getByCode(this.holidayType);
    }

    /**
     * 获取类型描述
     */
    public String getHolidayTypeDesc() {
        HolidayTypeEnum typeEnum = getHolidayTypeEnum();
        return typeEnum != null ? typeEnum.getDesc() : "未知";
    }
}
