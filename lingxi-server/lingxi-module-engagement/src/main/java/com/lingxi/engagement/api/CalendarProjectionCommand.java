package com.lingxi.engagement.api;

import java.time.Instant;

public record CalendarProjectionCommand(
    String requestKey,
    long userId,
    long bindingId,
    String resourceType,
    String resourceId,
    long resourceVersion,
    String title,
    Instant startAt,
    Instant endAt,
    String operation) {}
