package com.lingxi.goal.api;

/**
 * 复制行动命令。
 *
 * @param clientKey 新行动的客户端标识，在同一计划内必须唯一
 * @param title 新行动标题；为空时沿用原标题
 */
public record CopyActionCommand(
    String requestKey, long userId, long actionId, String clientKey, String title) {}
