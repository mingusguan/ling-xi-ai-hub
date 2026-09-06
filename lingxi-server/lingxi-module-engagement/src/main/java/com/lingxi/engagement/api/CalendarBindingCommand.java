package com.lingxi.engagement.api;

/** 创建日历授权绑定，credentialReference 只能是安全凭据引用。 */
public record CalendarBindingCommand(
    String requestKey,
    long userId,
    String provider,
    String credentialReference,
    boolean recentAuthentication) {}
