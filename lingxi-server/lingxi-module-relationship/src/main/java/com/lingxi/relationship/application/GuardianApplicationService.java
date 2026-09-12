package com.lingxi.relationship.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.AccessProfile;
import com.lingxi.identity.api.AccountStatus;
import com.lingxi.identity.api.ActivateTeenCommand;
import com.lingxi.identity.api.AgeBand;
import com.lingxi.identity.api.IdentityFacade;
import com.lingxi.identity.api.RestrictTeenCommand;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.kernel.PageResult;
import com.lingxi.relationship.api.AcceptGuardianInvitationCommand;
import com.lingxi.relationship.api.CreateGuardianInvitationCommand;
import com.lingxi.relationship.api.GuardianFacade;
import com.lingxi.relationship.api.GuardianInvitationResult;
import com.lingxi.relationship.api.GuardianPermissionType;
import com.lingxi.relationship.api.GuardianRelationResult;
import com.lingxi.relationship.api.RevokeGuardianRelationCommand;
import com.lingxi.relationship.domain.GuardianDisputeRepository;
import com.lingxi.relationship.domain.GuardianRelation;
import com.lingxi.relationship.domain.GuardianRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

/** 监护邀请、验证、最小权限和失效限制的应用服务。 */
@Service
public class GuardianApplicationService implements GuardianFacade {
  private static final Duration INVITATION_TTL = Duration.ofDays(3);
  /** 列表类查询允许的最大页大小。 */
  private static final int MAX_PAGE_SIZE = 200;
  private final GuardianRepository repository;
  private final GuardianDisputeRepository disputes;
  private final IdentityFacade identityFacade;
  private final IdGenerator idGenerator;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Autowired
  public GuardianApplicationService(
      GuardianRepository repository,
      GuardianDisputeRepository disputes,
      IdentityFacade identityFacade,
      IdGenerator idGenerator,
      ObjectMapper objectMapper) {
    this(repository, disputes, identityFacade, idGenerator, objectMapper, Clock.systemUTC());
  }

  GuardianApplicationService(
      GuardianRepository repository,
      GuardianDisputeRepository disputes,
      IdentityFacade identityFacade,
      IdGenerator idGenerator,
      ObjectMapper objectMapper,
      Clock clock) {
    this.repository = repository;
    this.disputes = disputes;
    this.identityFacade = identityFacade;
    this.idGenerator = idGenerator;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  @Transactional
  public GuardianInvitationResult createInvitation(CreateGuardianInvitationCommand command) {
    if (command == null
        || command.teenUserId() <= 0
        || command.requestKey() == null
        || command.requestKey().isBlank()
        || command.permissions() == null
        || command.permissions().isEmpty()) {
      throw new BusinessException("GUARDIAN_INVALID_INVITATION", "监护邀请参数不完整");
    }
    AccessProfile teen = identityFacade.getAccessProfile(command.teenUserId());
    if (teen.ageBand() != AgeBand.TEEN
        || (teen.status() != AccountStatus.PENDING_GUARDIAN
            && teen.status() != AccountStatus.RESTRICTED
            && teen.status() != AccountStatus.ACTIVE_TEEN)) {
      throw new BusinessException("GUARDIAN_TEEN_NOT_ELIGIBLE", "当前账号不能创建监护邀请");
    }
    String permissionsJson = serialize(sorted(command.permissions()));
    String digest = hash(command.teenUserId() + "|" + permissionsJson);
    GuardianRelation existing = repository.findByRequestKey(command.requestKey()).orElse(null);
    if (existing != null) {
      if (!existing.getRequestDigest().equals(digest)) {
        throw new BusinessException("GUARDIAN_IDEMPOTENCY_CONFLICT", "同一请求不能创建不同邀请");
      }
      return invitation(existing, null);
    }
    String rawToken = idGenerator.nextPublicId();
    LocalDateTime now = utc(clock.instant());
    GuardianRelation relation =
        GuardianRelation.invite(
            idGenerator.nextId(),
            command.requestKey(),
            digest,
            command.teenUserId(),
            hash(rawToken),
            permissionsJson,
            now.plus(INVITATION_TTL),
            now);
    repository.insert(relation);
    return invitation(relation, rawToken);
  }

  @Override
  @Transactional
  public GuardianRelationResult acceptInvitation(AcceptGuardianInvitationCommand command) {
    if (command == null || command.guardianUserId() <= 0 || command.invitationToken() == null) {
      throw new BusinessException("GUARDIAN_INVALID_ACCEPTANCE", "接受邀请参数不完整");
    }
    GuardianRelation relation =
        repository
            .findByInvitationTokenHash(hash(command.invitationToken()))
            .orElseThrow(() -> new BusinessException("GUARDIAN_INVITATION_INVALID", "监护邀请无效或已过期"));
    AccessProfile guardian = identityFacade.getAccessProfile(command.guardianUserId());
    if (guardian.ageBand() != AgeBand.ADULT || guardian.status() != AccountStatus.ACTIVE_ADULT) {
      throw new BusinessException("GUARDIAN_ADULT_REQUIRED", "监护人必须是已激活成人账号");
    }
    long previous = relation.getVersion();
    assertNoOpenDispute(relation.getId());
    relation.accept(command.guardianUserId(), utc(clock.instant()));
    if (!repository.update(relation, previous)) {
      throw new BusinessException("GUARDIAN_VERSION_CONFLICT", "监护关系已变化");
    }
    List<GuardianPermissionType> permissions = parse(relation.getRequestedPermissionsJson());
    repository.insertPermissions(relation.getId(), permissions, utc(clock.instant()));
    identityFacade.activateTeenAccount(
        new ActivateTeenCommand(relation.getTeenUserId(), relation.getId()));
    return result(relation, permissions);
  }

  @Override
  @Transactional
  public GuardianRelationResult revokeRelation(RevokeGuardianRelationCommand command) {
    if (command == null || command.reason() == null || command.reason().isBlank()) {
      throw new BusinessException("GUARDIAN_REVOKE_REASON_REQUIRED", "撤销监护关系必须填写原因");
    }
    GuardianRelation relation =
        repository
            .findById(command.relationId())
            .orElseThrow(() -> new BusinessException("GUARDIAN_RELATION_NOT_FOUND", "监护关系不存在"));
    long previous = relation.getVersion();
    assertNoOpenDispute(relation.getId());
    relation.revoke(command.operatorUserId(), command.reason(), utc(clock.instant()));
    if (!repository.update(relation, previous)) {
      throw new BusinessException("GUARDIAN_VERSION_CONFLICT", "监护关系已变化");
    }
    if (repository.countActiveRelations(relation.getTeenUserId()) == 0) {
      identityFacade.restrictTeenAccount(
          new RestrictTeenCommand(
              relation.getTeenUserId(), relation.getId(), "LAST_GUARDIAN_REVOKED"));
    }
    return result(relation, repository.findPermissions(relation.getId()));
  }

  @Override
  @Transactional(readOnly = true)
  public GuardianRelationResult getRelation(long relationId, long participantUserId) {
    GuardianRelation relation =
        repository
            .findById(relationId)
            .orElseThrow(() -> new BusinessException("GUARDIAN_RELATION_NOT_FOUND", "监护关系不存在"));
    relation.assertParticipant(participantUserId);
    return result(relation, repository.findPermissions(relationId));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<GuardianRelationResult> listRelations(
      long participantUserId, int page, int pageSize) {
    if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new BusinessException("GUARDIAN_INVALID_QUERY", "监护关系查询参数不合法");
    }
    long total = repository.countRelationsByParticipant(participantUserId);
    List<GuardianRelationResult> items =
        repository.findRelationsByParticipant(participantUserId, page, pageSize).stream()
            .map(relation -> result(relation, repository.findPermissions(relation.getId())))
            .toList();
    return new PageResult<>(items, total, page, pageSize);
  }

  private void assertNoOpenDispute(long relationId) {
    if (disputes.hasOpenDispute(relationId)) {
      throw new BusinessException(
          "GUARDIAN_DISPUTE_FROZEN", "监护关系存在未决争议，暂不能变更关系");
    }
  }

  private GuardianInvitationResult invitation(GuardianRelation r, String rawToken) {
    return new GuardianInvitationResult(
        r.getId(),
        rawToken,
        r.getStatus().name(),
        r.getExpiresAt().toInstant(ZoneOffset.UTC),
        Set.copyOf(parse(r.getRequestedPermissionsJson())));
  }

  private GuardianRelationResult result(
      GuardianRelation r, List<GuardianPermissionType> permissions) {
    Instant effective =
        r.getVerifiedAt() == null ? null : r.getVerifiedAt().toInstant(ZoneOffset.UTC);
    return new GuardianRelationResult(
        r.getId(),
        r.getTeenUserId(),
        r.getGuardianUserId(),
        r.getStatus().name(),
        Set.copyOf(permissions),
        effective);
  }

  private List<GuardianPermissionType> sorted(Set<GuardianPermissionType> values) {
    List<GuardianPermissionType> result = new ArrayList<>(values);
    result.sort(Comparator.comparing(Enum::name));
    return result;
  }

  private String serialize(List<GuardianPermissionType> permissions) {
    try {
      return objectMapper.writeValueAsString(permissions);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private List<GuardianPermissionType> parse(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<List<GuardianPermissionType>>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("监护权限快照损坏", e);
    }
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private LocalDateTime utc(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}
