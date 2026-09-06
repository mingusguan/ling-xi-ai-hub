package com.lingxi.commerce.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.commerce.api.*;
import com.lingxi.commerce.api.AdminCommerceManagementFacade.*;
import com.lingxi.commerce.domain.CommerceAdminWriteRepository;
import com.lingxi.commerce.domain.CommerceRepository;
import com.lingxi.commerce.domain.Order;
import com.lingxi.commerce.domain.SellablePrice;
import com.lingxi.identity.api.AdminActionAuditedEvent;
import com.lingxi.identity.api.AdminAuthorizationFacade;
import com.lingxi.kernel.*;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 商业化后台命令服务；所有资金与权益操作均要求近期认证和审计上下文。 */
@Service
public class CommerceAdminManagementApplicationService implements AdminCommerceManagementFacade {
  private final CommerceAdminWriteRepository writes;
  private final CommerceRepository commerce;
  private final CommerceTransactionService transactions;
  private final Map<String, RefundPolicyAdapter> refundPolicies;
  private final AdminAuthorizationFacade admins;
  private final IdGenerator ids;
  private final DomainEventPublisher events;
  private final ObjectMapper json;

  public CommerceAdminManagementApplicationService(
      CommerceAdminWriteRepository writes,
      CommerceRepository commerce,
      CommerceTransactionService transactions,
      List<RefundPolicyAdapter> refundPolicies,
      AdminAuthorizationFacade admins,
      IdGenerator ids,
      DomainEventPublisher events,
      ObjectMapper json) {
    this.writes = writes;
    this.commerce = commerce;
    this.transactions = transactions;
    this.refundPolicies = new HashMap<>();
    refundPolicies.forEach(policy -> this.refundPolicies.put(policy.channel(), policy));
    this.admins = admins;
    this.ids = ids;
    this.events = events;
    this.json = json;
  }

  @Override
  @Transactional
  public ManagedResult saveProduct(long adminId, ProductCommand c) {
    require(adminId, "commerce:catalog:manage");
    context(c.context());
    required(
        c.productKey(),
        c.name(),
        c.scene(),
        c.billingPeriod(),
        c.agePolicy(),
        c.entitlementKey(),
        c.status());
    oneOf(c.billingPeriod(), Set.of("ONE_TIME", "MONTHLY", "YEARLY"));
    oneOf(c.agePolicy(), Set.of("ADULT_ONLY", "TEEN_ALLOWED", "ALL"));
    oneOf(c.status(), Set.of("DRAFT", "ACTIVE", "INACTIVE"));
    if (c.entitlementAmount() <= 0) invalid("商品权益数量必须大于零");
    if (!"ONE_TIME".equals(c.billingPeriod()) && !"ADULT_ONLY".equals(c.agePolicy()))
      throw new BusinessException("PAY_TEEN_RECURRING_FORBIDDEN", "14-17 岁用户不得购买或自动续费周期商品");
    ManagedResult r = writes.saveProduct(id(c.id()), c, now());
    audit(adminId, "PRODUCT_SAVE", r.resourceType(), r.id(), r.version(), c.context());
    return r;
  }

  @Override
  @Transactional
  public ManagedResult savePrice(long adminId, PriceCommand c) {
    require(adminId, "commerce:catalog:manage");
    context(c.context());
    required(c.currency(), c.status());
    if (c.productId() <= 0
        || c.versionNo() <= 0
        || c.amountMinor() <= 0
        || !c.currency().matches("[A-Z]{3}")) invalid("价格版本参数不合法");
    oneOf(c.status(), Set.of("DRAFT", "ACTIVE", "INACTIVE"));
    ManagedResult r = writes.savePrice(id(c.id()), c, now());
    audit(adminId, "PRICE_SAVE", r.resourceType(), r.id(), r.version(), c.context());
    return r;
  }

  @Override
  @Transactional
  public ManagedResult savePromotion(long adminId, PromotionCommand c) {
    require(adminId, "commerce:catalog:manage");
    context(c.context());
    required(
        c.promotionKey(), c.name(), c.promotionType(), c.ruleJson(), c.audienceRule(), c.status());
    parse(c.ruleJson());
    validateMarketingAudience(c.audienceRule());
    if (c.startsAt() == null || c.endsAt() == null || !c.endsAt().isAfter(c.startsAt()))
      invalid("促销有效期不合法");
    oneOf(c.status(), Set.of("DRAFT", "ACTIVE", "PAUSED", "ENDED"));
    ManagedResult r = writes.savePromotion(id(c.id()), c, now());
    audit(adminId, "PROMOTION_SAVE", r.resourceType(), r.id(), r.version(), c.context());
    return r;
  }

  @Override
  public OrderResult confirmRefund(long adminId, RefundCommand c) {
    require(adminId, "commerce.refund.confirm");
    context(c.context());
    required(c.channel(), c.refundTransactionId(), c.orderNo(), c.refundReason());
    if (c.amountMinor() <= 0) invalid("退款金额必须大于零");
    Order order =
        commerce
            .findOrderByNo(c.orderNo())
            .orElseThrow(() -> new BusinessException("PAY_ORDER_NOT_FOUND", "订单不存在"));
    if (!order.getChannel().equals(c.channel()))
      throw new BusinessException("PAY_CHANNEL_MISMATCH", "退款渠道与订单不匹配");
    var existing = commerce.findRefund(c.channel(), c.refundTransactionId());
    if (existing.isPresent()) {
      if (existing.get().orderId() != order.getId()
          || existing.get().amountMinor() != c.amountMinor())
        throw new BusinessException("PAY_REFUND_CONFLICT", "重复退款交易与原退款不一致");
      return result(order);
    }
    SellablePrice price =
        commerce
            .findOrderPriceSnapshot(order.getId())
            .orElseThrow(() -> new BusinessException("PAY_PRICE_SNAPSHOT_NOT_FOUND", "订单价格快照不存在"));
    RefundPolicyAdapter policy = refundPolicies.get(c.channel());
    if (policy == null) throw new BusinessException("PAY_REFUND_POLICY_UNAVAILABLE", "退款渠道规则尚未配置");
    ConfirmRefundCommand command =
        new ConfirmRefundCommand(
            c.channel(), c.refundTransactionId(), c.orderNo(), c.amountMinor(), c.refundReason());
    RefundPolicyAdapter.RefundDecision decision = policy.evaluate(order, price, command);
    return result(
        transactions.applyAdminRefund(
            adminId, command, decision.entitlementToReclaim(), c.context()));
  }

  @Override
  @Transactional
  public EntitlementResult adjustEntitlement(long adminId, EntitlementAdjustmentCommand c) {
    require(adminId, "commerce:entitlement:adjust");
    context(c.context());
    required(c.resourceKey(), c.commandId());
    if (c.userId() <= 0 || c.delta() == 0 || c.delta() == Long.MIN_VALUE) invalid("权益调整参数不合法");
    LocalDateTime now = now();
    if (c.delta() > 0)
      commerce.grantEntitlement(
          ids.nextId(),
          c.userId(),
          c.resourceKey(),
          c.delta(),
          c.expiresAt(),
          "ADMIN_ADJUSTMENT",
          String.valueOf(adminId),
          c.commandId(),
          now);
    else
      commerce.consumeEntitlement(
          ids.nextId(),
          c.userId(),
          c.resourceKey(),
          -c.delta(),
          "ADMIN_ADJUSTMENT",
          String.valueOf(adminId),
          c.commandId(),
          now);
    CommerceRepository.EntitlementSnapshot s = commerce.getEntitlement(c.userId(), c.resourceKey());
    audit(adminId, "ENTITLEMENT_ADJUST", "ENTITLEMENT", s.id(), s.version(), c.context());
    return new EntitlementResult(
        s.userId(), s.resourceKey(), s.balance(), s.expiresAt(), s.version());
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<PromotionSummary> promotions(long adminId, String status, int page, int size) {
    require(adminId, "commerce:read");
    return writes.promotions(status, page(page), size(size));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<ReconciliationSummary> reconciliationCases(
      long adminId, String status, int page, int size) {
    require(adminId, "commerce:reconcile:manage");
    return writes.reconciliationCases(status, page(page), size(size));
  }

  @Override
  @Transactional
  public ReconciliationSummary resolveReconciliation(long adminId, ReconciliationCommand c) {
    require(adminId, "commerce:reconcile:manage");
    context(c.context());
    required(c.resolution());
    ReconciliationSummary r = writes.resolveReconciliation(adminId, c, now());
    audit(adminId, "RECONCILIATION_RESOLVE", "RECONCILIATION", r.id(), r.version(), c.context());
    return r;
  }

  private OrderResult result(Order o) {
    return new OrderResult(
        o.getId(),
        o.getOrderNo(),
        o.getUserId(),
        o.getProductId(),
        o.getAmountMinor(),
        o.getCurrency(),
        o.getStatus().name(),
        o.getPaymentReference(),
        o.getRefundedMinor(),
        o.getVersion(),
        o.getCreatedAt());
  }

  private void audit(
      long admin, String action, String type, long object, long version, OperationContext c) {
    events.publish(
        new AdminActionAuditedEvent(
            UUID.randomUUID().toString(),
            admin,
            action,
            type,
            String.valueOf(object),
            version,
            c.reason(),
            c.ticketNo(),
            Instant.now()));
  }

  private void require(long admin, String permission) {
    if (admin <= 0 || !admins.allowed(admin, permission))
      throw new BusinessException("ADMIN_FORBIDDEN", "管理员权限不足");
  }

  private void context(OperationContext c) {
    if (c == null || !c.recentAuthentication() || blank(c.reason()) || blank(c.ticketNo()))
      throw new BusinessException("ADMIN_HIGH_RISK_CONTEXT_REQUIRED", "操作需要近期认证、具体原因和工单号");
  }

  private void parse(String value) {
    try {
      json.readTree(value);
    } catch (Exception e) {
      invalid("JSON 规则不合法");
    }
  }

  private void validateMarketingAudience(String value) {
    try {
      JsonNode root = json.readTree(value);
      validateMarketingNode(root);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      invalid("JSON 规则不合法");
    }
  }

  private void validateMarketingNode(JsonNode node) {
    if (node == null) return;
    if (containsTeenMarker(node))
      throw new BusinessException("PAY_TEEN_MARKETING_FORBIDDEN", "14-17 岁用户不得进入营销人群");
    if (node.isObject()) {
      for (var field : node.properties()) {
        String key = field.getKey().replace("_", "").replace("-", "").toLowerCase();
        if (Set.of(
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
              "PAY_SENSITIVE_TARGETING_FORBIDDEN", "营销人群不得使用情绪、家庭、学业、目标失败或心理健康等敏感维度");
        if (Set.of("age", "ages", "agerange", "ageband", "agebands", "audienceage", "userageband")
                .contains(key)
            && containsTeen(field.getValue()))
          throw new BusinessException("PAY_TEEN_MARKETING_FORBIDDEN", "14-17 岁用户不得进入营销人群");
        validateMarketingNode(field.getValue());
      }
    } else if (node.isArray()) node.forEach(this::validateMarketingNode);
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
    return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
  }

  private void oneOf(String v, Set<String> a) {
    if (!a.contains(v)) invalid("资源状态或枚举不合法");
  }

  private void required(String... v) {
    for (String x : v) if (blank(x)) invalid("必填字段不能为空");
  }

  private boolean blank(String v) {
    return v == null || v.isBlank();
  }

  private long id(long id) {
    return id == 0 ? ids.nextId() : id;
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private int page(int v) {
    return Math.max(1, v);
  }

  private int size(int v) {
    return Math.min(100, Math.max(1, v));
  }

  private void invalid(String message) {
    throw new BusinessException("PAY_ADMIN_COMMAND_INVALID", message);
  }
}
