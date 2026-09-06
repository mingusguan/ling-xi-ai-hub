package com.lingxi.engagement.api;

import java.time.LocalTime;
import java.util.Set;

/** 更新用户场景通知偏好。 */
public record UpdateNotificationPreferenceCommand(
    long userId,
    String scene,
    Set<NotificationChannel> channels,
    LocalTime quietStart,
    LocalTime quietEnd,
    String timezone,
    long expectedVersion) {}
