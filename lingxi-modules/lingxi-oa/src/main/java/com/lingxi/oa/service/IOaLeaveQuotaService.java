package com.lingxi.oa.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.lingxi.oa.domain.OaLeaveQuota;
import com.lingxi.system.api.domain.SysUser;

/**
 * P0+P1+P2: 假期额度Service接口
 */
public interface IOaLeaveQuotaService 
{
    /**
     * P0: 查询用户假期余额(动态计算)
     */
    public Map<String, Object> queryLeaveBalance(Long userId, Integer year);

    /**
     * P0: 查询用户待审批请假统计
     */
    public Map<String, Object> queryPendingLeaveStats(Long userId);

    /**
     * P0: 查询用户某年假期额度
     */
    public OaLeaveQuota selectOaLeaveQuotaByUserAndYear(OaLeaveQuota oaLeaveQuota);

    /**
     * P0: 查询假期额度列表
     */
    public List<OaLeaveQuota> selectOaLeaveQuotaList(OaLeaveQuota oaLeaveQuota);

    /**
     * P0: 新增假期额度
     */
    public int insertOaLeaveQuota(OaLeaveQuota oaLeaveQuota);

    /**
     * P0: 修改假期额度
     */
    public int updateOaLeaveQuota(OaLeaveQuota oaLeaveQuota);

    /**
     * P0: 扣减假期额度(请假审批通过时调用)
     */
    public int deductLeaveQuota(Long userId, String leaveType, Integer year, BigDecimal days);

    /**
     * P0: 恢复假期额度(请假撤销/驳回时调用)
     */
    public int restoreLeaveQuota(Long userId, String leaveType, Integer year, BigDecimal days);

    /**
     * P1: 年初自动生成假期额度
     * @param year 年度
     * @param user
     */
    public void generateAnnualQuota(Integer year, SysUser user);

    /**
     * P2: 手动调整假期额度
     */
    public int adjustLeaveQuota(Long userId, String leaveType, Integer year, 
                                BigDecimal adjustDays, String reason, Long operatorId, String operatorName);
}
