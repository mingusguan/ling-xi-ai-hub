package com.lingxi.goal.api;

/** 完成周期复盘命令。 */
public record CompleteReviewCommand(
    String requestKey, long userId, long reviewId, String conclusionJson) {}
