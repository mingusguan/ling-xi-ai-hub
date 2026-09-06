package com.lingxi.content.api;

public record CreateImportJobCommand(
    String requestKey, long userId, long sourceFileId, String format) {}
