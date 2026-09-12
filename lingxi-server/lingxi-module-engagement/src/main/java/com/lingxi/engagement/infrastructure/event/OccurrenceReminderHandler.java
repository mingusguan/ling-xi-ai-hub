package com.lingxi.engagement.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lingxi.engagement.api.*;
import com.lingxi.engagement.application.NotificationTaskLifecycleService;
import com.lingxi.engagement.domain.EngagementRepository;
import com.lingxi.engagement.domain.NotificationPreference;
import com.lingxi.engagement.domain.NotificationTask;
import com.lingxi.kernel.*;
import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 把目标域的行动实例事件转换为提醒任务。
 *
 * <p>按 R05 设计「领域事件进入规则匹配，创建 NotificationTask」：
 * <ul>
 *   <li>实例生成（{@code goal.occurrence-scheduled}）→ 在建实例时间前创建提醒任务；
 *   <li>打卡（{@code goal.action-checked-in}）→ 取消该实例尚未发送的提醒，已发送记录保留。
 * </ul>
 *
 * <p>时区、免打扰、渠道与青少年限制在投递期由 {@code NotificationTaskLifecycleService.deliveryAllowed}
 * 按最新策略判定，本处理器只负责按用户偏好生成候选任务；任务去重键为实例+渠道，重复事件不会重复提醒。
 */
@Component
public class OccurrenceReminderHandler implements PublishedEventHandler {
  /** 提醒场景标识，与通知偏好中的 scene 对应。 */
  public static final String SCENE = "ACTION_REMINDER";
  /** 提醒任务引用的资源类型。 */
  public static final String RESOURCE_TYPE = "goal-occurrence";

  private static final Set<String> SUPPORTED_EVENTS =
      Set.of("goal.occurrence-scheduled", "goal.action-checked-in");

  private final EngagementFacade engagement;
  private final EngagementRepository repository;
  private final NotificationTaskLifecycleService lifecycle;
  private final ObjectMapper mapper;
  private final boolean enabled;
  private final long leadMinutes;

  public OccurrenceReminderHandler(
      EngagementFacade engagement,
      EngagementRepository repository,
      NotificationTaskLifecycleService lifecycle,
      ObjectMapper mapper,
      @Value("${lingxi.engagement.reminder-enabled:true}") boolean enabled,
      @Value("${lingxi.engagement.reminder-lead-minutes:0}") long leadMinutes) {
    this.engagement = engagement;
    this.repository = repository;
    this.lifecycle = lifecycle;
    this.mapper = mapper;
    this.enabled = enabled;
    this.leadMinutes = leadMinutes;
  }

  @Override
  public String consumerName() {
    return "engagement-occurrence-reminder-v1";
  }

  @Override
  public boolean supports(String eventType) {
    return SUPPORTED_EVENTS.contains(eventType);
  }

  @Override
  public void handle(PublishedEventMessage event) {
    JsonNode payload = parse(event.payloadJson());
    long userId = payload.path("userId").asLong();
    if (userId <= 0) {
      // 早期事件未携带归属用户：无法确定接收人，跳过而非死信。
      return;
    }
    if ("goal.action-checked-in".equals(event.eventType())) {
      cancelReminders(payload);
      return;
    }
    createReminders(userId, payload);
  }

  /** 为新建实例按渠道创建提醒任务；已到期的实例不再补发，避免生成即刷屏。 */
  private void createReminders(long userId, JsonNode payload) {
    if (!enabled) {
      return;
    }
    long occurrenceId = payload.path("occurrenceId").asLong();
    long goalId = payload.path("goalId").asLong();
    String actionTitle = payload.path("actionTitle").asText("");
    Instant scheduledAt = parseInstant(payload.path("scheduledAt").asText(null));
    if (occurrenceId <= 0 || scheduledAt == null) {
      return;
    }
    Instant remindAt = scheduledAt.minusSeconds(leadMinutes * 60L);
    if (!remindAt.isAfter(Instant.now())) {
      return;
    }
    for (NotificationChannel channel : channelsFor(userId)) {
      engagement.scheduleNotification(
          new ScheduleNotificationCommand(
              dedupeKey(occurrenceId, channel),
              userId,
              channel,
              SCENE,
              RESOURCE_TYPE,
              Long.toString(occurrenceId),
              payloadJson(goalId, occurrenceId, actionTitle),
              remindAt));
    }
  }

  /** 取消该实例尚未发送的提醒；已发送或投递中的任务不在此处改动。 */
  private void cancelReminders(JsonNode payload) {
    long occurrenceId = payload.path("occurrenceId").asLong();
    if (occurrenceId <= 0) {
      return;
    }
    for (NotificationChannel channel : NotificationChannel.values()) {
      NotificationTask task =
          repository.findTaskByDedupeKey(dedupeKey(occurrenceId, channel)).orElse(null);
      if (task != null && task.getStatus() == NotificationTaskStatus.PENDING) {
        lifecycle.cancelPending(task, "OCCURRENCE_CHECKED_IN");
      }
    }
  }

  /** 用户偏好优先；未设置时用站内信兜底，青少年由投递期策略强制为站内信。 */
  private Set<NotificationChannel> channelsFor(long userId) {
    NotificationPreference preference = repository.findPreference(userId, SCENE).orElse(null);
    if (preference == null || preference.getChannels().isEmpty()) {
      return Set.of(NotificationChannel.INBOX);
    }
    return preference.getChannels();
  }

  private String dedupeKey(long occurrenceId, NotificationChannel channel) {
    return "goal-reminder:" + occurrenceId + ":" + channel.name();
  }

  private String payloadJson(long goalId, long occurrenceId, String actionTitle) {
    try {
      ObjectNode node = mapper.createObjectNode();
      node.put(
          "safeSummary",
          actionTitle == null || actionTitle.isBlank() ? "该完成今天的行动了" : "该完成「" + actionTitle + "」了");
      node.put("goalId", goalId);
      node.put("occurrenceId", occurrenceId);
      node.put("safetyCritical", false);
      return mapper.writeValueAsString(node);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("提醒载荷序列化失败", exception);
    }
  }

  private Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (RuntimeException exception) {
      return null;
    }
  }

  private JsonNode parse(String payloadJson) {
    try {
      return mapper.readTree(payloadJson);
    } catch (Exception exception) {
      throw new BusinessException("ENG_REMINDER_EVENT_INVALID", "行动实例事件载荷无法解析");
    }
  }
}
