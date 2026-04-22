package com.lingxi.oa.task;

import com.lingxi.oa.service.IOaLeaveQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * P1: 假期额度定时任务
 * - 每年1月1日凌晨2点自动生成新年度的假期额度
 */
@Component
public class OaLeaveQuotaTask {

    private static final Logger log = LoggerFactory.getLogger(OaLeaveQuotaTask.class);

    @Autowired
    private IOaLeaveQuotaService leaveQuotaService;

    /**
     * 每年1月1日凌晨2点执行
     * cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 1 1 ?")
    public void generateAnnualQuota() {
        log.info("====== 开始执行年度假期额度生成任务 ======");
        
        try {
            int nextYear = LocalDate.now().getYear();
            // 定时任务不传userId,为所有用户生成
            leaveQuotaService.generateAnnualQuota(nextYear, null);
            log.info("====== {}年度假期额度生成任务执行完成 ======", nextYear);
        } catch (Exception e) {
            log.error("年度假期额度生成任务执行失败", e);
        }
    }

    /**
     * 测试用: 手动触发(可删除)
     */
    // @Scheduled(fixedDelay = Long.MAX_VALUE) // 默认不启用
    public void testGenerateQuota() {
        log.info("测试生成假期额度...");
        int currentYear = LocalDate.now().getYear();
        leaveQuotaService.generateAnnualQuota(currentYear,null);
    }
}
