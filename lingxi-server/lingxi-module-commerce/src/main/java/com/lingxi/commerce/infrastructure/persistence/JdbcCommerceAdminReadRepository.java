package com.lingxi.commerce.infrastructure.persistence;

import com.lingxi.commerce.api.AdminCommerceFacade.*;
import com.lingxi.commerce.domain.CommerceAdminReadRepository;
import com.lingxi.kernel.PageResult;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 只在 commerce 模块内部构建后台投影，所有查询排除逻辑删除。 */
@Repository
public class JdbcCommerceAdminReadRepository implements CommerceAdminReadRepository {
  private final JdbcTemplate jdbc;
  public JdbcCommerceAdminReadRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public CommerceOverview overview() {
    Map<String, Object> row = jdbc.queryForMap(
        "SELECT COUNT(*) order_count,SUM(status='PAID' OR status='PARTIALLY_REFUNDED' OR status='REFUNDED') paid_count,COALESCE(SUM(CASE WHEN status IN ('PAID','PARTIALLY_REFUNDED','REFUNDED') THEN amount_minor ELSE 0 END),0) revenue FROM pay_order WHERE deleted=0");
    long active = value(jdbc.queryForObject(
        "SELECT COUNT(*) FROM pay_subscription WHERE deleted=0 AND status IN ('ACTIVE','CANCEL_AT_PERIOD_END')", Long.class));
    long refunds = value(jdbc.queryForObject(
        "SELECT COUNT(*) FROM pay_refund WHERE deleted=0", Long.class));
    return new CommerceOverview(number(row.get("order_count")), number(row.get("paid_count")),
        number(row.get("revenue")), active, refunds);
  }

  public List<ProductSummary> products() {
    Map<Long, List<PriceSummary>> prices = new HashMap<>();
    jdbc.query("SELECT * FROM pay_price WHERE deleted=0 ORDER BY product_id,version_no DESC", rs -> {
      prices.computeIfAbsent(rs.getLong("product_id"), ignored -> new ArrayList<>())
          .add(new PriceSummary(rs.getLong("id"), rs.getInt("version_no"),
              rs.getLong("amount_minor"), rs.getString("currency"), rs.getString("status")));
    });
    return jdbc.query("SELECT * FROM pay_product WHERE deleted=0 ORDER BY created_at DESC", (rs, row) ->
        new ProductSummary(rs.getLong("id"), rs.getString("product_key"), rs.getString("name"),
            rs.getString("scene"), rs.getString("billing_period"), rs.getString("age_policy"),
            rs.getString("entitlement_key"), rs.getLong("entitlement_amount"),
            rs.getString("status"), List.copyOf(prices.getOrDefault(rs.getLong("id"), List.of()))));
  }

  public PageResult<OrderSummary> orders(String keyword, String status, int page, int size) {
    List<Object> args = new ArrayList<>();
    StringBuilder where = new StringBuilder(" WHERE deleted=0");
    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND (order_no LIKE ? OR CAST(user_id AS CHAR)=?)");
      args.add("%" + keyword.trim() + "%"); args.add(keyword.trim());
    }
    if (status != null && !status.isBlank()) { where.append(" AND status=?"); args.add(status); }
    long total = value(jdbc.queryForObject("SELECT COUNT(*) FROM pay_order" + where, Long.class, args.toArray()));
    args.add(size); args.add((page - 1) * size);
    List<OrderSummary> items = jdbc.query("SELECT * FROM pay_order" + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
        (rs, row) -> new OrderSummary(rs.getLong("id"), rs.getString("order_no"),
            rs.getLong("user_id"), rs.getLong("product_id"), rs.getLong("amount_minor"),
            rs.getString("currency"), rs.getString("channel"), rs.getString("status"),
            rs.getLong("refunded_minor"), rs.getLong("version"),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("updated_at", LocalDateTime.class)), args.toArray());
    return new PageResult<>(items, total, page, size);
  }

  public PageResult<SubscriptionSummary> subscriptions(String status, int page, int size) {
    List<Object> args = new ArrayList<>();
    String where = " WHERE deleted=0";
    if (status != null && !status.isBlank()) { where += " AND status=?"; args.add(status); }
    long total = value(jdbc.queryForObject("SELECT COUNT(*) FROM pay_subscription" + where, Long.class, args.toArray()));
    args.add(size); args.add((page - 1) * size);
    List<SubscriptionSummary> items = jdbc.query("SELECT * FROM pay_subscription" + where + " ORDER BY updated_at DESC LIMIT ? OFFSET ?",
        (rs, row) -> new SubscriptionSummary(rs.getLong("id"), rs.getLong("user_id"),
            rs.getLong("product_id"), rs.getString("channel"), rs.getString("status"),
            instant(rs.getObject("period_end", LocalDateTime.class)), rs.getString("cancel_mode"),
            rs.getLong("version"), rs.getObject("updated_at", LocalDateTime.class)), args.toArray());
    return new PageResult<>(items, total, page, size);
  }

  public PageResult<EntitlementSummary> entitlements(Long userId, int page, int size) {
    List<Object> args = new ArrayList<>();
    String where = " WHERE deleted=0";
    if (userId != null) { where += " AND user_id=?"; args.add(userId); }
    long total = value(jdbc.queryForObject("SELECT COUNT(*) FROM pay_entitlement" + where, Long.class, args.toArray()));
    args.add(size); args.add((page - 1) * size);
    List<EntitlementSummary> items = jdbc.query("SELECT * FROM pay_entitlement" + where + " ORDER BY updated_at DESC LIMIT ? OFFSET ?",
        (rs, row) -> new EntitlementSummary(rs.getLong("id"), rs.getLong("user_id"),
            rs.getString("resource_key"), rs.getLong("balance"),
            instant(rs.getObject("expires_at", LocalDateTime.class)), rs.getLong("version"),
            rs.getObject("updated_at", LocalDateTime.class)), args.toArray());
    return new PageResult<>(items, total, page, size);
  }

  private long value(Long value) { return value == null ? 0 : value; }
  private long number(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
  private Instant instant(LocalDateTime value) { return value == null ? null : value.toInstant(ZoneOffset.UTC); }
}
