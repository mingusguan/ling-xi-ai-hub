package com.lingxi.operations.infrastructure.config;

import com.lingxi.companion.api.AgentRuntimeConfigProvider;
import com.lingxi.kernel.BusinessException;
import com.lingxi.operations.api.RuntimeGovernanceFacade;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 从已发布配置和结构化资源生成客户端与 Agent 的只读运行时视图。 */
@Service
public class JdbcRuntimeGovernanceService
    implements RuntimeGovernanceFacade, AgentRuntimeConfigProvider {
  private final JdbcTemplate jdbc;

  public JdbcRuntimeGovernanceService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional(readOnly = true)
  public ClientBootstrapResult clientBootstrap(String platform, long currentVersionCode) {
    if (!"PC_WEB".equals(platform) && !"HARMONY".equals(platform))
      throw new BusinessException("OPS_CLIENT_PLATFORM_INVALID", "客户端平台不合法");
    if (currentVersionCode < 0)
      throw new BusinessException("OPS_CLIENT_VERSION_INVALID", "客户端版本号不能为负数");
    UpgradePolicy upgrade =
        jdbc
            .query(
                "SELECT"
                    + " r.version_name,r.version_code,r.minimum_version_code,r.force_upgrade,r.release_notes"
                    + " FROM ops_app_release r JOIN ops_config_release cr ON"
                    + " cr.id=r.current_release_id AND cr.config_type='APP_RELEASE' AND"
                    + " cr.content_ref=CONCAT('governance://APP_RELEASE/',r.id) AND"
                    + " cr.status='PUBLISHED' WHERE r.platform=? AND"
                    + " r.status='PUBLISHED' AND r.deleted=0 ORDER BY r.version_code DESC LIMIT 1",
                (rs, row) ->
                    new UpgradePolicy(
                        rs.getLong("version_code") > currentVersionCode,
                        rs.getBoolean("force_upgrade")
                            || currentVersionCode < rs.getLong("minimum_version_code"),
                        rs.getString("version_name"),
                        rs.getLong("version_code"),
                        rs.getLong("minimum_version_code"),
                        rs.getString("release_notes")),
                platform)
            .stream()
            .findFirst()
            .orElse(
                new UpgradePolicy(
                    false, false, null, currentVersionCode, currentVersionCode, null));
    List<ComplianceDocument> documents =
        jdbc.query(
            "SELECT document_type,version_no,title,content_ref,content_digest,effective_at FROM"
                + " (SELECT"
                + " d.document_type,d.version_no,d.title,d.content_ref,d.content_digest,d.effective_at,ROW_NUMBER()"
                + " OVER(PARTITION BY d.document_type ORDER BY d.effective_at DESC,d.version_no"
                + " DESC,d.id DESC) AS rn FROM ops_compliance_document d JOIN ops_config_release cr"
                + " ON cr.id=d.current_release_id AND cr.config_type='COMPLIANCE' AND"
                + " cr.content_ref=CONCAT('governance://COMPLIANCE/',d.id) AND"
                + " cr.status='PUBLISHED' WHERE d.status='PUBLISHED' AND"
                + " d.deleted=0 AND (d.effective_at IS NULL OR d.effective_at<=?)) latest WHERE"
                + " rn=1 ORDER BY document_type",
            (rs, row) ->
                new ComplianceDocument(
                    rs.getString("document_type"),
                    rs.getString("version_no"),
                    rs.getString("title"),
                    rs.getString("content_ref"),
                    rs.getString("content_digest"),
                    time(rs, "effective_at")),
            now());
    Map<String, Boolean> flags = new LinkedHashMap<>();
    jdbc.query(
        "SELECT f.flag_key,f.status,f.mandatory_policy FROM ops_feature_flag f JOIN"
            + " ops_config_release cr ON cr.id=f.current_release_id AND"
            + " cr.config_type='FEATURE_FLAG' AND"
            + " cr.content_ref=CONCAT('governance://FEATURE_FLAG/',f.id) AND cr.status='PUBLISHED'"
            + " WHERE f.status IN ('ACTIVE','DISABLED') AND f.deleted=0 ORDER BY"
            + " f.flag_key",
        rs -> {
          flags.put(
              rs.getString("flag_key"),
              rs.getBoolean("mandatory_policy") || "ACTIVE".equals(rs.getString("status")));
        });
    List<String> experiments =
        jdbc.query(
            "SELECT e.experiment_key FROM ops_experiment e JOIN ops_config_release cr ON"
                + " cr.id=e.current_release_id AND cr.config_type='EXPERIMENT' AND"
                + " cr.content_ref=CONCAT('governance://EXPERIMENT/',e.id) AND"
                + " cr.status='PUBLISHED' WHERE e.status='RUNNING' AND e.deleted=0"
                + " ORDER BY e.experiment_key",
            (rs, row) -> rs.getString(1));
    return new ClientBootstrapResult(
        upgrade, List.copyOf(documents), Map.copyOf(flags), List.copyOf(experiments), now());
  }

  @Override
  @Transactional(readOnly = true)
  public AgentRuntimeConfig resolve(String scene) {
    ActiveConfig provider = active("MODEL_PROVIDER");
    ActiveConfig route = active("MODEL_ROUTE");
    ActiveConfig prompt = active("PROMPT");
    if (provider == null || route == null || prompt == null)
      throw new BusinessException(
          "AGENT_RUNTIME_CONFIG_UNAVAILABLE", "Agent 模型提供方、场景路由或 Prompt 尚未完成发布");
    return new AgentRuntimeConfig(
        provider.ref(),
        provider.digest(),
        route.ref(),
        route.digest(),
        prompt.ref(),
        prompt.digest());
  }

  private ActiveConfig active(String type) {
    return jdbc
        .query(
            "SELECT content_ref,content_digest FROM ops_active_config WHERE config_type=? AND"
                + " deleted=0",
            (rs, row) -> new ActiveConfig(rs.getString(1), rs.getString(2)),
            type)
        .stream()
        .findFirst()
        .orElse(null);
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private LocalDateTime time(ResultSet rs, String column) throws SQLException {
    return rs.getObject(column, LocalDateTime.class);
  }

  private record ActiveConfig(String ref, String digest) {}
}
