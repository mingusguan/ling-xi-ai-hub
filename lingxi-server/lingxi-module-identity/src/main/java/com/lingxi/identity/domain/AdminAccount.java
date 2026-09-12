package com.lingxi.identity.domain;

import com.lingxi.kernel.BusinessException;
import java.time.Duration;
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

  /** 登录失败达到该次数即锁定账号。 */
  private static final int MAX_FAILED_ATTEMPTS = 5;
  /** 锁定时长（分钟）。 */
  private static final long LOCK_MINUTES = 15;

  public void ensureLoginAllowed(LocalDateTime now) {
    if (!"ACTIVE".equals(status)) throw new BusinessException("ADMIN_ACCOUNT_DISABLED", "管理员账号已停用");
    if (lockedUntil != null && lockedUntil.isAfter(now)) {
      throw new BusinessException(
          "ADMIN_ACCOUNT_LOCKED",
          "连续 "
              + MAX_FAILED_ATTEMPTS
              + " 次登录失败，账号已临时锁定，请 "
              + remainingMinutes(now)
              + " 分钟后再试");
    }
  }

  /** 锁定剩余分钟数，向上取整：正好 15 分钟显示 15，剩余不足 1 分钟显示 1，不出现 0。 */
  private long remainingMinutes(LocalDateTime now) {
    long seconds = Math.max(0, Duration.between(now, lockedUntil).getSeconds());
    return Math.max(1, (seconds + 59) / 60);
  }

  public void loginFailed(LocalDateTime now) {
    failedAttempts++;
    if (failedAttempts >= MAX_FAILED_ATTEMPTS) lockedUntil = now.plusMinutes(LOCK_MINUTES);
    version++;
    updatedAt = now;
  }

  /** 本次失败后仍可尝试的次数；已锁定则返回 0。 */
  public int remainingAttempts() {
    return lockedUntil != null ? 0 : Math.max(0, MAX_FAILED_ATTEMPTS - failedAttempts);
  }

  /** 密码错误提示：带剩余次数，避免用户在不知情的情况下把账号打到锁定。 */
  public String loginFailedMessage() {
    int remaining = remainingAttempts();
    if (remaining <= 0) {
      return "账号或密码错误，且失败次数已达上限，账号已临时锁定 " + LOCK_MINUTES + " 分钟";
    }
    return "账号或密码错误（还可尝试 " + remaining + " 次，之后账号将临时锁定 " + LOCK_MINUTES + " 分钟）";
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
