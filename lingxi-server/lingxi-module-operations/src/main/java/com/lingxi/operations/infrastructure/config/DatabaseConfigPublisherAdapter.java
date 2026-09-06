package com.lingxi.operations.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.operations.application.ConfigPublisherAdapter;
import com.lingxi.operations.domain.ConfigRelease;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 模块化单体的本地配置发布器。只激活不透明安全引用，不读取或展示生产密钥。
 */
@Component
public class DatabaseConfigPublisherAdapter implements ConfigPublisherAdapter {
  private static final Set<String> ALLOWED_TYPES = Set.of(
      "MODEL_PROVIDER", "MODEL_ROUTE", "PROMPT", "AGENT_POLICY", "SAFETY_RULE",
      "MESSAGE_CAMPAIGN", "EXPERIMENT", "FEATURE_FLAG", "APP_RELEASE", "COMPLIANCE");
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final IdGenerator ids;

  public DatabaseConfigPublisherAdapter(JdbcTemplate jdbc, ObjectMapper json, IdGenerator ids) {
    this.jdbc = jdbc; this.json = json; this.ids = ids;
  }

  @Override
  public ValidationResult validate(ConfigRelease release) {
    if (!ALLOWED_TYPES.contains(release.getConfigType())) return new ValidationResult(false, "不支持的配置类型");
    if (release.getContentRef() == null || release.getContentRef().isBlank()
        || release.getContentRef().length() > 512) return new ValidationResult(false, "内容引用不合法");
    if (release.getContentRef().startsWith("http://")) return new ValidationResult(false, "禁止使用不安全的 HTTP 内容引用");
    if (release.getContentDigest() == null || !release.getContentDigest().matches("[0-9a-fA-F]{64}")) return new ValidationResult(false, "内容摘要必须为 64 位 SHA-256 十六进制");
    if (release.getGrayRule() != null && !release.getGrayRule().isBlank()) {
      try { json.readTree(release.getGrayRule()); } catch (Exception exception) { return new ValidationResult(false, "灰度规则不是合法 JSON"); }
    }
    return new ValidationResult(true, null);
  }

  @Override
  public void startGray(ConfigRelease release) {
    LocalDateTime now = now();
    jdbc.update("INSERT INTO ops_config_deployment(id,release_id,stage,previous_release_id,gray_rule,created_at,updated_at) VALUES(?,?,'GRAY',?,?,?,?) ON DUPLICATE KEY UPDATE stage='GRAY',previous_release_id=VALUES(previous_release_id),gray_rule=VALUES(gray_rule),updated_at=VALUES(updated_at)",
        ids.nextId(), release.getId(), release.getPreviousReleaseId(), release.getGrayRule(), now, now);
  }

  @Override
  public void publish(ConfigRelease release) {
    LocalDateTime now = now();
    jdbc.update("INSERT INTO ops_active_config(config_type,current_release_id,content_ref,content_digest,activated_at,deleted,deleted_at,updated_at) VALUES(?,?,?,?,?,0,NULL,?) ON DUPLICATE KEY UPDATE current_release_id=VALUES(current_release_id),content_ref=VALUES(content_ref),content_digest=VALUES(content_digest),activated_at=VALUES(activated_at),deleted=0,deleted_at=NULL,updated_at=VALUES(updated_at)",
        release.getConfigType(), release.getId(), release.getContentRef(), release.getContentDigest(), now, now);
    jdbc.update("UPDATE ops_config_deployment SET stage='PUBLISHED',updated_at=? WHERE release_id=?", now, release.getId());
  }

  @Override
  public void rollback(ConfigRelease release) {
    LocalDateTime now = now();
    if (release.getPreviousReleaseId() == null) {
      jdbc.update("UPDATE ops_active_config SET deleted=1,deleted_at=?,updated_at=? WHERE config_type=? AND current_release_id=? AND deleted=0", now, now, release.getConfigType(), release.getId());
    } else {
      jdbc.update("INSERT INTO ops_active_config(config_type,current_release_id,content_ref,content_digest,activated_at,deleted,deleted_at,updated_at) SELECT config_type,id,content_ref,content_digest,?,0,NULL,? FROM ops_config_release WHERE id=? ON DUPLICATE KEY UPDATE current_release_id=VALUES(current_release_id),content_ref=VALUES(content_ref),content_digest=VALUES(content_digest),activated_at=VALUES(activated_at),deleted=0,deleted_at=NULL,updated_at=VALUES(updated_at)",
          now, now, release.getPreviousReleaseId());
    }
    jdbc.update("UPDATE ops_config_deployment SET stage='ROLLED_BACK',updated_at=? WHERE release_id=?", now, release.getId());
  }

  private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
}
