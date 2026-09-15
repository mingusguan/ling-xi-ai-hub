package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
  // 新手引导基础画像（PRD ONB-01）：全部可空，NULL 表示用户没有填写过该项。
  private String nickname;
  private LocalTime sleepTime;
  private LocalTime wakeTime;
  private Integer weeklyAvailableMinutes;
  private LocalTime remindWindowStart;
  private LocalTime remindWindowEnd;
  private LocalTime quietHoursStart;
  private LocalTime quietHoursEnd;
  private String communicationStyle;
  private String proactivityLevel;
  private String commonBlockersJson;
  private LocalDateTime onboardingCompletedAt;

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

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public LocalTime getSleepTime() {
    return sleepTime;
  }

  public void setSleepTime(LocalTime value) {
    this.sleepTime = value;
  }

  public LocalTime getWakeTime() {
    return wakeTime;
  }

  public void setWakeTime(LocalTime value) {
    this.wakeTime = value;
  }

  public Integer getWeeklyAvailableMinutes() {
    return weeklyAvailableMinutes;
  }

  public void setWeeklyAvailableMinutes(Integer value) {
    this.weeklyAvailableMinutes = value;
  }

  public LocalTime getRemindWindowStart() {
    return remindWindowStart;
  }

  public void setRemindWindowStart(LocalTime value) {
    this.remindWindowStart = value;
  }

  public LocalTime getRemindWindowEnd() {
    return remindWindowEnd;
  }

  public void setRemindWindowEnd(LocalTime value) {
    this.remindWindowEnd = value;
  }

  public LocalTime getQuietHoursStart() {
    return quietHoursStart;
  }

  public void setQuietHoursStart(LocalTime value) {
    this.quietHoursStart = value;
  }

  public LocalTime getQuietHoursEnd() {
    return quietHoursEnd;
  }

  public void setQuietHoursEnd(LocalTime value) {
    this.quietHoursEnd = value;
  }

  public String getCommunicationStyle() {
    return communicationStyle;
  }

  public void setCommunicationStyle(String value) {
    this.communicationStyle = value;
  }

  public String getProactivityLevel() {
    return proactivityLevel;
  }

  public void setProactivityLevel(String value) {
    this.proactivityLevel = value;
  }

  public String getCommonBlockersJson() {
    return commonBlockersJson;
  }

  public void setCommonBlockersJson(String value) {
    this.commonBlockersJson = value;
  }

  public LocalDateTime getOnboardingCompletedAt() {
    return onboardingCompletedAt;
  }

  public void setOnboardingCompletedAt(LocalDateTime value) {
    this.onboardingCompletedAt = value;
  }
}
