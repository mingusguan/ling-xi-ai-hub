package com.lingxi.engagement.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;

/** 系统日历授权聚合，凭据只保存安全引用。 */
public class CalendarBinding {
  public enum Status {
    ACTIVE,
    ERROR,
    REVOKED
  }

  private final long id;
  private final String requestKey;
  private final long userId;
  private final String provider;
  private String credentialReference;
  private boolean deleteCreatedEvents;
  private Status status;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private CalendarBinding(
      long id,
      String key,
      long user,
      String provider,
      String credential,
      boolean deleteCreated,
      Status status,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    this.id = id;
    requestKey = key;
    userId = user;
    this.provider = provider;
    credentialReference = credential;
    deleteCreatedEvents = deleteCreated;
    this.status = status;
    this.version = version;
    createdAt = created;
    updatedAt = updated;
  }

  public static CalendarBinding activate(
      long id, String key, long user, String provider, String credential, LocalDateTime now) {
    if (id <= 0 || user <= 0 || blank(key) || blank(provider) || blank(credential))
      throw new BusinessException("ENG_INVALID_CALENDAR_BINDING", "日历绑定参数不完整");
    return new CalendarBinding(
        id, key, user, provider, credential, false, Status.ACTIVE, 0, now, now);
  }

  public static CalendarBinding rehydrate(
      long id,
      String key,
      long user,
      String provider,
      String credential,
      boolean deleteCreated,
      Status status,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    return new CalendarBinding(
        id, key, user, provider, credential, deleteCreated, status, version, created, updated);
  }

  public void reactivate(String credential, long expected, LocalDateTime now) {
    check(expected);
    if (blank(credential))
      throw new BusinessException("ENG_INVALID_CALENDAR_BINDING", "日历凭据引用不能为空");
    credentialReference = credential;
    deleteCreatedEvents = false;
    status = Status.ACTIVE;
    version++;
    updatedAt = now;
  }

  public void revoke(long expected, boolean deleteCreated, LocalDateTime now) {
    check(expected);
    if (status != Status.REVOKED) {
      deleteCreatedEvents = deleteCreated;
      status = Status.REVOKED;
      version++;
      updatedAt = now;
    }
  }

  private void check(long e) {
    if (version != e) throw new BusinessException("ENG_CALENDAR_CONFLICT", "日历授权已变化");
  }

  private static boolean blank(String v) {
    return v == null || v.isBlank();
  }

  public long getId() {
    return id;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public long getUserId() {
    return userId;
  }

  public String getProvider() {
    return provider;
  }

  public String getCredentialReference() {
    return credentialReference;
  }

  public boolean isDeleteCreatedEvents() {
    return deleteCreatedEvents;
  }

  public Status getStatus() {
    return status;
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
