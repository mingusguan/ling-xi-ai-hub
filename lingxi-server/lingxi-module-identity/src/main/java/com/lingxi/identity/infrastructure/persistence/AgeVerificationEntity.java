package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 年龄验证持久化对象。 */
@TableName("id_age_verification")
public class AgeVerificationEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String method;
  private String evidenceReference;
  private LocalDate verifiedBirthDate;
  private String result;
  private LocalDateTime verifiedAt;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getEvidenceReference() {
    return evidenceReference;
  }

  public void setEvidenceReference(String value) {
    this.evidenceReference = value;
  }

  public LocalDate getVerifiedBirthDate() {
    return verifiedBirthDate;
  }

  public void setVerifiedBirthDate(LocalDate value) {
    this.verifiedBirthDate = value;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
