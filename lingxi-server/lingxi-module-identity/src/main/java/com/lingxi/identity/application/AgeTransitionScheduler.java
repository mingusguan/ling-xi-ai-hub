package com.lingxi.identity.application;

import com.lingxi.identity.domain.IdentityRepository;
import java.time.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 每日批量推进达到 18 周岁的账号，旧令牌因授权版本变化立即失效。 */
@Component
public class AgeTransitionScheduler {
  private final IdentityRepository repository;

  public AgeTransitionScheduler(IdentityRepository repository) {
    this.repository = repository;
  }

  @Scheduled(cron = "${lingxi.identity.age-transition-cron:0 15 2 * * *}", zone = "UTC")
  @Transactional
  public int transitionDueUsers() {
    Instant now = Instant.now();
    return repository.markDueAdultTransitions(
        LocalDate.ofInstant(now, ZoneOffset.UTC), LocalDateTime.ofInstant(now, ZoneOffset.UTC));
  }
}
