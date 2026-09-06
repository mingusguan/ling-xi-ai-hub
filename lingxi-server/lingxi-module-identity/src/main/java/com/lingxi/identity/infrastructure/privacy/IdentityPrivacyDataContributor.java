package com.lingxi.identity.infrastructure.privacy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.*;
import com.lingxi.identity.domain.IdentitySecurityRepository;
import com.lingxi.identity.infrastructure.persistence.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 导出或清理身份模块拥有的账号、设备、协议和年龄证据。 */
@Component
public class IdentityPrivacyDataContributor implements PrivacyDataContributor {
  private final IdentitySecurityRepository repository;
  private final UserMapper userMapper;
  private final DeviceMapper deviceMapper;
  private final UserConsentMapper consentMapper;
  private final AgeVerificationMapper ageMapper;
  private final ObjectMapper objectMapper;

  public IdentityPrivacyDataContributor(
      IdentitySecurityRepository repository,
      UserMapper userMapper,
      DeviceMapper deviceMapper,
      UserConsentMapper consentMapper,
      AgeVerificationMapper ageMapper,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.userMapper = userMapper;
    this.deviceMapper = deviceMapper;
    this.consentMapper = consentMapper;
    this.ageMapper = ageMapper;
    this.objectMapper = objectMapper;
  }

  @Override
  public String moduleName() {
    return "identity";
  }

  @Override
  @Transactional
  public PrivacyContribution process(PrivacyProcessingContext context) {
    long requestId = context.requestId();
    long userId = context.userId();
    PrivacyRequestType type = context.type();
    if (type == PrivacyRequestType.DELETE_DATA || type == PrivacyRequestType.CLOSE_ACCOUNT) {
      return PrivacyContribution.deleted(
          repository.logicallyDeletePrivateData(
              requestId, userId, type == PrivacyRequestType.CLOSE_ACCOUNT));
    }
    if (type != PrivacyRequestType.EXPORT) {
      return PrivacyContribution.unchanged();
    }
    Map<String, Object> data = new LinkedHashMap<>();
    UserEntity user = userMapper.selectById(userId);
    Map<String, Object> profile = new LinkedHashMap<>();
    if (user != null) {
      profile.put("publicId", user.getPublicId());
      profile.put("ageBand", user.getAgeBand());
      profile.put("accountStatus", user.getStatus());
      profile.put("adultTransitionDate", user.getAdultTransitionDate());
      profile.put("timezone", user.getTimezone());
      profile.put("createdAt", user.getCreatedAt());
      profile.put("updatedAt", user.getUpdatedAt());
    }
    // 注册幂等摘要和服务端授权版本属于安全实现细节，不进入用户导出包。
    data.put("profile", profile);
    data.put(
        "devices",
        deviceMapper.selectList(
            Wrappers.<DeviceEntity>lambdaQuery()
                .select(
                    DeviceEntity::getId, DeviceEntity::getUserId, DeviceEntity::getDeviceId,
                    DeviceEntity::getFirstSeenAt, DeviceEntity::getLastSeenAt)
                .eq(DeviceEntity::getUserId, userId)));
    data.put(
        "consents",
        consentMapper.selectList(
            Wrappers.<UserConsentEntity>lambdaQuery()
                .select(
                    UserConsentEntity::getId, UserConsentEntity::getUserId,
                    UserConsentEntity::getPurpose, UserConsentEntity::getDocumentVersion,
                    UserConsentEntity::getGranted, UserConsentEntity::getRecordedAt)
                .eq(UserConsentEntity::getUserId, userId)));
    data.put(
        "ageVerifications",
        ageMapper.selectList(
            Wrappers.<AgeVerificationEntity>lambdaQuery()
                .select(
                    AgeVerificationEntity::getId, AgeVerificationEntity::getUserId,
                    AgeVerificationEntity::getMethod, AgeVerificationEntity::getVerifiedBirthDate,
                    AgeVerificationEntity::getResult, AgeVerificationEntity::getVerifiedAt,
                    AgeVerificationEntity::getCreatedAt)
                .eq(AgeVerificationEntity::getUserId, userId)));
    try {
      return PrivacyContribution.exported(objectMapper.writeValueAsString(data));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("身份数据导出失败", e);
    }
  }
}
