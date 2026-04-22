package com.lingxi.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingxi.oa.domain.OaHolidayConfig;
import com.lingxi.oa.enums.HolidayTypeEnum;
import com.lingxi.oa.mapper.OaHolidayConfigMapper;
import com.lingxi.oa.service.IOaHolidayConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 休息日配置Service实现
 */
@Slf4j
@Service
public class OaHolidayConfigServiceImpl implements IOaHolidayConfigService {

    @Autowired
    private OaHolidayConfigMapper holidayConfigMapper;

    /**
     * 2026年中国法定休息日配置（可根据国务院通知调整）
     */
    private static final Map<String, List<HolidayInfo>> HOLIDAY_DATA = new HashMap<>();


    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateYearHolidays(Integer year) {
        if (year == null || year < 2020 || year > 2030) {
            throw new IllegalArgumentException("年份必须在2020-2030之间");
        }

        String yearStr = String.valueOf(year);
        List<HolidayInfo> holidayInfos = HOLIDAY_DATA.get(yearStr);
        
        if (holidayInfos == null || holidayInfos.isEmpty()) {
            throw new IllegalArgumentException("暂不支持" + year + "年的休息日生成，请先配置休息日数据");
        }

        int count = 0;
        
        // 1. 先生成周末休息日（周六、周日）
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            int dayOfWeek = currentDate.getDayOfWeek().getValue();
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                // 周六或周日
                Date sqlDate = Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                OaHolidayConfig existing = holidayConfigMapper.selectByDate(sqlDate);
                
                if (existing == null) {
                    OaHolidayConfig config = new OaHolidayConfig();
                    config.setHolidayDate(sqlDate);
                    config.setHolidayType(HolidayTypeEnum.FIXED_REST.getCode()); // 1-固定休息日
                    config.setHolidayName(dayOfWeek == 6 ? "星期六" : "星期日");
                    config.setYear(year);
                    config.setCreateTime(new Date());
                    config.setUpdateTime(new Date());
                    holidayConfigMapper.insert(config);
                    log.debug("新增固定休息日: {}", currentDate);
                    count++;
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        
        // 2. 再生成法定节假日
        for (HolidayInfo info : holidayInfos) {
            try {
                LocalDate holidayDate = LocalDate.parse(info.getDate());
                Date sqlDate = Date.from(holidayDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                
                // 查询是否已存在
                OaHolidayConfig existing = holidayConfigMapper.selectByDate(sqlDate);
                
                if (existing != null) {
                    // 更新已有记录
                    existing.setHolidayType(info.getType());
                    existing.setHolidayName(info.getName());
                    existing.setYear(year);
                    existing.setUpdateTime(new Date());
                    holidayConfigMapper.updateById(existing);
                    log.debug("更新法定节假日: {}", info.getDate());
                } else {
                    // 插入新记录
                    OaHolidayConfig config = new OaHolidayConfig();
                    config.setHolidayDate(sqlDate);
                    config.setHolidayType(info.getType());
                    config.setHolidayName(info.getName());
                    config.setYear(year);
                    config.setCreateTime(new Date());
                    config.setUpdateTime(new Date());
                    holidayConfigMapper.insert(config);
                    log.debug("新增法定节假日: {}", info.getDate());
                }
                count++;
            } catch (Exception e) {
                log.error("处理法定节假日 {} 失败", info.getDate(), e);
            }
        }

        log.info("成功生成/更新 {} 年 {} 条休息日数据", year, count);
        return count;
    }

    @Override
    public List<OaHolidayConfig> listByYear(Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        
        LambdaQueryWrapper<OaHolidayConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaHolidayConfig::getYear, year)
               .orderByAsc(OaHolidayConfig::getHolidayDate);
        
        return holidayConfigMapper.selectList(wrapper);
    }

    @Override
    public Page<OaHolidayConfig> listPage(Integer pageNum, Integer pageSize, Date startDate, Date endDate, Integer year, Integer holidayType) {
        // 默认页码和每页大小
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        
        Page<OaHolidayConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaHolidayConfig> wrapper = new LambdaQueryWrapper<>();
        
        // 日期范围筛选
        if (startDate != null) {
            wrapper.ge(OaHolidayConfig::getHolidayDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(OaHolidayConfig::getHolidayDate, endDate);
        }
        
        // 年份筛选
        if (year != null) {
            wrapper.eq(OaHolidayConfig::getYear, year);
        }
        
        // 节假日类型筛选
        if (holidayType != null) {
            wrapper.eq(OaHolidayConfig::getHolidayType, holidayType);
        }
        
        wrapper.orderByAsc(OaHolidayConfig::getHolidayDate);
        
        return holidayConfigMapper.selectPage(page, wrapper);
    }

    @Override
    public List<OaHolidayConfig> listByDateRange(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }
        
        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        
        LambdaQueryWrapper<OaHolidayConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(OaHolidayConfig::getHolidayDate, startDate)
               .le(OaHolidayConfig::getHolidayDate, endDate)
               .orderByAsc(OaHolidayConfig::getHolidayDate);
        
        return holidayConfigMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByYear(Integer year) {
        if (year == null) {
            throw new IllegalArgumentException("年份不能为空");
        }
        
        LambdaQueryWrapper<OaHolidayConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaHolidayConfig::getYear, year);
        
        return holidayConfigMapper.delete(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHolidayType(Long holidayId, Integer holidayType) {
        if (holidayType == null || holidayId == null) {
            throw new IllegalArgumentException("类型和ID不能为空");
        }
        
        OaHolidayConfig config = holidayConfigMapper.selectById(holidayId);

        config.setHolidayType(holidayType);
        
        if (holidayId != null && config.getId() != null) {
            holidayConfigMapper.updateById(config);
        }
    }

    private String getWeekDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
    }

    /**
     * 休息日信息内部类
     */
    private static class HolidayInfo {
        private String date;
        private Integer type; // 0-法定休息日，1-周末休息日
        private String name;

        public HolidayInfo(String date, Integer type, String name) {
            this.date = date;
            this.type = type;
            this.name = name;
        }

        public String getDate() {
            return date;
        }

        public Integer getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }
}
