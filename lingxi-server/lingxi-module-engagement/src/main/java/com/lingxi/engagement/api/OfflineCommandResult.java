package com.lingxi.engagement.api;

/** 离线命令应用结果。 */
public record OfflineCommandResult(
    String clientCommandId, String status, String resultJson, String errorCode) {}
