package com.lingxi.platform.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.kernel.AsyncJobRequest;
import com.lingxi.kernel.AsyncJobScheduler;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.platform.persistence.AsyncJobEntity;
import com.lingxi.platform.persistence.AsyncJobMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 数据库型幂等任务调度器。 */
@Component
public class DatabaseAsyncJobScheduler implements AsyncJobScheduler {
  private final AsyncJobMapper mapper;
  private final IdGenerator idGenerator;

  public DatabaseAsyncJobScheduler(AsyncJobMapper mapper, IdGenerator idGenerator) {
    this.mapper = mapper;
    this.idGenerator = idGenerator;
  }

  @Override
  @Transactional
  public long schedule(String jobType, String businessKey, String payloadJson, int maxAttempts) {
    return scheduleAt(jobType, businessKey, payloadJson, maxAttempts, Instant.now());
  }

  @Override
  @Transactional
  public long scheduleAt(
      String jobType, String businessKey, String payloadJson, int maxAttempts, Instant notBefore) {
    if (jobType == null
        || jobType.isBlank()
        || businessKey == null
        || businessKey.isBlank()
        || payloadJson == null
        || maxAttempts < 1
        || notBefore == null) {
      throw new BusinessException("PLATFORM_INVALID_ASYNC_JOB", "异步任务参数不合法");
    }
    AsyncJobEntity existing = find(jobType, businessKey);
    if (existing != null) {
      if (!existing.getPayloadJson().equals(payloadJson)) {
        throw new BusinessException("PLATFORM_JOB_IDEMPOTENCY_CONFLICT", "同一业务键不能提交不同任务");
      }
      return existing.getId();
    }

    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    AsyncJobEntity entity = new AsyncJobEntity();
    entity.setId(idGenerator.nextId());
    entity.setJobType(jobType);
    entity.setBusinessKey(businessKey);
    entity.setStatus(AsyncJobStatus.PENDING.name());
    entity.setPayloadJson(payloadJson);
    entity.setAttemptCount(0);
    entity.setMaxAttempts(maxAttempts);
    entity.setNextRetryAt(LocalDateTime.ofInstant(notBefore, ZoneOffset.UTC));
    entity.setProgress(0);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setVersion(0L);
    try {
      mapper.insert(entity);
      return entity.getId();
    } catch (DuplicateKeyException exception) {
      AsyncJobEntity raced = find(jobType, businessKey);
      if (raced != null && raced.getPayloadJson().equals(payloadJson)) {
        return raced.getId();
      }
      throw new BusinessException("PLATFORM_JOB_IDEMPOTENCY_CONFLICT", "同一业务键不能提交不同任务");
    }
  }

  @Override
  @Transactional
  public void scheduleBatch(List<AsyncJobRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return;
    }
    Map<String, AsyncJobRequest> uniqueRequests = new LinkedHashMap<>();
    for (AsyncJobRequest request : requests) {
      validate(request);
      String key = request.jobType() + "\u0000" + request.businessKey();
      AsyncJobRequest previous = uniqueRequests.putIfAbsent(key, request);
      if (previous != null && !previous.payloadJson().equals(request.payloadJson())) {
        throw idempotencyConflict();
      }
    }

    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    List<AsyncJobEntity> candidates = new ArrayList<>(uniqueRequests.size());
    for (AsyncJobRequest request : uniqueRequests.values()) {
      candidates.add(entity(request, now));
    }
    verifyExistingPayloads(candidates, mapper.selectByBusinessKeys(candidates));
    mapper.insertBatch(candidates);
    verifyExistingPayloads(candidates, mapper.selectByBusinessKeys(candidates));
  }

  private void validate(AsyncJobRequest request) {
    if (request == null
        || request.jobType() == null
        || request.jobType().isBlank()
        || request.businessKey() == null
        || request.businessKey().isBlank()
        || request.payloadJson() == null
        || request.maxAttempts() < 1
        || request.notBefore() == null) {
      throw new BusinessException("PLATFORM_INVALID_ASYNC_JOB", "异步任务参数不合法");
    }
  }

  private AsyncJobEntity entity(AsyncJobRequest request, LocalDateTime now) {
    AsyncJobEntity entity = new AsyncJobEntity();
    entity.setId(idGenerator.nextId());
    entity.setJobType(request.jobType());
    entity.setBusinessKey(request.businessKey());
    entity.setStatus(AsyncJobStatus.PENDING.name());
    entity.setPayloadJson(request.payloadJson());
    entity.setAttemptCount(0);
    entity.setMaxAttempts(request.maxAttempts());
    entity.setNextRetryAt(LocalDateTime.ofInstant(request.notBefore(), ZoneOffset.UTC));
    entity.setProgress(0);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setVersion(0L);
    return entity;
  }

  private void verifyExistingPayloads(
      List<AsyncJobEntity> candidates, List<AsyncJobEntity> persisted) {
    Map<String, String> expected = new LinkedHashMap<>();
    for (AsyncJobEntity candidate : candidates) {
      expected.put(
          candidate.getJobType() + "\u0000" + candidate.getBusinessKey(),
          candidate.getPayloadJson());
    }
    for (AsyncJobEntity existing : persisted) {
      String payload = expected.get(existing.getJobType() + "\u0000" + existing.getBusinessKey());
      if (payload != null && !payload.equals(existing.getPayloadJson())) {
        throw idempotencyConflict();
      }
    }
  }

  private BusinessException idempotencyConflict() {
    return new BusinessException("PLATFORM_JOB_IDEMPOTENCY_CONFLICT", "同一业务键不能提交不同任务");
  }

  private AsyncJobEntity find(String jobType, String businessKey) {
    return mapper.selectOne(
        Wrappers.<AsyncJobEntity>lambdaQuery()
            .eq(AsyncJobEntity::getJobType, jobType)
            .eq(AsyncJobEntity::getBusinessKey, businessKey)
            .last("LIMIT 1"));
  }
}
