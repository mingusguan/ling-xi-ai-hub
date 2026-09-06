package com.lingxi.identity.api;

/** 单个业务模块处理隐私请求时使用的稳定上下文。 */
public record PrivacyProcessingContext(
    long requestId, long userId, PrivacyRequestType type, PrivacyScope scope) {}
