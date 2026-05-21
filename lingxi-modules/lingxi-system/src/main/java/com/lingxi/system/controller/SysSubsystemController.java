package com.lingxi.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.log.annotation.Log;
import com.lingxi.common.log.enums.BusinessType;
import com.lingxi.common.security.annotation.RequiresPermissions;
import com.lingxi.system.domain.SysSubsystem;
import com.lingxi.system.service.ISysSubsystemService;

/**
 * 子系统信息
 * 
 * @author cloud
 */
@RestController
@RequestMapping("/system/subsystem")
public class SysSubsystemController extends BaseController
{
    @Autowired
    private ISysSubsystemService subsystemService;

    /**
     * 获取子系统列表
     */
    @RequiresPermissions("system:subsystem:list")
    @GetMapping("/list")
    public AjaxResult list(SysSubsystem subsystem)
    {
        List<SysSubsystem> list = subsystemService.selectSubsystemList(subsystem);
        return success(list);
    }

    /**
     * 获取所有子系统下拉框列表（用于菜单管理等）
     */
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        SysSubsystem subsystem = new SysSubsystem();
        subsystem.setStatus("0"); // 只查询正常的子系统
        List<SysSubsystem> list = subsystemService.selectSubsystemList(subsystem);
        return success(list);
    }

    /**
     * 根据子系统编号获取详细信息
     */
    @RequiresPermissions("system:subsystem:query")
    @GetMapping(value = "/{subsystemId}")
    public AjaxResult getInfo(@PathVariable Long subsystemId)
    {
        return success(subsystemService.selectSubsystemById(subsystemId));
    }

    /**
     * 新增子系统
     */
    @RequiresPermissions("system:subsystem:add")
    @Log(title = "子系统", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysSubsystem subsystem)
    {
        return toAjax(subsystemService.insertSubsystem(subsystem));
    }

    /**
     * 修改子系统
     */
    @RequiresPermissions("system:subsystem:edit")
    @Log(title = "子系统", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysSubsystem subsystem)
    {
        return toAjax(subsystemService.updateSubsystem(subsystem));
    }

    /**
     * 删除子系统
     */
    @RequiresPermissions("system:subsystem:remove")
    @Log(title = "子系统", businessType = BusinessType.DELETE)
    @DeleteMapping("/{subsystemIds}")
    public AjaxResult remove(@PathVariable Long[] subsystemIds)
    {
        return toAjax(subsystemService.deleteSubsystemByIds(subsystemIds));
    }
}
