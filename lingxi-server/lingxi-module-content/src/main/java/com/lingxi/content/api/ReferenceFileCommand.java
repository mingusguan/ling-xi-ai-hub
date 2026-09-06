package com.lingxi.content.api;

public record ReferenceFileCommand(
    long userId, long fileId, String resourceType, String resourceId) {}
