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
  /** 同一任务因免打扰被顺延的次数上限，防止异常配置导致无限推迟。 */
  private static final int MAX_DEFERRALS = 3;

  /**
   * 顺延允许的最长迟到时间。
   *
   * <p>免打扰一般只跨越一个夜晚，正常顺延最多一两次；但如果用户把免打扰设成
   * 十几个小时、或任务本身就在时段边缘，反复顺延就会把提醒推到很久以后。
   * 一条迟到半天的「该做某事了」比不发更糟——用户早就出门了，这条提醒只会变成噪声。
   * 因此超过该时长就放弃补发，而不是无限等待。
   */
  private static final Duration MAX_DEFERRAL_LATENESS = Duration.ofHours(12);

  private final EngagementRepository repository;
  private final IdGenerator ids;
  private final IdentityFacade identity;
  private final QuietHoursProvider quietHours;
  private final CompanionPreferenceProvider companionPreferences;
  private final ObjectMapper mapper;

  public NotificationTaskLifecycleService(
      EngagementRepository repository,
      IdGenerator ids,
      IdentityFacade identity,
      QuietHoursProvider quietHours,
      CompanionPreferenceProvider companionPreferences,
      ObjectMapper mapper) {
    this.repository = repository;
    this.ids = ids;
    this.identity = identity;
    this.quietHours = quietHours;
    this.companionPreferences = companionPreferences;
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

  /**
   * 判定这条任务现在能不能投递。
   *
   * <p>三种结果要分开处理：可以发、命中免打扰因此稍后补发、被场景策略永久拒绝。
   * 账号级免打扰只有一份来源——通知偏好；用户还没建过偏好时回退到新手引导画像里的
   * 免打扰时段，否则用户在引导里设的时段完全不生效。
   */
  @Transactional(readOnly = true)
  public DeliveryDecision deliveryDecision(NotificationTask task, Instant now) {
    AccessProfile profile = identity.getAccessProfile(task.getRecipientUserId());
    if (!profile.coreFeaturesAllowed()) {
      return DeliveryDecision.denied();
    }
    // 主动程度为「低」时不再发起主动提醒：用户在新手引导里选的就是「只在我想聊的时候出现」，
    // 如果还照常推送，这个选项等于没生效。站内信与安全类消息不受影响。
    if (isProactiveScene(task) && prefersNoProactiveOutreach(task.getRecipientUserId())) {
      return DeliveryDecision.denied();
    }
    NotificationPreference preference =
        repository.findPreference(task.getRecipientUserId(), task.getScene()).orElse(null);
    if (preference == null) {
      // 青少年默认只允许站内信；不允许的渠道无法补发，按拒绝处理。
      if (profile.ageBand() == AgeBand.TEEN && task.getChannel() != NotificationChannel.INBOX) {
        return DeliveryDecision.denied();
      }
    } else if (!preference.allowsChannel(task.getChannel())) {
      return DeliveryDecision.denied();
    }
    if (safetyCritical(task.getPayloadJson())) {
      return DeliveryDecision.allow();
    }
    if (preference != null) {
      Instant quietUntil = preference.quietHoursEndAt(now);
      return quietUntil == null
          ? DeliveryDecision.allow()
          : DeliveryDecision.deferredUntil(quietUntil, DeliveryDecision.DEFERRED_ACCOUNT_QUIET_HOURS);
    }
    ZoneId zone = quietHours.findAccountZone(task.getRecipientUserId());
    if (zone == null) {
      return DeliveryDecision.allow();
    }
    QuietHoursWindow window = quietHours.findQuietHours(task.getRecipientUserId(), zone);
    Instant quietUntil = window == null ? null : window.endsAt(now);
    return quietUntil == null
        ? DeliveryDecision.allow()
        : DeliveryDecision.deferredUntil(quietUntil, DeliveryDecision.DEFERRED_PROFILE_QUIET_HOURS);
  }

  /**
   * 是否属于「伙伴主动发起」的消息。
   *
   * <p>行动提醒与陪伴类消息由伙伴主动推给用户，因此受主动程度影响；
   * 站内信、安全通知与账号类消息不属于主动打扰，不受该偏好限制。
   */
  private boolean isProactiveScene(NotificationTask task) {
    if (task.getChannel() == NotificationChannel.INBOX) {
      return false;
    }
    return !safetyCritical(task.getPayloadJson());
  }

  /** 用户在引导里把主动程度设为「低」时，不再主动推送。 */
  private boolean prefersNoProactiveOutreach(long userId) {
    CompanionPreference preference = companionPreferences.findPreference(userId);
    return preference.proactivityLevel() == CompanionPreference.ProactivityPreference.LOW;
  }

  /**
   * 把任务推迟到免打扰结束再投递。
   *
   * <p>顺延次数有上限：免打扰配置异常时不能让提醒被无限推迟，达到上限后按最终失败收口。
   * 返回 false 表示超过上限，调用方应改为抑制。
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean defer(NotificationTask task, Instant until) {
    // 迟到的提醒会变成噪声，因此除了次数上限还要看顺延后到底迟了多久。
    if (until.isAfter(task.getScheduledAt().plus(MAX_DEFERRAL_LATENESS))) {
      return false;
    }
    String previous = task.getStatus().name();
    if (!task.deferredTo(until, MAX_DEFERRALS, now())) {
      return false;
    }
    if (!repository.updateTask(task, previous)) {
      throw new BusinessException("ENG_TASK_CONFLICT", "通知任务状态已变化");
    }
    repository.insertDelivery(
        ids.nextId(),
        task.getId(),
        task.getAttemptCount(),
        null,
        "DEFERRED",
        task.getLastError(),
        now());
    return true;
  }

  /**
   * 把已经无法补发的任务收口为最终失败。
   *
   * <p>用于顺延次数用尽的情况：此时既不该投递（用户正在免打扰），
   * 也不该无限等待，需要留下明确的终态和原因，便于后台排查配置异常。
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void failPermanently(NotificationTask task, String reason) {
    String previous = task.getStatus().name();
    task.failed(reason, false, now());
    if (!repository.updateTask(task, previous)) {
      throw new BusinessException("ENG_TASK_CONFLICT", "通知任务状态已变化");
    }
    repository.insertDelivery(
        ids.nextId(),
        task.getId(),
        task.getAttemptCount(),
        null,
        "FAILED",
        reason,
        now());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void suppress(NotificationTask task, String reason) {    String previous = task.getStatus().name();
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
