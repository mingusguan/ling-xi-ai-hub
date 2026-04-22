package com.lingxi.oa.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingxi.oa.domain.OaHolidayConfig;
import com.lingxi.oa.enums.HolidayTypeEnum;
import com.lingxi.oa.mapper.OaHolidayConfigMapper;
import com.lingxi.oa.service.IHolidaySyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 节假日同步Service实现
 */
@Slf4j
@Service
public class HolidaySyncServiceImpl implements IHolidaySyncService {

    @Autowired
    private OaHolidayConfigMapper holidayConfigMapper;

    /**
     * 万年历JSON接口地址模板
     */
    private static final String RILI_API_URL = "https://www.rili.com.cn/rili/json/pc_wnl/%d/%02d.js?_=%d";

    /**
     * 从万年历同步指定年份的节假日数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncHolidaysFromRili(Integer year) {
        if (year == null || year < 2020 || year > 2030) {
            throw new IllegalArgumentException("年份必须在2020-2030之间");
        }

        // 检查该年份是否已存在数据
        LambdaQueryWrapper<OaHolidayConfig> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(OaHolidayConfig::getYear, year);
        Long count = holidayConfigMapper.selectCount(checkWrapper);
        
        if (count != null && count > 0) {
            throw new RuntimeException(year + "年数据已存在，不支持重复同步。如需重新同步，请先删除该年份数据。");
        }

        int totalCount = 0;

        // 遍历1-12月
        for (int month = 1; month <= 12; month++) {
            try {
                log.info("开始同步{}年{}月数据...", year, month);

                // 1. 调用万年历接口获取当月数据
                JSONArray monthData = crawlMonthData(year, month);

                if (monthData == null || monthData.isEmpty()) {
                    log.warn("{}年{}月数据为空，跳过", year, month);
                    continue;
                }

                // 2. 解析并保存到数据库
                int monthCount = parseAndSave(monthData, year, month);
                totalCount += monthCount;

                log.info("{}年{}月同步完成，共{}条数据", year, month, monthCount);

                // 3. 延迟500ms，避免频繁请求
                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("同步被中断", e);
                throw new RuntimeException("同步被中断", e);
            } catch (Exception e) {
                log.error("同步{}年{}月数据失败，继续处理下一个月", year, month, e);
                // 继续处理下一个月，不中断整个流程
            }
        }

        log.info("{}年全年数据同步完成，共{}条数据", year, totalCount);
        return totalCount;
    }

    /**
     * 爬取指定月份的数据
     */
    private JSONArray crawlMonthData(Integer year, Integer month) {
        long timestamp = System.currentTimeMillis();
        String url = String.format(RILI_API_URL, year, month, timestamp);

        try {
            // 发送HTTP请求
            String response = HttpUtil.get(url, 5000);

            // 提取JSON数据（接口返回的是 jsonrun_PcWnl({...}) 格式）
            String jsonStr = extractJson(response);

            if (jsonStr == null || jsonStr.isEmpty()) {
                log.error("无法解析JSON数据: {}", response.substring(0, Math.min(100, response.length())));
                return null;
            }

            // 解析JSON
            JSONObject jsonObject = JSONUtil.parseObj(jsonStr);
            return jsonObject.getJSONArray("data");

        } catch (Exception e) {
            log.error("请求万年历接口失败: {}", url, e);
            throw new RuntimeException("请求万年历接口失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从响应中提取JSON字符串
     * 响应格式: jsonrun_PcWnl({...})
     */
    private String extractJson(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        try {
            // 找到第一个 { 和最后一个 } 的位置
            int startIndex = response.indexOf('{');
            int endIndex = response.lastIndexOf('}');
            
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                return response.substring(startIndex, endIndex + 1);
            }
        } catch (Exception e) {
            log.error("提取JSON失败", e);
        }

        return null;
    }

    /**
     * 解析并保存数据
     */
    private int parseAndSave(JSONArray dataArray, Integer year, Integer month) {
        int count = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject item = dataArray.getJSONObject(i);

            try {
                // 只处理当前月份的数据（yuethis=0表示当前月）
                Integer yuethis = item.getInt("yuethis");
                if (yuethis == null || yuethis != 0) {
                    continue;
                }

                // 提取日期
                int day = item.getInt("ri");
                LocalDate date = LocalDate.of(year, month, day);
                Date sqlDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());

                // 判断日期类型
                Integer holidayType = null;
                String holidayName = null;

                // 1. 判断是否是法定节假日（jia=92或94表示法定节假日）
                Integer jia = item.getInt("jia");
                if (jia != null && (jia == 92 || jia == 94)) {
                    holidayType = HolidayTypeEnum.LEGAL_HOLIDAY.getCode();
                    // 提取节日名称
                    holidayName = extractHolidayName(item);
                }
                // 2. 判断是否是调休工作日（jia=90表示周末需要上班）
                else if (jia != null && jia == 90) {
                    holidayType = HolidayTypeEnum.WORKDAY.getCode();
                    holidayName = "调休上班";
                }
                // 3. 判断是否是周末（week=6或7）
                else {
                    Integer week = item.getInt("week");
                    if (week != null && (week == 6 || week == 7)) {
                        holidayType = HolidayTypeEnum.FIXED_REST.getCode();
                    }
                }
                // 4. 其他为工作日
                if (holidayType == null) {
                    holidayType = HolidayTypeEnum.WORKDAY.getCode();
                }

                // 保存数据（无论是否节假日，都保存完整信息）
                saveOrUpdateHoliday(sqlDate, holidayType, holidayName, year, item);
                count++;

            } catch (Exception e) {
                log.error("解析第{}条数据失败", i, e);
            }
        }

        return count;
    }

    /**
     * 提取节日名称
     */
    private String extractHolidayName(JSONObject item) {
        // 优先从jie字段提取
        String jie = item.getStr("jie");
        if (jie != null && !jie.isEmpty()) {
            // 去除HTML标签
            return jie.replaceAll("<[^>]+>", "").trim();
        }

        // 其次从jieri字段提取
        String jieri = item.getStr("jieri");
        if (jieri != null && !jieri.isEmpty()) {
            // 取第一个节日
            String[] festivals = jieri.split(",");
            if (festivals.length > 0) {
                return festivals[0].replaceAll("<[^>]+>", "").trim();
            }
        }

        return "法定假日";
    }

    /**
     * 保存或更新节假日数据
     */
    private void saveOrUpdateHoliday(Date holidayDate, Integer holidayType, String holidayName, Integer year, JSONObject item) {
        // 查询是否已存在
        LambdaQueryWrapper<OaHolidayConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaHolidayConfig::getHolidayDate, holidayDate);
        OaHolidayConfig existing = holidayConfigMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新已有记录
            existing.setHolidayType(holidayType);
            existing.setHolidayName(holidayName);
            existing.setYear(year);
            
            // 更新详细信息
            fillDetailFields(existing, item);
            
            existing.setUpdateTime(new Date());
            holidayConfigMapper.updateById(existing);
            log.debug("更新日期: {}", holidayDate);
        } else {
            // 插入新记录
            OaHolidayConfig config = new OaHolidayConfig();
            config.setHolidayDate(holidayDate);
            config.setHolidayType(holidayType);
            config.setHolidayName(holidayName);
            config.setYear(year);
            
            // 填充详细信息
            fillDetailFields(config, item);
            
            config.setCreateTime(new Date());
            config.setUpdateTime(new Date());
            holidayConfigMapper.insert(config);
            log.debug("新增日期: {}", holidayDate);
        }
    }

    /**
     * 填充详细字段
     */
    private void fillDetailFields(OaHolidayConfig config, JSONObject item) {
        // 农历信息
        config.setLunarDate(item.getStr("r2")); // 农历日
        config.setLunarMonth(item.getStr("n_yueri")); // 农历月日
        
        // 宜忌（JSON数组转字符串）
        JSONArray yiArr = item.getJSONArray("yi");
        if (yiArr != null && !yiArr.isEmpty()) {
            config.setYi(yiArr.toString());
        }
        
        JSONArray jiArr = item.getJSONArray("ji");
        if (jiArr != null && !jiArr.isEmpty()) {
            config.setJi(jiArr.toString());
        }
        
        // 星座生肖
        config.setXingzuo(extractXingzuo(item));
        config.setShengxiao(item.getStr("shengxiao"));
        
        // 五行八字
        config.setGzNian(item.getStr("gz_nian"));
        config.setGzYue(item.getStr("gz_yue"));
        config.setGzRi(item.getStr("gz_ri"));
        config.setWxNian(item.getStr("wx_nian"));
        config.setWxYue(item.getStr("wx_yue"));
        config.setWxRi(item.getStr("wx_ri"));
        
        // 其他
        config.setWeekDay(item.getStr("dddd")); // 星期
        config.setYuexiang(item.getStr("yuexiang")); // 月相
    }

    /**
     * 提取星座（去除HTML标签）
     */
    private String extractXingzuo(JSONObject item) {
        String xingzuoLink = item.getStr("xingzuo_link");
        if (xingzuoLink != null && !xingzuoLink.isEmpty()) {
            return xingzuoLink.replaceAll("<[^>]+>", "").trim();
        }
        return null;
    }
}
