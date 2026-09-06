package com.lingxi.relationship.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.util.Objects;

/** 监护关系聚合。 */
public class GuardianRelation {
  private final long id;
  private final String requestKey;
  private final String requestDigest;
  private final long teenUserId;
  private Long guardianUserId;
  private final String invitationTokenHash;
  private final String requestedPermissionsJson;
  private GuardianRelationStatus status;
  private final LocalDateTime expiresAt;
  private LocalDateTime verifiedAt;
  private LocalDateTime revokedAt;
  private String revokeReason;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private GuardianRelation(
      long id,
      String requestKey,
      String requestDigest,
      long teenUserId,
      Long guardianUserId,
      String tokenHash,
      String permissionsJson,
      GuardianRelationStatus status,
      LocalDateTime expiresAt,
      LocalDateTime verifiedAt,
      LocalDateTime revokedAt,
      String revokeReason,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.requestKey = Objects.requireNonNull(requestKey);
    this.requestDigest = Objects.requireNonNull(requestDigest);
    this.teenUserId = teenUserId;
    this.guardianUserId = guardianUserId;
    this.invitationTokenHash = Objects.requireNonNull(tokenHash);
    this.requestedPermissionsJson = Objects.requireNonNull(permissionsJson);
    this.status = Objects.requireNonNull(status);
    this.expiresAt = Objects.requireNonNull(expiresAt);
    this.verifiedAt = verifiedAt;
    this.revokedAt = revokedAt;
    this.revokeReason = revokeReason;
    this.version = version;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static GuardianRelation invite(
      long id,
      String requestKey,
      String digest,
      long teenUserId,
      String tokenHash,
      String permissionsJson,
      LocalDateTime expiresAt,
      LocalDateTime now) {
    return new GuardianRelation(
        id,
        requestKey,
        digest,
        teenUserId,
        null,
        tokenHash,
        permissionsJson,
        GuardianRelationStatus.INVITED,
        expiresAt,
        null,
        null,
        null,
        0,
        now,
        now);
  }

  public static GuardianRelation rehydrate(
      long id,
      String requestKey,
      String digest,
      long teenUserId,
      Long guardianUserId,
      String tokenHash,
      String permissionsJson,
      GuardianRelationStatus status,
      LocalDateTime expiresAt,
      LocalDateTime verifiedAt,
      LocalDateTime revokedAt,
      String revokeReason,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new GuardianRelation(
        id,
        requestKey,
        digest,
        teenUserId,
        guardianUserId,
        tokenHash,
        permissionsJson,
        status,
        expiresAt,
        verifiedAt,
        revokedAt,
        revokeReason,
        version,
        createdAt,
        updatedAt);
  }

  public void accept(long guardianUserId, LocalDateTime now) {
    if (status != GuardianRelationStatus.INVITED || !expiresAt.isAfter(now)) {
      throw new BusinessException("GUARDIAN_INVITATION_INVALID", "监护邀请无效或已过期");
    }
    if (guardianUserId == teenUserId) {
      throw new BusinessException("GUARDIAN_SELF_BINDING", "不能绑定自己为监护人");
    }
    this.guardianUserId = guardianUserId;
    status = GuardianRelationStatus.ACTIVE;
    verifiedAt = now;
    version++;
    updatedAt = now;
  }

  public void revoke(long operatorUserId, String reason, LocalDateTime now) {
    if (status != GuardianRelationStatus.ACTIVE) {
      throw new BusinessException("GUARDIAN_NOT_ACTIVE", "监护关系不是有效状态");
    }
    if (operatorUserId != teenUserId && !Objects.equals(operatorUserId, guardianUserId)) {
      throw new BusinessException("GUARDIAN_RELATION_NOT_FOUND", "监护关系不存在");
    }
    status = GuardianRelationStatus.REVOKED;
    revokedAt = now;
    revokeReason = reason;
    version++;
    updatedAt = now;
  }

  public void assertParticipant(long userId) {
    if (userId != teenUserId && !Objects.equals(userId, guardianUserId)) {
      throw new BusinessException("GUARDIAN_RELATION_NOT_FOUND", "监护关系不存在");
    }
  }

  public long getId() {
    return id;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public String getRequestDigest() {
    return requestDigest;
  }

  public long getTeenUserId() {
    return teenUserId;
  }

  public Long getGuardianUserId() {
    return guardianUserId;
  }

  public String getInvitationTokenHash() {
    return invitationTokenHash;
  }

  public String getRequestedPermissionsJson() {
    return requestedPermissionsJson;
  }

  public GuardianRelationStatus getStatus() {
    return status;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public LocalDateTime getRevokedAt() {
    return revokedAt;
  }

  public String getRevokeReason() {
    return revokeReason;
  }

  public long getVersion() {
    return version;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
