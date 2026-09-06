package com.lingxi.companion.api;

public record UpdateMemoryCommand(
    long userId, long memoryId, String contentText, long expectedVersion) {}
