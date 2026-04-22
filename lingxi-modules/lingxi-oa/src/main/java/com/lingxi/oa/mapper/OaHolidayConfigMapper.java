package com.lingxi.oa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lingxi.oa.domain.OaHolidayConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 休息日配置Mapper
 */
@Mapper
public interface OaHolidayConfigMapper extends BaseMapper<OaHolidayConfig> {

    /**
     * 查询指定日期范围内的所有休息日配置
     */
    List<OaHolidayConfig> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 查询某天的休息日配置
     */
    OaHolidayConfig selectByDate(@Param("date") Date date);
}
