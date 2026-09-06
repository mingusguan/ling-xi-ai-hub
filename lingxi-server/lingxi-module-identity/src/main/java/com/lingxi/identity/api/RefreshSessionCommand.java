package com.lingxi.identity.api;

/** 刷新并轮换会话令牌的命令。 */
public record RefreshSessionCommand(String sessionFamilyId, String refreshToken, String deviceId) {}
