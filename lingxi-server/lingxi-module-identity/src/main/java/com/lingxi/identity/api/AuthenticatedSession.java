package com.lingxi.identity.api;

/** Access Token 鉴权后的最小会话结果。 */
public record AuthenticatedSession(
    long userId, String deviceId, long authorizationVersion, boolean recentAuthentication) {}
