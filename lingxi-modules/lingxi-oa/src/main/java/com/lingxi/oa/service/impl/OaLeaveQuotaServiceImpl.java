package com.lingxi.oa.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.oa.util.HolidayUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.system.api.RemoteUserService;
import com.lingxi.system.api.RemoteDictService;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.domain.SysDictData;
import com.lingxi.common.core.domain.R;
import com.lingxi.oa.domain.OaLeave;
import com.lingxi.oa.domain.OaLeaveQuota;
import com.lingxi.oa.domain.OaLeaveRule;
import com.lingxi.oa.domain.OaLeaveAdjustment;
import com.lingxi.oa.mapper.OaLeaveQuotaMapper;
import com.lingxi.oa.mapper.OaLeaveRuleMapper;
import com.lingxi.oa.mapper.OaLeaveAdjustmentMapper;
import com.lingxi.oa.mapper.OaLeaveMapper;
import com.lingxi.oa.service.IOaLeaveQuotaService;

/**
 * P0+P1+P2: 假期额度Service业务层处理
 */
@Service
public class OaLeaveQuotaServiceImpl implements IOaLeaveQuotaService 
{
    private static final Logger log = LoggerFactory.getLogger(OaLeaveQuotaServiceImpl.class);

    @Autowired
    private OaLeaveQuotaMapper leaveQuotaMapper;

    @Autowired
    private OaLeaveRuleMapper leaveRuleMapper;

    @Autowired
    private OaLeaveAdjustmentMapper leaveAdjustmentMapper;

    @Autowired
    private OaLeaveMapper leaveMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private RemoteDictService remoteDictService;

    @Resource
    private HolidayUtil holidayUtil; // 休息日工具类

    /**
     * P0: 查询用户假期余额(动态计算)
     */
    @Override
    public Map<String, Object> queryLeaveBalance(Long userId, Integer year)
    {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        int queryYear = year != null ? year : LocalDate.now().getYear();
        List<OaLeaveQuota> quotas = getUserQuotas(userId, queryYear);
        List<OaLeaveRule> rules = getAllActiveRules();

        // 构建返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> balanceList = new ArrayList<>();

        for (OaLeaveRule rule : rules) {
            Map<String, Object> item = new HashMap<>();
            String leaveType = rule.getLeaveType();

            // 查找该用户的额度记录
            OaLeaveQuota quota = quotas.stream()
                .filter(q -> q.getLeaveType().equals(leaveType))
                .findFirst()
                .orElse(null);

            item.put("leaveType", leaveType);
            item.put("leaveName", rule.getLeaveName());

            if (quota != null) {
                item.put("totalDays", quota.getTotalDays());
                item.put("usedDays", quota.getUsedDays());
                item.put("remainingDays", quota.getRemainingDays());
                item.put("carryOverDays", quota.getCarryOverDays());
                item.put("status", quota.getStatus());
                item.put("initialized", true);
            } else {
                // 如果没有额度记录,返回未初始化标记
                item.put("totalDays", null);
                item.put("usedDays", null);
                item.put("remainingDays", null);
                item.put("carryOverDays", null);
                item.put("status", "0");
                item.put("initialized", false);
            }

            balanceList.add(item);
        }

        result.put("year", queryYear);
        result.put("balances", balanceList);
        return result;
    }

    /**
     * P0: 查询用户待审批请假统计
     */
    @Override
    public Map<String, Object> queryPendingLeaveStats(Long userId)
    {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        // 查询当前用户所有待审批的请假申请
        List<OaLeave> pendingLeaves = leaveMapper.selectList(
            new LambdaQueryWrapper<OaLeave>()
                .eq(OaLeave::getApplicantUserId, userId)
                .eq(OaLeave::getApprovalStatus, "pending")
        );

        // 按假期类型统计天数
        Map<String, BigDecimal> statsByType = new HashMap<>();
        int totalCount = pendingLeaves.size();
        BigDecimal totalDays = BigDecimal.ZERO;

        for (OaLeave leave : pendingLeaves) {
            String leaveType = leave.getLeaveType();
            BigDecimal days = BigDecimal.ZERO;
            
            // 计算请假天数（小时转天，按8小时工作制）
            if (leave.getLeaveHours() != null && leave.getLeaveHours().compareTo(BigDecimal.ZERO) > 0) {
                days = leave.getLeaveHours().divide(new BigDecimal("8"), 2, RoundingMode.HALF_UP);
            } else if (leave.getStartTime() != null && leave.getEndTime() != null) {
                // 使用休息日工具计算工作日小时数
                long workHours = holidayUtil.calculateWorkHours(leave.getStartTime(), leave.getEndTime());
                days = new BigDecimal(workHours).divide(new BigDecimal("8"), 2, RoundingMode.HALF_UP);
            }
            
            statsByType.merge(leaveType, days, BigDecimal::add);
            totalDays = totalDays.add(days);
        }

        // 构建返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", totalCount);
        result.put("totalDays", totalDays);
        
        // 按类型详细统计
        List<Map<String, Object>> details = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : statsByType.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("leaveType", entry.getKey());
            item.put("leaveName", getLeaveTypeName(entry.getKey()));
            item.put("days", entry.getValue());
            item.put("count", pendingLeaves.stream()
                .filter(l -> l.getLeaveType().equals(entry.getKey()))
                .count());
            details.add(item);
        }
        result.put("details", details);

        return result;
    }

    /**
     * P0: 查询用户某年假期额度
     */
    @Override
    public OaLeaveQuota selectOaLeaveQuotaByUserAndYear(OaLeaveQuota oaLeaveQuota)
    {
        return leaveQuotaMapper.selectOaLeaveQuotaByUserAndYear(oaLeaveQuota);
    }

    /**
     * P0: 查询假期额度列表
     */
    @Override
    public List<OaLeaveQuota> selectOaLeaveQuotaList(OaLeaveQuota oaLeaveQuota)
    {
        return leaveQuotaMapper.selectOaLeaveQuotaList(oaLeaveQuota);
    }

    /**
     * P0: 新增假期额度
     */
    @Override
    public int insertOaLeaveQuota(OaLeaveQuota oaLeaveQuota)
    {
        return leaveQuotaMapper.insertOaLeaveQuota(oaLeaveQuota);
    }

    /**
     * P0: 修改假期额度
     */
    @Override
    public int updateOaLeaveQuota(OaLeaveQuota oaLeaveQuota)
    {
        return leaveQuotaMapper.updateOaLeaveQuota(oaLeaveQuota);
    }

    /**
     * P0: 扣减假期额度(请假审批通过时调用)
     */
    @Override
    @Transactional
    public int deductLeaveQuota(Long userId, String leaveType, Integer year, BigDecimal days)
    {
        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("扣减天数无效: {}", days);
            return 0;
        }

        // 查询或创建额度记录
        OaLeaveQuota quota = getOrCreateQuota(userId, leaveType, year);

        // 检查剩余额度是否充足
        if (quota.getRemainingDays().compareTo(days) < 0) {
            throw new RuntimeException(String.format(
                "假期余额不足! %s剩余%.1f天,申请%.1f天",
                getLeaveTypeName(leaveType),
                quota.getRemainingDays(),
                days.doubleValue()
            ));
        }

        // 扣减额度
        BigDecimal newUsedDays = quota.getUsedDays().add(days);
        BigDecimal newRemainingDays = quota.getRemainingDays().subtract(days);

        quota.setUsedDays(newUsedDays);
        quota.setRemainingDays(newRemainingDays);

        return leaveQuotaMapper.updateOaLeaveQuota(quota);
    }

    /**
     * P0: 恢复假期额度(请假撤销/驳回时调用)
     */
    @Override
    @Transactional
    public int restoreLeaveQuota(Long userId, String leaveType, Integer year, BigDecimal days)
    {
        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("恢复天数无效: {}", days);
            return 0;
        }

        OaLeaveQuota quota = leaveQuotaMapper.selectOaLeaveQuotaByUserAndYear(
            buildQueryKey(userId, leaveType, year)
        );

        if (quota == null) {
            log.warn("未找到额度记录: userId={}, leaveType={}, year={}", userId, leaveType, year);
            return 0;
        }

        // 恢复额度
        BigDecimal newUsedDays = quota.getUsedDays().subtract(days);
        BigDecimal newRemainingDays = quota.getRemainingDays().add(days);

        // 确保不会出现负数
        if (newUsedDays.compareTo(BigDecimal.ZERO) < 0) {
            newUsedDays = BigDecimal.ZERO;
        }

        quota.setUsedDays(newUsedDays);
        quota.setRemainingDays(newRemainingDays);

        return leaveQuotaMapper.updateOaLeaveQuota(quota);
    }

    /**
     * P1: 年初自动生成假期额度
     */
    @Override
    @Transactional
    /**
     * P0: 生成年度假期额度
     * @param year 年度
     * @param userId 用户ID(可选,如果传入则只生成该用户的假期)
     */
    public void generateAnnualQuota(Integer year, SysUser user)
    {
        log.info("开始生成{}年度假期额度...", year);

        List<SysUser> targetUser = new ArrayList<>();
        
        // 如果传入了user,只处理该用户
        if (user != null) {
            targetUser = Arrays.asList(user);
            log.info("指定生成用户 {} 的假期额度", user.getUserId());
        } else {
            // 从用户服务获取所有在职员工列表
            try {
                R<List<SysUser>> result = remoteUserService.getActiveUsers(SecurityConstants.INNER);
                if (result != null && result.getData() != null) {
                    targetUser = result.getData();
                    log.info("从远程服务获取到 {} 个在职员工", targetUser.size());
                } else {
                    log.warn("远程服务返回为空...");
                }
            } catch (Exception e) {
                log.error("调用远程服务失败,使用示例数据", e);
            }
        }

        List<OaLeaveRule> rules = getAllActiveRules();
        List<OaLeaveQuota> newQuotas = new ArrayList<>();

        for (SysUser u : targetUser) {
            for (OaLeaveRule rule : rules) {
                // 检查是否已存在
                OaLeaveQuota existing = leaveQuotaMapper.selectOaLeaveQuotaByUserAndYear(
                    buildQueryKey(u.getUserId(), rule.getLeaveType(), year)
                );

                // 计算总额度(根据最新用户信息)
                BigDecimal totalDays = calculateTotalDays(rule, u);

                // 计算结转天数(P1功能)
                BigDecimal carryOverDays = calculateCarryOverDays(u.getUserId(), rule.getLeaveType(), year - 1, rule);

                if (existing != null) {
                    // 已存在,更新额度数据
                    existing.setTotalDays(totalDays.add(carryOverDays));
                    // 重新计算剩余额度 = 总额度 - 已使用
                    existing.setRemainingDays(totalDays.add(carryOverDays).subtract(existing.getUsedDays()));
                    existing.setCarryOverDays(carryOverDays);
                    existing.setUpdateBy(SecurityUtils.getUserId() +  "");
                    existing.setStatus("0");
                    leaveQuotaMapper.updateOaLeaveQuota(existing);
                    log.info("更新用户 {} 的 {} 假期额度: 总额度={}, 已使用={}, 剩余={}", 
                        u.getUserId(), rule.getLeaveType(), existing.getTotalDays(), existing.getUsedDays(), existing.getRemainingDays());
                } else {
                    // 不存在,创建新记录
                    OaLeaveQuota quota = new OaLeaveQuota();
                    quota.setUserId(u.getUserId());
                    quota.setLeaveType(rule.getLeaveType());
                    quota.setYear(year);
                    quota.setTotalDays(totalDays.add(carryOverDays));
                    quota.setUsedDays(BigDecimal.ZERO);
                    quota.setRemainingDays(totalDays.add(carryOverDays));
                    quota.setCarryOverDays(carryOverDays);
                    quota.setStatus("0");
                    quota.setCreateBy(SecurityUtils.getUserId() +  "");

                    newQuotas.add(quota);
                }
            }
        }

        if (!newQuotas.isEmpty()) {
            leaveQuotaMapper.batchUpsertOaLeaveQuota(newQuotas);
            log.info("{}年度假期额度生成完成,共{}条记录", year, newQuotas.size());
        } else {
            log.info("{}年度假期额度无需生成(可能已全部存在)", year);
        }
    }

    /**
     * P2: 手动调整假期额度
     */
    @Override
    @Transactional
    public int adjustLeaveQuota(Long userId, String leaveType, Integer year,
                                BigDecimal adjustDays, String reason, Long operatorId, String operatorName)
    {
        if (adjustDays == null || adjustDays.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("调整天数不能为0");
        }

        OaLeaveQuota quota = getOrCreateQuota(userId, leaveType, year);

        // 记录调整前的额度
        BigDecimal beforeDays = quota.getTotalDays();

        // 调整总额度和剩余额度
        BigDecimal newTotalDays = quota.getTotalDays().add(adjustDays);
        BigDecimal newRemainingDays = quota.getRemainingDays().add(adjustDays);

        // 确保不会出现负数
        if (newTotalDays.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("调整后总额度不能为负数");
        }
        if (newRemainingDays.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("调整后剩余额度不能为负数");
        }

        quota.setTotalDays(newTotalDays);
        quota.setRemainingDays(newRemainingDays);
        leaveQuotaMapper.updateOaLeaveQuota(quota);

        // 记录调整日志(P2功能)
        OaLeaveAdjustment adjustment = new OaLeaveAdjustment();
        adjustment.setQuotaId(quota.getQuotaId());
        adjustment.setUserId(userId);
        adjustment.setLeaveType(leaveType);
        adjustment.setYear(year);
        adjustment.setAdjustDays(adjustDays);
        adjustment.setBeforeDays(beforeDays);
        adjustment.setAfterDays(newTotalDays);
        adjustment.setReason(reason);
        adjustment.setOperatorId(operatorId);
        adjustment.setOperatorName(operatorName);
        adjustment.setCreateTime(new Date());

        leaveAdjustmentMapper.insertOaLeaveAdjustment(adjustment);

        log.info("假期额度调整成功: userId={}, leaveType={}, adjustDays={}, reason={}",
                 userId, leaveType, adjustDays, reason);

        return 1;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取用户某年的所有假期额度
     */
    private List<OaLeaveQuota> getUserQuotas(Long userId, Integer year)
    {
        OaLeaveQuota query = new OaLeaveQuota();
        query.setUserId(userId);
        query.setYear(year);
        return leaveQuotaMapper.selectOaLeaveQuotaList(query);
    }

    /**
     * 获取所有启用的假期规则
     */
    private List<OaLeaveRule> getAllActiveRules()
    {
        OaLeaveRule query = new OaLeaveRule();
        query.setStatus("0");
        return leaveRuleMapper.selectOaLeaveRuleList(query);
    }

    /**
     * 获取或创建额度记录
     */
    private OaLeaveQuota getOrCreateQuota(Long userId, String leaveType, Integer year)
    {
        OaLeaveQuota quota = leaveQuotaMapper.selectOaLeaveQuotaByUserAndYear(
            buildQueryKey(userId, leaveType, year)
        );

        if (quota == null) {
            // 不存在则创建
            OaLeaveRule rule = leaveRuleMapper.selectOaLeaveRuleByType(leaveType);
            if (rule == null) {
                throw new RuntimeException("未找到假期类型规则: " + leaveType);
            }
            // 获取用户信息
            R<SysUser> userResult = remoteUserService.getUserById(userId, SecurityConstants.INNER);
            if (userResult == null || userResult.getData() == null) {
                log.warn("无法获取用户 {} 信息", userId);
                throw new RuntimeException("无法获取用户信息: " + userId);
            }

            SysUser user = userResult.getData();
            BigDecimal totalDays = calculateTotalDays(rule, user);
            quota = new OaLeaveQuota();
            quota.setUserId(userId);
            quota.setLeaveType(leaveType);
            quota.setYear(year);
            quota.setTotalDays(totalDays);
            quota.setUsedDays(BigDecimal.ZERO);
            quota.setRemainingDays(totalDays);
            quota.setCarryOverDays(BigDecimal.ZERO);
            quota.setStatus("0");
            quota.setCreateBy("system");

            leaveQuotaMapper.insertOaLeaveQuota(quota);
        }

        return quota;
    }

    /**
     * 构建查询条件
     */
    private OaLeaveQuota buildQueryKey(Long userId, String leaveType, Integer year)
    {
        OaLeaveQuota query = new OaLeaveQuota();
        query.setUserId(userId);
        query.setLeaveType(leaveType);
        query.setYear(year);
        return query;
    }

    /**
     * 计算总额度(根据规则配置)
     */
    private BigDecimal calculateTotalDays(OaLeaveRule rule, SysUser user)
    {
        String calculationRule = rule.getCalculationRule();
        
        // 如果没有配置计算规则或为fixed,直接返回默认天数
        if (StringUtils.isEmpty(calculationRule) || "fixed".equals(calculationRule)) {
            return rule.getDefaultDays();
        }

        try {
            Date entryDate = user.getEntryDate();
            
            // 按工龄计算
            if ("by_seniority".equals(calculationRule)) {
                return calculateBySeniority(rule, entryDate, user.getUserId());
            }
            // 按职级计算
            else if ("by_level".equals(calculationRule)) {
                return calculateByLevel(rule, user.getJobLevel(), user.getUserId());
            }
            
            // 未知规则,使用默认天数
            log.warn("固定天数计算规则: {}, 使用默认天数", calculationRule);
            return rule.getDefaultDays();

        } catch (Exception e) {
            log.error("计算用户 {} 假期额度失败,使用默认天数", user.getUserId(), e);
            return rule.getDefaultDays();
        }
    }

    /**
     * 按工龄计算假期天数
     */
    private BigDecimal calculateBySeniority(OaLeaveRule rule, Date entryDate, Long userId)
    {
        // 如果没有入职日期,使用默认天数
        if (entryDate == null) {
            log.warn("用户 {} 没有入职日期,使用默认天数", userId);
            return rule.getDefaultDays();
        }

        // 计算工龄(年)
        LocalDate entryLocalDate = entryDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalDate currentDate = LocalDate.now();
        long workYears = java.time.temporal.ChronoUnit.YEARS.between(entryLocalDate, currentDate);

        // 检查最小工龄要求
        if (rule.getMinWorkYears() != null && workYears < rule.getMinWorkYears().longValue()) {
            log.info("用户 {} 工龄 {} 年,不满足最小工龄要求 {} 年,返回0天", 
                userId, workYears, rule.getMinWorkYears());
            return BigDecimal.ZERO;
        }

        BigDecimal totalDays;
        
        // 工作不满1年,按比例计算
        if (workYears < 1) {
            long daysSinceEntry = java.time.temporal.ChronoUnit.DAYS.between(entryLocalDate, currentDate);
            double ratio = daysSinceEntry / 365.0;
            totalDays = rule.getDefaultDays().multiply(BigDecimal.valueOf(ratio))
                .setScale(1, RoundingMode.HALF_UP);
            log.info("用户 {} 入职不满1年({}天),按比例计算: {}天", userId, daysSinceEntry, totalDays);
        } else {
            // 满1年后,根据工龄分段计算
            // 这里使用defaultDays作为基础,可以根据实际业务调整
            if (workYears >= 10) {
                totalDays = BigDecimal.valueOf(10);
            } else if (workYears >= 5) {
                totalDays = BigDecimal.valueOf(7);
            } else if (workYears >= 1) {
                totalDays = BigDecimal.valueOf(5);
            } else {
                totalDays = rule.getDefaultDays();
            }
            log.info("用户 {} 工龄 {} 年,计算假期天数: {}", userId, workYears, totalDays);
        }

        return totalDays;
    }

    /**
     * 按职级计算假期天数
     */
    private BigDecimal calculateByLevel(OaLeaveRule rule, String jobLevel, Long userId)
    {
        // 如果没有职级信息,使用默认天数
        if (StringUtils.isEmpty(jobLevel)) {
            log.warn("用户 {} 没有职级信息,使用默认天数", userId);
            return rule.getDefaultDays();
        }

        // 验证职级是否在字典表中存在
        if (!isValidJobLevel(jobLevel)) {
            log.warn("用户 {} 的职级 {} 不在字典配置中,使用默认天数", userId, jobLevel);
            return rule.getDefaultDays();
        }

        // 获取职级系数(代码中配置)
        double coefficient = getJobLevelCoefficient(jobLevel);
        
        BigDecimal levelDays = rule.getDefaultDays().multiply(BigDecimal.valueOf(coefficient))
            .setScale(1, RoundingMode.HALF_UP);
        
        log.info("用户 {} 职级为 {}, 系数为 {}, 计算假期天数: {}", userId, jobLevel, coefficient, levelDays);
        return levelDays;
    }

    /**
     * 验证职级是否在字典表中存在
     */
    private boolean isValidJobLevel(String jobLevel)
    {
        try {
            // 调用远程服务获取职级字典数据
            R<List<SysDictData>> result = remoteDictService.getDictDataByType("sys_job_level", SecurityConstants.INNER);
            
            if (result == null || result.getData() == null) {
                log.warn("获取职级字典失败,使用默认验证逻辑");
                // 降级: 使用硬编码列表
                return getDefaultValidLevels().contains(jobLevel.toLowerCase());
            }
            
            // 检查职级是否在字典中
            List<SysDictData> dictDataList = result.getData();
            return dictDataList.stream()
                .anyMatch(dict -> jobLevel.equalsIgnoreCase(dict.getDictValue()));
                
        } catch (Exception e) {
            log.error("验证职级失败,使用默认验证逻辑", e);
            // 降级: 使用硬编码列表
            return getDefaultValidLevels().contains(jobLevel.toLowerCase());
        }
    }

    /**
     * 获取默认的有效职级列表(降级方案)
     */
    private List<String> getDefaultValidLevels()
    {
        return Arrays.asList(
            "junior",      // 初级
            "intermediate",      // 中级
            "senior",         // 高级
            "expert",      // 专家
            "senior_expert"    // 资深专家
        );
    }

    /**
     * 获取职级系数
     */
    private double getJobLevelCoefficient(String jobLevel)
    {
        // 职级系数配置(按字典排序顺序)
        // 第1个: 0.5, 第2个: 1.0, 第3个: 1.2, 第4个: 1.5, 第5个: 2.0
        double[] coefficients = {0.5, 1.0, 1.2, 1.5, 2.0};
        
        try {
            // 调用远程服务获取职级字典数据(按sort_order排序)
            R<List<SysDictData>> result = remoteDictService.getDictDataByType("sys_job_level", SecurityConstants.INNER);
            
            if (result != null && result.getData() != null) {
                List<SysDictData> dictDataList = result.getData();
                
                // 查找当前职级在字典中的位置(索引)
                for (int i = 0; i < dictDataList.size(); i++) {
                    if (jobLevel.equalsIgnoreCase(dictDataList.get(i).getDictValue())) {
                        // 使用对应位置的系数,如果超出数组范围则使用最后一个
                        int index = Math.min(i, coefficients.length - 1);
                        double coefficient = coefficients[index];
                        log.info("用户职级 {} 在字典中排第{}位,使用系数 {}", jobLevel, i + 1, coefficient);
                        return coefficient;
                    }
                }
            }
            
            // 未找到或调用失败,使用默认系数
            log.warn("未找到职级 {} 的字典配置,使用默认系数1.0", jobLevel);
            return 1.0;
            
        } catch (Exception e) {
            log.error("获取职级系数失败,使用默认系数1.0", e);
            return 1.0;
        }
    }

    /**
     * 计算结转天数(P1功能)
     */
    private BigDecimal calculateCarryOverDays(Long userId, String leaveType, Integer lastYear, OaLeaveRule rule)
    {
        // 如果不可结转,返回0
        if ("0".equals(rule.getCanCarryOver())) {
            return BigDecimal.ZERO;
        }

        // 查询上一年度的剩余额度
        OaLeaveQuota lastYearQuota = leaveQuotaMapper.selectOaLeaveQuotaByUserAndYear(
            buildQueryKey(userId, leaveType, lastYear)
        );

        if (lastYearQuota == null) {
            return BigDecimal.ZERO;
        }

        // 计算可结转天数 = 剩余天数 * 结转比例
        BigDecimal carryOver = lastYearQuota.getRemainingDays()
            .multiply(rule.getCarryOverRatio())
            .setScale(1, RoundingMode.HALF_UP);

        // 不超过最大结转天数
        if (rule.getMaxCarryOverDays() != null && carryOver.compareTo(rule.getMaxCarryOverDays()) > 0) {
            carryOver = rule.getMaxCarryOverDays();
        }

        return carryOver;
    }

    /**
     * 获取假期类型名称
     */
    private String getLeaveTypeName(String leaveType)
    {
        OaLeaveRule rule = leaveRuleMapper.selectOaLeaveRuleByType(leaveType);
        return rule != null ? rule.getLeaveName() : leaveType;
    }
}
