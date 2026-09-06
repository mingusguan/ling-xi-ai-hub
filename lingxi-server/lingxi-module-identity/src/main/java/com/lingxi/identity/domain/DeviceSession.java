package com.lingxi.identity.domain;

import com.lingxi.kernel.BusinessException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/** 设备会话聚合，负责刷新令牌轮换与重放防护。 */
public class DeviceSession {
  private final long id;
  private final String familyId;
  private final long userId;
  private final String deviceId;
  private String accessTokenHash;
  private String refreshTokenHash;
  private String previousRefreshTokenHash;
  private long authorizationVersion;
  private LocalDateTime accessExpiresAt;
  private LocalDateTime refreshExpiresAt;
  private LocalDateTime revokedAt;
  private String revokeReason;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private DeviceSession(
      long id,
      String familyId,
      long userId,
      String deviceId,
      String accessTokenHash,
      String refreshTokenHash,
      String previousRefreshTokenHash,
      long authorizationVersion,
      LocalDateTime accessExpiresAt,
      LocalDateTime refreshExpiresAt,
      LocalDateTime revokedAt,
      String revokeReason,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.familyId = Objects.requireNonNull(familyId);
    this.userId = userId;
    this.deviceId = Objects.requireNonNull(deviceId);
    this.accessTokenHash = Objects.requireNonNull(accessTokenHash);
    this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash);
    this.previousRefreshTokenHash = previousRefreshTokenHash;
    this.authorizationVersion = authorizationVersion;
    this.accessExpiresAt = Objects.requireNonNull(accessExpiresAt);
    this.refreshExpiresAt = Objects.requireNonNull(refreshExpiresAt);
    this.revokedAt = revokedAt;
    this.revokeReason = revokeReason;
    this.version = version;
    this.createdAt = Objects.requireNonNull(createdAt);
    this.updatedAt = Objects.requireNonNull(updatedAt);
  }

  public static DeviceSession create(
      long id,
      String familyId,
      long userId,
      String deviceId,
      String accessHash,
      String refreshHash,
      long authorizationVersion,
      LocalDateTime accessExpiresAt,
      LocalDateTime refreshExpiresAt,
      LocalDateTime now) {
    return new DeviceSession(
        id,
        familyId,
        userId,
        deviceId,
        accessHash,
        refreshHash,
        null,
        authorizationVersion,
        accessExpiresAt,
        refreshExpiresAt,
        null,
        null,
        0,
        now,
        now);
  }

  public static DeviceSession rehydrate(
      long id,
      String familyId,
      long userId,
      String deviceId,
      String accessHash,
      String refreshHash,
      String previousRefreshHash,
      long authorizationVersion,
      LocalDateTime accessExpiresAt,
      LocalDateTime refreshExpiresAt,
      LocalDateTime revokedAt,
      String revokeReason,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new DeviceSession(
        id,
        familyId,
        userId,
        deviceId,
        accessHash,
        refreshHash,
        previousRefreshHash,
        authorizationVersion,
        accessExpiresAt,
        refreshExpiresAt,
        revokedAt,
        revokeReason,
        version,
        createdAt,
        updatedAt);
  }

  public void rotate(
      String presentedRefreshHash,
      String newAccessHash,
      String newRefreshHash,
      long currentAuthorizationVersion,
      LocalDateTime newAccessExpiresAt,
      LocalDateTime newRefreshExpiresAt,
      LocalDateTime now) {
    ensureActive(now);
    if (!refreshTokenHash.equals(presentedRefreshHash)) {
      revoke("REFRESH_TOKEN_REPLAY", now);
      throw new BusinessException("AUTH_REFRESH_REPLAY", "检测到刷新令牌重放，会话族已撤销");
    }
    previousRefreshTokenHash = refreshTokenHash;
    refreshTokenHash = newRefreshHash;
    accessTokenHash = newAccessHash;
    authorizationVersion = currentAuthorizationVersion;
    accessExpiresAt = newAccessExpiresAt;
    refreshExpiresAt = newRefreshExpiresAt;
    version++;
    updatedAt = now;
  }

  public void ensureAccessAllowed(long currentAuthorizationVersion, LocalDateTime now) {
    ensureActive(now);
    if (authorizationVersion != currentAuthorizationVersion || !accessExpiresAt.isAfter(now)) {
      throw new BusinessException("AUTH_SESSION_STALE", "会话授权已变化，请重新登录");
    }
  }

  /** 刷新令牌不会刷新认证时刻，避免长期会话绕过敏感操作的重新认证。 */
  public boolean isRecentlyAuthenticated(LocalDateTime now, Duration window) {
    if (window == null || window.isZero() || window.isNegative()) {
      return false;
    }
    return !createdAt.isBefore(now.minus(window));
  }

  public void revoke(String reason, LocalDateTime now) {
    if (revokedAt == null) {
      revokedAt = now;
      revokeReason = reason;
      version++;
      updatedAt = now;
    }
  }

  private void ensureActive(LocalDateTime now) {
    if (revokedAt != null || !refreshExpiresAt.isAfter(now)) {
      throw new BusinessException("AUTH_SESSION_REVOKED", "会话已失效");
    }
  }

  public long getId() {
    return id;
  }

  public String getFamilyId() {
    return familyId;
  }

  public long getUserId() {
    return userId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public String getAccessTokenHash() {
    return accessTokenHash;
  }

  public String getRefreshTokenHash() {
    return refreshTokenHash;
  }

  public String getPreviousRefreshTokenHash() {
    return previousRefreshTokenHash;
  }

  public long getAuthorizationVersion() {
    return authorizationVersion;
  }

  public LocalDateTime getAccessExpiresAt() {
    return accessExpiresAt;
  }

  public LocalDateTime getRefreshExpiresAt() {
    return refreshExpiresAt;
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
