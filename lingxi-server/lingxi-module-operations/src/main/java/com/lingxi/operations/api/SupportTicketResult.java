package com.lingxi.operations.api;

import java.time.LocalDateTime;

public record SupportTicketResult(
    long ticketId,
    String ticketNo,
    long userId,
    String category,
    String subject,
    String status,
    String priority,
    Long assigneeAdminId,
    long version,
    LocalDateTime createdAt) {}
