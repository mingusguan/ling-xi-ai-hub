package com.lingxi.commerce.application;

import com.lingxi.commerce.api.*;
import com.lingxi.commerce.domain.*;
import com.lingxi.identity.api.*;
import com.lingxi.kernel.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 商品、订单、支付、退款、订阅和权益应用服务。 */
@Service
public class CommerceApplicationService implements CommerceFacade, EntitlementFacade {
  /** 列表类查询允许的最大页大小。 */
  private static final int MAX_PAGE_SIZE = 200;

  private final CommerceRepository repo;
  private final IdentityFacade identities;
  private final IdGenerator ids;
  private final Map<String, PaymentChannelAdapter> channels;
  private final PurchaseApprovalAdapter approvals;
  private final CommerceTransactionService transactions;
  private final Map<String, RefundPolicyAdapter> refundPolicies;

  public CommerceApplicationService(
      CommerceRepository repo,
      IdentityFacade identities,
      IdGenerator ids,
      List<PaymentChannelAdapter> channels,
      List<PurchaseApprovalAdapter> approvals,
      CommerceTransactionService transactions,
      List<RefundPolicyAdapter> refundPolicies) {
    this.repo = repo;
    this.identities = identities;
    this.ids = ids;
    this.channels = new HashMap<>();
    channels.forEach(x -> this.channels.put(x.channel(), x));
    this.approvals = approvals.stream().findFirst().orElse(null);
    this.transactions = transactions;
    this.refundPolicies = new HashMap<>();
    refundPolicies.forEach(policy -> this.refundPolicies.put(policy.channel(), policy));
  }

  @Transactional(readOnly = true)
  public List<ProductResult> listProducts(long user, String scene) {
    AccessProfile p = profile(user);
    return repo.listPrices().stream()
        .filter(x -> allowed(x, p.ageBand()))
        .filter(x -> "ALL".equals(x.scene()) || Objects.equals(x.scene(), scene))
        .map(this::product)
        .toList();
  }

  public OrderResult createOrder(CreateOrderCommand c) {
    Order order = repo.findOrderByBusinessKey(c.businessOrderKey()).orElse(null);
    if (order != null) {
      if (order.getUserId() != c.userId()
          || order.getProductId() != c.productId()
          || order.getPriceVersion() != c.priceVersion()
          || !order.getChannel().equals(c.channel()))
        throw error("PAY_IDEMPOTENCY_CONFLICT", "业务订单键对应不同订单");
    } else {
      AccessProfile profile = profile(c.userId());
      SellablePrice price =
          repo.findPrice(c.productId(), c.priceVersion())
              .orElseThrow(() -> error("PAY_PRICE_NOT_FOUND", "商品价格版本不存在"));
      if (!allowed(price, profile.ageBand())) throw error("PAY_PRODUCT_NOT_ALLOWED", "该商品不适用于当前年龄");
      if (profile.ageBand() == AgeBand.TEEN
          && (approvals == null
              || !approvals.approved(c.userId(), c.productId(), c.guardianApprovalReference())))
        throw error("PAY_GUARDIAN_APPROVAL_REQUIRED", "青少年购买需要有效监护审批");
      order =
          transactions.createOrder(
              Order.create(
                  ids.nextId(),
                  "LX" + ids.nextId(),
                  c.businessOrderKey(),
                  c.userId(),
                  price,
                  c.channel(),
                  now()),
              price);
    }
    if (order.getStatus() == Order.Status.CREATED) {
      String ref = channel(c.channel()).initiate(order).opaqueReference();
      order = transactions.markPaying(order.getId(), ref);
    }
    return result(order);
  }

  public OrderResult handlePaymentCallback(PaymentCallbackCommand c) {
    PaymentChannelAdapter adapter = channel(c.channel());
    PaymentChannelAdapter.VerifiedPayment paid = adapter.verify(c.rawPayload(), c.headers());
    return result(transactions.applyVerifiedPayment(c.channel(), paid));
  }

  public SubscriptionResult cancelSubscription(CancelSubscriptionCommand c) {
    if (!c.recentAuthentication()) {
      throw error("AUTH_RECENT_AUTHENTICATION_REQUIRED", "取消订阅需要近期认证");
    }
    Subscription s =
        repo.findSubscription(c.subscriptionId())
            .orElseThrow(() -> error("PAY_SUBSCRIPTION_NOT_FOUND", "订阅不存在"));
    if (s.isCompletedCancellation(c.userId(), c.cancelMode(), c.expectedVersion())) {
      return subscription(s);
    }
    s.assertCancellable(c.userId(), c.cancelMode(), c.expectedVersion());
    channel(s.getChannel()).cancel(s, c.cancelMode(), c.requestKey());
    return subscription(transactions.completeCancellation(c));
  }

  public OrderResult confirmRefund(ConfirmRefundCommand c) {
    Order o =
        repo.findOrderByNo(c.orderNo()).orElseThrow(() -> error("PAY_ORDER_NOT_FOUND", "订单不存在"));
    if (!o.getChannel().equals(c.channel())) throw error("PAY_CHANNEL_MISMATCH", "退款渠道与订单不匹配");
    var existing = repo.findRefund(c.channel(), c.refundTransactionId());
    if (existing.isPresent()) {
      if (existing.get().orderId() != o.getId() || existing.get().amountMinor() != c.amountMinor())
        throw error("PAY_REFUND_CONFLICT", "重复退款交易与原退款不一致");
      return result(o);
    }
    SellablePrice price =
        repo.findOrderPriceSnapshot(o.getId())
            .orElseThrow(() -> error("PAY_PRICE_SNAPSHOT_NOT_FOUND", "订单价格快照不存在"));
    RefundPolicyAdapter policy = refundPolicies.get(c.channel());
    if (policy == null) throw error("PAY_REFUND_POLICY_UNAVAILABLE", "退款渠道规则尚未配置");
    RefundPolicyAdapter.RefundDecision decision = policy.evaluate(o, price, c);
    return result(transactions.applyRefund(c, decision.entitlementToReclaim()));
  }

  @Transactional(readOnly = true)
  public EntitlementResult get(long user, String resource) {
    profile(user);
    return entitlement(repo.getEntitlement(user, resource));
  }

  @Override
  @Transactional(readOnly = true)
  public List<EntitlementResult> list(long user) {
    profile(user);
    return repo.findEntitlementsByUser(user).stream().map(this::entitlement).toList();
  }

  @Transactional
  public EntitlementResult consume(ConsumeEntitlementCommand c) {
    profile(c.userId());
    repo.consumeEntitlement(
        ids.nextId(),
        c.userId(),
        c.resourceKey(),
        c.amount(),
        c.sourceType(),
        c.sourceId(),
        c.commandId(),
        now());
    return entitlement(repo.getEntitlement(c.userId(), c.resourceKey()));
  }

  private AccessProfile profile(long user) {
    AccessProfile p = identities.getAccessProfile(user);
    if (!p.coreFeaturesAllowed()) throw error("PAY_ACCOUNT_RESTRICTED", "当前账号不可购买或使用权益");
    return p;
  }

  private boolean allowed(SellablePrice p, AgeBand age) {
    return age == AgeBand.ADULT
        || age == AgeBand.TEEN
            && "ONE_TIME".equals(p.billingPeriod())
            && ("ALL".equals(p.agePolicy()) || "TEEN_ALLOWED".equals(p.agePolicy()));
  }

  /** 分页查询本人订单：只按归属用户过滤，账号受限时仍可查看历史订单。 */
  @Override
  @Transactional(readOnly = true)
  public PageResult<OrderResult> listOrders(long user, int page, int pageSize) {
    requirePaging(page, pageSize);
    identities.getAccessProfile(user);
    long total = repo.countOrdersByUser(user);
    List<OrderResult> items =
        repo.findOrdersByUser(user, page, pageSize).stream().map(this::result).toList();
    return new PageResult<>(items, total, page, pageSize);
  }

  /** 分页查询本人订阅，含已取消与已过期记录。 */
  @Override
  @Transactional(readOnly = true)
  public PageResult<SubscriptionResult> listSubscriptions(long user, int page, int pageSize) {
    requirePaging(page, pageSize);
    identities.getAccessProfile(user);
    long total = repo.countSubscriptionsByUser(user);
    List<SubscriptionResult> items =
        repo.findSubscriptionsByUser(user, page, pageSize).stream()
            .map(this::subscription)
            .toList();
    return new PageResult<>(items, total, page, pageSize);
  }

  private void requirePaging(int page, int pageSize) {
    if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw error("PAY_INVALID_QUERY", "查询参数不合法");
    }
  }

  private PaymentChannelAdapter channel(String key) {    PaymentChannelAdapter a = channels.get(key);
    if (a == null) throw error("PAY_CHANNEL_UNAVAILABLE", "支付渠道尚未配置");
    return a;
  }

  private ProductResult product(SellablePrice p) {
    return new ProductResult(
        p.productId(),
        p.productKey(),
        p.productName(),
        p.scene(),
        p.priceId(),
        p.priceVersion(),
        p.amountMinor(),
        p.currency(),
        p.billingPeriod(),
        p.agePolicy());
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

  private SubscriptionResult subscription(Subscription s) {
    return new SubscriptionResult(
        s.getId(),
        s.getUserId(),
        s.getProductId(),
        s.getStatus().name(),
        s.getPeriodEnd(),
        s.getCancelMode(),
        s.getVersion());
  }

  private EntitlementResult entitlement(CommerceRepository.EntitlementSnapshot e) {
    return new EntitlementResult(
        e.userId(), e.resourceKey(), e.balance(), e.expiresAt(), e.version());
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private void updated(boolean ok) {
    if (!ok) throw error("PAY_CONCURRENT_UPDATE", "数据已被并发修改");
  }

  private BusinessException error(String c, String m) {
    return new BusinessException(c, m);
  }
}
