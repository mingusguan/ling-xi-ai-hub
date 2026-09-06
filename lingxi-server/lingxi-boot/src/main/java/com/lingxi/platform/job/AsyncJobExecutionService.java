package com.lingxi.platform.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.kernel.AsyncJobHandler;
import com.lingxi.kernel.AsyncJobMessage;
import com.lingxi.platform.persistence.AsyncJobEntity;
import com.lingxi.platform.persistence.AsyncJobMapper;
import java.time.ZoneOffset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 任务执行器。业务处理在数据库事务外运行，避免第三方调用占用长事务。 */
@Slf4j
@Service
public class AsyncJobExecutionService {
  private static final int MAX_ERROR_LENGTH = 1000;

  private final AsyncJobMapper mapper;
  private final List<AsyncJobHandler> handlers;

  public AsyncJobExecutionService(AsyncJobMapper mapper, List<AsyncJobHandler> handlers) {
    this.mapper = mapper;
    this.handlers = handlers;
  }

  public void execute(long jobId, String leaseOwner) {
    AsyncJobEntity job = mapper.selectById(jobId);
    if (job == null
        || !AsyncJobStatus.RUNNING.name().equals(job.getStatus())
        || !leaseOwner.equals(job.getLeaseOwner())) {
      return;
    }
    AsyncJobHandler handler =
        handlers.stream()
            .filter(candidate -> candidate.supports(job.getJobType()))
            .findFirst()
            .orElse(null);
    if (handler == null) {
      fail(job, "没有匹配的任务处理器");
      return;
    }
    try {
      AsyncJobMessage message =
          new AsyncJobMessage(
              job.getId(),
              job.getJobType(),
              job.getBusinessKey(),
              job.getPayloadJson(),
              job.getAttemptCount() + 1,
              job.getCreatedAt().toInstant(ZoneOffset.UTC));
      succeed(job, handler.handle(message));
    } catch (Exception exception) {
      fail(job, exception.getMessage());
      log.warn(
          "异步任务执行失败, jobId={}, type={}, attempt={}",
          job.getId(),
          job.getJobType(),
          job.getAttemptCount() + 1,
          exception);
    }
  }

  private void succeed(AsyncJobEntity job, String resultJson) {
    var now = java.time.LocalDateTime.now(ZoneOffset.UTC);
    mapper.update(
        null,
        Wrappers.<AsyncJobEntity>lambdaUpdate()
            .eq(AsyncJobEntity::getId, job.getId())
            .eq(AsyncJobEntity::getStatus, AsyncJobStatus.RUNNING.name())
            .eq(AsyncJobEntity::getLeaseOwner, job.getLeaseOwner())
            .set(AsyncJobEntity::getStatus, AsyncJobStatus.SUCCEEDED.name())
            .set(AsyncJobEntity::getResultJson, resultJson)
            .set(AsyncJobEntity::getProgress, 100)
            .set(AsyncJobEntity::getLeaseOwner, null)
            .set(AsyncJobEntity::getLeaseUntil, null)
            .set(AsyncJobEntity::getCompletedAt, now)
            .set(AsyncJobEntity::getUpdatedAt, now));
  }

  private void fail(AsyncJobEntity job, String error) {
    int attempts = job.getAttemptCount() + 1;
    boolean exhausted = attempts >= job.getMaxAttempts();
    long delaySeconds = Math.min(900, 1L << Math.min(attempts, 9));
    var now = java.time.LocalDateTime.now(ZoneOffset.UTC);
    mapper.update(
        null,
        Wrappers.<AsyncJobEntity>lambdaUpdate()
            .eq(AsyncJobEntity::getId, job.getId())
            .eq(AsyncJobEntity::getStatus, AsyncJobStatus.RUNNING.name())
            .eq(AsyncJobEntity::getLeaseOwner, job.getLeaseOwner())
            .set(
                AsyncJobEntity::getStatus,
                exhausted ? AsyncJobStatus.FAILED.name() : AsyncJobStatus.RETRY_WAIT.name())
            .set(AsyncJobEntity::getAttemptCount, attempts)
            .set(AsyncJobEntity::getNextRetryAt, now.plusSeconds(delaySeconds))
            .set(AsyncJobEntity::getLeaseOwner, null)
            .set(AsyncJobEntity::getLeaseUntil, null)
            .set(AsyncJobEntity::getLastError, abbreviate(error))
            .set(AsyncJobEntity::getUpdatedAt, now));
  }

  private String abbreviate(String error) {
    String message = error == null ? "unknown error" : error;
    return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
  }
}
