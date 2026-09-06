package com.lingxi.content.api;

import java.time.LocalDateTime;

public record TemplateVersionResult(
    long versionId,
    long templateId,
    int versionNo,
    String ageScope,
    String contentSnapshot,
    String status,
    Long reviewerUserId,
    String reviewReason,
    LocalDateTime publishedAt,
    long version) {}
