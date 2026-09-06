package com.lingxi.engagement.api;

import java.time.Instant;

/** 通知或站内消息视图。 */
public record NotificationResult(
    long id,
    String type,
    String resourceType,
    String resourceId,
    String summary,
    String status,
    Instant createdAt,
    Instant readAt,
    long cursor) {}
