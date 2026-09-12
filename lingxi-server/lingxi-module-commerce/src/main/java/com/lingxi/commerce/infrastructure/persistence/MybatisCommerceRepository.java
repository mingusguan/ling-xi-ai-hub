package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.commerce.domain.*;
import com.lingxi.kernel.BusinessException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisCommerceRepository implements CommerceRepository {
  private final ProductMapper products;
  private final PriceMapper prices;
  private final OrderMapper orders;
  private final OrderItemMapper orderItems;
  private final PaymentTransactionMapper transactions;
  private final ChannelCallbackMapper callbacks;
  private final RefundMapper refunds;
  private final SubscriptionMapper subscriptions;
  private final EntitlementMapper entitlements;
  private final EntitlementLedgerMapper ledger;
  private final ObjectMapper json;

  public MybatisCommerceRepository(
      ProductMapper p,
      PriceMapper pr,
      OrderMapper o,
      OrderItemMapper oi,
      PaymentTransactionMapper tr,
      ChannelCallbackMapper cb,
      RefundMapper rf,
      SubscriptionMapper s,
      EntitlementMapper e,
      EntitlementLedgerMapper l,
      ObjectMapper json) {
    products = p;
    prices = pr;
    orders = o;
    orderItems = oi;
    transactions = tr;
    callbacks = cb;
    refunds = rf;
    subscriptions = s;
    entitlements = e;
    ledger = l;
    this.json = json;
  }

  public List<SellablePrice> listPrices() {
    Map<Long, ProductEntity> ps = new HashMap<>();
    products
        .selectList(Wrappers.<ProductEntity>lambdaQuery().eq(ProductEntity::getStatus, "ACTIVE"))
        .forEach(p -> ps.put(p.getId(), p));
    return prices
        .selectList(Wrappers.<PriceEntity>lambdaQuery().eq(PriceEntity::getStatus, "ACTIVE"))
        .stream()
        .filter(x -> ps.containsKey(x.getProductId()))
        .map(x -> price(ps.get(x.getProductId()), x))
        .toList();
  }

  public Optional<SellablePrice> findPrice(long productId, int version) {
    ProductEntity p = products.selectById(productId);
    if (p == null || !"ACTIVE".equals(p.getStatus())) return Optional.empty();
    return Optional.ofNullable(
            prices.selectOne(
                Wrappers.<PriceEntity>lambdaQuery()
                    .eq(PriceEntity::getProductId, productId)
                    .eq(PriceEntity::getVersionNo, version)
                    .eq(PriceEntity::getStatus, "ACTIVE")
                    .last("LIMIT 1")))
        .map(x -> price(p, x));
  }

  public Optional<SellablePrice> findPriceByProduct(long id) {
    ProductEntity p = products.selectById(id);
    if (p == null) return Optional.empty();
    return Optional.ofNullable(
            prices.selectOne(
                Wrappers.<PriceEntity>lambdaQuery()
                    .eq(PriceEntity::getProductId, id)
                    .eq(PriceEntity::getStatus, "ACTIVE")
                    .orderByDesc(PriceEntity::getVersionNo)
                    .last("LIMIT 1")))
        .map(x -> price(p, x));
  }

  public Optional<Order> findOrder(long id) {
    return Optional.ofNullable(orders.selectById(id)).map(this::order);
  }

  public Optional<Order> findOrderByNo(String no) {
    return Optional.ofNullable(
            orders.selectOne(
                Wrappers.<OrderEntity>lambdaQuery()
                    .eq(OrderEntity::getOrderNo, no)
                    .last("LIMIT 1")))
        .map(this::order);
  }

  public Optional<Order> findOrderByBusinessKey(String key) {
    return Optional.ofNullable(
            orders.selectOne(
                Wrappers.<OrderEntity>lambdaQuery()
                    .eq(OrderEntity::getBusinessOrderKey, key)
                    .last("LIMIT 1")))
        .map(this::order);
  }

  public List<Order> findClosableOrders(LocalDateTime before, int limit) {
    return orders
        .selectList(
            Wrappers.<OrderEntity>lambdaQuery()
                // PAYING 必须先查询渠道事实，不能仅按本地超时关闭。
                .eq(OrderEntity::getStatus, "CREATED")
                .le(OrderEntity::getUpdatedAt, before)
                .orderByAsc(OrderEntity::getUpdatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500))))
        .stream()
        .map(this::order)
        .toList();
  }

  @Override
  public long countOrdersByUser(long user) {
    Long total =
        orders.selectCount(Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getUserId, user));
    return total == null ? 0L : total;
  }

  @Override
  public List<Order> findOrdersByUser(long user, int page, int pageSize) {
    long offset = (long) (page - 1) * pageSize;
    return orders
        .selectList(
            Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getUserId, user)
                .orderByDesc(OrderEntity::getCreatedAt)
                .orderByDesc(OrderEntity::getId)
                .last("LIMIT " + pageSize + " OFFSET " + offset))
        .stream()
        .map(this::order)
        .toList();
  }

  public void insertOrder(Order o, SellablePrice priceSnapshot) {
    OrderEntity e = new OrderEntity();
    e.setId(o.getId());
    e.setOrderNo(o.getOrderNo());
    e.setBusinessOrderKey(o.getBusinessKey());
    e.setUserId(o.getUserId());
    e.setProductId(o.getProductId());
    e.setPriceId(o.getPriceId());
    e.setPriceVersion(o.getPriceVersion());
    e.setAmountMinor(o.getAmountMinor());
    e.setCurrency(o.getCurrency());
    e.setChannel(o.getChannel());
    e.setStatus(o.getStatus().name());
    e.setPaymentReference(o.getPaymentReference());
    e.setRefundedMinor(o.getRefundedMinor());
    e.setVersion(o.getVersion());
    e.setCreatedAt(o.getCreatedAt());
    e.setUpdatedAt(o.getUpdatedAt());
    orders.insert(e);
    OrderItemEntity item = new OrderItemEntity();
    item.setId(o.getId());
    item.setOrderId(o.getId());
    item.setProductId(o.getProductId());
    item.setPriceId(o.getPriceId());
    item.setQuantity(1);
    item.setAmountMinor(o.getAmountMinor());
    item.setSnapshotJson(writePriceSnapshot(priceSnapshot));
    orderItems.insert(item);
  }

  public Optional<SellablePrice> findOrderPriceSnapshot(long orderId) {
    OrderItemEntity item =
        orderItems.selectOne(
            Wrappers.<OrderItemEntity>lambdaQuery()
                .eq(OrderItemEntity::getOrderId, orderId)
                .last("LIMIT 1"));
    return item == null ? Optional.empty() : readPriceSnapshot(item.getSnapshotJson());
  }

  public boolean updateOrder(Order o, long v) {
    return orders.update(
            null,
            Wrappers.<OrderEntity>lambdaUpdate()
                .eq(OrderEntity::getId, o.getId())
                .eq(OrderEntity::getVersion, v)
                .set(OrderEntity::getStatus, o.getStatus().name())
                .set(OrderEntity::getPaymentReference, o.getPaymentReference())
                .set(OrderEntity::getRefundedMinor, o.getRefundedMinor())
                .set(OrderEntity::getVersion, o.getVersion())
                .set(OrderEntity::getUpdatedAt, o.getUpdatedAt()))
        == 1;
  }

  public Optional<CallbackSnapshot> findCallback(String c, String id) {
    return Optional.ofNullable(
            callbacks.selectOne(
                Wrappers.<ChannelCallbackEntity>lambdaQuery()
                    .eq(ChannelCallbackEntity::getChannel, c)
                    .eq(ChannelCallbackEntity::getCallbackId, id)
                    .last("LIMIT 1")))
        .map(callback -> new CallbackSnapshot(callback.getDigest(), callback.getStatus()));
  }

  public void insertCallback(
      long id, String c, String cb, String digest, String status, LocalDateTime now) {
    ChannelCallbackEntity e = new ChannelCallbackEntity();
    e.setId(id);
    e.setChannel(c);
    e.setCallbackId(cb);
    e.setDigest(digest);
    e.setStatus(status);
    e.setReceivedAt(now);
    e.setUpdatedAt(now);
    callbacks.insert(e);
  }

  public void updateCallback(String c, String id, String status) {
    callbacks.update(
        null,
        Wrappers.<ChannelCallbackEntity>lambdaUpdate()
            .eq(ChannelCallbackEntity::getChannel, c)
            .eq(ChannelCallbackEntity::getCallbackId, id)
            .set(ChannelCallbackEntity::getStatus, status)
            .set(ChannelCallbackEntity::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC)));
  }

  public Optional<TransactionSnapshot> findTransaction(String c, String id) {
    return Optional.ofNullable(
            transactions.selectOne(
                Wrappers.<PaymentTransactionEntity>lambdaQuery()
                    .eq(PaymentTransactionEntity::getChannel, c)
                    .eq(PaymentTransactionEntity::getTransactionId, id)
                    .last("LIMIT 1")))
        .map(
            transaction ->
                new TransactionSnapshot(
                    transaction.getOrderId(),
                    transaction.getAmountMinor(),
                    transaction.getCurrency()));
  }

  public void insertTransaction(
      long id,
      long order,
      String c,
      String tx,
      long amount,
      String currency,
      String digest,
      Instant verified) {
    PaymentTransactionEntity e = new PaymentTransactionEntity();
    e.setId(id);
    e.setOrderId(order);
    e.setChannel(c);
    e.setTransactionId(tx);
    e.setAmountMinor(amount);
    e.setCurrency(currency);
    e.setRawDigest(digest);
    e.setVerifiedAt(verified);
    transactions.insert(e);
  }

  public Optional<RefundSnapshot> findRefund(String c, String id) {
    return Optional.ofNullable(
            refunds.selectOne(
                Wrappers.<RefundEntity>lambdaQuery()
                    .eq(RefundEntity::getChannel, c)
                    .eq(RefundEntity::getRefundTransactionId, id)
                    .last("LIMIT 1")))
        .map(refund -> new RefundSnapshot(refund.getOrderId(), refund.getAmountMinor()));
  }

  public void insertRefund(
      long id, long order, String c, String refund, long amount, String reason, LocalDateTime now) {
    RefundEntity e = new RefundEntity();
    e.setId(id);
    e.setOrderId(order);
    e.setChannel(c);
    e.setRefundTransactionId(refund);
    e.setAmountMinor(amount);
    e.setReason(reason);
    e.setStatus("CONFIRMED");
    e.setCreatedAt(now);
    refunds.insert(e);
  }

  public Optional<Subscription> findSubscription(long id) {
    return Optional.ofNullable(subscriptions.selectById(id)).map(this::subscription);
  }

  public Optional<Subscription> findActiveSubscription(long user, long product) {
    return Optional.ofNullable(
            subscriptions.selectOne(
                Wrappers.<SubscriptionEntity>lambdaQuery()
                    .eq(SubscriptionEntity::getUserId, user)
                    .eq(SubscriptionEntity::getProductId, product)
                    .in(SubscriptionEntity::getStatus, "ACTIVE", "CANCEL_AT_PERIOD_END")
                    .last("LIMIT 1")))
        .map(this::subscription);
  }

  public List<Subscription> findSubscriptionsForReconciliation(int limit) {
    return subscriptions
        .selectList(
            Wrappers.<SubscriptionEntity>lambdaQuery()
                .in(
                    SubscriptionEntity::getStatus,
                    "ACTIVE",
                    "PAUSED",
                    "CANCEL_AT_PERIOD_END",
                    "SUSPENDED")
                .orderByAsc(SubscriptionEntity::getLastReconciledAt)
                .orderByAsc(SubscriptionEntity::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500))))
        .stream()
        .map(this::subscription)
        .toList();
  }

  @Override
  public long countSubscriptionsByUser(long user) {
    Long total =
        subscriptions.selectCount(
            Wrappers.<SubscriptionEntity>lambdaQuery().eq(SubscriptionEntity::getUserId, user));
    return total == null ? 0L : total;
  }

  @Override
  public List<Subscription> findSubscriptionsByUser(long user, int page, int pageSize) {
    long offset = (long) (page - 1) * pageSize;
    return subscriptions
        .selectList(
            Wrappers.<SubscriptionEntity>lambdaQuery()
                .eq(SubscriptionEntity::getUserId, user)
                .orderByDesc(SubscriptionEntity::getCreatedAt)
                .orderByDesc(SubscriptionEntity::getId)
                .last("LIMIT " + pageSize + " OFFSET " + offset))
        .stream()
        .map(this::subscription)
        .toList();
  }

  public void markSubscriptionReconciled(long subscriptionId, LocalDateTime reconciledAt) {
    subscriptions.update(
        null,
        Wrappers.<SubscriptionEntity>lambdaUpdate()
            .eq(SubscriptionEntity::getId, subscriptionId)
            .set(SubscriptionEntity::getLastReconciledAt, reconciledAt));
  }

  public void insertSubscription(Subscription s) {
    SubscriptionEntity e = new SubscriptionEntity();
    e.setId(s.getId());
    e.setUserId(s.getUserId());
    e.setProductId(s.getProductId());
    e.setChannel(s.getChannel());
    e.setChannelSubscriptionId(s.getChannelSubscriptionId());
    e.setStatus(s.getStatus().name());
    e.setPeriodEnd(s.getPeriodEnd());
    e.setCancelMode(s.getCancelMode());
    e.setVersion(s.getVersion());
    e.setCreatedAt(s.getCreatedAt());
    e.setUpdatedAt(s.getUpdatedAt());
    subscriptions.insert(e);
  }

  public boolean updateSubscription(Subscription s, long v) {
    return subscriptions.update(
            null,
            Wrappers.<SubscriptionEntity>lambdaUpdate()
                .eq(SubscriptionEntity::getId, s.getId())
                .eq(SubscriptionEntity::getVersion, v)
                .set(SubscriptionEntity::getStatus, s.getStatus().name())
                .set(SubscriptionEntity::getCancelMode, s.getCancelMode())
                .set(SubscriptionEntity::getVersion, s.getVersion())
                .set(SubscriptionEntity::getUpdatedAt, s.getUpdatedAt()))
        == 1;
  }

  public long grantEntitlement(
      long id,
      long user,
      String resource,
      long delta,
      Instant expires,
      String sourceType,
      String sourceId,
      String commandId,
      LocalDateTime now) {
    EntitlementLedgerEntity old =
        ledger.selectOne(
            Wrappers.<EntitlementLedgerEntity>lambdaQuery()
                .eq(EntitlementLedgerEntity::getCommandId, commandId)
                .last("LIMIT 1"));
    if (old != null) return old.getBalanceAfter();
    entitlements.grant(id, user, resource, delta, expires, now);
    EntitlementEntity e = findEnt(user, resource);
    insertLedger(id, user, resource, delta, e.getBalance(), sourceType, sourceId, commandId, now);
    return e.getBalance();
  }

  public long consumeEntitlement(
      long id,
      long user,
      String resource,
      long amount,
      String sourceType,
      String sourceId,
      String commandId,
      LocalDateTime now) {
    EntitlementLedgerEntity old =
        ledger.selectOne(
            Wrappers.<EntitlementLedgerEntity>lambdaQuery()
                .eq(EntitlementLedgerEntity::getCommandId, commandId)
                .last("LIMIT 1"));
    if (old != null) return old.getBalanceAfter();
    if (amount <= 0 || entitlements.consume(user, resource, amount, now) != 1)
      throw new BusinessException("PAY_ENTITLEMENT_INSUFFICIENT", "权益不足或已过期");
    EntitlementEntity e = findEnt(user, resource);
    insertLedger(id, user, resource, -amount, e.getBalance(), sourceType, sourceId, commandId, now);
    return e.getBalance();
  }

  public EntitlementSnapshot getEntitlement(long user, String resource) {
    EntitlementEntity e =
        entitlements.selectOne(
            Wrappers.<EntitlementEntity>lambdaQuery()
                .eq(EntitlementEntity::getUserId, user)
                .eq(EntitlementEntity::getResourceKey, resource)
                .last("LIMIT 1"));
    return e == null
        ? new EntitlementSnapshot(0, user, resource, 0, null, 0)
        : new EntitlementSnapshot(
            e.getId(),
            e.getUserId(),
            e.getResourceKey(),
            e.getBalance(),
            e.getExpiresAt(),
            e.getVersion());
  }

  @Override
  public List<EntitlementSnapshot> findEntitlementsByUser(long user) {
    return entitlements
        .selectList(
            Wrappers.<EntitlementEntity>lambdaQuery()
                .eq(EntitlementEntity::getUserId, user)
                .orderByAsc(EntitlementEntity::getResourceKey))
        .stream()
        .map(
            e ->
                new EntitlementSnapshot(
                    e.getId(),
                    e.getUserId(),
                    e.getResourceKey(),
                    e.getBalance(),
                    e.getExpiresAt(),
                    e.getVersion()))
        .toList();
  }

  private EntitlementEntity findEnt(long user, String resource) {
    return entitlements.selectOne(
        Wrappers.<EntitlementEntity>lambdaQuery()
            .eq(EntitlementEntity::getUserId, user)
            .eq(EntitlementEntity::getResourceKey, resource)
            .last("LIMIT 1"));
  }

  private void insertLedger(
      long id,
      long user,
      String resource,
      long delta,
      long balance,
      String sourceType,
      String sourceId,
      String commandId,
      LocalDateTime now) {
    EntitlementLedgerEntity l = new EntitlementLedgerEntity();
    l.setId(id);
    l.setUserId(user);
    l.setResourceKey(resource);
    l.setDelta(delta);
    l.setBalanceAfter(balance);
    l.setSourceType(sourceType);
    l.setSourceId(sourceId);
    l.setCommandId(commandId);
    l.setCreatedAt(now);
    ledger.insert(l);
  }

  private SellablePrice price(ProductEntity p, PriceEntity x) {
    return new SellablePrice(
        p.getId(),
        p.getProductKey(),
        p.getName(),
        p.getScene(),
        x.getId(),
        x.getVersionNo(),
        x.getAmountMinor(),
        x.getCurrency(),
        p.getBillingPeriod(),
        p.getAgePolicy(),
        p.getEntitlementKey(),
        p.getEntitlementAmount(),
        "ACTIVE".equals(p.getStatus()) && "ACTIVE".equals(x.getStatus()));
  }

  private String writePriceSnapshot(SellablePrice price) {
    try {
      return json.writeValueAsString(
          Map.ofEntries(
              Map.entry("productId", price.productId()),
              Map.entry("productKey", price.productKey()),
              Map.entry("productName", price.productName()),
              Map.entry("scene", price.scene()),
              Map.entry("priceId", price.priceId()),
              Map.entry("priceVersion", price.priceVersion()),
              Map.entry("amountMinor", price.amountMinor()),
              Map.entry("currency", price.currency()),
              Map.entry("billingPeriod", price.billingPeriod()),
              Map.entry("agePolicy", price.agePolicy()),
              Map.entry("entitlementKey", price.entitlementKey()),
              Map.entry("entitlementAmount", price.entitlementAmount())));
    } catch (Exception exception) {
      throw new BusinessException("PAY_ORDER_SNAPSHOT_INVALID", "订单价格快照序列化失败");
    }
  }

  private Optional<SellablePrice> readPriceSnapshot(String value) {
    try {
      var node = json.readTree(value);
      String productKey = requiredText(node, "productKey");
      String productName = requiredText(node, "productName");
      String scene = requiredText(node, "scene");
      String currency = requiredText(node, "currency");
      String billingPeriod = requiredText(node, "billingPeriod");
      String agePolicy = requiredText(node, "agePolicy");
      String entitlementKey = requiredText(node, "entitlementKey");
      long productId = node.path("productId").asLong();
      long priceId = node.path("priceId").asLong();
      int priceVersion = node.path("priceVersion").asInt();
      long amountMinor = node.path("amountMinor").asLong();
      long entitlementAmount = node.path("entitlementAmount").asLong();
      if (productId <= 0
          || priceId <= 0
          || priceVersion <= 0
          || amountMinor <= 0
          || entitlementAmount <= 0) {
        return Optional.empty();
      }
      return Optional.of(
          new SellablePrice(
              productId,
              productKey,
              productName,
              scene,
              priceId,
              priceVersion,
              amountMinor,
              currency,
              billingPeriod,
              agePolicy,
              entitlementKey,
              entitlementAmount,
              true));
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private String requiredText(com.fasterxml.jackson.databind.JsonNode node, String field) {
    String value = node.path(field).asText(null);
    if (value == null || value.isBlank()) throw new IllegalArgumentException(field);
    return value;
  }

  private Order order(OrderEntity e) {
    return Order.rehydrate(
        e.getId(),
        e.getOrderNo(),
        e.getBusinessOrderKey(),
        e.getUserId(),
        e.getProductId(),
        e.getPriceId(),
        e.getPriceVersion(),
        e.getAmountMinor(),
        e.getCurrency(),
        e.getChannel(),
        Order.Status.valueOf(e.getStatus()),
        e.getPaymentReference(),
        e.getRefundedMinor(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private Subscription subscription(SubscriptionEntity e) {
    return Subscription.rehydrate(
        e.getId(),
        e.getUserId(),
        e.getProductId(),
        e.getChannel(),
        e.getChannelSubscriptionId(),
        Subscription.Status.valueOf(e.getStatus()),
        e.getPeriodEnd(),
        e.getCancelMode(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}
