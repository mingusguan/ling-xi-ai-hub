package com.lingxi.operations.api;

public record AdminActionCommand(
    long adminId, long releaseId, long expectedVersion, AuditContext audit) {}
