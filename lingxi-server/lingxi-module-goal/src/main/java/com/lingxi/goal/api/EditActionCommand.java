package com.lingxi.goal.api;

/**
 * 编辑行动命令，「本次及未来」整体生效。
 *
 * @param expectedVersion 客户端持有的行动版本；不一致时拒绝写入
 */
public record EditActionCommand(
    String requestKey, long userId, long actionId, long expectedVersion, ActionInput input) {}
