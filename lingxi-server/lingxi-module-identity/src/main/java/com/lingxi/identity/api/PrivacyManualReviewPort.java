package com.lingxi.identity.api;

/** 隐私自动处理超时后的人工复核端口；实现必须按请求和类别幂等。 */
public interface PrivacyManualReviewPort {
  String open(long requestId, long userId, String category, String description);
}
