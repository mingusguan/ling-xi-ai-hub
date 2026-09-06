package com.lingxi.content.application;

import java.time.Instant;
import java.util.List;

public interface ObjectStorageAdapter {
  UploadAuthorization authorizePrivateUpload(
      String objectKey, String mimeType, long sizeBytes, Instant expiresAt);

  UploadedObject verifyPrivateUpload(String objectKey);

  /** 幂等删除私有对象；对象不存在也视为成功。 */
  void deletePrivateObject(String objectKey);

  /** 按隐私请求批量隔离对象，返回可审计的非敏感凭证。 */
  String logicallyIsolateUserObjects(long requestId, long userId, List<String> objectKeys);

  record UploadedObject(long sizeBytes, String mimeType, String contentHash) {}

  record UploadAuthorization(String opaqueReference) {}
}
