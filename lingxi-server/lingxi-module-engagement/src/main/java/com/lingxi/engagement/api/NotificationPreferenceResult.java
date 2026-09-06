package com.lingxi.engagement.api;

import java.time.LocalTime;
import java.util.Set;

/** 通知偏好视图。 */
public record NotificationPreferenceResult(
    long userId,
    String scene,
    Set<NotificationChannel> channels,
    LocalTime quietStart,
    LocalTime quietEnd,
    String timezone,
    long version) {}
