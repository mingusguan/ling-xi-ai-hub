package com.lingxi.operations.infrastructure.persistence;

import com.lingxi.kernel.BusinessException;
import com.lingxi.operations.domain.SafetyAlertDeliveryRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 使用乐观版本抢占和更新安全告警投递状态。 */
@Repository
public class JdbcSafetyAlertDeliveryRepository implements SafetyAlertDeliveryRepository {
  private final JdbcTemplate jdbc;

  public JdbcSafetyAlertDeliveryRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<SafetyAlertDelivery> findDue(
      LocalDateTime now, LocalDateTime staleBefore, int limit) {
    return jdbc.query(
        "SELECT"
            + " id,safety_case_id,alert_channel,recipient_ref,status,attempt_count,next_retry_at,failure_reason,version"
            + " FROM ops_safety_alert WHERE (status IN ('PENDING','FAILED') AND (next_retry_at IS"
            + " NULL OR next_retry_at<=?)) OR (status='SENDING' AND updated_at<=?) ORDER BY"
            + " created_at LIMIT ?",
        (rs, row) -> map(rs),
        now,
        staleBefore,
        limit);
  }

  @Override
  public SafetyAlertDelivery find(long id) {
    return jdbc
        .query(
            "SELECT"
                + " id,safety_case_id,alert_channel,recipient_ref,status,attempt_count,next_retry_at,failure_reason,version"
                + " FROM ops_safety_alert WHERE id=?",
            (rs, row) -> map(rs),
            id)
        .stream()
        .findFirst()
        .orElse(null);
  }

  @Override
  public boolean claim(long id, long expected, LocalDateTime staleBefore, LocalDateTime now) {
    return jdbc.update(
            "UPDATE ops_safety_alert SET"
                + " status='SENDING',attempt_count=attempt_count+1,version=version+1,updated_at=?"
                + " WHERE id=? AND version=? AND (status IN ('PENDING','FAILED') OR"
                + " (status='SENDING' AND updated_at<=?))",
            now,
            id,
            expected,
            staleBefore)
        == 1;
  }

  @Override
  public boolean markSent(long id, long claimedVersion, LocalDateTime now) {
    return jdbc.update(
            "UPDATE ops_safety_alert SET"
                + " status='SENT',failure_reason=NULL,next_retry_at=NULL,version=version+1,updated_at=?"
                + " WHERE id=? AND version=? AND status='SENDING'",
            now,
            id,
            claimedVersion)
        == 1;
  }

  @Override
  public boolean markFailed(
      long id, long claimedVersion, String reason, LocalDateTime retry, LocalDateTime now) {
    return jdbc.update(
            "UPDATE ops_safety_alert SET"
                + " status='FAILED',failure_reason=?,next_retry_at=?,version=version+1,updated_at=?"
                + " WHERE id=? AND version=? AND status='SENDING'",
            safe(reason),
            retry,
            now,
            id,
            claimedVersion)
        == 1;
  }

  @Override
  public SafetyAlertDelivery transition(long id, String target, long expected, LocalDateTime now) {
    String sql =
        switch (target) {
          case "PENDING" ->
              "UPDATE ops_safety_alert SET"
                  + " status='PENDING',failure_reason=NULL,next_retry_at=?,version=version+1,updated_at=?"
                  + " WHERE id=? AND version=? AND status='FAILED'";
          case "ACKNOWLEDGED" ->
              "UPDATE ops_safety_alert SET"
                  + " status='ACKNOWLEDGED',acknowledged_at=?,version=version+1,updated_at=? WHERE"
                  + " id=? AND version=? AND status='SENT'";
          case "RESOLVED" ->
              "UPDATE ops_safety_alert SET"
                  + " status='RESOLVED',resolved_at=?,version=version+1,updated_at=? WHERE id=? AND"
                  + " version=? AND status='ACKNOWLEDGED'";
          default -> throw new BusinessException("OPS_SAFETY_ALERT_STATUS_INVALID", "告警状态不合法");
        };
    updated(jdbc.update(sql, now, now, id, expected));
    return find(id);
  }

  @Override
  public int resolveByCase(long caseId, LocalDateTime now) {
    return jdbc.update(
        "UPDATE ops_safety_alert SET status='RESOLVED',resolved_at=?,version=version+1,updated_at=?"
            + " WHERE safety_case_id=? AND status NOT IN ('RESOLVED')",
        now,
        now,
        caseId);
  }

  private SafetyAlertDelivery map(ResultSet rs) throws SQLException {
    return new SafetyAlertDelivery(
        rs.getLong("id"),
        rs.getLong("safety_case_id"),
        rs.getString("alert_channel"),
        rs.getString("recipient_ref"),
        rs.getString("status"),
        rs.getInt("attempt_count"),
        rs.getObject("next_retry_at", LocalDateTime.class),
        rs.getString("failure_reason"),
        rs.getLong("version"));
  }

  private String safe(String value) {
    if (value == null) return "DELIVERY_FAILED";
    return value.length() > 512 ? value.substring(0, 512) : value;
  }

  private void updated(int count) {
    if (count != 1) throw new BusinessException("OPS_SAFETY_ALERT_CONFLICT", "安全告警状态已变化");
  }
}
