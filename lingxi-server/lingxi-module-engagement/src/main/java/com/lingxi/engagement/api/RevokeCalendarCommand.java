package com.lingxi.engagement.api;

/** 撤销日历绑定命令，近期认证事实必须由服务端认证上下文提供。 */
public record RevokeCalendarCommand(
    long userId, long bindingId, boolean deleteCreatedEvents, boolean recentAuthentication) {}
