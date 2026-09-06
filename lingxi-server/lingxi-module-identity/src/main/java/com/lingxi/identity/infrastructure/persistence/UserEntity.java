package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 用户账号持久化对象。 */
@TableName("id_user")
public class UserEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long id;
  private String publicId;
  private String registrationKey;
  private String registrationDigest;
  private String ageBand;
  private String status;
  private String closingPreviousStatus;
  private LocalDate adultTransitionDate;
  private String timezone;
  private Long authorizationVersion;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public String getRegistrationKey() {
    return registrationKey;
  }

  public void setRegistrationKey(String registrationKey) {
    this.registrationKey = registrationKey;
  }

  public String getRegistrationDigest() {
    return registrationDigest;
  }

  public void setRegistrationDigest(String registrationDigest) {
    this.registrationDigest = registrationDigest;
  }

  public String getAgeBand() {
    return ageBand;
  }

  public void setAgeBand(String ageBand) {
    this.ageBand = ageBand;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getClosingPreviousStatus() {
    return closingPreviousStatus;
  }

  public void setClosingPreviousStatus(String closingPreviousStatus) {
    this.closingPreviousStatus = closingPreviousStatus;
  }

  public LocalDate getAdultTransitionDate() {
    return adultTransitionDate;
  }

  public void setAdultTransitionDate(LocalDate value) {
    this.adultTransitionDate = value;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public Long getAuthorizationVersion() {
    return authorizationVersion;
  }

  public void setAuthorizationVersion(Long value) {
    this.authorizationVersion = value;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
