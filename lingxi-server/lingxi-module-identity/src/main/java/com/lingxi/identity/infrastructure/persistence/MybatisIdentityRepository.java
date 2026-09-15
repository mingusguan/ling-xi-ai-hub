package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.AccountStatus;
import com.lingxi.identity.api.AgeBand;
import com.lingxi.identity.api.CommonBlocker;
import com.lingxi.identity.api.CommunicationStyle;
import com.lingxi.identity.api.OnboardingProfile;
import com.lingxi.identity.api.ProactivityLevel;
import com.lingxi.identity.domain.AgeVerification;
import com.lingxi.identity.domain.IdentityRepository;
import com.lingxi.identity.domain.User;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis-Plus 的身份聚合仓储实现。 */
@Repository
public class MybatisIdentityRepository implements IdentityRepository {
  private final UserMapper userMapper;
  private final AgeVerificationMapper ageVerificationMapper;
  private final ObjectMapper objectMapper;

  public MybatisIdentityRepository(
      UserMapper userMapper,
      AgeVerificationMapper ageVerificationMapper,
      ObjectMapper objectMapper) {
    this.userMapper = userMapper;
    this.ageVerificationMapper = ageVerificationMapper;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<User> findById(long userId) {
    return Optional.ofNullable(userMapper.selectById(userId)).map(this::toDomain);
  }

  @Override
  public Optional<User> findByRegistrationKey(String registrationKey) {
    return Optional.ofNullable(
            userMapper.selectOne(
                Wrappers.<UserEntity>lambdaQuery()
                    .eq(UserEntity::getRegistrationKey, registrationKey)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public void insertRegistration(User user, AgeVerification verification) {
    userMapper.insert(toEntity(user));
    ageVerificationMapper.insert(toEntity(verification));
  }

  @Override
  public boolean update(User user, long previousVersion) {
    // 画像列必须一起写入：User 是聚合根，状态机流转与画像共用同一行；
    // 漏写画像列会在每次账号状态流转时静默清空用户填过的引导数据。
    return userMapper.update(
            null,
            Wrappers.<UserEntity>lambdaUpdate()
                .eq(UserEntity::getId, user.getId())
                .eq(UserEntity::getVersion, previousVersion)
                .set(UserEntity::getAgeBand, user.getAgeBand().name())
                .set(UserEntity::getStatus, user.getStatus().name())
                .set(
                    UserEntity::getClosingPreviousStatus,
                    user.getClosingPreviousStatus() == null
                        ? null
                        : user.getClosingPreviousStatus().name())
                .set(UserEntity::getAuthorizationVersion, user.getAuthorizationVersion())
                .set(UserEntity::getVersion, user.getVersion())
                .set(UserEntity::getUpdatedAt, user.getUpdatedAt())
                .set(UserEntity::getNickname, profile(user).nickname())
                .set(UserEntity::getSleepTime, profile(user).sleepTime())
                .set(UserEntity::getWakeTime, profile(user).wakeTime())
                .set(UserEntity::getWeeklyAvailableMinutes, profile(user).weeklyAvailableMinutes())
                .set(UserEntity::getRemindWindowStart, profile(user).remindWindowStart())
                .set(UserEntity::getRemindWindowEnd, profile(user).remindWindowEnd())
                .set(UserEntity::getQuietHoursStart, profile(user).quietHoursStart())
                .set(UserEntity::getQuietHoursEnd, profile(user).quietHoursEnd())
                .set(
                    UserEntity::getCommunicationStyle,
                    profile(user).communicationStyle() == null
                        ? null
                        : profile(user).communicationStyle().name())
                .set(
                    UserEntity::getProactivityLevel,
                    profile(user).proactivityLevel() == null
                        ? null
                        : profile(user).proactivityLevel().name())
                .set(UserEntity::getCommonBlockersJson, writeBlockers(profile(user).orderedBlockers()))
                .set(UserEntity::getOnboardingCompletedAt, user.getOnboardingCompletedAt()))
        == 1;
  }

  @Override
  public int markDueAdultTransitions(java.time.LocalDate dueDate, java.time.LocalDateTime now) {
    return userMapper.update(
        null,
        Wrappers.<UserEntity>lambdaUpdate()
            .eq(UserEntity::getAgeBand, AgeBand.TEEN.name())
            .in(
                UserEntity::getStatus,
                AccountStatus.ACTIVE_TEEN.name(),
                AccountStatus.RESTRICTED.name())
            .le(UserEntity::getAdultTransitionDate, dueDate)
            .set(UserEntity::getStatus, AccountStatus.AGE_TRANSITION.name())
            .setSql("authorization_version = authorization_version + 1, version = version + 1")
            .set(UserEntity::getUpdatedAt, now));
  }

  @Override
  public boolean isLogicallyDeleted(long userId) {
    return userMapper.countDeletedUser(userId) == 1;
  }

  @Override
  public void logicallyDeleteClosedUser(long userId, long requestId) {
    int affectedRows = userMapper.tombstoneClosedUser(userId, requestId);
    if (affectedRows == 0 && !isLogicallyDeleted(userId)) {
      throw new IllegalStateException("注销账户逻辑删除失败");
    }
  }

  private User toDomain(UserEntity e) {
    return User.rehydrate(
        e.getId(),
        e.getPublicId(),
        e.getRegistrationKey(),
        e.getRegistrationDigest(),
        AgeBand.valueOf(e.getAgeBand()),
        AccountStatus.valueOf(e.getStatus()),
        e.getClosingPreviousStatus() == null
            ? null
            : AccountStatus.valueOf(e.getClosingPreviousStatus()),
        e.getAdultTransitionDate(),
        e.getTimezone(),
        e.getAuthorizationVersion(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        toProfile(e),
        e.getOnboardingCompletedAt());
  }

  /** 数据库中全为 NULL 的画像行按「用户没填过」还原，而不是造出一个有默认值的画像。 */
  private OnboardingProfile toProfile(UserEntity e) {
    return new OnboardingProfile(
        e.getNickname(),
        e.getSleepTime(),
        e.getWakeTime(),
        e.getWeeklyAvailableMinutes(),
        e.getRemindWindowStart(),
        e.getRemindWindowEnd(),
        e.getQuietHoursStart(),
        e.getQuietHoursEnd(),
        e.getCommunicationStyle() == null
            ? null
            : CommunicationStyle.valueOf(e.getCommunicationStyle()),
        e.getProactivityLevel() == null ? null : ProactivityLevel.valueOf(e.getProactivityLevel()),
        parseBlockers(e.getCommonBlockersJson()));
  }

  private OnboardingProfile profile(User u) {
    return u.getOnboardingProfile() == null ? OnboardingProfile.empty() : u.getOnboardingProfile();
  }

  private String writeBlockers(Set<CommonBlocker> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(values.stream().map(Enum::name).toList());
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private Set<CommonBlocker> parseBlockers(String json) {
    if (json == null || json.isBlank()) {
      return Set.of();
    }
    try {
      List<String> names = objectMapper.readValue(json, new TypeReference<List<String>>() {});
      Set<CommonBlocker> result = new LinkedHashSet<>();
      for (String name : names) {
        result.add(CommonBlocker.valueOf(name));
      }
      return result;
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      throw new IllegalStateException("阻塞原因数据损坏：" + json, ex);
    }
  }

  private UserEntity toEntity(User u) {
    UserEntity e = new UserEntity();
    e.setId(u.getId());
    e.setPublicId(u.getPublicId());
    e.setRegistrationKey(u.getRegistrationKey());
    e.setRegistrationDigest(u.getRegistrationDigest());
    e.setAgeBand(u.getAgeBand().name());
    e.setStatus(u.getStatus().name());
    e.setClosingPreviousStatus(
        u.getClosingPreviousStatus() == null ? null : u.getClosingPreviousStatus().name());
    e.setAdultTransitionDate(u.getAdultTransitionDate());
    e.setTimezone(u.getTimezone());
    e.setAuthorizationVersion(u.getAuthorizationVersion());
    e.setVersion(u.getVersion());
    e.setCreatedAt(u.getCreatedAt());
    e.setUpdatedAt(u.getUpdatedAt());
    OnboardingProfile p = profile(u);
    e.setNickname(p.nickname());
    e.setSleepTime(p.sleepTime());
    e.setWakeTime(p.wakeTime());
    e.setWeeklyAvailableMinutes(p.weeklyAvailableMinutes());
    e.setRemindWindowStart(p.remindWindowStart());
    e.setRemindWindowEnd(p.remindWindowEnd());
    e.setQuietHoursStart(p.quietHoursStart());
    e.setQuietHoursEnd(p.quietHoursEnd());
    e.setCommunicationStyle(p.communicationStyle() == null ? null : p.communicationStyle().name());
    e.setProactivityLevel(p.proactivityLevel() == null ? null : p.proactivityLevel().name());
    e.setCommonBlockersJson(writeBlockers(p.orderedBlockers()));
    e.setOnboardingCompletedAt(u.getOnboardingCompletedAt());
    return e;
  }

  private AgeVerificationEntity toEntity(AgeVerification v) {
    AgeVerificationEntity e = new AgeVerificationEntity();
    e.setId(v.id());
    e.setUserId(v.userId());
    e.setMethod(v.method());
    e.setEvidenceReference(v.evidenceReference());
    e.setVerifiedBirthDate(v.verifiedBirthDate());
    e.setResult(v.result().name());
    e.setVerifiedAt(v.verifiedAt());
    e.setCreatedAt(v.createdAt());
    return e;
  }
}
