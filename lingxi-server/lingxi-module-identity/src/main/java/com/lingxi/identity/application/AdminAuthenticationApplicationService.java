package com.lingxi.identity.application;

import com.lingxi.identity.api.AdminAuthorizationFacade;
import com.lingxi.identity.domain.AdminAccount;
import com.lingxi.identity.domain.AdminIdentityRepository;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 管理端 Opaque Token 登录，和 C 端会话完全隔离。 */
@Service
public class AdminAuthenticationApplicationService implements AdminAuthorizationFacade {
  private static final int HASH_ITERATIONS = 210_000;
  private static final int HASH_BITS = 256;

  private final AdminIdentityRepository repository;
  private final SecureTokenGenerator tokens;
  private final IdGenerator ids;
  private final TransactionTemplate transactions;
  private final Duration sessionTtl;
  private final Duration recentWindow;

  public AdminAuthenticationApplicationService(
      AdminIdentityRepository repository,
      SecureTokenGenerator tokens,
      IdGenerator ids,
      TransactionTemplate transactions,
      @Value("${lingxi.identity.admin-session-ttl:PT8H}") Duration sessionTtl,
      @Value("${lingxi.identity.recent-authentication-window:PT5M}") Duration recentWindow) {
    this.repository = repository;
    this.tokens = tokens;
    this.ids = ids;
    this.transactions = transactions;
    this.sessionTtl = sessionTtl;
    this.recentWindow = recentWindow;
  }

  @Override
  public AdminSessionTokens login(AdminLoginCommand command) {
    if (command == null || blank(command.username()) || blank(command.password())
        || blank(command.deviceId())) {
      throw new BusinessException("ADMIN_LOGIN_INVALID", "账号、密码和设备标识不能为空");
    }
    AdminAccount account = repository.findByUsername(command.username().trim())
        .orElseThrow(() -> new BusinessException("ADMIN_LOGIN_FAILED", "账号或密码错误"));
    LocalDateTime now = now();
    account.ensureLoginAllowed(now);
    if (!verifyPassword(command.password(), account.getPasswordHash())) {
      long old = account.getVersion();
      account.loginFailed(now);
      if (!repository.updateAccount(account, old)) {
        throw new BusinessException("ADMIN_LOGIN_CONFLICT", "登录状态已变化，请重试");
      }
      throw new BusinessException("ADMIN_LOGIN_FAILED", "账号或密码错误");
    }

    String accessToken = tokens.nextToken();
    LocalDateTime expiresAt = now.plus(sessionTtl);
    transactions.executeWithoutResult(status -> {
      long old = account.getVersion();
      account.loginSucceeded(now);
      if (!repository.updateAccount(account, old)) {
        throw new BusinessException("ADMIN_LOGIN_CONFLICT", "登录状态已变化，请重试");
      }
      repository.insertSession(ids.nextId(), account.getId(), hashToken(accessToken),
          command.deviceId().trim(), command.ipAddress(), command.userAgent(), now, expiresAt);
    });
    return new AdminSessionTokens(accessToken, expiresAt.toInstant(ZoneOffset.UTC), profile(account));
  }

  @Override
  public AuthenticatedAdminSession authenticate(String accessToken) {
    if (blank(accessToken)) throw new BusinessException("AUTH_UNAUTHENTICATED", "请先登录");
    LocalDateTime now = now();
    var session = repository.findSession(hashToken(accessToken))
        .orElseThrow(() -> new BusinessException("AUTH_UNAUTHENTICATED", "管理端登录凭证无效"));
    if (session.revokedAt() != null || !session.expiresAt().isAfter(now)) {
      throw new BusinessException("AUTH_UNAUTHENTICATED", "管理端登录已过期");
    }
    AdminAccount account = repository.findById(session.adminId())
        .orElseThrow(() -> new BusinessException("AUTH_UNAUTHENTICATED", "管理员账号不存在"));
    account.ensureLoginAllowed(now);
    Set<String> permissions = Set.copyOf(repository.findPermissions(account.getId()));
    return new AuthenticatedAdminSession(account.getId(), account.getUsername(),
        account.getDisplayName(), permissions,
        !session.authenticatedAt().isBefore(now.minus(recentWindow)));
  }

  @Override
  public void reauthenticate(long adminId, String accessToken, String password) {
    if (adminId <= 0 || blank(accessToken) || blank(password)) {
      throw new BusinessException("ADMIN_REAUTH_INVALID", "重新认证参数不完整");
    }
    LocalDateTime now = now();
    AdminAccount account = repository.findById(adminId)
        .orElseThrow(() -> new BusinessException("AUTH_UNAUTHENTICATED", "管理员账号不存在"));
    account.ensureLoginAllowed(now);
    if (!verifyPassword(password, account.getPasswordHash())) {
      throw new BusinessException("ADMIN_REAUTH_FAILED", "管理员密码错误");
    }
    if (!repository.refreshSessionAuthentication(adminId, hashToken(accessToken), now)) {
      throw new BusinessException("AUTH_UNAUTHENTICATED", "管理端登录凭证无效或已过期");
    }
  }

  @Override
  public void logout(long adminId, String accessToken) {
    repository.revokeSession(adminId, hashToken(accessToken), now(), "ADMIN_LOGOUT");
  }

  @Override
  public boolean allowed(long adminId, String permission) {
    if (blank(permission)) return false;
    Set<String> permissions = repository.findPermissions(adminId);
    return permissions.contains("*") || permissions.contains(permission);
  }

  @Override
  public AdminProfile profile(long adminId) {
    return profile(repository.findById(adminId)
        .orElseThrow(() -> new BusinessException("ADMIN_NOT_FOUND", "管理员不存在")));
  }

  public static String encodePassword(String rawPassword) {
    if (rawPassword == null || rawPassword.length() < 12) {
      throw new IllegalArgumentException("管理员密码至少 12 位");
    }
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    byte[] derived = derive(rawPassword.toCharArray(), salt, HASH_ITERATIONS);
    return "pbkdf2$" + HASH_ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
        + "$" + Base64.getEncoder().encodeToString(derived);
  }

  private boolean verifyPassword(String rawPassword, String encoded) {
    try {
      String[] parts = encoded.split("\\$");
      if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;
      int iterations = Integer.parseInt(parts[1]);
      if (iterations < 100_000 || iterations > 1_000_000) return false;
      byte[] salt = Base64.getDecoder().decode(parts[2]);
      byte[] expected = Base64.getDecoder().decode(parts[3]);
      return MessageDigest.isEqual(expected, derive(rawPassword.toCharArray(), salt, iterations));
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private static byte[] derive(char[] password, byte[] salt, int iterations) {
    PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("JVM 不支持 PBKDF2WithHmacSHA256", exception);
    } finally {
      spec.clearPassword();
      java.util.Arrays.fill(password, '\0');
    }
  }

  private AdminProfile profile(AdminAccount account) {
    return new AdminProfile(account.getId(), account.getUsername(), account.getDisplayName(),
        account.getStatus(), Set.copyOf(repository.findRoles(account.getId())),
        Set.copyOf(repository.findPermissions(account.getId())), account.getLastLoginAt());
  }

  private String hashToken(String token) {
    try {
      return java.util.HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("JVM 不支持 SHA-256", exception);
    }
  }

  private LocalDateTime now() { return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC); }
  private boolean blank(String value) { return value == null || value.isBlank(); }
}
