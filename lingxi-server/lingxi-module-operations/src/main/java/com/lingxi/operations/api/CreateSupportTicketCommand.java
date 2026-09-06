package com.lingxi.operations.api;

public record CreateSupportTicketCommand(
    long userId, String category, String subject, String description, String priority) {}
