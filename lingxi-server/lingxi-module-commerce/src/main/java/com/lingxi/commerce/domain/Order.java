package com.lingxi.commerce.domain;

import com.lingxi.kernel.BusinessException;
import java.time.*;

/** 服务端金额快照订单聚合。 */
public class Order {
  public enum Status {
    CREATED,
    PAYING,
    PAID,
    CLOSED,
    PARTIALLY_REFUNDED,
    REFUNDED
  }

  private final long id;
  private final String orderNo, businessKey;
  private final long userId, productId, priceId;
  private final int priceVersion;
  private final long amountMinor;
  private final String currency, channel;
  private Status status;
  private String paymentReference;
  private long refundedMinor, version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private Order(
      long id,
      String no,
      String key,
      long user,
      long product,
      long price,
      int pv,
      long amount,
      String currency,
      String channel,
      Status status,
      String reference,
      long refunded,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    this.id = id;
    orderNo = no;
    businessKey = key;
    userId = user;
    productId = product;
    priceId = price;
    priceVersion = pv;
    amountMinor = amount;
    this.currency = currency;
    this.channel = channel;
    this.status = status;
    paymentReference = reference;
    refundedMinor = refunded;
    this.version = version;
    createdAt = created;
    updatedAt = updated;
  }

  public static Order create(
      long id,
      String no,
      String key,
      long user,
      SellablePrice price,
      String channel,
      LocalDateTime now) {
    if (id <= 0
        || user <= 0
        || key == null
        || key.isBlank()
        || channel == null
        || channel.isBlank()) throw new BusinessException("PAY_INVALID_ORDER", "订单参数不合法");
    return new Order(
        id,
        no,
        key,
        user,
        price.productId(),
        price.priceId(),
        price.priceVersion(),
        price.amountMinor(),
        price.currency(),
        channel,
        Status.CREATED,
        null,
        0,
        0,
        now,
        now);
  }

  public static Order rehydrate(
      long id,
      String no,
      String key,
      long user,
      long product,
      long price,
      int pv,
      long amount,
      String currency,
      String channel,
      Status status,
      String reference,
      long refunded,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    return new Order(
        id, no, key, user, product, price, pv, amount, currency, channel, status, reference,
        refunded, version, created, updated);
  }

  public void paying(String reference, LocalDateTime now) {
    if (status != Status.CREATED)
      throw new BusinessException("PAY_ORDER_STATE_INVALID", "订单状态不允许发起支付");
    paymentReference = reference;
    status = Status.PAYING;
    version++;
    updatedAt = now;
  }

  public boolean pay(String transactionId, long amount, String currency, LocalDateTime now) {
    if (status == Status.PAID || status == Status.PARTIALLY_REFUNDED || status == Status.REFUNDED)
      return false;
    if (status != Status.CREATED && status != Status.PAYING)
      throw new BusinessException("PAY_ORDER_STATE_INVALID", "订单状态不允许支付");
    if (amount != amountMinor || !this.currency.equals(currency))
      throw new BusinessException("PAY_AMOUNT_MISMATCH", "支付金额或币种不匹配");
    paymentReference = transactionId;
    status = Status.PAID;
    version++;
    updatedAt = now;
    return true;
  }

  public boolean refund(long amount, LocalDateTime now) {
    if (status != Status.PAID && status != Status.PARTIALLY_REFUNDED)
      throw new BusinessException("PAY_ORDER_STATE_INVALID", "订单状态不允许退款");
    if (amount <= 0 || refundedMinor + amount > amountMinor)
      throw new BusinessException("PAY_REFUND_AMOUNT_INVALID", "退款金额不合法");
    refundedMinor += amount;
    status = refundedMinor == amountMinor ? Status.REFUNDED : Status.PARTIALLY_REFUNDED;
    version++;
    updatedAt = now;
    return true;
  }

  public boolean close(LocalDateTime now) {
    if (status == Status.CLOSED) return false;
    if (status != Status.CREATED && status != Status.PAYING) {
      throw new BusinessException("PAY_ORDER_STATE_INVALID", "订单状态不允许关闭");
    }
    status = Status.CLOSED;
    version++;
    updatedAt = now;
    return true;
  }

  public long getId() {
    return id;
  }

  public String getOrderNo() {
    return orderNo;
  }

  public String getBusinessKey() {
    return businessKey;
  }

  public long getUserId() {
    return userId;
  }

  public long getProductId() {
    return productId;
  }

  public long getPriceId() {
    return priceId;
  }

  public int getPriceVersion() {
    return priceVersion;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public String getCurrency() {
    return currency;
  }

  public String getChannel() {
    return channel;
  }

  public Status getStatus() {
    return status;
  }

  public String getPaymentReference() {
    return paymentReference;
  }

  public long getRefundedMinor() {
    return refundedMinor;
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
