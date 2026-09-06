package com.lingxi.operations.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.AdminActionAuditedEvent;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.operations.api.AuditContext;
import com.lingxi.operations.api.OperationsGovernanceFacade;
import com.lingxi.operations.domain.OperationsGovernanceRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 运营治理用例：权限、近期认证、参数与审计在同一事务边界内完成。 */
@Service
public class OperationsGovernanceApplicationService implements OperationsGovernanceFacade {
  private final OperationsGovernanceRepository repository;
  private final AdminPermissionAdapter permissions;
  private final IdGenerator ids;
  private final DomainEventPublisher events;
  private final ObjectMapper json;

  public OperationsGovernanceApplicationService(
      OperationsGovernanceRepository repository,
      AdminPermissionAdapter permissions,
      IdGenerator ids,
      DomainEventPublisher events,
      ObjectMapper json) {
    this.repository = repository;
    this.permissions = permissions;
    this.ids = ids;
    this.events = events;
    this.json = json;
  }

  @Override
  @Transactional
  public ManagedResource saveCampaign(long adminId, CampaignCommand c) {
    require(adminId, "message:campaign:manage");
    highRisk(c.context());
    required(c.campaignKey(), c.name(), c.channel(), c.templateContent());
    validateMarketingAudience(c.audienceRule());
    json(c.frequencyRule());
    draftOnly(c.id(), c.status(), "CAMPAIGN");
    if (c.teenMarketingEnabled())
      throw new BusinessException("OPS_TEEN_MARKETING_FORBIDDEN", "14-17 岁用户不得进入营销人群");
    ManagedResource result = repository.saveCampaign(id(c.id()), c, now());
    audit(adminId, "CAMPAIGN_SAVE", result, c.context());
    return result;
  }

  @Override
  @Transactional
  public ManagedResource saveAppRelease(long adminId, AppReleaseCommand c) {
    require(adminId, "app:release:manage");
    highRisk(c.context());
    required(c.platform(), c.versionName(), c.releaseNotes());
    oneOf(c.platform(), Set.of("PC_WEB", "HARMONY"));
    draftOnly(c.id(), c.status(), "APP_RELEASE");
    if (c.versionCode() <= 0
        || c.minimumVersionCode() <= 0
        || c.minimumVersionCode() > c.versionCode()) invalid("客户端版本号不合法");
    jsonOptional(c.grayRule());
    ManagedResource result = repository.saveAppRelease(id(c.id()), c, now());
    audit(adminId, "APP_RELEASE_SAVE", result, c.context());
    return result;
  }

  @Override
  @Transactional
  public ManagedResource saveComplianceDocument(long adminId, ComplianceDocumentCommand c) {
    require(adminId, "compliance:manage");
    highRisk(c.context());
    required(c.documentType(), c.versionNo(), c.title(), c.contentRef(), c.contentDigest());
    if (!c.contentDigest().matches("[0-9a-fA-F]{64}")) invalid("合规文档摘要必须是 SHA-256");
    draftOnly(c.id(), c.status(), "COMPLIANCE");
    ManagedResource result = repository.saveComplianceDocument(id(c.id()), c, now());
    audit(adminId, "COMPLIANCE_SAVE", result, c.context());
    return result;
  }

  @Override
  @Transactional
  public ManagedResource saveFeatureFlag(long adminId, FeatureFlagCommand c) {
    require(adminId, "feature:flag:manage");
    highRisk(c.context());
    required(c.flagKey());
    draftOnly(c.id(), c.status(), "FEATURE_FLAG");
    if (c.currentReleaseId() != null)
      throw new BusinessException("OPS_DRAFT_RELEASE_BINDING_FORBIDDEN", "草稿不得直接绑定运行发布单");
    OperationsGovernanceRepository.ResourceState existing = existing(c.id(), "FEATURE_FLAG");
    if (existing != null && existing.mandatoryPolicy() != c.mandatoryPolicy())
      throw new BusinessException("OPS_MANDATORY_POLICY_IMMUTABLE", "强制安全策略属性创建后不可修改");
    ManagedResource result = repository.saveFeatureFlag(id(c.id()), c, now());
    audit(adminId, "FEATURE_FLAG_SAVE", result, c.context());
    return result;
  }

  @Override
  @Transactional
  public ManagedResource saveExperiment(long adminId, ExperimentCommand c) {
    require(adminId, "experiment:manage");
    highRisk(c.context());
    required(c.experimentKey(), c.hypothesis());
    validateExperimentAudience(c.audienceRule());
    json(c.metricsJson());
    draftOnly(c.id(), c.status(), "EXPERIMENT");
    if (c.currentReleaseId() != null)
      throw new BusinessException("OPS_DRAFT_RELEASE_BINDING_FORBIDDEN", "草稿不得直接绑定运行发布单");
    ManagedResource result = repository.saveExperiment(id(c.id()), c, now());
    audit(adminId, "EXPERIMENT_SAVE", result, c.context());
    return result;
  }

  @Override
  @Transactional
  public ManagedResource changeRuntimeStatus(long adminId, RuntimeStatusCommand c) {
    String permission = permission(c.resourceType());
    require(adminId, permission);
    highRisk(c.context());
    OperationsGovernanceRepository.ResourceState current =
        requiredResource(c.resourceType(), c.id());
    if (current.version() != c.expectedVersion())
      throw new BusinessException("OPS_RESOURCE_CONFLICT", "治理资源已变化，请刷新后重试");
    Set<String> allowed = runtimeStatuses(c.resourceType());
    oneOf(c.targetStatus(), allowed);
    validateRuntimeTransition(c.resourceType(), current.status(), c.targetStatus());
    if (current.mandatoryPolicy() && !"ACTIVE".equals(c.targetStatus()))
      throw new BusinessException("OPS_MANDATORY_POLICY_DISABLE_FORBIDDEN", "强制安全策略不可关闭、暂停或退役");
    String configType = configType(c.resourceType());
    String contentRef = "governance://" + c.resourceType() + "/" + c.id();
    if (!repository.isPublishedRelease(c.releaseId(), configType, contentRef))
      throw new BusinessException(
          "OPS_PUBLISHED_RELEASE_REQUIRED", "资源运行状态必须关联类型、内容引用匹配且已发布的配置发布单");
    ManagedResource result =
        repository.changeRuntimeStatus(
            c.resourceType(), c.id(), c.releaseId(), c.targetStatus(), c.expectedVersion(), now());
    audit(adminId, "RESOURCE_STATUS_CHANGE", result, c.context());
    return result;
  }

  @Override
  @Transactional
  public ManagedResource retire(long adminId, RetireResourceCommand c) {
    String permission =
        switch (c.resourceType()) {
          case "CAMPAIGN" -> "message:campaign:manage";
          case "APP_RELEASE" -> "app:release:manage";
          case "COMPLIANCE" -> "compliance:manage";
          case "FEATURE_FLAG" -> "feature:flag:manage";
          case "EXPERIMENT" -> "experiment:manage";
          default -> throw new BusinessException("OPS_RESOURCE_TYPE_INVALID", "治理资源类型不合法");
        };
    require(adminId, permission);
    highRisk(c.context());
    OperationsGovernanceRepository.ResourceState current =
        requiredResource(c.resourceType(), c.id());
    if (current.mandatoryPolicy())
      throw new BusinessException("OPS_MANDATORY_POLICY_RETIRE_FORBIDDEN", "强制安全策略不可退役");
    if (!Set.of("DRAFT", "COMPLETED", "CANCELLED", "RETIRED").contains(current.status()))
      throw new BusinessException("OPS_ACTIVE_RESOURCE_RETIRE_FORBIDDEN", "运行中的资源必须先通过发布流程停止或回滚");
    ManagedResource result =
        repository.retire(c.resourceType(), c.id(), c.expectedVersion(), now());
    audit(adminId, "RESOURCE_RETIRE", result, c.context());
    return result;
  }

  @Override
  @Transactional
  public TicketMessageResult replyTicket(long adminId, TicketReplyCommand c) {
    require(adminId, "support:ticket:reply");
    context(c.context(), false);
    required(c.content());
    TicketMessageResult result =
        repository.replyTicket(ids.nextId(), ids.nextId(), adminId, c, now());
    audit(
        adminId,
        "SUPPORT_TICKET_REPLY",
        new ManagedResource(
            c.ticketId(),
            "SUPPORT_TICKET",
            String.valueOf(c.ticketId()),
            "REPLIED",
            0,
            result.createdAt()),
        c.context());
    return result;
  }

  @Override
  @Transactional
  public MetricResult recordMetric(long adminId, MetricCommand c) {
    require(adminId, "analytics:write");
    highRisk(c.context());
    required(c.metricKey(), c.dimensionType(), c.dimensionValue());
    if (c.metricDate() == null || c.metricValue() == null || c.sampleCount() < 0)
      invalid("指标事实不完整");
    MetricResult result = repository.recordMetric(ids.nextId(), c, now());
    audit(
        adminId,
        "METRIC_UPSERT",
        new ManagedResource(ids.nextId(), "METRIC", c.metricKey(), "RECORDED", 0, now()),
        c.context());
    return result;
  }

  private void audit(long adminId, String action, ManagedResource r, AuditContext c) {
    events.publish(
        new AdminActionAuditedEvent(
            UUID.randomUUID().toString(),
            adminId,
            action,
            r.resourceType(),
            String.valueOf(r.id()),
            r.version(),
            c.reason(),
            c.ticketNo(),
            Instant.now()));
  }

  private long id(long current) {
    return current == 0 ? ids.nextId() : current;
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private void require(long adminId, String permission) {
    if (adminId <= 0 || !permissions.allowed(adminId, permission))
      throw new BusinessException("ADMIN_FORBIDDEN", "管理员权限不足");
  }

  private void highRisk(AuditContext c) {
    context(c, true);
  }

  private void context(AuditContext c, boolean recent) {
    if (c == null
        || blank(c.reason())
        || blank(c.ticketNo())
        || (recent && !c.recentAuthentication()))
      throw new BusinessException("ADMIN_HIGH_RISK_CONTEXT_REQUIRED", "操作需要近期认证、具体原因和工单号");
  }

  private void json(String value) {
    required(value);
    try {
      json.readTree(value);
    } catch (Exception e) {
      invalid("JSON 规则不合法");
    }
  }

  private void jsonOptional(String value) {
    if (!blank(value)) json(value);
  }

  private void draftOnly(long id, String status, String type) {
    if (!"DRAFT".equals(status))
      throw new BusinessException("OPS_RELEASE_WORKFLOW_REQUIRED", "治理资源只能保存为草稿，运行状态必须经过配置发布流程");
    OperationsGovernanceRepository.ResourceState current = existing(id, type);
    if (current != null && !"DRAFT".equals(current.status()))
      throw new BusinessException("OPS_PUBLISHED_RESOURCE_IMMUTABLE", "已进入运行流程的资源不可原地覆盖，请创建新版本");
  }

  private OperationsGovernanceRepository.ResourceState existing(long id, String type) {
    return id == 0 ? null : repository.findResource(type, id);
  }

  private OperationsGovernanceRepository.ResourceState requiredResource(String type, long id) {
    OperationsGovernanceRepository.ResourceState state = repository.findResource(type, id);
    if (state == null) throw new BusinessException("OPS_RESOURCE_NOT_FOUND", "治理资源不存在");
    return state;
  }

  private String permission(String type) {
    return switch (type) {
      case "CAMPAIGN" -> "message:campaign:manage";
      case "APP_RELEASE" -> "app:release:manage";
      case "COMPLIANCE" -> "compliance:manage";
      case "FEATURE_FLAG" -> "feature:flag:manage";
      case "EXPERIMENT" -> "experiment:manage";
      default -> throw new BusinessException("OPS_RESOURCE_TYPE_INVALID", "治理资源类型不合法");
    };
  }

  private String configType(String type) {
    return switch (type) {
      case "CAMPAIGN" -> "MESSAGE_CAMPAIGN";
      case "APP_RELEASE" -> "APP_RELEASE";
      case "COMPLIANCE" -> "COMPLIANCE";
      case "FEATURE_FLAG" -> "FEATURE_FLAG";
      case "EXPERIMENT" -> "EXPERIMENT";
      default -> throw new BusinessException("OPS_RESOURCE_TYPE_INVALID", "治理资源类型不合法");
    };
  }

  private Set<String> runtimeStatuses(String type) {
    return switch (type) {
      case "CAMPAIGN" -> Set.of("SCHEDULED", "RUNNING", "PAUSED", "COMPLETED", "CANCELLED");
      case "APP_RELEASE" -> Set.of("PUBLISHED", "RETIRED");
      case "COMPLIANCE" -> Set.of("PUBLISHED", "RETIRED");
      case "FEATURE_FLAG" -> Set.of("ACTIVE", "DISABLED");
      case "EXPERIMENT" -> Set.of("RUNNING", "PAUSED", "COMPLETED");
      default -> throw new BusinessException("OPS_RESOURCE_TYPE_INVALID", "治理资源类型不合法");
    };
  }

  private void validateRuntimeTransition(String type, String current, String target) {
    Set<String> allowed =
        switch (type) {
          case "CAMPAIGN" ->
              switch (current) {
                case "DRAFT" -> Set.of("SCHEDULED", "RUNNING", "CANCELLED");
                case "SCHEDULED" -> Set.of("RUNNING", "PAUSED", "CANCELLED");
                case "RUNNING" -> Set.of("PAUSED", "COMPLETED", "CANCELLED");
                case "PAUSED" -> Set.of("RUNNING", "COMPLETED", "CANCELLED");
                default -> Set.of();
              };
          case "APP_RELEASE", "COMPLIANCE" ->
              switch (current) {
                case "DRAFT" -> Set.of("PUBLISHED");
                case "PUBLISHED" -> Set.of("RETIRED");
                default -> Set.of();
              };
          case "FEATURE_FLAG" ->
              switch (current) {
                case "DRAFT" -> Set.of("ACTIVE");
                case "ACTIVE" -> Set.of("DISABLED");
                case "DISABLED" -> Set.of("ACTIVE");
                default -> Set.of();
              };
          case "EXPERIMENT" ->
              switch (current) {
                case "DRAFT" -> Set.of("RUNNING");
                case "RUNNING" -> Set.of("PAUSED", "COMPLETED");
                case "PAUSED" -> Set.of("RUNNING", "COMPLETED");
                default -> Set.of();
              };
          default -> throw new BusinessException("OPS_RESOURCE_TYPE_INVALID", "治理资源类型不合法");
        };
    if (!allowed.contains(target))
      throw new BusinessException("OPS_RESOURCE_TRANSITION_INVALID", "治理资源状态不可按当前路径流转");
  }

  private void validateMarketingAudience(String value) {
    JsonNode root = parseNode(value);
    validateAudienceNode(root, true);
  }

  private void validateExperimentAudience(String value) {
    JsonNode root = parseNode(value);
    validateAudienceNode(root, false);
  }

  private JsonNode parseNode(String value) {
    required(value);
    try {
      return json.readTree(value);
    } catch (Exception e) {
      invalid("JSON 规则不合法");
      return null;
    }
  }

  private void validateAudienceNode(JsonNode node, boolean marketing) {
    if (node == null) return;
    if (marketing && containsTeenMarker(node))
      throw new BusinessException("OPS_TEEN_MARKETING_FORBIDDEN", "14-17 岁用户不得进入营销人群");
    if (node.isObject()) {
      for (var field : node.properties()) {
        String key = field.getKey().replace("_", "").replace("-", "").toLowerCase();
        if (marketing
            && Set.of(
                    "emotion",
                    "emotionalstate",
                    "family",
                    "familystatus",
                    "school",
                    "schoolperformance",
                    "academicperformance",
                    "goalfailure",
                    "mentalhealth")
                .contains(key))
          throw new BusinessException(
              "OPS_SENSITIVE_TARGETING_FORBIDDEN", "营销人群不得使用情绪、家庭、学业、目标失败或心理健康等敏感维度");
        if (marketing
            && Set.of(
                    "age", "ages", "agerange", "ageband", "agebands", "audienceage", "userageband")
                .contains(key)
            && containsTeen(field.getValue()))
          throw new BusinessException("OPS_TEEN_MARKETING_FORBIDDEN", "14-17 岁用户不得进入营销人群");
        validateAudienceNode(field.getValue(), marketing);
      }
    } else if (node.isArray()) node.forEach(child -> validateAudienceNode(child, marketing));
  }

  private boolean containsTeen(JsonNode node) {
    if (node == null) return false;
    if (node.isIntegralNumber()) return node.asInt() >= 14 && node.asInt() < 18;
    if (node.isTextual()) {
      String value = normalize(node.asText());
      return containsTeenWord(value) || value.contains("1417") || value.contains("UNDER18");
    }
    if (node.isContainerNode()) {
      for (JsonNode item : node) if (containsTeen(item)) return true;
    }
    return false;
  }

  private boolean containsTeenMarker(JsonNode node) {
    if (node == null) return false;
    if (node.isTextual()) return containsTeenWord(normalize(node.asText()));
    if (node.isContainerNode()) {
      for (JsonNode item : node) if (containsTeenMarker(item)) return true;
    }
    return false;
  }

  private boolean containsTeenWord(String value) {
    return value.contains("TEEN")
        || value.contains("MINOR")
        || value.contains("YOUTH")
        || value.contains("ADOLESCENT")
        || value.contains("UNDER18");
  }

  private String normalize(String value) {
    return value == null
        ? ""
        : value.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "");
  }

  private void oneOf(String value, Set<String> allowed) {
    if (!allowed.contains(value)) invalid("资源状态不合法");
  }

  private void required(String... values) {
    for (String value : values) if (blank(value)) invalid("必填字段不能为空");
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private void invalid(String message) {
    throw new BusinessException("OPS_GOVERNANCE_COMMAND_INVALID", message);
  }
}
