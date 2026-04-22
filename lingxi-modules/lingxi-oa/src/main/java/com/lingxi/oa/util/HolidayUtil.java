package com.lingxi.oa.util;

import com.lingxi.oa.domain.OaHolidayConfig;
import com.lingxi.oa.enums.HolidayTypeEnum;
import com.lingxi.oa.mapper.OaHolidayConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 休息日工具类
 */
@Slf4j
@Component
public class HolidayUtil {

    @Autowired
    private OaHolidayConfigMapper holidayConfigMapper;

    /**
     * 判断是否是工作日
     *
     * @param date 日期
     * @return true-工作日，false-非工作日
     */
    public boolean isWorkDay(LocalDate date) {
        if (date == null) {
            return false;
        }

        // 1. 先判断是否是周末
        int dayOfWeek = date.getDayOfWeek().getValue();
        boolean isWeekend = (dayOfWeek == 6 || dayOfWeek == 7);

        // 2. 查询数据库中的配置
        Date sqlDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        OaHolidayConfig config = holidayConfigMapper.selectByDate(sqlDate);

        if (config != null) {
            HolidayTypeEnum typeEnum = config.getHolidayTypeEnum();
            if (typeEnum != null && typeEnum.isRestDay()) {
                // 法定休息日或固定休息日，算休息
                log.debug("日期 {} 是休息日: {}", date, config.getHolidayName());
                return false;
            } else if (typeEnum != null && typeEnum.isWorkDay()) {
                // 工作日，即使是周末也算上班
                log.debug("日期 {} 是工作日: {}", date, config.getHolidayName());
                return true;
            }
        }

        // 3. 没有配置：周末=休息，工作日=上班
        return !isWeekend;
    }

    /**
     * 计算两个日期之间的工作日天数
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 工作日天数
     */
    public long countWorkDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }

        long count = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (isWorkDay(currentDate)) {
                count++;
            }
            currentDate = currentDate.plusDays(1);
        }

        return count;
    }

    /**
     * 计算两个日期之间的工作小时数（按每天8小时计算）
     *
     * @param startDate 开始日期时间
     * @param endDate   结束日期时间
     * @return 工作小时数
     */
    public long calculateWorkHours(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }

        LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        if (startLocalDate.isAfter(endLocalDate)) {
            return 0;
        }

        // 批量查询日期范围内的休息日配置
        List<OaHolidayConfig> configs = holidayConfigMapper.selectByDateRange(startDate, endDate);
        Map<LocalDate, OaHolidayConfig> configMap = new HashMap<>();
        if (configs != null) {
            configMap = configs.stream()
                    .collect(Collectors.toMap(
                            config -> config.getHolidayDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                            config -> config,
                            (k1, k2) -> k1
                    ));
        }

        long totalHours = 0;
        LocalDate currentDate = startLocalDate;

        while (!currentDate.isAfter(endLocalDate)) {
            if (isWorkDayWithConfig(currentDate, configMap)) {
                java.time.LocalDateTime workStart = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                java.time.LocalDateTime workEnd = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                
                if (currentDate.equals(startLocalDate) && currentDate.equals(endLocalDate)) {
                    // 同一天：计算工作时间内的小时差
                    totalHours += calculateWorkHoursInDay(workStart, workEnd);
                } else if (currentDate.equals(startLocalDate)) {
                    // 第一天：从开始时间到当天18:00
                    java.time.LocalDateTime endOfDay = currentDate.atTime(18, 0);
                    totalHours += calculateWorkHoursInDay(workStart, endOfDay);
                } else if (currentDate.equals(endLocalDate)) {
                    // 最后一天：从当天9:00到结束时间
                    java.time.LocalDateTime startOfDay = currentDate.atTime(9, 0);
                    totalHours += calculateWorkHoursInDay(startOfDay, workEnd);
                } else {
                    // 中间整天：固定8小时（9-12点3小时 + 13-18点5小时）
                    totalHours += 8;
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        return totalHours;
    }

    /**
     * 使用预加载的配置判断是否是工作日（避免重复查询数据库）
     */
    private boolean isWorkDayWithConfig(LocalDate date, Map<LocalDate, OaHolidayConfig> configMap) {
        OaHolidayConfig config = configMap.get(date);
        if (config != null) {
            HolidayTypeEnum typeEnum = config.getHolidayTypeEnum();
            // 0-法定休息日，1-固定休息日，2-工作日
            if (typeEnum != null && typeEnum.isRestDay()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算两个时间点之间的工作时间（只计算9:00-12:00和13:00-18:00）
     *
     * @param startDateTime 开始时间
     * @param endDateTime   结束时间
     * @return 工作小时数
     */
    private long calculateWorkHoursInDay(java.time.LocalDateTime startDateTime, java.time.LocalDateTime endDateTime) {
        if (startDateTime.isAfter(endDateTime)) {
            return 0;
        }

        long totalMinutes = 0;

        // 定义工作时间段
        java.time.LocalTime morningStart = java.time.LocalTime.of(9, 0);
        java.time.LocalTime morningEnd = java.time.LocalTime.of(12, 0);
        java.time.LocalTime afternoonStart = java.time.LocalTime.of(13, 0);
        java.time.LocalTime afternoonEnd = java.time.LocalTime.of(18, 0);

        // 计算上午工作时间
        java.time.LocalDateTime morningActualStart = startDateTime.toLocalDate().atTime(
                startDateTime.toLocalTime().isBefore(morningStart) ? morningStart : startDateTime.toLocalTime()
        );
        java.time.LocalDateTime morningActualEnd = endDateTime.toLocalDate().atTime(
                endDateTime.toLocalTime().isAfter(morningEnd) ? morningEnd : endDateTime.toLocalTime()
        );

        if (!morningActualStart.isAfter(morningActualEnd)) {
            totalMinutes += java.time.temporal.ChronoUnit.MINUTES.between(morningActualStart, morningActualEnd);
        }

        // 计算下午工作时间
        java.time.LocalDateTime afternoonActualStart = startDateTime.toLocalDate().atTime(
                startDateTime.toLocalTime().isBefore(afternoonStart) ? afternoonStart : startDateTime.toLocalTime()
        );
        java.time.LocalDateTime afternoonActualEnd = endDateTime.toLocalDate().atTime(
                endDateTime.toLocalTime().isAfter(afternoonEnd) ? afternoonEnd : endDateTime.toLocalTime()
        );

        if (!afternoonActualStart.isAfter(afternoonActualEnd)) {
            totalMinutes += java.time.temporal.ChronoUnit.MINUTES.between(afternoonActualStart, afternoonActualEnd);
        }

        // 转换为小时（保留1位小数）
        return totalMinutes / 60;
    }

    /**
     * 获取时间范围内的所有休息日（包括周末和法定节假日）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 休息日列表
     */
    public List<OaHolidayConfig> getRestDaysInRange(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return new ArrayList<>();
        }

        // 批量查询日期范围内的休息日配置
        List<OaHolidayConfig> configs = holidayConfigMapper.selectByDateRange(startDate, endDate);
        if (configs == null || configs.isEmpty()) {
            return new ArrayList<>();
        }

        // 过滤出休息日（类型0-法定休息日，1-固定休息日）
        return configs.stream()
                .filter(config -> {
                    HolidayTypeEnum typeEnum = config.getHolidayTypeEnum();
                    return typeEnum != null && typeEnum.isRestDay();
                })
                .collect(Collectors.toList());
    }
}
