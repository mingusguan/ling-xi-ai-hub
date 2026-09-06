package com.lingxi.identity.infrastructure.security;

import com.lingxi.identity.application.AdminAuthenticationApplicationService;
import com.lingxi.kernel.IdGenerator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 通过安全配置首次创建超级管理员。未提供密码时保持关闭，禁止内置默认口令。
 */
@Component
public class AdminBootstrapInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final IdGenerator ids;
  private final String username;
  private final String password;
  private final String displayName;

  public AdminBootstrapInitializer(
      JdbcTemplate jdbc,
      IdGenerator ids,
      @Value("${lingxi.identity.bootstrap-admin.username:}") String username,
      @Value("${lingxi.identity.bootstrap-admin.password:}") String password,
      @Value("${lingxi.identity.bootstrap-admin.display-name:平台管理员}") String displayName) {
    this.jdbc = jdbc;
    this.ids = ids;
    this.username = username;
    this.password = password;
    this.displayName = displayName;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (username == null || username.isBlank() || password == null || password.isBlank()) return;
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM id_admin_account WHERE username=? AND deleted=0",
        Integer.class, username.trim());
    if (count != null && count > 0) return;
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    long adminId = ids.nextId();
    jdbc.update(
        "INSERT INTO id_admin_account(id,username,display_name,password_hash,status,failed_attempts,password_changed_at,version,deleted,created_at,updated_at) VALUES(?,?,?,?, 'ACTIVE',0,?,0,0,?,?)",
        adminId, username.trim(), displayName.trim(),
        AdminAuthenticationApplicationService.encodePassword(password), now, now, now);
    jdbc.update(
        "INSERT INTO id_admin_account_role(id,admin_id,role_id,deleted,created_at) VALUES(?,?,800001,0,?)",
        ids.nextId(), adminId, now);
  }
}
