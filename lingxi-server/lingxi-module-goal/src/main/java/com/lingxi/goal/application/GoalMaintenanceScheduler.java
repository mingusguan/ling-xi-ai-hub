package com.lingxi.goal.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 行动实例和周期复盘的幂等滚动调度器。 */
@Component
public class GoalMaintenanceScheduler {
  private final GoalApplicationService service;

  public GoalMaintenanceScheduler(GoalApplicationService service) {
    this.service = service;
  }

  @Scheduled(cron = "${lingxi.goal.occurrence-cron:0 5 * * * *}", zone = "UTC")
  public void generateOccurrences() {
    service.generateRollingOccurrences();
  }

  @Scheduled(cron = "${lingxi.goal.review-cron:0 15 0 * * MON}", zone = "UTC")
  public void scheduleReviews() {
    service.scheduleCurrentWeekReviews();
  }
}
