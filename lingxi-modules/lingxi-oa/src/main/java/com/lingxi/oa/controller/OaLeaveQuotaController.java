package com.lingxi.oa.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.lingxi.common.security.annotation.InnerAuth;
import com.lingxi.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.oa.domain.OaLeaveQuota;
import com.lingxi.oa.service.IOaLeaveQuotaService;

/**
 * P0+P1+P2: 假期额度管理Controller
 */
@RestController
@RequestMapping("/oa/leave/quota")
public class OaLeaveQuotaController extends BaseController
{
    @Autowired
    private IOaLeaveQuotaService leaveQuotaService;

    /**
     * P0: 查询当前用户假期余额
     */
    @GetMapping("/balance")
    public R<Map<String, Object>> getBalance(@RequestParam(value = "year", required = false) Integer year)
    {
        Long userId = SecurityUtils.getUserId();
        Map<String, Object> balance = leaveQuotaService.queryLeaveBalance(userId, year);
        return R.ok(balance);
    }

    /**
     * P0: 查询当前用户待审批请假统计
     */
    @GetMapping("/pending-stats")
    public R<Map<String, Object>> getPendingStats()
    {
        Long userId = SecurityUtils.getUserId();
        Map<String, Object> stats = leaveQuotaService.queryPendingLeaveStats(userId);
        return R.ok(stats);
    }

    /**
     * P0: 查询指定用户假期余额(内部调用)
     */
    @InnerAuth
    @GetMapping("/balance/{userId}")
    public R<Map<String, Object>> getBalanceByUserId(@PathVariable("userId") Long userId)
    {
        Map<String, Object> balance = leaveQuotaService.queryLeaveBalance(userId, null);
        return R.ok(balance);
    }

    /**
     * P1: 手动触发生成年度额度(管理员)
     */
    @PostMapping("/generate/{year}")
    public R<String> generateAnnualQuota(@PathVariable("year") Integer year)
    {
        leaveQuotaService.generateAnnualQuota(year, null);
        String msg = year + "年度所有用户的假期额度生成成功";
        return R.ok(msg);
    }

    /**
     * P0: 查询假期额度列表
     */
    @GetMapping("/list")
    public TableDataInfo list(OaLeaveQuota oaLeaveQuota)
    {
        startPage();
        List<OaLeaveQuota> list = leaveQuotaService.selectOaLeaveQuotaList(oaLeaveQuota);
        return getDataTable(list);
    }

    /**
     * P2: 手动调整假期额度
     */
    @PostMapping("/adjust")
    public R<String> adjust(@RequestBody Map<String, Object> params)
    {
        Long userId = Long.valueOf(params.get("userId").toString());
        String leaveType = params.get("leaveType").toString();
        Integer year = Integer.valueOf(params.get("year").toString());
        BigDecimal adjustDays = new BigDecimal(params.get("adjustDays").toString());
        String reason = params.get("reason").toString();
        
        Long operatorId = SecurityUtils.getUserId();
        String operatorName = SecurityUtils.getUsername();
        
    leaveQuotaService.adjustLeaveQuota(userId, leaveType, year, adjustDays, reason, operatorId, operatorName);
        return R.ok("调整成功");
    }

    /**
     * P2: 修改额度状态
     */
    @PutMapping("/changeStatus")
    public R<String> changeStatus(@RequestBody OaLeaveQuota oaLeaveQuota)
    {
        int rows = leaveQuotaService.updateOaLeaveQuota(oaLeaveQuota);
        return rows > 0 ? R.ok() : R.fail("修改状态失败");
    }
}
