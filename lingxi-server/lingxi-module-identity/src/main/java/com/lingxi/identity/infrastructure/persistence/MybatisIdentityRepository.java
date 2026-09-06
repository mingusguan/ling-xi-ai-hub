package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.identity.api.AccountStatus;
import com.lingxi.identity.api.AgeBand;
import com.lingxi.identity.domain.AgeVerification;
import com.lingxi.identity.domain.IdentityRepository;
import com.lingxi.identity.domain.User;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis-Plus 的身份聚合仓储实现。 */
@Repository
public class MybatisIdentityRepository implements IdentityRepository {
  private final UserMapper userMapper;
  private final AgeVerificationMapper ageVerificationMapper;

  public MybatisIdentityRepository(
      UserMapper userMapper, AgeVerificationMapper ageVerificationMapper) {
    this.userMapper = userMapper;
    this.ageVerificationMapper = ageVerificationMapper;
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
                .set(UserEntity::getUpdatedAt, user.getUpdatedAt()))
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
        e.getUpdatedAt());
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
