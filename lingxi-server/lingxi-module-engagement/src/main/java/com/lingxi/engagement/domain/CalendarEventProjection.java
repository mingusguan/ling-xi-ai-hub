package com.lingxi.engagement.domain;

import java.time.LocalDateTime;

public record CalendarEventProjection(
    long id,
    long bindingId,
    String resourceType,
    String resourceId,
    String externalId,
    String externalVersion,
    String syncStatus,
    LocalDateTime updatedAt) {}
