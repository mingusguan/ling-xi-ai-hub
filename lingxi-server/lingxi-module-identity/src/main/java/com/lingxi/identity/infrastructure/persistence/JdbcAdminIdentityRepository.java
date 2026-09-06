package com.lingxi.identity.infrastructure.persistence;

import com.lingxi.identity.domain.AdminAccount;
import com.lingxi.identity.domain.AdminIdentityRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 管理员身份持久化适配器；权限查询只读取 identity 自有表。 */
@Repository
public class JdbcAdminIdentityRepository implements AdminIdentityRepository {
  private final JdbcTemplate jdbc;

  public JdbcAdminIdentityRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<AdminAccount> findByUsername(String username) {
    return jdbc.query(
        "SELECT * FROM id_admin_account WHERE username=? AND deleted=0 LIMIT 1",
        this::account,
        username).stream().findFirst();
  }

  @Override
  public Optional<AdminAccount> findById(long id) {
    return jdbc.query(
        "SELECT * FROM id_admin_account WHERE id=? AND deleted=0 LIMIT 1",
        this::account,
        id).stream().findFirst();
  }

  @Override
  public boolean updateAccount(AdminAccount a, long previousVersion) {
    return jdbc.update(
        "UPDATE id_admin_account SET failed_attempts=?,locked_until=?,last_login_at=?,version=?,updated_at=? WHERE id=? AND version=? AND deleted=0",
        a.getFailedAttempts(), a.getLockedUntil(), a.getLastLoginAt(), a.getVersion(),
        a.getUpdatedAt(), a.getId(), previousVersion) == 1;
  }

  @Override
  public void insertSession(long id, long adminId, String tokenHash, String deviceId,
      String ipAddress, String userAgent, LocalDateTime authenticatedAt, LocalDateTime expiresAt) {
    jdbc.update(
        "INSERT INTO id_admin_session(id,admin_id,access_token_hash,device_id,ip_address,user_agent,authenticated_at,expires_at,deleted,created_at) VALUES(?,?,?,?,?,?,?,?,0,?)",
        id, adminId, tokenHash, deviceId, ipAddress, truncate(userAgent, 512),
        authenticatedAt, expiresAt, authenticatedAt);
  }

  @Override
  public Optional<AdminSession> findSession(String tokenHash) {
    return jdbc.query(
        "SELECT id,admin_id,authenticated_at,expires_at,revoked_at FROM id_admin_session WHERE access_token_hash=? AND deleted=0 LIMIT 1",
        (rs, row) -> new AdminSession(rs.getLong("id"), rs.getLong("admin_id"),
            rs.getObject("authenticated_at", LocalDateTime.class),
            rs.getObject("expires_at", LocalDateTime.class),
            rs.getObject("revoked_at", LocalDateTime.class)),
        tokenHash).stream().findFirst();
  }

  @Override
  public boolean refreshSessionAuthentication(
      long adminId, String tokenHash, LocalDateTime authenticatedAt) {
    return jdbc.update(
        "UPDATE id_admin_session SET authenticated_at=? WHERE admin_id=? AND access_token_hash=? AND revoked_at IS NULL AND expires_at>? AND deleted=0",
        authenticatedAt, adminId, tokenHash, authenticatedAt) == 1;
  }

  @Override
  public void revokeSession(long adminId, String tokenHash, LocalDateTime now, String reason) {
    jdbc.update(
        "UPDATE id_admin_session SET revoked_at=?,revoke_reason=? WHERE admin_id=? AND access_token_hash=? AND revoked_at IS NULL AND deleted=0",
        now, truncate(reason, 256), adminId, tokenHash);
  }

  @Override
  public int revokeAllSessions(long adminId, LocalDateTime now, String reason) {
    return jdbc.update(
        "UPDATE id_admin_session SET revoked_at=?,revoke_reason=? WHERE admin_id=? AND revoked_at IS NULL AND deleted=0",
        now, truncate(reason, 256), adminId);
  }

  @Override
  public Set<String> findPermissions(long adminId) {
    RoleAccess roles = activeRoles(adminId);
    if (roles.ids().isEmpty()) return Set.of();
    String roleMarks = marks(roles.ids().size());
    List<Long> permissionIds = jdbc.queryForList(
        "SELECT DISTINCT permission_id FROM id_admin_role_permission WHERE role_id IN ("
            + roleMarks + ") AND deleted=0", Long.class, roles.ids().toArray());
    if (permissionIds.isEmpty()) return Set.of();
    return new HashSet<>(jdbc.queryForList(
        "SELECT permission_key FROM id_admin_permission WHERE id IN ("
            + marks(permissionIds.size()) + ") AND status='ACTIVE'",
        String.class, permissionIds.toArray()));
  }

  @Override
  public Set<String> findRoles(long adminId) {
    return new HashSet<>(activeRoles(adminId).keys());
  }

  private RoleAccess activeRoles(long adminId) {
    List<Long> assigned = jdbc.queryForList(
        "SELECT role_id FROM id_admin_account_role WHERE admin_id=? AND deleted=0",
        Long.class, adminId);
    if (assigned.isEmpty()) return new RoleAccess(List.of(), Set.of());
    Map<Long, String> active = new LinkedHashMap<>();
    jdbc.query("SELECT id,role_key FROM id_admin_role WHERE id IN (" + marks(assigned.size())
        + ") AND deleted=0 AND status='ACTIVE'",
        rs -> { active.put(rs.getLong("id"), rs.getString("role_key")); }, assigned.toArray());
    return new RoleAccess(List.copyOf(active.keySet()), Set.copyOf(active.values()));
  }

  private String marks(int size) {
    return String.join(",", Collections.nCopies(size, "?"));
  }

  private record RoleAccess(List<Long> ids, Set<String> keys) {}

  private AdminAccount account(ResultSet rs, int row) throws SQLException {
    return AdminAccount.rehydrate(
        rs.getLong("id"), rs.getString("username"), rs.getString("display_name"),
        rs.getString("password_hash"), rs.getString("status"), rs.getInt("failed_attempts"),
        rs.getObject("locked_until", LocalDateTime.class),
        rs.getObject("password_changed_at", LocalDateTime.class),
        rs.getObject("last_login_at", LocalDateTime.class), rs.getLong("version"),
        rs.getObject("created_at", LocalDateTime.class),
        rs.getObject("updated_at", LocalDateTime.class));
  }

  private String truncate(String value, int length) {
    if (value == null) return null;
    return value.length() <= length ? value : value.substring(0, length);
  }
}
