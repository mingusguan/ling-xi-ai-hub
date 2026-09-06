package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.relationship.api.GuardianPermissionType;
import com.lingxi.relationship.domain.GuardianRelation;
import com.lingxi.relationship.domain.GuardianRelationStatus;
import com.lingxi.relationship.domain.GuardianRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 监护关系 MyBatis-Plus 仓储实现。 */
@Repository
public class MybatisGuardianRepository implements GuardianRepository {
  private final GuardianRelationMapper relationMapper;
  private final GuardianPermissionMapper permissionMapper;
  private final IdGenerator idGenerator;

  public MybatisGuardianRepository(
      GuardianRelationMapper relationMapper,
      GuardianPermissionMapper permissionMapper,
      IdGenerator idGenerator) {
    this.relationMapper = relationMapper;
    this.permissionMapper = permissionMapper;
    this.idGenerator = idGenerator;
  }

  @Override
  public Optional<GuardianRelation> findById(long id) {
    return Optional.ofNullable(relationMapper.selectById(id)).map(this::toDomain);
  }

  @Override
  public Optional<GuardianRelation> findByRequestKey(String key) {
    return Optional.ofNullable(
            relationMapper.selectOne(
                Wrappers.<GuardianRelationEntity>lambdaQuery()
                    .eq(GuardianRelationEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public Optional<GuardianRelation> findByInvitationTokenHash(String hash) {
    return Optional.ofNullable(
            relationMapper.selectOne(
                Wrappers.<GuardianRelationEntity>lambdaQuery()
                    .eq(GuardianRelationEntity::getInvitationTokenHash, hash)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public void insert(GuardianRelation relation) {
    relationMapper.insert(toEntity(relation));
  }

  @Override
  public boolean update(GuardianRelation r, long previous) {
    return relationMapper.update(
            null,
            Wrappers.<GuardianRelationEntity>lambdaUpdate()
                .eq(GuardianRelationEntity::getId, r.getId())
                .eq(GuardianRelationEntity::getVersion, previous)
                .set(GuardianRelationEntity::getGuardianUserId, r.getGuardianUserId())
                .set(GuardianRelationEntity::getStatus, r.getStatus().name())
                .set(GuardianRelationEntity::getVerifiedAt, r.getVerifiedAt())
                .set(GuardianRelationEntity::getRevokedAt, r.getRevokedAt())
                .set(GuardianRelationEntity::getRevokeReason, r.getRevokeReason())
                .set(GuardianRelationEntity::getVersion, r.getVersion())
                .set(GuardianRelationEntity::getUpdatedAt, r.getUpdatedAt()))
        == 1;
  }

  @Override
  public long countActiveRelations(long teenUserId) {
    return relationMapper.selectCount(
        Wrappers.<GuardianRelationEntity>lambdaQuery()
            .eq(GuardianRelationEntity::getTeenUserId, teenUserId)
            .eq(GuardianRelationEntity::getStatus, GuardianRelationStatus.ACTIVE.name()));
  }

  @Override
  public void insertPermissions(
      long relationId, List<GuardianPermissionType> permissions, LocalDateTime now) {
    List<GuardianPermissionEntity> entities =
        permissions.stream()
            .map(
                permission -> {
                  GuardianPermissionEntity e = new GuardianPermissionEntity();
                  e.setId(idGenerator.nextId());
                  e.setRelationId(relationId);
                  e.setPermission(permission.name());
                  e.setScope("MINIMUM");
                  e.setEffectiveAt(now);
                  return e;
                })
            .toList();
    if (!entities.isEmpty()) {
      permissionMapper.insertBatch(entities);
    }
  }

  @Override
  public List<GuardianPermissionType> findPermissions(long relationId) {
    return permissionMapper
        .selectList(
            Wrappers.<GuardianPermissionEntity>lambdaQuery()
                .eq(GuardianPermissionEntity::getRelationId, relationId))
        .stream()
        .map(e -> GuardianPermissionType.valueOf(e.getPermission()))
        .toList();
  }

  private GuardianRelation toDomain(GuardianRelationEntity e) {
    return GuardianRelation.rehydrate(
        e.getId(),
        e.getRequestKey(),
        e.getRequestDigest(),
        e.getTeenUserId(),
        e.getGuardianUserId(),
        e.getInvitationTokenHash(),
        e.getRequestedPermissionsJson(),
        GuardianRelationStatus.valueOf(e.getStatus()),
        e.getExpiresAt(),
        e.getVerifiedAt(),
        e.getRevokedAt(),
        e.getRevokeReason(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private GuardianRelationEntity toEntity(GuardianRelation r) {
    GuardianRelationEntity e = new GuardianRelationEntity();
    e.setId(r.getId());
    e.setRequestKey(r.getRequestKey());
    e.setRequestDigest(r.getRequestDigest());
    e.setTeenUserId(r.getTeenUserId());
    e.setGuardianUserId(r.getGuardianUserId());
    e.setInvitationTokenHash(r.getInvitationTokenHash());
    e.setRequestedPermissionsJson(r.getRequestedPermissionsJson());
    e.setStatus(r.getStatus().name());
    e.setExpiresAt(r.getExpiresAt());
    e.setVerifiedAt(r.getVerifiedAt());
    e.setRevokedAt(r.getRevokedAt());
    e.setRevokeReason(r.getRevokeReason());
    e.setVersion(r.getVersion());
    e.setCreatedAt(r.getCreatedAt());
    e.setUpdatedAt(r.getUpdatedAt());
    return e;
  }
}
