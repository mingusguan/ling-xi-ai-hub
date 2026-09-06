package com.lingxi.identity.domain;

import com.lingxi.identity.api.ConsentPurpose;
import java.time.LocalDateTime;
import java.util.Optional;

/** 登录、会话、协议和隐私请求持久化端口。 */
public interface IdentitySecurityRepository {
  Optional<LoginIdentity> findLoginIdentity(String channel, String subjectHash);

  void insertLoginIdentity(LoginIdentity identity);

  void ensureDevice(long id, long userId, String deviceId, LocalDateTime now);

  void insertSession(DeviceSession session);

  Optional<DeviceSession> findSessionByFamilyId(String familyId);

  Optional<DeviceSession> findSessionByAccessHash(String accessHash);

  boolean updateSession(DeviceSession session, long previousVersion);

  void revokeAllSessions(long userId, String reason, LocalDateTime now);

  void insertConsent(
      long id,
      long userId,
      ConsentPurpose purpose,
      String documentVersion,
      boolean granted,
      String evidenceReference,
      LocalDateTime recordedAt);

  Optional<PrivacyRequest> findPrivacyRequest(long requestId);

  Optional<PrivacyRequest> findPrivacyRequestByKey(String requestKey);

  void insertPrivacyRequest(PrivacyRequest request);

  boolean updatePrivacyRequest(PrivacyRequest request, long previousVersion);

  int logicallyDeletePrivateData(long requestId, long userId, boolean closeAccount);

  void recordDeletionAudit(
      long requestId,
      long userId,
      String moduleName,
      int affectedRows,
      LocalDateTime completedAt);

  Optional<Integer> findDeletionAuditAffectedRows(long requestId, String moduleName);

  void minimizeClosedPrivacyRequest(long requestId);
}
