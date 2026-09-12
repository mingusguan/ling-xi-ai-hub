package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.kernel.BusinessException;
import com.lingxi.relationship.api.PartnerPermission;
import com.lingxi.relationship.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

/** 伙伴与分享 MyBatis-Plus 仓储。 */
@Repository
public class MybatisPartnerRepository implements PartnerRepository {
  private final PartnerRelationMapper relations;
  private final PartnerGrantMapper grants;
  private final ShareLinkMapper shares;
  private final PartnerInteractionMapper interactions;
  private final BlockRelationMapper blocks;
  private final ReportMapper reports;
  private final ObjectMapper json;

  public MybatisPartnerRepository(
      PartnerRelationMapper relations,
      PartnerGrantMapper grants,
      ShareLinkMapper shares,
      PartnerInteractionMapper interactions,
      BlockRelationMapper blocks,
      ReportMapper reports,
      ObjectMapper json) {
    this.relations = relations;
    this.grants = grants;
    this.shares = shares;
    this.interactions = interactions;
    this.blocks = blocks;
    this.reports = reports;
    this.json = json;
  }

  public Optional<PartnerRelation> findRelation(long id) {
    return Optional.ofNullable(relations.selectById(id)).map(this::relation);
  }

  public Optional<PartnerRelation> findRelationByRequestKey(String key) {
    return Optional.ofNullable(
            relations.selectOne(
                Wrappers.<PartnerRelationEntity>lambdaQuery()
                    .eq(PartnerRelationEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::relation);
  }

  public Optional<PartnerRelation> findOpenRelation(long a, long b) {
    return Optional.ofNullable(
            relations.selectOne(
                Wrappers.<PartnerRelationEntity>lambdaQuery()
                    .and(
                        q ->
                            q.eq(PartnerRelationEntity::getInviterUserId, a)
                                .eq(PartnerRelationEntity::getInviteeUserId, b)
                                .or()
                                .eq(PartnerRelationEntity::getInviterUserId, b)
                                .eq(PartnerRelationEntity::getInviteeUserId, a))
                    .in(PartnerRelationEntity::getStatus, "INVITED", "ACTIVE")
                    .last("LIMIT 1")))
        .map(this::relation);
  }

  @Override
  public long countRelationsByParticipant(long user) {
    Long total = relations.selectCount(participantQuery(user));
    return total == null ? 0L : total;
  }

  @Override
  public List<PartnerRelation> findRelationsByParticipant(long user, int page, int pageSize) {
    long offset = (long) (page - 1) * pageSize;
    return relations
        .selectList(
            participantQuery(user)
                .orderByDesc(PartnerRelationEntity::getCreatedAt)
                .orderByDesc(PartnerRelationEntity::getId)
                .last("LIMIT " + pageSize + " OFFSET " + offset))
        .stream()
        .map(this::relation)
        .toList();
  }

  /** 关系双方都可能查询：命中邀请方或被邀请方都算参与。 */
  private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PartnerRelationEntity>
      participantQuery(long user) {
    return Wrappers.<PartnerRelationEntity>lambdaQuery()
        .and(
            wrapper ->
                wrapper
                    .eq(PartnerRelationEntity::getInviterUserId, user)
                    .or()
                    .eq(PartnerRelationEntity::getInviteeUserId, user));
  }

  public void insertRelation(PartnerRelation r) {
    PartnerRelationEntity e = new PartnerRelationEntity();
    e.setId(r.getId());
    e.setRequestKey(r.getRequestKey());
    e.setInviterUserId(r.getInviter());
    e.setInviteeUserId(r.getInvitee());
    e.setStatus(r.getStatus().name());
    e.setVersion(r.getVersion());
    e.setCreatedAt(r.getCreatedAt());
    e.setUpdatedAt(r.getUpdatedAt());
    relations.insert(e);
  }

  public boolean updateRelation(PartnerRelation r, long v) {
    return relations.update(
            null,
            Wrappers.<PartnerRelationEntity>lambdaUpdate()
                .eq(PartnerRelationEntity::getId, r.getId())
                .eq(PartnerRelationEntity::getVersion, v)
                .set(PartnerRelationEntity::getStatus, r.getStatus().name())
                .set(PartnerRelationEntity::getVersion, r.getVersion())
                .set(PartnerRelationEntity::getUpdatedAt, r.getUpdatedAt()))
        == 1;
  }

  public Optional<PartnerGrant> findGrant(long id) {
    return Optional.ofNullable(grants.selectById(id)).map(this::grant);
  }

  public Optional<PartnerGrant> findGrant(long relationId, long goalId) {
    return Optional.ofNullable(
            grants.selectOne(
                Wrappers.<PartnerGrantEntity>lambdaQuery()
                    .eq(PartnerGrantEntity::getRelationId, relationId)
                    .eq(PartnerGrantEntity::getGoalId, goalId)
                    .last("LIMIT 1")))
        .map(this::grant);
  }

  public List<PartnerGrant> findActiveGrants(long goalId) {
    return grants
        .selectList(
            Wrappers.<PartnerGrantEntity>lambdaQuery()
                .eq(PartnerGrantEntity::getGoalId, goalId)
                .eq(PartnerGrantEntity::getStatus, "ACTIVE"))
        .stream()
        .map(this::grant)
        .toList();
  }

  @Override
  public List<PartnerGrant> findGrantsByOwner(long ownerUserId) {
    return grants
        .selectList(
            Wrappers.<PartnerGrantEntity>lambdaQuery()
                .eq(PartnerGrantEntity::getOwnerUserId, ownerUserId)
                .orderByDesc(PartnerGrantEntity::getCreatedAt)
                .orderByDesc(PartnerGrantEntity::getId))
        .stream()
        .map(this::grant)
        .toList();
  }

  public void insertGrant(PartnerGrant r) {
    PartnerGrantEntity e = new PartnerGrantEntity();
    e.setId(r.getId());
    e.setRelationId(r.getRelationId());
    e.setOwnerUserId(r.getOwnerUserId());
    e.setGoalId(r.getGoalId());
    e.setPermissionsJson(write(r.getPermissions()));
    e.setExpiresAt(r.getExpiresAt());
    e.setStatus(r.getStatus().name());
    e.setVersion(r.getVersion());
    e.setCreatedAt(r.getCreatedAt());
    e.setUpdatedAt(r.getUpdatedAt());
    grants.insert(e);
  }

  public boolean updateGrant(PartnerGrant r, long v) {
    return grants.update(
            null,
            Wrappers.<PartnerGrantEntity>lambdaUpdate()
                .eq(PartnerGrantEntity::getId, r.getId())
                .eq(PartnerGrantEntity::getVersion, v)
                .set(PartnerGrantEntity::getPermissionsJson, write(r.getPermissions()))
                .set(PartnerGrantEntity::getExpiresAt, r.getExpiresAt())
                .set(PartnerGrantEntity::getStatus, r.getStatus().name())
                .set(PartnerGrantEntity::getVersion, r.getVersion())
                .set(PartnerGrantEntity::getUpdatedAt, r.getUpdatedAt()))
        == 1;
  }

  public void insertInteraction(
      long id,
      long relation,
      long grant,
      long actor,
      String type,
      String resourceId,
      String contentJson,
      LocalDateTime now) {
    PartnerInteractionEntity e = new PartnerInteractionEntity();
    e.setId(id);
    e.setRelationId(relation);
    e.setGrantId(grant);
    e.setActorUserId(actor);
    e.setInteractionType(type);
    e.setResourceId(resourceId);
    e.setContentJson(contentJson);
    e.setCreatedAt(now);
    interactions.insert(e);
  }

  public void insertBlock(long id, long blocker, long blocked, String reason, LocalDateTime now) {
    if (blocks.selectCount(
            Wrappers.<BlockRelationEntity>lambdaQuery()
                .eq(BlockRelationEntity::getBlockerUserId, blocker)
                .eq(BlockRelationEntity::getBlockedUserId, blocked))
        > 0) return;
    BlockRelationEntity e = new BlockRelationEntity();
    e.setId(id);
    e.setBlockerUserId(blocker);
    e.setBlockedUserId(blocked);
    e.setReasonCode(reason);
    e.setCreatedAt(now);
    blocks.insert(e);
  }

  public void insertReport(
      long id,
      long reporter,
      String targetType,
      String targetId,
      String reason,
      String evidence,
      LocalDateTime now) {
    ReportEntity e = new ReportEntity();
    e.setId(id);
    e.setReporterUserId(reporter);
    e.setTargetType(targetType);
    e.setTargetId(targetId);
    e.setReasonCode(reason);
    e.setEvidenceRef(evidence);
    e.setStatus("OPEN");
    e.setCreatedAt(now);
    reports.insert(e);
  }

  public Optional<ShareLink> findShare(long id) {
    return Optional.ofNullable(shares.selectById(id)).map(this::share);
  }

  public Optional<ShareLink> findShareByRequestKey(String key) {
    return Optional.ofNullable(
            shares.selectOne(
                Wrappers.<ShareLinkEntity>lambdaQuery()
                    .eq(ShareLinkEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::share);
  }

  public Optional<ShareLink> findShareByTokenHash(String hash) {
    return Optional.ofNullable(
            shares.selectOne(
                Wrappers.<ShareLinkEntity>lambdaQuery()
                    .eq(ShareLinkEntity::getTokenHash, hash)
                    .last("LIMIT 1")))
        .map(this::share);
  }

  @Override
  public long countSharesByOwner(long ownerUserId) {
    Long total =
        shares.selectCount(
            Wrappers.<ShareLinkEntity>lambdaQuery().eq(ShareLinkEntity::getOwnerUserId, ownerUserId));
    return total == null ? 0L : total;
  }

  @Override
  public List<ShareLink> findSharesByOwner(long ownerUserId, int page, int pageSize) {
    long offset = (long) (page - 1) * pageSize;
    return shares
        .selectList(
            Wrappers.<ShareLinkEntity>lambdaQuery()
                .eq(ShareLinkEntity::getOwnerUserId, ownerUserId)
                .orderByDesc(ShareLinkEntity::getCreatedAt)
                .orderByDesc(ShareLinkEntity::getId)
                .last("LIMIT " + pageSize + " OFFSET " + offset))
        .stream()
        .map(this::share)
        .toList();
  }

  public void insertShare(ShareLink r) {
    ShareLinkEntity e = new ShareLinkEntity();
    e.setId(r.getId());
    e.setRequestKey(r.getRequestKey());
    e.setRequestDigest(r.getRequestDigest());
    e.setOwnerUserId(r.getOwnerUserId());
    e.setResourceType(r.getResourceType());
    e.setResourceId(r.getResourceId());
    e.setFieldsJson(write(r.getFields()));
    e.setSnapshotJson(r.getSnapshotJson());
    e.setTokenHash(r.getTokenHash());
    e.setPasswordHash(r.getPasswordHash());
    e.setExpiresAt(r.getExpiresAt());
    e.setVisitLimit(r.getVisitLimit());
    e.setVisitCount(r.getVisitCount());
    e.setStatus(r.getStatus().name());
    e.setVersion(r.getVersion());
    e.setCreatedAt(r.getCreatedAt());
    e.setUpdatedAt(r.getUpdatedAt());
    shares.insert(e);
  }

  public boolean updateShare(ShareLink r, long v) {
    return shares.update(
            null,
            Wrappers.<ShareLinkEntity>lambdaUpdate()
                .eq(ShareLinkEntity::getId, r.getId())
                .eq(ShareLinkEntity::getVersion, v)
                .set(ShareLinkEntity::getStatus, r.getStatus().name())
                .set(ShareLinkEntity::getVersion, r.getVersion())
                .set(ShareLinkEntity::getUpdatedAt, r.getUpdatedAt()))
        == 1;
  }

  public boolean consumeShare(long id, Instant now, LocalDateTime updatedAt) {
    return shares.consume(id, now, updatedAt) == 1;
  }

  private PartnerRelation relation(PartnerRelationEntity e) {
    return PartnerRelation.rehydrate(
        e.getId(),
        e.getRequestKey(),
        e.getInviterUserId(),
        e.getInviteeUserId(),
        PartnerRelation.Status.valueOf(e.getStatus()),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private PartnerGrant grant(PartnerGrantEntity e) {
    return PartnerGrant.rehydrate(
        e.getId(),
        e.getRelationId(),
        e.getOwnerUserId(),
        e.getGoalId(),
        readPermissions(e.getPermissionsJson()),
        e.getExpiresAt(),
        PartnerGrant.Status.valueOf(e.getStatus()),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private ShareLink share(ShareLinkEntity e) {
    return ShareLink.rehydrate(
        e.getId(),
        e.getRequestKey(),
        e.getRequestDigest(),
        e.getOwnerUserId(),
        e.getResourceType(),
        e.getResourceId(),
        readStrings(e.getFieldsJson()),
        e.getSnapshotJson(),
        e.getTokenHash(),
        e.getPasswordHash(),
        e.getExpiresAt(),
        e.getVisitLimit(),
        e.getVisitCount(),
        ShareLink.Status.valueOf(e.getStatus()),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private String write(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (Exception e) {
      throw new BusinessException("REL_SERIALIZATION_ERROR", "关系数据序列化失败");
    }
  }

  private Set<PartnerPermission> readPermissions(String s) {
    try {
      return json.readValue(s, new TypeReference<Set<PartnerPermission>>() {});
    } catch (Exception e) {
      throw new BusinessException("REL_DATA_CORRUPTED", "伙伴权限数据损坏");
    }
  }

  private Set<String> readStrings(String s) {
    try {
      return json.readValue(s, new TypeReference<Set<String>>() {});
    } catch (Exception e) {
      throw new BusinessException("REL_DATA_CORRUPTED", "分享字段数据损坏");
    }
  }
}
