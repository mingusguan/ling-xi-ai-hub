package com.lingxi.identity.api;

import java.time.LocalDateTime;

/**
 * 新手引导状态与画像的读取结果。
 *
 * @param completed 是否已经结束引导（含「跳过」）
 * @param completedAt 结束时间；未完成时为 null
 * @param profile 已填画像；从未填写时各项为 null、阻塞原因为空集合
 * @param effectiveCommunicationStyle 生效沟通风格（未选择时按简洁兜底）
 * @param effectiveProactivityLevel 生效主动程度（未选择时按「中」兜底）
 * @param version 账号当前版本号，前端更新画像时必须原样回传
 */
public record OnboardingProfileResult(
    boolean completed,
    LocalDateTime completedAt,
    OnboardingProfile profile,
    CommunicationStyle effectiveCommunicationStyle,
    ProactivityLevel effectiveProactivityLevel,
    long version) {}
