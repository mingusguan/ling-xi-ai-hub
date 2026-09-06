package com.lingxi.engagement.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.engagement.api.NotificationChannel;
import com.lingxi.engagement.domain.NotificationTask;
import com.lingxi.kernel.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 数据库任务通知投递处理器。 */
@Component
public class NotificationJobHandler implements AsyncJobHandler {
  public static final String JOB_TYPE = "engagement.notification";
  private final NotificationTaskLifecycleService lifecycle;
  private final Map<NotificationChannel, NotificationChannelAdapter> adapters;
  private final ObjectMapper mapper;

  public NotificationJobHandler(
      NotificationTaskLifecycleService lifecycle,
      List<NotificationChannelAdapter> adapters,
      ObjectMapper mapper) {
    this.lifecycle = lifecycle;
    this.mapper = mapper;
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
    NotificationTask task = lifecycle.start(id);
    if (!lifecycle.deliveryAllowed(task)) {
      lifecycle.suppress(task, "LATEST_POLICY_DENIED");
      return "{\"status\":\"suppressed\"}";
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

  private NotificationChannelAdapter requireAdapter(NotificationTask task) {
    NotificationChannelAdapter adapter = adapters.get(task.getChannel());
    if (adapter == null) {
      throw new BusinessException("ENG_CHANNEL_UNAVAILABLE", "通知渠道暂不可用");
    }
    return adapter;
  }
}
