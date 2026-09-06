package com.lingxi.operations.api;

public record AuditContext(
    String reason, String ticketNo, String requestId, boolean recentAuthentication) {}
