package com.lingxi.engagement.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.engagement.api.NotificationChannel;
import com.lingxi.engagement.api.NotificationTaskStatus;
import com.lingxi.engagement.domain.NotificationTask;
import com.lingxi.engagement.infrastructure.event.OccurrenceReminderHandler;
import com.lingxi.goal.api.GoalFacade;
import com.lingxi.goal.api.OccurrenceResult;
import com.lingxi.kernel.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 数据库任务通知投递处理器。 */
@Component
public class NotificationJobHandler implements AsyncJobHandler {
  public static final String JOB_TYPE = "engagement.notification";
  private final NotificationTaskLifecycleService lifecycle;
  private final Map<NotificationChannel, NotificationChannelAdapter> adapters;
  private final GoalFacade goals;
  private final ObjectMapper mapper;

  public NotificationJobHandler(
      NotificationTaskLifecycleService lifecycle,
      List<NotificationChannelAdapter> adapters,
      GoalFacade goals,
      ObjectMapper mapper) {
    this.lifecycle = lifecycle;
    this.mapper = mapper;
    this.goals = goals;
    this.adapters = new EnumMap<>(NotificationChannel.class);
    adapters.forEach(a -> this.adapters.put(a.channel(), a));
  }

  @Override
  public boolean supports(String type) {
    return JOB_TYPE.equals(type);
  }

  @Override
  public String handle(AsyncJobMessage message) throws Exception {
    long id = mapper.readTree(message.payloadJson()).path("taskId").asLong();
    // 任务可能在到期前被取消（行动已打卡或计划已变更）；此时按成功无操作返回，不产生重试噪声。
    NotificationTask scheduled =
        lifecycle
            .find(id)
            .orElseThrow(() -> new BusinessException("ENG_TASK_NOT_FOUND", "通知任务不存在"));
    if (scheduled.getStatus() == NotificationTaskStatus.CANCELLED) {
      return "{\"status\":\"cancelled\"}";
    }
    NotificationTask task = lifecycle.start(id);
    if (!lifecycle.deliveryAllowed(task)) {
      lifecycle.suppress(task, "LATEST_POLICY_DENIED");
      return "{\"status\":\"suppressed\"}";
    }
    if (!occurrenceStillPending(task)) {
      // 行动实例已被完成后修正、跳过或计划调整取代：不再投递过期提醒。
      lifecycle.suppress(task, "OCCURRENCE_NOT_PENDING");
      return "{\"status\":\"stale\"}";
    }
    try {
      String receipt =
          task.getChannel() == NotificationChannel.INBOX
              ? "local"
              : requireAdapter(task).deliver(task);
      lifecycle.complete(task, receipt);
      return "{\"status\":\"sent\"}";
    } catch (Exception e) {
      lifecycle.fail(task, e);
      throw e;
    }
  }

  /** 行动提醒类任务在投递前校验实例仍处于待执行状态；其他任务不受影响。 */
  private boolean occurrenceStillPending(NotificationTask task) {
    if (!OccurrenceReminderHandler.RESOURCE_TYPE.equals(task.getResourceType())) {
      return true;
    }
    long occurrenceId;
    try {
      occurrenceId = Long.parseLong(task.getResourceId());
    } catch (RuntimeException exception) {
      return true;
    }
    Optional<OccurrenceResult> occurrence =
        goals.findOccurrence(task.getRecipientUserId(), occurrenceId);
    return occurrence.map(item -> "SCHEDULED".equals(item.status())).orElse(false);
  }

  private NotificationChannelAdapter requireAdapter(NotificationTask task) {
    NotificationChannelAdapter adapter = adapters.get(task.getChannel());
    if (adapter == null) {
      throw new BusinessException("ENG_CHANNEL_UNAVAILABLE", "通知渠道暂不可用");
    }
    return adapter;
  }
}
