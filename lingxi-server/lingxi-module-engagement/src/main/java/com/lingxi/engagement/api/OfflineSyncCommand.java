package com.lingxi.engagement.api;

/** 客户端离线命令。 */
public record OfflineSyncCommand(
    String clientCommandId,
    long userId,
    String deviceId,
    String commandType,
    String payloadJson,
    long baseVersion) {}
