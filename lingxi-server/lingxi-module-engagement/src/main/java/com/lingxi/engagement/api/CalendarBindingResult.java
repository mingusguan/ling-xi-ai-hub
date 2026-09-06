package com.lingxi.engagement.api;

/** 日历绑定状态。 */
public record CalendarBindingResult(long bindingId, String provider, String status, long version) {}
