package com.lingxi.companion.api;

/**
 * @param requestKey 客户端幂等键
 */
public record CreateConversationCommand(
    String requestKey, long userId, String scene, String title) {}
