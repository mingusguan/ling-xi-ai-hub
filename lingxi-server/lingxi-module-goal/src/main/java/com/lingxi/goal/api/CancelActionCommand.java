package com.lingxi.goal.api;

/**
 * 取消行动命令。
 *
 * <p>取消只停止后续生成，已产生的实例与打卡事实保留；已取消时重复调用是安全的空操作。
 */
public record CancelActionCommand(
    String requestKey, long userId, long actionId, long expectedVersion) {}
