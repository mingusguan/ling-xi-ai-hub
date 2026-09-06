package com.lingxi.identity.api;

import java.util.Set;

/** 管理端独立身份与最小权限校验，只向其他业务模块暴露认证事实。 */
public interface AdminAuthorizationFacade {
  AdminSessionTokens login(AdminLoginCommand command);

  AuthenticatedAdminSession authenticate(String accessToken);

  void reauthenticate(long adminId, String accessToken, String password);

  void logout(long adminId, String accessToken);

  boolean allowed(long adminId, String permission);

  AdminProfile profile(long adminId);

  record AdminLoginCommand(
      String username,
      String password,
      String deviceId,
      String ipAddress,
      String userAgent) {}

  record AdminSessionTokens(
      String accessToken,
      java.time.Instant expiresAt,
      AdminProfile profile) {}

  record AuthenticatedAdminSession(
      long adminId,
      String username,
      String displayName,
      Set<String> permissions,
      boolean recentAuthentication) {}

  record AdminProfile(
      long adminId,
      String username,
      String displayName,
      String status,
      Set<String> roles,
      Set<String> permissions,
      java.time.LocalDateTime lastLoginAt) {}
}
