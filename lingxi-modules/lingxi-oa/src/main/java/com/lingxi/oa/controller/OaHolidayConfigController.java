package com.lingxi.oa.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.security.annotation.Logical;
import com.lingxi.common.security.annotation.RequiresPermissions;
import com.lingxi.oa.service.IHolidaySyncService;
import com.lingxi.oa.service.IOaHolidayConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 假期配置 Controller。
 */
@RestController
@RequestMapping("/oa/holiday")
public class OaHolidayConfigController extends BaseController
{
    @Autowired
    private IOaHolidayConfigService holidayConfigService;

    @Autowired
    private IHolidaySyncService holidaySyncService;

    @RequiresPermissions(value = {"oa:holiday:list", "oa:leave:edit"}, logical = Logical.OR)
    @PostMapping("/sync/{year}")
    public AjaxResult syncHolidays(@PathVariable Integer year)
    {
        int count = holidaySyncService.syncHolidaysFromRili(year);
        return AjaxResult.success("同步成功，共处理 " + count + " 条数据");
    }

    @RequiresPermissions(value = {"oa:holiday:list", "oa:leave:remove"}, logical = Logical.OR)
    @DeleteMapping("/deleteYear/{year}")
    public AjaxResult deleteYear(@PathVariable Integer year)
    {
        int count = holidayConfigService.deleteByYear(year);
        return AjaxResult.success("删除成功，共删除 " + count + " 条数据");
    }

    @RequiresPermissions("oa:holiday:list")
    @GetMapping("/list/{year}")
    public AjaxResult listHolidays(@PathVariable Integer year)
    {
        return AjaxResult.success(holidayConfigService.listByYear(year));
    }

    @RequiresPermissions("oa:holiday:list")
    @GetMapping("/page")
    public AjaxResult pageHolidays(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer holidayType)
    {
        Page result = holidayConfigService.listPage(pageNum, pageSize, startDate, endDate, year, holidayType);
        return AjaxResult.success(result);
    }

    @RequiresPermissions("oa:holiday:list")
    @GetMapping("/list/range")
    public AjaxResult listHolidaysByRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate)
    {
        return AjaxResult.success(holidayConfigService.listByDateRange(startDate, endDate));
    }

    @RequiresPermissions(value = {"oa:holiday:list", "oa:leave:edit"}, logical = Logical.OR)
    @PutMapping("/updateType")
    public AjaxResult updateHolidayType(@RequestParam Long holidayId, @RequestParam Integer holidayType)
    {
        if (holidayType == null || holidayType < 0 || holidayType > 2) {
            throw new ServiceException("假期类型非法");
        }
        holidayConfigService.updateHolidayType(holidayId, holidayType);
        return AjaxResult.success("设置成功");
    }
}
