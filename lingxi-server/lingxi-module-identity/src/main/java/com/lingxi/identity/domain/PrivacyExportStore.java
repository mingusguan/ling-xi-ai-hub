package com.lingxi.identity.domain;

/** 加密隐私导出包存储端口。 */
public interface PrivacyExportStore {
  String store(long requestId, long userId, String exportJson);

  String load(long requestId, long userId);
}
