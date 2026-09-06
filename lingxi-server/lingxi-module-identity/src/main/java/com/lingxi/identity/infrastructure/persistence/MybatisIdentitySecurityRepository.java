package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.identity.api.ConsentPurpose;
import com.lingxi.identity.api.PrivacyRequestStatus;
import com.lingxi.identity.api.PrivacyRequestType;
import com.lingxi.identity.domain.DeviceSession;
import com.lingxi.identity.domain.IdentitySecurityRepository;
import com.lingxi.identity.domain.LoginIdentity;
import com.lingxi.identity.domain.PrivacyRequest;
import com.lingxi.kernel.IdGenerator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

/** 身份安全数据的 MyBatis-Plus 仓储实现。 */
@Repository
public class MybatisIdentitySecurityRepository implements IdentitySecurityRepository {
  private final LoginIdentityMapper loginMapper;
  private final DeviceMapper deviceMapper;
  private final SessionMapper sessionMapper;
  private final UserConsentMapper consentMapper;
  private final PrivacyRequestMapper privacyMapper;
  private final AgeVerificationMapper ageVerificationMapper;
  private final PrivacyExportMapper privacyExportMapper;
  private final PrivacyDeletionAuditMapper deletionAuditMapper;
  private final IdGenerator idGenerator;

  public MybatisIdentitySecurityRepository(
      LoginIdentityMapper loginMapper,
      DeviceMapper deviceMapper,
      SessionMapper sessionMapper,
      UserConsentMapper consentMapper,
      PrivacyRequestMapper privacyMapper,
      AgeVerificationMapper ageVerificationMapper,
      PrivacyExportMapper privacyExportMapper,
      PrivacyDeletionAuditMapper deletionAuditMapper,
      IdGenerator idGenerator) {
    this.loginMapper = loginMapper;
    this.deviceMapper = deviceMapper;
    this.sessionMapper = sessionMapper;
    this.consentMapper = consentMapper;
    this.privacyMapper = privacyMapper;
    this.ageVerificationMapper = ageVerificationMapper;
    this.privacyExportMapper = privacyExportMapper;
    this.deletionAuditMapper = deletionAuditMapper;
    this.idGenerator = idGenerator;
  }

  @Override
  public Optional<LoginIdentity> findLoginIdentity(String channel, String subjectHash) {
    return Optional.ofNullable(
            loginMapper.selectOne(
                Wrappers.<LoginIdentityEntity>lambdaQuery()
                    .eq(LoginIdentityEntity::getChannel, channel)
                    .eq(LoginIdentityEntity::getSubjectHash, subjectHash)
                    .last("LIMIT 1")))
        .map(
            e ->
                new LoginIdentity(
                    e.getId(),
                    e.getUserId(),
                    e.getChannel(),
                    e.getSubjectHash(),
                    e.getVerifiedAt(),
                    e.getCreatedAt()));
  }

  @Override
  public void insertLoginIdentity(LoginIdentity identity) {
    LoginIdentityEntity e = new LoginIdentityEntity();
    e.setId(identity.id());
    e.setUserId(identity.userId());
    e.setChannel(identity.channel());
    e.setSubjectHash(identity.subjectHash());
    e.setVerifiedAt(identity.verifiedAt());
    e.setCreatedAt(identity.createdAt());
    loginMapper.insert(e);
  }

  @Override
  public void ensureDevice(long id, long userId, String deviceId, LocalDateTime now) {
    DeviceEntity existing =
        deviceMapper.selectOne(
            Wrappers.<DeviceEntity>lambdaQuery()
                .eq(DeviceEntity::getUserId, userId)
                .eq(DeviceEntity::getDeviceId, deviceId)
                .last("LIMIT 1"));
    if (existing != null) {
      deviceMapper.update(
          null,
          Wrappers.<DeviceEntity>lambdaUpdate()
              .eq(DeviceEntity::getId, existing.getId())
              .set(DeviceEntity::getLastSeenAt, now));
      return;
    }
    DeviceEntity e = new DeviceEntity();
    e.setId(id);
    e.setUserId(userId);
    e.setDeviceId(deviceId);
    e.setFirstSeenAt(now);
    e.setLastSeenAt(now);
    try {
      deviceMapper.insert(e);
    } catch (DuplicateKeyException ignored) {
      deviceMapper.update(
          null,
          Wrappers.<DeviceEntity>lambdaUpdate()
              .eq(DeviceEntity::getUserId, userId)
              .eq(DeviceEntity::getDeviceId, deviceId)
              .set(DeviceEntity::getLastSeenAt, now));
    }
  }

  @Override
  public void insertSession(DeviceSession s) {
    sessionMapper.insert(toEntity(s));
  }

  @Override
  public Optional<DeviceSession> findSessionByFamilyId(String familyId) {
    return Optional.ofNullable(
            sessionMapper.selectOne(
                Wrappers.<SessionEntity>lambdaQuery()
                    .eq(SessionEntity::getFamilyId, familyId)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public Optional<DeviceSession> findSessionByAccessHash(String accessHash) {
    return Optional.ofNullable(
            sessionMapper.selectOne(
                Wrappers.<SessionEntity>lambdaQuery()
                    .eq(SessionEntity::getAccessTokenHash, accessHash)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public boolean updateSession(DeviceSession s, long previousVersion) {
    return sessionMapper.update(
            null,
            Wrappers.<SessionEntity>lambdaUpdate()
                .eq(SessionEntity::getId, s.getId())
                .eq(SessionEntity::getVersion, previousVersion)
                .set(SessionEntity::getAccessTokenHash, s.getAccessTokenHash())
                .set(SessionEntity::getRefreshTokenHash, s.getRefreshTokenHash())
                .set(SessionEntity::getPreviousRefreshTokenHash, s.getPreviousRefreshTokenHash())
                .set(SessionEntity::getAuthorizationVersion, s.getAuthorizationVersion())
                .set(SessionEntity::getAccessExpiresAt, s.getAccessExpiresAt())
                .set(SessionEntity::getRefreshExpiresAt, s.getRefreshExpiresAt())
                .set(SessionEntity::getRevokedAt, s.getRevokedAt())
                .set(SessionEntity::getRevokeReason, s.getRevokeReason())
                .set(SessionEntity::getVersion, s.getVersion())
                .set(SessionEntity::getUpdatedAt, s.getUpdatedAt()))
        == 1;
  }

  @Override
  public void revokeAllSessions(long userId, String reason, LocalDateTime now) {
    sessionMapper.update(
        null,
        Wrappers.<SessionEntity>lambdaUpdate()
            .eq(SessionEntity::getUserId, userId)
            .isNull(SessionEntity::getRevokedAt)
            .set(SessionEntity::getRevokedAt, now)
            .set(SessionEntity::getRevokeReason, reason)
            .set(SessionEntity::getUpdatedAt, now)
            .setSql("version = version + 1"));
  }

  @Override
  public void insertConsent(
      long id,
      long userId,
      ConsentPurpose purpose,
      String documentVersion,
      boolean granted,
      String evidenceReference,
      LocalDateTime recordedAt) {
    UserConsentEntity e = new UserConsentEntity();
    e.setId(id);
    e.setUserId(userId);
    e.setPurpose(purpose.name());
    e.setDocumentVersion(documentVersion);
    e.setGranted(granted);
    e.setEvidenceReference(evidenceReference);
    e.setRecordedAt(recordedAt);
    consentMapper.insert(e);
  }

  @Override
  public Optional<PrivacyRequest> findPrivacyRequest(long requestId) {
    return Optional.ofNullable(privacyMapper.selectById(requestId)).map(this::toDomain);
  }

  @Override
  public Optional<PrivacyRequest> findPrivacyRequestByKey(String key) {
    return Optional.ofNullable(
            privacyMapper.selectOne(
                Wrappers.<PrivacyRequestEntity>lambdaQuery()
                    .eq(PrivacyRequestEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public void insertPrivacyRequest(PrivacyRequest r) {
    privacyMapper.insert(toEntity(r));
  }

  @Override
  public boolean updatePrivacyRequest(PrivacyRequest r, long previousVersion) {
    return privacyMapper.update(
            null,
            Wrappers.<PrivacyRequestEntity>lambdaUpdate()
                .eq(PrivacyRequestEntity::getId, r.getId())
                .eq(PrivacyRequestEntity::getVersion, previousVersion)
                .set(PrivacyRequestEntity::getStatus, r.getStatus().name())
                .set(PrivacyRequestEntity::getProgress, r.getProgress())
                .set(PrivacyRequestEntity::getResultReference, r.getResultReference())
                .set(PrivacyRequestEntity::getLastError, r.getLastError())
                .set(PrivacyRequestEntity::getVersion, r.getVersion())
                .set(PrivacyRequestEntity::getUpdatedAt, r.getUpdatedAt()))
        == 1;
  }

  @Override
  public int logicallyDeletePrivateData(long requestId, long userId, boolean closeAccount) {
    if (!closeAccount) {
      return 0;
    }
    int affectedRows = sessionMapper.tombstoneByUser(userId, requestId);
    affectedRows += loginMapper.tombstoneByUser(userId, requestId);
    affectedRows +=
        deviceMapper.delete(
            Wrappers.<DeviceEntity>lambdaQuery().eq(DeviceEntity::getUserId, userId));
    affectedRows +=
        consentMapper.delete(
            Wrappers.<UserConsentEntity>lambdaQuery().eq(UserConsentEntity::getUserId, userId));
    affectedRows +=
        ageVerificationMapper.delete(
            Wrappers.<AgeVerificationEntity>lambdaQuery()
                .eq(AgeVerificationEntity::getUserId, userId));
    affectedRows +=
        privacyExportMapper.delete(
            Wrappers.<PrivacyExportEntity>lambdaQuery()
                .eq(PrivacyExportEntity::getUserId, userId));
    return affectedRows;
  }

  @Override
  public void recordDeletionAudit(
      long requestId,
      long userId,
      String moduleName,
      int affectedRows,
      LocalDateTime completedAt) {
    Long existing =
        deletionAuditMapper.selectCount(
            Wrappers.<PrivacyDeletionAuditEntity>lambdaQuery()
                .eq(PrivacyDeletionAuditEntity::getRequestId, requestId)
                .eq(PrivacyDeletionAuditEntity::getModuleName, moduleName));
    if (existing != null && existing > 0) {
      return;
    }
    PrivacyDeletionAuditEntity entity = new PrivacyDeletionAuditEntity();
    entity.setId(idGenerator.nextId());
    entity.setRequestId(requestId);
    entity.setUserId(userId);
    entity.setModuleName(moduleName);
    entity.setAffectedRows(Math.max(affectedRows, 0));
    entity.setCompletedAt(completedAt);
    try {
      deletionAuditMapper.insert(entity);
    } catch (DuplicateKeyException ignored) {
      // 唯一键保证同一请求的模块审计只记录一次，重试无需重复写入。
    }
  }

  @Override
  public Optional<Integer> findDeletionAuditAffectedRows(long requestId, String moduleName) {
    PrivacyDeletionAuditEntity entity =
        deletionAuditMapper.selectOne(
            Wrappers.<PrivacyDeletionAuditEntity>lambdaQuery()
                .select(PrivacyDeletionAuditEntity::getAffectedRows)
                .eq(PrivacyDeletionAuditEntity::getRequestId, requestId)
                .eq(PrivacyDeletionAuditEntity::getModuleName, moduleName)
                .last("LIMIT 1"));
    return entity == null ? Optional.empty() : Optional.of(entity.getAffectedRows());
  }

  @Override
  public void minimizeClosedPrivacyRequest(long requestId) {
    privacyMapper.update(
        null,
        Wrappers.<PrivacyRequestEntity>lambdaUpdate()
            .eq(PrivacyRequestEntity::getId, requestId)
            .eq(PrivacyRequestEntity::getType, PrivacyRequestType.CLOSE_ACCOUNT.name())
            .eq(PrivacyRequestEntity::getStatus, PrivacyRequestStatus.COMPLETED.name())
            .set(PrivacyRequestEntity::getRequestKey, "closed:" + requestId)
            .set(PrivacyRequestEntity::getRequestDigest, "0".repeat(64))
            .set(PrivacyRequestEntity::getScopeJson, "{}")
            .set(PrivacyRequestEntity::getResultReference, null)
            .set(PrivacyRequestEntity::getLastError, null));
  }

  private DeviceSession toDomain(SessionEntity e) {
    return DeviceSession.rehydrate(
        e.getId(),
        e.getFamilyId(),
        e.getUserId(),
        e.getDeviceId(),
        e.getAccessTokenHash(),
        e.getRefreshTokenHash(),
        e.getPreviousRefreshTokenHash(),
        e.getAuthorizationVersion(),
        e.getAccessExpiresAt(),
        e.getRefreshExpiresAt(),
        e.getRevokedAt(),
        e.getRevokeReason(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private SessionEntity toEntity(DeviceSession s) {
    SessionEntity e = new SessionEntity();
    e.setId(s.getId());
    e.setFamilyId(s.getFamilyId());
    e.setUserId(s.getUserId());
    e.setDeviceId(s.getDeviceId());
    e.setAccessTokenHash(s.getAccessTokenHash());
    e.setRefreshTokenHash(s.getRefreshTokenHash());
    e.setPreviousRefreshTokenHash(s.getPreviousRefreshTokenHash());
    e.setAuthorizationVersion(s.getAuthorizationVersion());
    e.setAccessExpiresAt(s.getAccessExpiresAt());
    e.setRefreshExpiresAt(s.getRefreshExpiresAt());
    e.setRevokedAt(s.getRevokedAt());
    e.setRevokeReason(s.getRevokeReason());
    e.setVersion(s.getVersion());
    e.setCreatedAt(s.getCreatedAt());
    e.setUpdatedAt(s.getUpdatedAt());
    return e;
  }

  private PrivacyRequest toDomain(PrivacyRequestEntity e) {
    return PrivacyRequest.rehydrate(
        e.getId(),
        e.getRequestKey(),
        e.getRequestDigest(),
        e.getUserId(),
        PrivacyRequestType.valueOf(e.getType()),
        e.getScopeJson(),
        PrivacyRequestStatus.valueOf(e.getStatus()),
        e.getProgress(),
        e.getDeadline(),
        e.getResultReference(),
        e.getLastError(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private PrivacyRequestEntity toEntity(PrivacyRequest r) {
    PrivacyRequestEntity e = new PrivacyRequestEntity();
    e.setId(r.getId());
    e.setRequestKey(r.getRequestKey());
    e.setRequestDigest(r.getRequestDigest());
    e.setUserId(r.getUserId());
    e.setType(r.getType().name());
    e.setScopeJson(r.getScopeJson());
    e.setStatus(r.getStatus().name());
    e.setProgress(r.getProgress());
    e.setDeadline(r.getDeadline());
    e.setResultReference(r.getResultReference());
    e.setLastError(r.getLastError());
    e.setVersion(r.getVersion());
    e.setCreatedAt(r.getCreatedAt());
    e.setUpdatedAt(r.getUpdatedAt());
    return e;
  }
}
