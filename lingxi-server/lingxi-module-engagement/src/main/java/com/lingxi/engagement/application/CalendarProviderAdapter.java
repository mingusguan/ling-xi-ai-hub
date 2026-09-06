package com.lingxi.engagement.application;

import com.lingxi.engagement.api.CalendarProjectionCommand;
import java.util.List;

public interface CalendarProviderAdapter {
  String provider();

  ProjectionReceipt upsert(
      String credentialReference,
      CalendarProjectionCommand command,
      String existingExternalId,
      String existingVersion)
      throws Exception;

  void delete(String credentialReference, String externalId, String requestKey) throws Exception;

  /** 注销时撤销用户授权并隔离外部日历读模型，返回非敏感凭证。 */
  String logicallyDisconnectUserData(
      long requestId, long userId, List<String> credentialReferences) throws Exception;

  record ProjectionReceipt(String externalId, String externalVersion) {}
}
