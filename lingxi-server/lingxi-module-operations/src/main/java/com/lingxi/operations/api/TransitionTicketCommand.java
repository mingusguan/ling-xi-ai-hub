package com.lingxi.operations.api;

public record TransitionTicketCommand(
    long adminId,
    long ticketId,
    String status,
    Long assigneeAdminId,
    long expectedVersion,
    AuditContext audit) {}
