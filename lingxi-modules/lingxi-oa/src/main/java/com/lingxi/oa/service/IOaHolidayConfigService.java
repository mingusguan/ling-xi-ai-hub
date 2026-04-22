package com.lingxi.oa.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingxi.oa.domain.OaHolidayConfig;

import java.util.Date;
import java.util.List;

/**
 * 休息日配置Service接口
 */
public interface IOaHolidayConfigService {

    /**
     * 生成指定年度的休息日数据
     *
     * @param year 年份
     * @return 生成/更新的记录数
     */
    int generateYearHolidays(Integer year);

    /**
     * 查询指定年份的休息日列表
     *
     * @param year 年份
     * @return 休息日列表
     */
    List<OaHolidayConfig> listByYear(Integer year);

    /**
     * 分页查询休息日列表
     *
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @param year      年份（可选）
     * @param holidayType 节假日类型（可选）
     * @return 分页结果
     */
    Page<OaHolidayConfig> listPage(Integer pageNum, Integer pageSize, Date startDate, Date endDate, Integer year, Integer holidayType);

    /**
     * 根据日期范围查询休息日列表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 休息日列表
     */
    List<OaHolidayConfig> listByDateRange(Date startDate, Date endDate);

    /**
     * 删除指定年份的所有数据
     *
     * @param year 年份
     * @return 删除的记录数
     */
    int deleteByYear(Integer year);

    /**
     * 更新日期的节假日类型
     *
     * @param holidayId   节假日ID
     * @param holidayType 节假日类型
     */
    void updateHolidayType(Long holidayId, Integer holidayType);
}
