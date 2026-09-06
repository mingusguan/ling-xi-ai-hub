package com.lingxi.operations.api;

public record ConfigReleaseResult(
    long releaseId,
    String releaseKey,
    String configType,
    int versionNo,
    String status,
    long createdBy,
    Long approvedBy,
    Long publishedBy,
    Long previousReleaseId,
    String failureReason,
    long version) {}
