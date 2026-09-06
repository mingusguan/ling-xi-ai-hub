package com.lingxi.platform.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingxi.platform.persistence.AsyncJobEntity;
import com.lingxi.platform.persistence.AsyncJobMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 多实例安全的异步任务领取器。 */
@Component
public class AsyncJobDispatcher {
  private final AsyncJobMapper mapper;
  private final AsyncJobExecutionService executionService;
  private final String leaseOwner = UUID.randomUUID().toString();
  private final int batchSize;
  private final int leaseSeconds;

  public AsyncJobDispatcher(
      AsyncJobMapper mapper,
      AsyncJobExecutionService executionService,
      @Value("${lingxi.platform.jobs.batch-size:20}") int batchSize,
      @Value("${lingxi.platform.jobs.lease-seconds:120}") int leaseSeconds) {
    this.mapper = mapper;
    this.executionService = executionService;
    this.batchSize = batchSize;
    this.leaseSeconds = leaseSeconds;
  }

  @Scheduled(fixedDelayString = "${lingxi.platform.jobs.poll-delay-ms:2000}")
  public void dispatch() {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    List<AsyncJobEntity> candidates =
        mapper
            .selectPage(
                Page.of(1, batchSize),
                Wrappers.<AsyncJobEntity>lambdaQuery()
                    .and(
                        wrapper ->
                            wrapper
                                .and(
                                    ready ->
                                        ready
                                            .in(
                                                AsyncJobEntity::getStatus,
                                                AsyncJobStatus.PENDING.name(),
                                                AsyncJobStatus.RETRY_WAIT.name())
                                            .le(AsyncJobEntity::getNextRetryAt, now))
                                .or(
                                    expired ->
                                        expired
                                            .eq(
                                                AsyncJobEntity::getStatus,
                                                AsyncJobStatus.RUNNING.name())
                                            .le(AsyncJobEntity::getLeaseUntil, now)))
                    .orderByAsc(AsyncJobEntity::getId))
            .getRecords();
    for (AsyncJobEntity candidate : candidates) {
      if (claim(candidate.getId(), now)) {
        executionService.execute(candidate.getId(), leaseOwner);
      }
    }
  }

  private boolean claim(long jobId, LocalDateTime now) {
    return mapper.update(
            null,
            Wrappers.<AsyncJobEntity>lambdaUpdate()
                .eq(AsyncJobEntity::getId, jobId)
                .and(
                    wrapper ->
                        wrapper
                            .and(
                                ready ->
                                    ready
                                        .in(
                                            AsyncJobEntity::getStatus,
                                            AsyncJobStatus.PENDING.name(),
                                            AsyncJobStatus.RETRY_WAIT.name())
                                        .le(AsyncJobEntity::getNextRetryAt, now))
                            .or(
                                expired ->
                                    expired
                                        .eq(
                                            AsyncJobEntity::getStatus,
                                            AsyncJobStatus.RUNNING.name())
                                        .le(AsyncJobEntity::getLeaseUntil, now)))
                .set(AsyncJobEntity::getStatus, AsyncJobStatus.RUNNING.name())
                .set(AsyncJobEntity::getLeaseOwner, leaseOwner)
                .set(AsyncJobEntity::getLeaseUntil, now.plusSeconds(leaseSeconds))
                .set(AsyncJobEntity::getUpdatedAt, now))
        == 1;
  }
}
