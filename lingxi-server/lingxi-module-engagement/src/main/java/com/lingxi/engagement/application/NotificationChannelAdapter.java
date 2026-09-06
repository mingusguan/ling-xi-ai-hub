package com.lingxi.engagement.application;

import com.lingxi.engagement.api.NotificationChannel;
import com.lingxi.engagement.domain.NotificationTask;

/** 外部通知渠道适配器；实现必须使用渠道幂等键。 */
public interface NotificationChannelAdapter {
  NotificationChannel channel();

  String deliver(NotificationTask task) throws Exception;
}
