package com.lingxi.operations.application;

import com.lingxi.identity.api.IdentityFacade;
import com.lingxi.identity.api.PrivacyFacade;
import com.lingxi.kernel.*;
import com.lingxi.operations.api.*;
import com.lingxi.operations.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class OperationsApplicationService implements OperationsFacade {
  private final OperationsRepository repo;
  private final IdentityFacade identities;
  private final IdGenerator ids;
  private final AuditService audit;
  private final ConfigPublisherAdapter publisher;
  private final AdminPermissionAdapter permissions;
  private final PrivacyFacade privacy;

  public OperationsApplicationService(
      OperationsRepository repo,
      IdentityFacade identities,
      IdGenerator ids,
      AuditService audit,
      List<ConfigPublisherAdapter> publishers,
      List<AdminPermissionAdapter> permissions,
      PrivacyFacade privacy) {
    this.repo = repo;
    this.identities = identities;
    this.ids = ids;
    this.audit = audit;
    publisher = publishers.stream().findFirst().orElse(null);
    this.permissions = permissions.stream().findFirst().orElse(null);
    this.privacy = privacy;
  }

  @Transactional
  public SupportTicketResult createTicket(CreateSupportTicketCommand c) {
    identities.getAccessProfile(c.userId());
    LocalDateTime now = now();
    SupportTicket t =
        SupportTicket.create(
            ids.nextId(),
            "TK" + ids.nextId(),
            c.userId(),
            c.category(),
            c.subject(),
            c.description(),
            c.priority(),
            now);
    repo.insertTicket(t);
    repo.appendTicketHistory(ids.nextId(),t.getId(),"CREATED",null,t.getStatus().name(),
        "USER",c.userId(),"工单创建",now);
    // 用户首条描述同时作为工单对话消息持久化，后台详情可按对话查看原始诉求。
    repo.appendTicketMessage(ids.nextId(),t.getId(),"USER",c.userId(),c.description(),false,now);
    return result(t);
  }

  @Transactional(readOnly = true)
  public SupportTicketResult getTicket(long user, long id) {
    SupportTicket t = ticket(id);
    t.owner(user);
    return result(t);
  }

  @Transactional
  public SupportTicketResult transitionTicket(TransitionTicketCommand c) {
    secure(c.adminId(), "support.ticket.manage", c.audit());
    SupportTicket t = ticket(c.ticketId());
    long v = t.getVersion();
    String before = digest(t.getStatus().name() + ":" + v);
    String beforeStatus = t.getStatus().name();
    SupportTicket.Status targetStatus;
    try {
      targetStatus = SupportTicket.Status.valueOf(c.status());
    } catch (IllegalArgumentException | NullPointerException ignored) {
      throw error("OPS_TICKET_STATUS_INVALID", "工单状态不合法");
    }
    t.transition(targetStatus, c.assigneeAdminId(), c.expectedVersion(), now());
    updated(repo.updateTicket(t, v));
    repo.appendTicketHistory(ids.nextId(),t.getId(),"STATUS_TRANSITION",beforeStatus,
        t.getStatus().name(),"ADMIN",c.adminId(),"后台流转",now());
    if (targetStatus == SupportTicket.Status.RESOLVED
        && "PRIVACY_CORRECTION".equals(t.getCategory())) {
      repo.findTicketPrivacyRequestId(t.getId())
          .ifPresent(requestId -> privacy.completeCorrection(t.getUserId(), requestId, t.getId()));
    }
    if (targetStatus == SupportTicket.Status.RESOLVED
        && "ACCOUNT_CLOSURE_TRANSACTION".equals(t.getCategory())) {
      repo.findTicketPrivacyRequestId(t.getId())
          .ifPresent(requestId -> privacy.resumeAccountClosure(t.getUserId(), requestId, t.getId()));
    }
    audit.append(
        c.adminId(),
        "TICKET_TRANSITION",
        "SUPPORT_TICKET",
        String.valueOf(t.getId()),
        c.audit(),
        before,
        digest(t.getStatus().name() + ":" + t.getVersion()),
        "SUCCESS");
    return result(t);
  }

  @Transactional
  public ConfigReleaseResult createRelease(CreateConfigReleaseCommand c) {
    secure(c.adminId(), "config.release.create", c.audit());
    var old = repo.findReleaseByKey(c.releaseKey());
    if (old.isPresent()) {
      ConfigRelease existing = old.get();
      if (!existing.getConfigType().equals(c.configType())
          || existing.getVersionNo() != c.versionNo()
          || !existing.getContentRef().equals(c.contentRef())
          || !existing.getContentDigest().equals(c.contentDigest())
          || !Objects.equals(existing.getGrayRule(), c.grayRule())) {
        throw error("OPS_RELEASE_IDEMPOTENCY_CONFLICT", "发布键对应不同配置内容");
      }
      return result(existing);
    }
    ConfigRelease r =
        ConfigRelease.create(
            ids.nextId(),
            c.releaseKey(),
            c.configType(),
            c.versionNo(),
            c.contentRef(),
            c.contentDigest(),
            c.grayRule(),
            c.adminId(),
            now());
    repo.insertRelease(r);
    audit.append(
        c.adminId(),
        "RELEASE_CREATE",
        "CONFIG_RELEASE",
        String.valueOf(r.getId()),
        c.audit(),
        null,
        r.getContentDigest(),
        "SUCCESS");
    return result(r);
  }

  @Transactional
  public ConfigReleaseResult validateRelease(AdminActionCommand c) {
    secure(c.adminId(), "config.release.validate", c.audit());
    ConfigRelease r = release(c.releaseId());
    long old = r.getVersion();
    String before = r.getContentDigest() + ":" + r.getStatus();
    if (publisher == null) {
      r.fail("发布适配器尚未配置", c.expectedVersion(), now());
    } else {
      var validation = publisher.validate(r);
      if (validation.valid()) r.validated(c.expectedVersion(), now());
      else r.fail(validation.reason(), c.expectedVersion(), now());
    }
    updated(repo.updateRelease(r, old));
    audit.append(
        c.adminId(),
        "RELEASE_VALIDATE",
        "CONFIG_RELEASE",
        String.valueOf(r.getId()),
        c.audit(),
        digest(before),
        digest(r.getContentDigest() + ":" + r.getStatus()),
        r.getStatus() == ConfigRelease.Status.PENDING_APPROVAL ? "SUCCESS" : "FAILED");
    return result(r);
  }

  @Transactional
  public ConfigReleaseResult approveRelease(AdminActionCommand c) {
    secure(c.adminId(), "config.release.approve", c.audit());
    return mutate(
        c, "RELEASE_APPROVE", (r) -> r.approve(c.adminId(), c.expectedVersion(), now()), null);
  }

  @Transactional
  public ConfigReleaseResult startGray(AdminActionCommand c) {
    secure(c.adminId(), "config.release.publish", c.audit());
    if (publisher == null) throw error("OPS_PUBLISHER_UNAVAILABLE", "配置发布适配器尚未配置");
    ConfigRelease r = release(c.releaseId());
    Long previous = repo.findPublished(r.getConfigType()).map(ConfigRelease::getId).orElse(null);
    long old = r.getVersion();
    String before = digest(r.getStatus() + ":" + r.getVersion());
    r.gray(c.adminId(), c.expectedVersion(), previous, now());
    publisher.startGray(r);
    updated(repo.updateRelease(r, old));
    audit.append(
        c.adminId(),
        "RELEASE_GRAY",
        "CONFIG_RELEASE",
        String.valueOf(r.getId()),
        c.audit(),
        before,
        digest(r.getStatus() + ":" + r.getVersion()),
        "SUCCESS");
    return result(r);
  }

  @Transactional
  public ConfigReleaseResult publishRelease(AdminActionCommand c) {
    secure(c.adminId(), "config.release.publish", c.audit());
    if (publisher == null) throw error("OPS_PUBLISHER_UNAVAILABLE", "配置发布适配器尚未配置");
    return mutate(
        c,
        "RELEASE_PUBLISH",
        (r) -> {
          r.publish(c.adminId(), c.expectedVersion(), now());
          publisher.publish(r);
        },
        null);
  }

  @Transactional
  public ConfigReleaseResult rollbackRelease(AdminActionCommand c) {
    secure(c.adminId(), "config.release.rollback", c.audit());
    if (publisher == null) throw error("OPS_PUBLISHER_UNAVAILABLE", "配置发布适配器尚未配置");
    return mutate(
        c,
        "RELEASE_ROLLBACK",
        (r) -> {
          r.rollback(c.adminId(), c.expectedVersion(), now());
          publisher.rollback(r);
        },
        null);
  }

  private ConfigReleaseResult mutate(
      AdminActionCommand c,
      String action,
      java.util.function.Consumer<ConfigRelease> change,
      String ignored) {
    ConfigRelease r = release(c.releaseId());
    long old = r.getVersion();
    String before = digest(r.getStatus() + ":" + r.getVersion());
    change.accept(r);
    updated(repo.updateRelease(r, old));
    audit.append(
        c.adminId(),
        action,
        "CONFIG_RELEASE",
        String.valueOf(r.getId()),
        c.audit(),
        before,
        digest(r.getStatus() + ":" + r.getVersion()),
        "SUCCESS");
    return result(r);
  }

  private void secure(long admin, String permission, AuditContext c) {
    if (c == null
        || !c.recentAuthentication()
        || c.reason() == null
        || c.reason().isBlank()
        || c.ticketNo() == null
        || c.ticketNo().isBlank())
      throw error("OPS_HIGH_RISK_CONTEXT_REQUIRED", "管理操作需要近期认证、原因和工单号");
    if (permissions == null || !permissions.allowed(admin, permission))
      throw error("OPS_ADMIN_FORBIDDEN", "管理员权限不足");
  }

  private SupportTicket ticket(long id) {
    return repo.findTicket(id).orElseThrow(() -> error("OPS_TICKET_NOT_FOUND", "工单不存在"));
  }

  private ConfigRelease release(long id) {
    return repo.findRelease(id).orElseThrow(() -> error("OPS_RELEASE_NOT_FOUND", "配置发布不存在"));
  }

  private SupportTicketResult result(SupportTicket t) {
    return new SupportTicketResult(
        t.getId(),
        t.getTicketNo(),
        t.getUserId(),
        t.getCategory(),
        t.getSubject(),
        t.getStatus().name(),
        t.getPriority(),
        t.getAssigneeAdminId(),
        t.getVersion(),
        t.getCreatedAt());
  }

  private ConfigReleaseResult result(ConfigRelease r) {
    return new ConfigReleaseResult(
        r.getId(),
        r.getReleaseKey(),
        r.getConfigType(),
        r.getVersionNo(),
        r.getStatus().name(),
        r.getCreatedBy(),
        r.getApprovedBy(),
        r.getPublishedBy(),
        r.getPreviousReleaseId(),
        r.getFailureReason(),
        r.getVersion());
  }

  private String digest(String s) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private void updated(boolean ok) {
    if (!ok) throw error("OPS_CONCURRENT_UPDATE", "数据已被并发修改");
  }

  private BusinessException error(String c, String m) {
    return new BusinessException(c, m);
  }
}
