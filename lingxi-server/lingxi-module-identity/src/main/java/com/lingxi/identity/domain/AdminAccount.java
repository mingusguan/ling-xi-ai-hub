package com.lingxi.identity.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.util.Objects;

/** 管理员账号聚合，负责锁定、登录成功和密码事实。 */
public class AdminAccount {
  private final long id;
  private final String username;
  private final String displayName;
  private String passwordHash;
  private String status;
  private int failedAttempts;
  private LocalDateTime lockedUntil;
  private LocalDateTime passwordChangedAt;
  private LocalDateTime lastLoginAt;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private AdminAccount(
      long id,
      String username,
      String displayName,
      String passwordHash,
      String status,
      int failedAttempts,
      LocalDateTime lockedUntil,
      LocalDateTime passwordChangedAt,
      LocalDateTime lastLoginAt,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.username = Objects.requireNonNull(username);
    this.displayName = Objects.requireNonNull(displayName);
    this.passwordHash = Objects.requireNonNull(passwordHash);
    this.status = Objects.requireNonNull(status);
    this.failedAttempts = failedAttempts;
    this.lockedUntil = lockedUntil;
    this.passwordChangedAt = passwordChangedAt;
    this.lastLoginAt = lastLoginAt;
    this.version = version;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static AdminAccount rehydrate(
      long id, String username, String displayName, String passwordHash, String status,
      int failedAttempts, LocalDateTime lockedUntil, LocalDateTime passwordChangedAt,
      LocalDateTime lastLoginAt, long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
    return new AdminAccount(id, username, displayName, passwordHash, status, failedAttempts,
        lockedUntil, passwordChangedAt, lastLoginAt, version, createdAt, updatedAt);
  }

  public static AdminAccount create(long id, String username, String displayName,
      String passwordHash, LocalDateTime now) {
    if (id <= 0 || username == null || !username.matches("[A-Za-z0-9._-]{3,64}")
        || displayName == null || displayName.isBlank() || passwordHash == null
        || passwordHash.isBlank()) {
      throw new BusinessException("ADMIN_ACCOUNT_INVALID", "管理员账号参数不合法");
    }
    return new AdminAccount(id, username, displayName.trim(), passwordHash, "ACTIVE", 0,
        null, now, null, 0, now, now);
  }

  public void changeStatus(String targetStatus, LocalDateTime now) {
    if (!"ACTIVE".equals(targetStatus) && !"DISABLED".equals(targetStatus)) {
      throw new BusinessException("ADMIN_ACCOUNT_STATUS_INVALID", "管理员状态不合法");
    }
    status = targetStatus;
    version++;
    updatedAt = now;
  }

  public void changePassword(String encodedPassword, LocalDateTime now) {
    if (encodedPassword == null || encodedPassword.isBlank()) {
      throw new BusinessException("ADMIN_PASSWORD_INVALID", "管理员密码不合法");
    }
    passwordHash = encodedPassword;
    passwordChangedAt = now;
    failedAttempts = 0;
    lockedUntil = null;
    version++;
    updatedAt = now;
  }

  public void ensureLoginAllowed(LocalDateTime now) {
    if (!"ACTIVE".equals(status)) throw new BusinessException("ADMIN_ACCOUNT_DISABLED", "管理员账号已停用");
    if (lockedUntil != null && lockedUntil.isAfter(now)) {
      throw new BusinessException("ADMIN_ACCOUNT_LOCKED", "登录失败次数过多，请稍后再试");
    }
  }

  public void loginFailed(LocalDateTime now) {
    failedAttempts++;
    if (failedAttempts >= 5) lockedUntil = now.plusMinutes(15);
    version++;
    updatedAt = now;
  }

  public void loginSucceeded(LocalDateTime now) {
    failedAttempts = 0;
    lockedUntil = null;
    lastLoginAt = now;
    version++;
    updatedAt = now;
  }

  public long getId() { return id; }
  public String getUsername() { return username; }
  public String getDisplayName() { return displayName; }
  public String getPasswordHash() { return passwordHash; }
  public String getStatus() { return status; }
  public int getFailedAttempts() { return failedAttempts; }
  public LocalDateTime getLockedUntil() { return lockedUntil; }
  public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
  public LocalDateTime getLastLoginAt() { return lastLoginAt; }
  public long getVersion() { return version; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}
