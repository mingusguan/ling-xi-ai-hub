package com.lingxi.relationship.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;

/** 双方明确同意的伙伴关系聚合。 */
public class PartnerRelation {
  public enum Status {
    INVITED,
    ACTIVE,
    TERMINATED,
    BLOCKED
  }

  private final long id;
  private final String requestKey;
  private final long inviter;
  private final long invitee;
  private Status status;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private PartnerRelation(
      long id,
      String key,
      long inviter,
      long invitee,
      Status status,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    this.id = id;
    this.requestKey = key;
    this.inviter = inviter;
    this.invitee = invitee;
    this.status = status;
    this.version = version;
    this.createdAt = created;
    this.updatedAt = updated;
  }

  public static PartnerRelation invite(
      long id, String key, long inviter, long invitee, LocalDateTime now) {
    if (id <= 0
        || inviter <= 0
        || invitee <= 0
        || inviter == invitee
        || key == null
        || key.isBlank()) throw new BusinessException("REL_INVALID_PARTNER_INVITE", "伙伴邀请参数不合法");
    return new PartnerRelation(id, key, inviter, invitee, Status.INVITED, 0, now, now);
  }

  public static PartnerRelation rehydrate(
      long id,
      String key,
      long inviter,
      long invitee,
      Status status,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    return new PartnerRelation(id, key, inviter, invitee, status, version, created, updated);
  }

  public void accept(long user, long expected, LocalDateTime now) {
    if (user != invitee) throw new BusinessException("REL_PARTNER_FORBIDDEN", "仅被邀请人可以接受");
    check(expected);
    if (status != Status.INVITED)
      throw new BusinessException("REL_PARTNER_NOT_INVITED", "邀请状态不可接受");
    status = Status.ACTIVE;
    version++;
    updatedAt = now;
  }

  public void terminate(long user, long expected, boolean block, LocalDateTime now) {
    assertParticipant(user);
    check(expected);
    if (status != Status.ACTIVE && status != Status.INVITED)
      throw new BusinessException("REL_PARTNER_NOT_ACTIVE", "关系已结束");
    status = block ? Status.BLOCKED : Status.TERMINATED;
    version++;
    updatedAt = now;
  }

  public void assertParticipant(long user) {
    if (user != inviter && user != invitee)
      throw new BusinessException("REL_PARTNER_NOT_FOUND", "伙伴关系不存在");
  }

  private void check(long expected) {
    if (version != expected) throw new BusinessException("REL_PARTNER_CONFLICT", "伙伴关系已变化");
  }

  public long getId() {
    return id;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public long getInviter() {
    return inviter;
  }

  public long getInvitee() {
    return invitee;
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
