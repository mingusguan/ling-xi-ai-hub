package com.lingxi.operations.api;

public record CreateConfigReleaseCommand(
    String releaseKey,
    String configType,
    int versionNo,
    String contentRef,
    String contentDigest,
    String grayRule,
    long adminId,
    AuditContext audit) {}
