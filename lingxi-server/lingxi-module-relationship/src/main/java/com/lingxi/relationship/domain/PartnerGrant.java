package com.lingxi.relationship.domain;

import com.lingxi.kernel.BusinessException;
import com.lingxi.relationship.api.PartnerPermission;
import java.time.*;
import java.util.Set;

/** 单目标伙伴最小授权聚合，显式记录资源所有者。 */
public class PartnerGrant {
  public enum Status {
    ACTIVE,
    REVOKED
  }

  private final long id, relationId, ownerUserId, goalId;
  private Set<PartnerPermission> permissions;
  private Instant expiresAt;
  private Status status;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private PartnerGrant(
      long id,
      long relationId,
      long owner,
      long goal,
      Set<PartnerPermission> p,
      Instant expires,
      Status status,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    this.id = id;
    this.relationId = relationId;
    ownerUserId = owner;
    goalId = goal;
    permissions = Set.copyOf(p);
    expiresAt = expires;
    this.status = status;
    this.version = version;
    createdAt = created;
    updatedAt = updated;
  }

  public static PartnerGrant create(
      long id,
      long relation,
      long owner,
      long goal,
      Set<PartnerPermission> p,
      Instant expires,
      LocalDateTime now) {
    validate(id, relation, owner, goal, p, expires, now.toInstant(ZoneOffset.UTC));
    return new PartnerGrant(id, relation, owner, goal, p, expires, Status.ACTIVE, 0, now, now);
  }

  public static PartnerGrant rehydrate(
      long id,
      long relation,
      long owner,
      long goal,
      Set<PartnerPermission> p,
      Instant expires,
      Status status,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    return new PartnerGrant(
        id, relation, owner, goal, p, expires, status, version, created, updated);
  }

  public void update(
      long owner, Set<PartnerPermission> p, Instant expires, long expected, LocalDateTime now) {
    if (ownerUserId != owner) throw new BusinessException("REL_GRANT_NOT_FOUND", "伙伴授权不存在");
    check(expected);
    validate(id, relationId, owner, goalId, p, expires, now.toInstant(ZoneOffset.UTC));
    permissions = Set.copyOf(p);
    expiresAt = expires;
    status = Status.ACTIVE;
    version++;
    updatedAt = now;
  }

  public void revoke(long owner, long expected, LocalDateTime now) {
    if (ownerUserId != owner) throw new BusinessException("REL_GRANT_NOT_FOUND", "伙伴授权不存在");
    check(expected);
    if (status != Status.REVOKED) {
      status = Status.REVOKED;
      version++;
      updatedAt = now;
    }
  }

  public boolean permits(long actor, PartnerPermission p, Instant now) {
    return actor != ownerUserId
        && status == Status.ACTIVE
        && (expiresAt == null || expiresAt.isAfter(now))
        && permissions.contains(p);
  }

  private void check(long e) {
    if (version != e) throw new BusinessException("REL_GRANT_CONFLICT", "伙伴授权已变化");
  }

  private static void validate(
      long id,
      long relation,
      long owner,
      long goal,
      Set<PartnerPermission> p,
      Instant expires,
      Instant now) {
    if (id <= 0
        || relation <= 0
        || owner <= 0
        || goal <= 0
        || p == null
        || p.isEmpty()
        || (expires != null && !expires.isAfter(now)))
      throw new BusinessException("REL_INVALID_GRANT", "伙伴授权参数不合法");
  }

  public long getId() {
    return id;
  }

  public long getRelationId() {
    return relationId;
  }

  public long getOwnerUserId() {
    return ownerUserId;
  }

  public long getGoalId() {
    return goalId;
  }

  public Set<PartnerPermission> getPermissions() {
    return permissions;
  }

  public Instant getExpiresAt() {
    return expiresAt;
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
