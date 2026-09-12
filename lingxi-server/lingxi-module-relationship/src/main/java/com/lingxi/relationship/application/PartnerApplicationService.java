package com.lingxi.relationship.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.goal.api.*;
import com.lingxi.identity.api.*;
import com.lingxi.kernel.*;
import com.lingxi.relationship.api.*;
import com.lingxi.relationship.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 伙伴授权和受控分享应用服务。 */
@Service
public class PartnerApplicationService implements PartnerFacade {
  private static final Set<String> GOAL_FIELDS =
      Set.of("title", "successCriteria", "status", "progress");
  /** 列表类查询允许的最大页大小。 */
  private static final int MAX_PAGE_SIZE = 200;
  private final PartnerRepository repo;
  private final IdentityFacade identities;
  private final GoalFacade goals;
  private final IdGenerator ids;
  private final ObjectMapper json;
  private final SecureRandom random = new SecureRandom();

  public PartnerApplicationService(
      PartnerRepository repo,
      IdentityFacade identities,
      GoalFacade goals,
      IdGenerator ids,
      ObjectMapper json) {
    this.repo = repo;
    this.identities = identities;
    this.goals = goals;
    this.ids = ids;
    this.json = json;
  }

  public PartnerRelationResult invite(InvitePartnerCommand c) {
    requireAdult(c.inviterUserId());
    requireAdult(c.inviteeUserId());
    if (c.inviterUserId() == c.inviteeUserId()) throw error("REL_INVALID_PARTNER_INVITE", "不能邀请自己");
    var keyed = repo.findRelationByRequestKey(c.requestKey());
    if (keyed.isPresent()) {
      PartnerRelation r = keyed.get();
      if (r.getInviter() != c.inviterUserId() || r.getInvitee() != c.inviteeUserId())
        throw error("REL_IDEMPOTENCY_CONFLICT", "幂等键对应不同邀请");
      return result(r);
    }
    var open = repo.findOpenRelation(c.inviterUserId(), c.inviteeUserId());
    if (open.isPresent()) return result(open.get());
    var r =
        PartnerRelation.invite(
            ids.nextId(),
            c.requestKey(),
            c.inviterUserId(),
            c.inviteeUserId(),
            LocalDateTime.now(ZoneOffset.UTC));
    repo.insertRelation(r);
    return result(r);
  }

  public PartnerRelationResult accept(AcceptPartnerCommand c) {
    requireAdult(c.inviteeUserId());
    PartnerRelation r = relation(c.relationId());
    long v = r.getVersion();
    r.accept(c.inviteeUserId(), c.expectedVersion(), now());
    updated(repo.updateRelation(r, v));
    return result(r);
  }

  @Transactional
  public PartnerRelationResult terminate(long user, long relationId, long expected, String reason) {
    PartnerRelation r = relation(relationId);
    long v = r.getVersion();
    r.terminate(user, expected, "BLOCK".equalsIgnoreCase(reason), now());
    updated(repo.updateRelation(r, v));
    if ("BLOCK".equalsIgnoreCase(reason)) {
      long blocked = r.getInviter() == user ? r.getInvitee() : r.getInviter();
      repo.insertBlock(ids.nextId(), user, blocked, reason, now());
    }
    return result(r);
  }

  public PartnerGrantResult updateGrant(UpdatePartnerGrantCommand c) {
    requireAdult(c.ownerUserId());
    PartnerRelation relation = activeRelation(c.relationId());
    relation.assertParticipant(c.ownerUserId());
    goals.getGoal(c.ownerUserId(), c.goalId());
    PartnerGrant grant = repo.findGrant(c.relationId(), c.goalId()).orElse(null);
    if (grant == null) {
      if (c.expectedVersion() != 0) throw error("REL_GRANT_CONFLICT", "伙伴授权版本不匹配");
      grant =
          PartnerGrant.create(
              ids.nextId(),
              c.relationId(),
              c.ownerUserId(),
              c.goalId(),
              c.permissions(),
              c.expiresAt(),
              now());
      repo.insertGrant(grant);
    } else {
      long v = grant.getVersion();
      grant.update(c.ownerUserId(), c.permissions(), c.expiresAt(), c.expectedVersion(), now());
      updated(repo.updateGrant(grant, v));
    }
    return result(grant);
  }

  public PartnerGrantResult revokeGrant(long owner, long grantId, long expected) {
    PartnerGrant g =
        repo.findGrant(grantId).orElseThrow(() -> error("REL_GRANT_NOT_FOUND", "伙伴授权不存在"));
    PartnerRelation r = relation(g.getRelationId());
    r.assertParticipant(owner);
    long v = g.getVersion();
    g.revoke(owner, expected, now());
    updated(repo.updateGrant(g, v));
    return result(g);
  }

  public PartnerInteractionResult interact(CreatePartnerInteractionCommand c) {
    PartnerPermission permission =
        switch (c.interactionType()) {
          case "ENCOURAGE" -> PartnerPermission.ENCOURAGE;
          case "COMMENT" -> PartnerPermission.COMMENT;
          case "CO_CHECK_IN" -> PartnerPermission.CO_CHECK_IN;
          default -> throw error("REL_INTERACTION_NOT_ALLOWED", "互动类型不允许");
        };
    PartnerGrant grant = authorizedGrant(c.actorUserId(), c.goalId(), permission);
    long id = ids.nextId();
    LocalDateTime now = now();
    repo.insertInteraction(
        id,
        grant.getRelationId(),
        grant.getId(),
        c.actorUserId(),
        c.interactionType(),
        c.resourceId(),
        c.contentJson(),
        now);
    return new PartnerInteractionResult(
        id,
        grant.getRelationId(),
        grant.getId(),
        c.actorUserId(),
        c.interactionType(),
        c.resourceId(),
        now);
  }

  public ReportResult report(CreateReportCommand c) {
    identities.getAccessProfile(c.reporterUserId());
    if (c.targetType() == null || c.targetId() == null || c.reasonCode() == null)
      throw error("REL_INVALID_REPORT", "举报参数不完整");
    long id = ids.nextId();
    LocalDateTime now = now();
    repo.insertReport(
        id,
        c.reporterUserId(),
        c.targetType(),
        c.targetId(),
        c.reasonCode(),
        c.evidenceReference(),
        now);
    return new ReportResult(id, "OPEN", now);
  }

  @Transactional(readOnly = true)
  public boolean authorizeGoal(long actor, long goalId, PartnerPermission p) {
    try {
      authorizedGrant(actor, goalId, p);
      return true;
    } catch (BusinessException e) {
      return false;
    }
  }

  private PartnerGrant authorizedGrant(long actor, long goalId, PartnerPermission p) {
    for (PartnerGrant g : repo.findActiveGrants(goalId)) {
      PartnerRelation r = repo.findRelation(g.getRelationId()).orElse(null);
      if (r != null
          && r.getStatus() == PartnerRelation.Status.ACTIVE
          && (r.getInviter() == actor || r.getInvitee() == actor)
          && g.permits(actor, p, Instant.now())) return g;
    }
    throw error("REL_GRANT_REQUIRED", "伙伴授权不足或已失效");
  }

  public ShareLinkResult createShare(CreateShareCommand c) {
    validateShareCommand(c);
    requireCoreUser(c.ownerUserId());
    long goalId = parseGoalId(c.resourceId());
    String digest =
        sha256(
            c.ownerUserId()
                + "|"
                + c.resourceType()
                + "|"
                + c.resourceId()
                + "|"
                + new TreeSet<>(c.fields())
                + "|"
                + c.expiresAt()
                + "|"
                + c.visitLimit());
    var existing = repo.findShareByRequestKey(c.requestKey());
    if (existing.isPresent()) {
      if (!MessageDigest.isEqual(
          existing.get().getRequestDigest().getBytes(StandardCharsets.UTF_8),
          digest.getBytes(StandardCharsets.UTF_8)))
        throw error("REL_IDEMPOTENCY_CONFLICT", "幂等键对应不同分享");
      return result(existing.get(), null);
    }
    GoalResult goal = goals.getGoal(c.ownerUserId(), goalId);
    Map<String, Object> snapshot = new LinkedHashMap<>();
    for (String f : c.fields())
      switch (f) {
        case "title" -> snapshot.put(f, goal.title());
        case "successCriteria" -> snapshot.put(f, goal.successCriteria());
        case "status" -> snapshot.put(f, goal.status());
        case "progress" -> snapshot.put(f, goal.progress());
        default -> throw error("REL_INVALID_SHARE_FIELDS", "包含不可分享字段");
      }
    String raw = token();
    ShareLink share =
        ShareLink.create(
            ids.nextId(),
            c.requestKey(),
            digest,
            c.ownerUserId(),
            "GOAL",
            c.resourceId(),
            c.fields(),
            write(snapshot),
            sha256(raw),
            password(c.password()),
            c.expiresAt(),
            c.visitLimit(),
            now());
    repo.insertShare(share);
    return result(share, raw);
  }

  public ShareLinkResult accessShare(String raw, String password) {
    if (raw == null || raw.isBlank()) {
      throw error("REL_SHARE_NOT_FOUND", "分享不存在");
    }
    ShareLink s =
        repo.findShareByTokenHash(sha256(raw))
            .orElseThrow(() -> error("REL_SHARE_NOT_FOUND", "分享不存在"));
    s.assertAccessible(Instant.now());
    if (!verify(password, s.getPasswordHash())) throw error("REL_SHARE_PASSWORD_INVALID", "访问密码错误");
    if (!repo.consumeShare(s.getId(), Instant.now(), now()))
      throw error("REL_SHARE_UNAVAILABLE", "分享已失效");
    ShareLink consumed = repo.findShare(s.getId()).orElseThrow();
    return result(consumed, null);
  }

  public void revokeShare(long owner, long id, long expected) {
    ShareLink s = repo.findShare(id).orElseThrow(() -> error("REL_SHARE_NOT_FOUND", "分享不存在"));
    long v = s.getVersion();
    s.revoke(owner, expected, now());
    updated(repo.updateShare(s, v));
  }

  private void requireAdult(long user) {
    AccessProfile p = identities.getAccessProfile(user);
    if (p.ageBand() != AgeBand.ADULT || !p.coreFeaturesAllowed())
      throw error("REL_PARTNER_ADULT_ONLY", "伙伴功能仅对成人开放");
  }

  private void requireCoreUser(long userId) {
    AccessProfile profile = identities.getAccessProfile(userId);
    if (!profile.coreFeaturesAllowed() || profile.ageBand() == AgeBand.UNDER_14) {
      throw error("REL_ACCOUNT_RESTRICTED", "当前账号不可创建分享");
    }
  }

  private void validateShareCommand(CreateShareCommand command) {
    if (command == null
        || command.requestKey() == null
        || command.requestKey().isBlank()
        || command.ownerUserId() <= 0
        || !"GOAL".equalsIgnoreCase(command.resourceType())
        || command.resourceId() == null
        || command.resourceId().isBlank()
        || command.fields() == null
        || command.fields().isEmpty()
        || !GOAL_FIELDS.containsAll(command.fields())
        || command.expiresAt() == null
        || !command.expiresAt().isAfter(Instant.now())
        || (command.visitLimit() != null && command.visitLimit() <= 0)) {
      throw error("REL_INVALID_SHARE", "分享参数不合法或包含不可分享字段");
    }
  }

  private long parseGoalId(String resourceId) {
    try {
      long goalId = Long.parseLong(resourceId);
      if (goalId <= 0) {
        throw new NumberFormatException();
      }
      return goalId;
    } catch (NumberFormatException exception) {
      throw error("REL_INVALID_SHARE", "目标分享资源标识不合法");
    }
  }

  private PartnerRelation relation(long id) {
    return repo.findRelation(id).orElseThrow(() -> error("REL_PARTNER_NOT_FOUND", "伙伴关系不存在"));
  }

  /** 分页查询与本人相关的伙伴关系，只按归属过滤，不暴露对方私密数据。 */
  @Override
  @Transactional(readOnly = true)
  public PageResult<PartnerRelationResult> listRelations(
      long participantUserId, int page, int pageSize) {
    requirePaging(page, pageSize);
    identities.getAccessProfile(participantUserId);
    long total = repo.countRelationsByParticipant(participantUserId);
    List<PartnerRelationResult> items =
        repo.findRelationsByParticipant(participantUserId, page, pageSize).stream()
            .map(this::result)
            .toList();
    return new PageResult<>(items, total, page, pageSize);
  }

  /** 本人发出的目标授权列表，用于“我授权了谁”的可见性管理。 */
  @Override
  @Transactional(readOnly = true)
  public List<PartnerGrantResult> listGrants(long ownerUserId) {
    identities.getAccessProfile(ownerUserId);
    return repo.findGrantsByOwner(ownerUserId).stream().map(this::result).toList();
  }

  /** 本人创建的分享链接；明文 token 只在创建时返回一次，列表不回显。 */
  @Override
  @Transactional(readOnly = true)
  public PageResult<ShareLinkResult> listShares(long ownerUserId, int page, int pageSize) {
    requirePaging(page, pageSize);
    identities.getAccessProfile(ownerUserId);
    long total = repo.countSharesByOwner(ownerUserId);
    List<ShareLinkResult> items =
        repo.findSharesByOwner(ownerUserId, page, pageSize).stream()
            .map(s -> result(s, null))
            .toList();
    return new PageResult<>(items, total, page, pageSize);
  }

  private void requirePaging(int page, int pageSize) {
    if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw error("REL_INVALID_QUERY", "查询参数不合法");
    }
  }

  private PartnerRelation activeRelation(long id) {
    PartnerRelation r = relation(id);
    if (r.getStatus() != PartnerRelation.Status.ACTIVE)
      throw error("REL_PARTNER_NOT_ACTIVE", "伙伴关系未生效");
    return r;
  }

  private PartnerRelationResult result(PartnerRelation r) {
    return new PartnerRelationResult(
        r.getId(), r.getInviter(), r.getInvitee(), r.getStatus().name(), r.getVersion());
  }

  private PartnerGrantResult result(PartnerGrant g) {
    return new PartnerGrantResult(
        g.getId(),
        g.getRelationId(),
        g.getOwnerUserId(),
        g.getGoalId(),
        g.getPermissions(),
        g.getExpiresAt(),
        g.getStatus().name(),
        g.getVersion());
  }

  private ShareLinkResult result(ShareLink s, String raw) {
    return new ShareLinkResult(
        s.getId(),
        raw,
        s.getResourceType(),
        s.getResourceId(),
        s.getFields(),
        s.getSnapshotJson(),
        s.getStatus().name(),
        s.getVisitCount(),
        s.getVisitLimit(),
        s.getExpiresAt(),
        s.getVersion());
  }

  private String token() {
    byte[] b = new byte[32];
    random.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  private String password(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      byte[] salt = new byte[16];
      random.nextBytes(salt);
      byte[] hash =
          SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
              .generateSecret(new PBEKeySpec(value.toCharArray(), salt, 120000, 256))
              .getEncoded();
      return Base64.getEncoder().encodeToString(salt)
          + ":"
          + Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private boolean verify(String value, String stored) {
    if (stored == null) return true;
    if (value == null) return false;
    try {
      String[] p = stored.split(":");
      byte[] salt = Base64.getDecoder().decode(p[0]);
      byte[] expected = Base64.getDecoder().decode(p[1]);
      byte[] actual =
          SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
              .generateSecret(new PBEKeySpec(value.toCharArray(), salt, 120000, 256))
              .getEncoded();
      return MessageDigest.isEqual(expected, actual);
    } catch (Exception e) {
      return false;
    }
  }

  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception e) {
      throw error("REL_SERIALIZATION_ERROR", "分享快照生成失败");
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private void updated(boolean ok) {
    if (!ok) throw error("REL_CONCURRENT_UPDATE", "数据已被并发修改");
  }

  private BusinessException error(String code, String message) {
    return new BusinessException(code, message);
  }
}
