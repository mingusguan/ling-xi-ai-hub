package com.lingxi.identity.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingxi.identity.api.*;
import com.lingxi.identity.infrastructure.persistence.UserEntity;
import com.lingxi.identity.infrastructure.persistence.UserMapper;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.PageResult;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户后台用例：仅提供脱敏查询和受控限制/解限，不允许手工修改年龄模式。 */
@Service
public class IdentityAdminApplicationService implements AdminUserFacade {
  private static final Set<String> QUERYABLE_AGE_BANDS = Set.of("UNDER_14", "TEEN", "ADULT");
  private static final Set<String> QUERYABLE_STATUSES = Set.of(
      "PENDING_GUARDIAN", "ACTIVE_TEEN", "ACTIVE_ADULT", "RESTRICTED",
      "AGE_TRANSITION", "CLOSING", "CLOSED");

  private final UserMapper users;
  private final AdminAuthorizationFacade admins;
  private final AuthenticationFacade authentication;
  private final com.lingxi.kernel.DomainEventPublisher events;
  private final GuardianStatusProvider guardianStatus;

  public IdentityAdminApplicationService(
      UserMapper users,
      AdminAuthorizationFacade admins,
      AuthenticationFacade authentication,
      com.lingxi.kernel.DomainEventPublisher events,
      List<GuardianStatusProvider> guardianStatusProviders) {
    this.users = users;
    this.admins = admins;
    this.authentication = authentication;
    this.events = events;
    this.guardianStatus = guardianStatusProviders.stream().findFirst().orElse(userId -> false);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<AdminUserSummary> listUsers(AdminUserQuery query) {
    require(query == null ? 0 : currentAdmin(), "identity:user:read");
    int page = query == null ? 1 : Math.max(1, query.page());
    int size = query == null ? 20 : Math.min(100, Math.max(1, query.pageSize()));
    var wrapper = Wrappers.<UserEntity>lambdaQuery()
        .orderByDesc(UserEntity::getCreatedAt);
    if (query != null && query.keyword() != null && !query.keyword().isBlank()) {
      wrapper.like(UserEntity::getPublicId, query.keyword().trim());
    }
    if (query != null && query.ageBand() != null && !query.ageBand().isBlank()) {
      if (!QUERYABLE_AGE_BANDS.contains(query.ageBand())) invalidFilter();
      wrapper.eq(UserEntity::getAgeBand, query.ageBand());
    }
    if (query != null && query.status() != null && !query.status().isBlank()) {
      if (!QUERYABLE_STATUSES.contains(query.status())) invalidFilter();
      wrapper.eq(UserEntity::getStatus, query.status());
    }
    Page<UserEntity> result = users.selectPage(Page.of(page, size), wrapper);
    return new PageResult<>(result.getRecords().stream().map(this::summary).toList(),
        result.getTotal(), page, size);
  }

  @Override
  @Transactional
  public AdminUserSummary changeStatus(AdminUserStatusCommand command) {
    if (command == null || command.operatorAdminId() <= 0) invalidCommand();
    require(command.operatorAdminId(), "identity:user:restrict");
    if (!command.recentAuthentication() || blank(command.reason()) || blank(command.ticketNo())) {
      throw new BusinessException("ADMIN_HIGH_RISK_CONTEXT_REQUIRED",
          "限制账号需要近期认证、操作原因和工单号");
    }
    UserEntity user = users.selectById(command.userId());
    if (user == null) throw new BusinessException("IDENTITY_USER_NOT_FOUND", "用户不存在");
    if (user.getVersion() != command.expectedVersion()) {
      throw new BusinessException("IDENTITY_CONCURRENT_UPDATE", "用户状态已被其他管理员修改");
    }
    String target = command.targetStatus();
    if (!"RESTRICTED".equals(target) && !expectedActiveStatus(user).equals(target)) {
      throw new BusinessException("IDENTITY_ADMIN_STATUS_INVALID",
          "后台仅允许限制账号或恢复到与可信年龄一致的活动状态");
    }
    if ("CLOSING".equals(user.getStatus()) || "CLOSED".equals(user.getStatus())) {
      throw new BusinessException("IDENTITY_ADMIN_STATUS_FORBIDDEN", "注销流程中的账号不能后台解限");
    }
    if ("ACTIVE_TEEN".equals(target) && !guardianStatus.hasActiveGuardian(user.getId())) {
      throw new BusinessException("IDENTITY_ACTIVE_GUARDIAN_REQUIRED",
          "青少年账号恢复前必须存在有效监护关系");
    }
    long nextAuthorizationVersion = user.getAuthorizationVersion() + 1;
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    int updated = users.update(null, Wrappers.<UserEntity>lambdaUpdate()
        .eq(UserEntity::getId, user.getId())
        .eq(UserEntity::getVersion, command.expectedVersion())
        .set(UserEntity::getStatus, target)
        .set(UserEntity::getAuthorizationVersion, nextAuthorizationVersion)
        .set(UserEntity::getVersion, command.expectedVersion() + 1)
        .set(UserEntity::getUpdatedAt, now));
    if (updated != 1) throw new BusinessException("IDENTITY_CONCURRENT_UPDATE", "用户状态已变化");
    authentication.revokeAllSessions(user.getId(), "ADMIN_STATUS_CHANGED");
    events.publish(new AdminActionAuditedEvent(java.util.UUID.randomUUID().toString(),
        command.operatorAdminId(), "USER_STATUS_CHANGE", "USER", String.valueOf(user.getId()),
        command.expectedVersion() + 1, command.reason(), command.ticketNo(), java.time.Instant.now()));
    user.setStatus(target);
    user.setAuthorizationVersion(nextAuthorizationVersion);
    user.setVersion(command.expectedVersion() + 1);
    user.setUpdatedAt(now);
    return summary(user);
  }

  private long currentAdmin() {
    return com.lingxi.kernel.ActorContextHolder.requireAdmin().actorId();
  }

  private void require(long adminId, String permission) {
    if (adminId <= 0 || !admins.allowed(adminId, permission)) {
      throw new BusinessException("ADMIN_FORBIDDEN", "管理员权限不足");
    }
  }

  private String expectedActiveStatus(UserEntity user) {
    return switch (user.getAgeBand()) {
      case "TEEN" -> "ACTIVE_TEEN";
      case "ADULT" -> "ACTIVE_ADULT";
      default -> "PENDING_GUARDIAN";
    };
  }

  private AdminUserSummary summary(UserEntity user) {
    return new AdminUserSummary(user.getId(), user.getPublicId(), user.getAgeBand(),
        user.getStatus(), user.getTimezone(), user.getAuthorizationVersion(), user.getVersion(),
        user.getCreatedAt(), user.getUpdatedAt());
  }

  private void invalidFilter() {
    throw new BusinessException("IDENTITY_ADMIN_FILTER_INVALID", "查询筛选值不合法");
  }
  private void invalidCommand() {
    throw new BusinessException("IDENTITY_ADMIN_COMMAND_INVALID", "管理命令不完整");
  }
  private boolean blank(String value) { return value == null || value.isBlank(); }
}
