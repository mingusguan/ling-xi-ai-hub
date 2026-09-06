package com.lingxi.engagement.api;

import java.time.Instant;

/** 幂等创建通知任务。敏感正文不得进入 payloadJson。 */
public record ScheduleNotificationCommand(
    String dedupeKey,
    long recipientUserId,
    NotificationChannel channel,
    String scene,
    String resourceType,
    String resourceId,
    String payloadJson,
    Instant scheduledAt) {}
