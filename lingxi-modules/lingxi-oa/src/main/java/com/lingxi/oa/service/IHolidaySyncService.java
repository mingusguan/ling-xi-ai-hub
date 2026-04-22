package com.lingxi.oa.service;

/**
 * 节假日同步Service接口
 */
public interface IHolidaySyncService {

    /**
     * 从万年历同步指定年份的节假日数据
     *
     * @param year 年份
     * @return 同步的数据条数
     */
    int syncHolidaysFromRili(Integer year);
}
