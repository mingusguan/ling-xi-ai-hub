package com.lingxi.oa.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.oa.service.IOaHolidayConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 休息日配置Controller
 */
@RestController
@RequestMapping("/oa/holiday")
public class OaHolidayConfigController extends BaseController {

    @Autowired
    private IOaHolidayConfigService holidayConfigService;

    @Autowired
    private com.lingxi.oa.service.IHolidaySyncService holidaySyncService;

    /**
     * 从万年历同步指定年份的节假日数据
     *
     * @param year 年份
     * @return 结果
     */
    @PostMapping("/sync/{year}")
    public AjaxResult syncHolidays(@PathVariable Integer year) {
        try {
            int count = holidaySyncService.syncHolidaysFromRili(year);
            return AjaxResult.success("成功从万年历同步 " + count + " 条数据");
        } catch (Exception e) {
            return AjaxResult.error("同步失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定年份的所有数据
     *
     * @param year 年份
     * @return 结果
     */
    @DeleteMapping("/deleteYear/{year}")
    public AjaxResult deleteYear(@PathVariable Integer year) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.lingxi.oa.domain.OaHolidayConfig> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(com.lingxi.oa.domain.OaHolidayConfig::getYear, year);
            int count = holidayConfigService.deleteByYear(year);
            return AjaxResult.success("成功删除 " + count + " 条数据");
        } catch (Exception e) {
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 查询指定年份的休息日列表
     *
     * @param year 年份
     * @return 休息日列表
     */
    @GetMapping("/list/{year}")
    public AjaxResult listHolidays(@PathVariable Integer year) {
        return AjaxResult.success(holidayConfigService.listByYear(year));
    }

    /**
     * 分页查询休息日列表
     *
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @param startDate   开始日期（可选）
     * @param endDate     结束日期（可选）
     * @param year        年份（可选）
     * @param holidayType 节假日类型（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    public AjaxResult pageHolidays(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer holidayType) {
        Page result = holidayConfigService.listPage(pageNum, pageSize, startDate, endDate, year, holidayType);
        return AjaxResult.success(result);
    }

    /**
     * 根据日期范围查询休息日列表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 休息日列表
     */
    @GetMapping("/list/range")
    public AjaxResult listHolidaysByRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return AjaxResult.success(holidayConfigService.listByDateRange(startDate, endDate));
    }

    /**
     * 更新日期的节假日类型
     *
     * @param holidayId   节假日ID
     * @param holidayType 节假日类型（0:法定休息日 1:固定休息日 2:工作日）
     * @return 结果
     */
    @PutMapping("/updateType")
    public AjaxResult updateHolidayType(
            @RequestParam Long holidayId,
            @RequestParam Integer holidayType) {
        try {
            holidayConfigService.updateHolidayType(holidayId, holidayType);
            return AjaxResult.success("设置成功");
        } catch (Exception e) {
            return AjaxResult.error("设置失败: " + e.getMessage());
        }
    }
}
