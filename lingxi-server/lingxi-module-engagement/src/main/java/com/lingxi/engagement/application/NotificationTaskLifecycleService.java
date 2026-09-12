package com.lingxi.engagement.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.engagement.api.NotificationChannel;
import com.lingxi.engagement.domain.*;
import com.lingxi.identity.api.*;
import com.lingxi.kernel.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 以短事务推进通知任务，外部渠道调用不占用数据库事务。 */
@Service
public class NotificationTaskLifecycleService {
  private final EngagementRepository repository;
  private final IdGenerator ids;
  private final IdentityFacade identity;
  private final ObjectMapper mapper;

  public NotificationTaskLifecycleService(
      EngagementRepository repository,
      IdGenerator ids,
      IdentityFacade identity,
      ObjectMapper mapper) {
    this.repository = repository;
    this.ids = ids;
    this.identity = identity;
    this.mapper = mapper;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public NotificationTask start(long id) {
    NotificationTask t =
        repository
            .findTask(id)
            .orElseThrow(() -> new BusinessException("ENG_TASK_NOT_FOUND", "通知任务不存在"));
    String previous = t.getStatus().name();
    t.start(now());
    if (!repository.updateTask(t, previous)) {
      throw new BusinessException("ENG_TASK_CONFLICT", "通知任务已被其他实例领取");
    }
    return t;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void complete(NotificationTask t, String receipt) {
    String previous = t.getStatus().name();
    t.sent(now());
    if (!repository.updateTask(t, previous)) {
      throw new BusinessException("ENG_TASK_CONFLICT", "通知任务状态已变化");
    }
    repository.insertDelivery(
        ids.nextId(), t.getId(), t.getAttemptCount() + 1, receipt, "SENT", null, now());
    if (t.getChannel() == NotificationChannel.INBOX) {
      long cursor = ids.nextId();
      repository.insertInbox(
          ids.nextId(),
          t.getRecipientUserId(),
          t.getScene(),
          t.getResourceType(),
          t.getResourceId(),
          safeSummary(t.getPayloadJson()),
          cursor,
          now());
      repository.insertChange(
          ids.nextId(),
          t.getRecipientUserId(),
          cursor,
          "engagement",
          "notification",
          Long.toString(t.getId()),
          0,
          "CREATED",
          "{}",
          now());
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void fail(NotificationTask t, Exception error) {
    String previous = t.getStatus().name();
    t.failed(error.getMessage(), true, now());
    if (repository.updateTask(t, previous)) {
      repository.insertDelivery(
          ids.nextId(),
          t.getId(),
          t.getAttemptCount(),
          null,
          t.getStatus().name(),
          error.getClass().getSimpleName(),
          now());
    }
  }

  @Transactional(readOnly = true)
  public boolean deliveryAllowed(NotificationTask task) {
    AccessProfile profile = identity.getAccessProfile(task.getRecipientUserId());
    if (!profile.coreFeaturesAllowed()) return false;
    NotificationPreference preference =
        repository.findPreference(task.getRecipientUserId(), task.getScene()).orElse(null);
    if (preference == null)
      return profile.ageBand() != AgeBand.TEEN || task.getChannel() == NotificationChannel.INBOX;
    return preference.allows(
        task.getChannel(), Instant.now(), safetyCritical(task.getPayloadJson()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void suppress(NotificationTask task, String reason) {
    String previous = task.getStatus().name();
    task.cancel(reason, now());
    if (repository.updateTask(task, previous)) {
      repository.insertDelivery(
          ids.nextId(),
          task.getId(),
          task.getAttemptCount() + 1,
          null,
          "SUPPRESSED",
          reason,
          now());
    }
  }

  /** 读取任务当前状态；投递前用它判断任务是否已被取消。 */
  @Transactional(readOnly = true)
  public java.util.Optional<NotificationTask> find(long id) {
    return repository.findTask(id);
  }

  /** 取消尚未开始投递的任务，例如行动已完成或计划已变更。 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cancelPending(NotificationTask task, String reason) {
    String previous = task.getStatus().name();
    task.cancelBeforeSend(reason, now());
    if (repository.updateTask(task, previous)) {
      repository.insertDelivery(
          ids.nextId(),
          task.getId(),
          task.getAttemptCount() + 1,
          null,
          "CANCELLED",
          reason,
          now());
    }
  }

  private boolean safetyCritical(String payload) {
    try {
      return mapper.readTree(payload).path("safetyCritical").asBoolean(false);
    } catch (Exception ignored) {
      return false;
    }
  }

  private String safeSummary(String payload) {
    try {
      String summary = mapper.readTree(payload).path("safeSummary").asText("").strip();
      if (summary.isEmpty()) return "你有一条新消息";
      return summary.length() <= 200 ? summary : summary.substring(0, 200);
    } catch (Exception ignored) {
      return "你有一条新消息";
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }
}
