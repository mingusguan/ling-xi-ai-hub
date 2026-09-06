package com.lingxi.identity.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/** 管理员账号、会话和 RBAC 的持久化端口。 */
public interface AdminIdentityRepository {
  Optional<AdminAccount> findByUsername(String username);
  Optional<AdminAccount> findById(long id);
  boolean updateAccount(AdminAccount account, long previousVersion);
  void insertSession(long id, long adminId, String tokenHash, String deviceId, String ipAddress,
      String userAgent, LocalDateTime authenticatedAt, LocalDateTime expiresAt);
  Optional<AdminSession> findSession(String tokenHash);
  boolean refreshSessionAuthentication(long adminId, String tokenHash, LocalDateTime authenticatedAt);
  void revokeSession(long adminId, String tokenHash, LocalDateTime now, String reason);
  int revokeAllSessions(long adminId, LocalDateTime now, String reason);
  Set<String> findPermissions(long adminId);
  Set<String> findRoles(long adminId);

  record AdminSession(long id, long adminId, LocalDateTime authenticatedAt,
      LocalDateTime expiresAt, LocalDateTime revokedAt) {}
}
