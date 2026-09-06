package com.lingxi.content.api;

public record CreateExportJobCommand(
    String requestKey,
    long userId,
    String scopeJson,
    String format,
    boolean recentAuthentication) {}
