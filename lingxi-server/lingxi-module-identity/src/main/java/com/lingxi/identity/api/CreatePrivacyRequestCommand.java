package com.lingxi.identity.api;

/** 创建隐私权利请求命令。 */
public record CreatePrivacyRequestCommand(
    String requestKey,
    long userId,
    PrivacyRequestType type,
    String scopeJson,
    boolean recentAuthentication) {}
