package com.lingxi.identity.domain;

import com.lingxi.identity.api.AccountStatus;
import com.lingxi.identity.api.AgeBand;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** 用户账号聚合，负责年龄模式与授权版本的业务不变量。 */
public class User {
  private final long id;
  private final String publicId;
  private final String registrationKey;
  private final String registrationDigest;
  private AgeBand ageBand;
  private AccountStatus status;
  private AccountStatus closingPreviousStatus;
  private final LocalDate adultTransitionDate;
  private final String timezone;
  private long authorizationVersion;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private User(
      long id,
      String publicId,
      String registrationKey,
      String registrationDigest,
      AgeBand ageBand,
      AccountStatus status,
      AccountStatus closingPreviousStatus,
      LocalDate adultTransitionDate,
      String timezone,
      long authorizationVersion,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.publicId = Objects.requireNonNull(publicId);
    this.registrationKey = Objects.requireNonNull(registrationKey);
    this.registrationDigest = Objects.requireNonNull(registrationDigest);
    this.ageBand = Objects.requireNonNull(ageBand);
    this.status = Objects.requireNonNull(status);
    this.closingPreviousStatus = closingPreviousStatus;
    this.adultTransitionDate = Objects.requireNonNull(adultTransitionDate);
    this.timezone = Objects.requireNonNull(timezone);
    this.authorizationVersion = authorizationVersion;
    this.version = version;
    this.createdAt = Objects.requireNonNull(createdAt);
    this.updatedAt = Objects.requireNonNull(updatedAt);
  }

  public static User register(
      long id,
      String publicId,
      String registrationKey,
      String registrationDigest,
      AgeAssessment assessment,
      String timezone,
      LocalDateTime now) {
    if (assessment.ageBand() == AgeBand.UNDER_14) {
      throw new BusinessException("IDENTITY_UNDER_MINIMUM_AGE", "仅向年满14周岁的用户提供服务");
    }
    AccountStatus initial =
        assessment.ageBand() == AgeBand.TEEN
            ? AccountStatus.PENDING_GUARDIAN
            : AccountStatus.ACTIVE_ADULT;
    return new User(
        id,
        publicId,
        registrationKey,
        registrationDigest,
        assessment.ageBand(),
        initial,
        null,
        assessment.adultTransitionDate(),
        timezone,
        1,
        0,
        now,
        now);
  }

  public static User rehydrate(
      long id,
      String publicId,
      String registrationKey,
      String registrationDigest,
      AgeBand ageBand,
      AccountStatus status,
      AccountStatus closingPreviousStatus,
      LocalDate adultTransitionDate,
      String timezone,
      long authorizationVersion,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new User(
        id,
        publicId,
        registrationKey,
        registrationDigest,
        ageBand,
        status,
        closingPreviousStatus,
        adultTransitionDate,
        timezone,
        authorizationVersion,
        version,
        createdAt,
        updatedAt);
  }

  public void activateTeenAccount(LocalDateTime now) {
    if (ageBand != AgeBand.TEEN
        || (status != AccountStatus.PENDING_GUARDIAN && status != AccountStatus.RESTRICTED)) {
      throw new BusinessException("IDENTITY_INVALID_TEEN_ACTIVATION", "当前账号不能激活青少年模式");
    }
    status = AccountStatus.ACTIVE_TEEN;
    advanceAuthorization(now);
  }

  public void restrictForGuardianLoss(LocalDateTime now) {
    if (ageBand != AgeBand.TEEN
        || status == AccountStatus.CLOSED
        || status == AccountStatus.CLOSING) {
      throw new BusinessException("IDENTITY_INVALID_GUARDIAN_RESTRICTION", "当前账号不能执行监护限制");
    }
    status = AccountStatus.RESTRICTED;
    advanceAuthorization(now);
  }

  public void beginAdultTransition(LocalDate today, LocalDateTime now) {
    if (ageBand != AgeBand.TEEN || today.isBefore(adultTransitionDate)) {
      throw new BusinessException("IDENTITY_ADULT_TRANSITION_NOT_READY", "账号尚未达到成人迁移条件");
    }
    status = AccountStatus.AGE_TRANSITION;
    advanceAuthorization(now);
  }

  public void completeAdultTransition(LocalDateTime now) {
    if (status != AccountStatus.AGE_TRANSITION) {
      throw new BusinessException("IDENTITY_INVALID_ADULT_TRANSITION", "当前账号不在成人迁移流程中");
    }
    ageBand = AgeBand.ADULT;
    status = AccountStatus.ACTIVE_ADULT;
    advanceAuthorization(now);
  }

  public void beginClosing(LocalDateTime now) {
    if (status == AccountStatus.CLOSED) {
      throw new BusinessException("IDENTITY_ACCOUNT_CLOSED", "账号已关闭");
    }
    if (status == AccountStatus.CLOSING) {
      return;
    }
    closingPreviousStatus = status;
    status = AccountStatus.CLOSING;
    advanceAuthorization(now);
  }

  public void cancelClosing(LocalDateTime now) {
    if (status != AccountStatus.CLOSING || closingPreviousStatus == null) {
      throw new BusinessException("IDENTITY_NOT_CLOSING", "账号未处于可撤销的注销冷静期");
    }
    status = closingPreviousStatus;
    closingPreviousStatus = null;
    advanceAuthorization(now);
  }

  public void completeClosing(LocalDateTime now) {
    if (status != AccountStatus.CLOSING) {
      throw new BusinessException("IDENTITY_NOT_CLOSING", "账号未进入注销流程");
    }
    status = AccountStatus.CLOSED;
    closingPreviousStatus = null;
    advanceAuthorization(now);
  }

  private void advanceAuthorization(LocalDateTime now) {
    authorizationVersion++;
    version++;
    updatedAt = now;
  }

  public boolean canUseCoreFeatures() {
    return status == AccountStatus.ACTIVE_TEEN || status == AccountStatus.ACTIVE_ADULT;
  }

  public long getId() {
    return id;
  }

  public String getPublicId() {
    return publicId;
  }

  public String getRegistrationKey() {
    return registrationKey;
  }

  public String getRegistrationDigest() {
    return registrationDigest;
  }

  public AgeBand getAgeBand() {
    return ageBand;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public AccountStatus getClosingPreviousStatus() {
    return closingPreviousStatus;
  }

  public LocalDate getAdultTransitionDate() {
    return adultTransitionDate;
  }

  public String getTimezone() {
    return timezone;
  }

  public long getAuthorizationVersion() {
    return authorizationVersion;
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
