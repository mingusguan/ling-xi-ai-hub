package com.lingxi.oa.controller;

import java.time.LocalDate;
import java.util.List;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.security.annotation.InnerAuth;
import com.lingxi.oa.domain.OaLeaveQuota;
import com.lingxi.oa.service.IOaLeaveQuotaService;
import com.lingxi.oa.service.IOaLeaveRuleService;
import com.lingxi.system.api.domain.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 假期额度远程调用接口(内部接口,供其他模块调用)
 */
@RestController
@RequestMapping("/leave/quota")
public class OaLeaveQuotaRemoteController
{
    @Autowired
    private IOaLeaveQuotaService leaveQuotaService;
    
    @Autowired
    private IOaLeaveRuleService leaveRuleService;

    /**
     * 停用用户所有假期额度(离职时调用)
     */
    @InnerAuth
    @PostMapping("/disable")
    public R<Boolean> disableUserQuotas(@RequestParam("userId") Long userId)
    {
        int currentYear = LocalDate.now().getYear();
        List<OaLeaveQuota> quotas = leaveQuotaService.selectOaLeaveQuotaList(
            new OaLeaveQuota() {{ setUserId(userId); }}
        );
        
        for (OaLeaveQuota quota : quotas) {
            quota.setStatus("1"); // 1-停用
            leaveQuotaService.updateOaLeaveQuota(quota);
        }
        
        return R.ok(true);
    }

    /**
     * P1: 手动触发生成年度额度(管理员)
     */
    @InnerAuth
    @PostMapping("/generate")
    public R<Boolean> generateAnnualQuota(@RequestParam("year") Integer year,
                                         @RequestBody SysUser user)
    {
        leaveQuotaService.generateAnnualQuota(year, user);
        return R.ok(true);
    }
}
