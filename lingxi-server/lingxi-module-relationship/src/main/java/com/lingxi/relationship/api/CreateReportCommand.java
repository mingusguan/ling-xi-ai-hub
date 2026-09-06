package com.lingxi.relationship.api;

public record CreateReportCommand(
    long reporterUserId,
    String targetType,
    String targetId,
    String reasonCode,
    String evidenceReference) {}
