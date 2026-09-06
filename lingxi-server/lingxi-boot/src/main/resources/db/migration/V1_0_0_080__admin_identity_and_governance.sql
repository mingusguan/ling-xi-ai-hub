-- V1.0.0 灵犀伴行后台：独立管理员身份、最小权限 RBAC 与运营治理资源
CREATE TABLE IF NOT EXISTS id_admin_account (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  password_hash VARCHAR(512) NOT NULL,
  status VARCHAR(24) NOT NULL,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until DATETIME(6) NULL,
  password_changed_at DATETIME(6) NOT NULL,
  last_login_at DATETIME(6) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_id_admin_username(username),
  KEY idx_id_admin_status(status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS id_admin_role (
  id BIGINT PRIMARY KEY,
  role_key VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  status VARCHAR(24) NOT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_id_admin_role_key(role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS id_admin_permission (
  id BIGINT PRIMARY KEY,
  permission_key VARCHAR(96) NOT NULL,
  name VARCHAR(96) NOT NULL,
  permission_group VARCHAR(64) NOT NULL,
  risk_level VARCHAR(16) NOT NULL,
  status VARCHAR(24) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_id_admin_permission_key(permission_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS id_admin_account_role (
  id BIGINT PRIMARY KEY,
  admin_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_id_admin_account_role(admin_id, role_id),
  KEY idx_id_admin_account_role_role(role_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS id_admin_role_permission (
  id BIGINT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_id_admin_role_permission(role_id, permission_id),
  KEY idx_id_admin_role_permission_permission(permission_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS id_admin_session (
  id BIGINT PRIMARY KEY,
  admin_id BIGINT NOT NULL,
  access_token_hash CHAR(64) NOT NULL,
  device_id VARCHAR(128) NOT NULL,
  ip_address VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  authenticated_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6) NULL,
  revoke_reason VARCHAR(256) NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_id_admin_session_token(access_token_hash),
  KEY idx_id_admin_session_admin(admin_id, expires_at, revoked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_safety_case (
  id BIGINT PRIMARY KEY,
  case_no VARCHAR(64) NOT NULL,
  user_id BIGINT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_id VARCHAR(128) NULL,
  risk_category VARCHAR(32) NOT NULL,
  risk_level VARCHAR(16) NOT NULL,
  content_excerpt VARCHAR(512) NULL,
  status VARCHAR(24) NOT NULL,
  reviewer_admin_id BIGINT NULL,
  resolution VARCHAR(512) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_ops_safety_case_no(case_no),
  KEY idx_ops_safety_queue(status, risk_level, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_message_campaign (
  id BIGINT PRIMARY KEY,
  campaign_key VARCHAR(96) NOT NULL,
  name VARCHAR(128) NOT NULL,
  channel VARCHAR(24) NOT NULL,
  audience_rule JSON NOT NULL,
  template_content TEXT NOT NULL,
  frequency_rule JSON NOT NULL,
  teen_marketing_enabled TINYINT(1) NOT NULL DEFAULT 0,
  status VARCHAR(24) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_ops_message_campaign_key(campaign_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_app_release (
  id BIGINT PRIMARY KEY,
  platform VARCHAR(24) NOT NULL,
  version_name VARCHAR(32) NOT NULL,
  version_code BIGINT NOT NULL,
  minimum_version_code BIGINT NOT NULL,
  force_upgrade TINYINT(1) NOT NULL DEFAULT 0,
  gray_rule JSON NULL,
  release_notes TEXT NOT NULL,
  status VARCHAR(24) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_ops_app_release(platform, version_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_compliance_document (
  id BIGINT PRIMARY KEY,
  document_type VARCHAR(48) NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  content_ref VARCHAR(512) NOT NULL,
  content_digest CHAR(64) NOT NULL,
  effective_at DATETIME(6) NULL,
  status VARCHAR(24) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_ops_compliance_document(document_type, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_active_config (
  config_type VARCHAR(64) PRIMARY KEY,
  current_release_id BIGINT NOT NULL,
  content_ref VARCHAR(512) NOT NULL,
  content_digest CHAR(64) NOT NULL,
  activated_at DATETIME(6) NOT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_config_deployment (
  id BIGINT PRIMARY KEY,
  release_id BIGINT NOT NULL,
  stage VARCHAR(24) NOT NULL,
  previous_release_id BIGINT NULL,
  gray_rule JSON NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_ops_config_deployment_release(release_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ops_ticket_queue ON ops_support_ticket(status, priority, updated_at);
CREATE INDEX idx_ops_release_queue ON ops_config_release(status, config_type, updated_at);
CREATE INDEX idx_ops_audit_query ON ops_audit_log(admin_id, action, created_at);
ALTER TABLE ops_audit_log ADD COLUMN source_event_id VARCHAR(64) NULL;
CREATE UNIQUE INDEX uk_ops_audit_source_event ON ops_audit_log(source_event_id);

-- 仅初始化权限目录和超级管理员角色，不写入任何默认密码。
INSERT IGNORE INTO id_admin_role(id, role_key, name, status, deleted, created_at, updated_at)
VALUES (800001, 'SUPER_ADMIN', '超级管理员', 'ACTIVE', 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT IGNORE INTO id_admin_permission
  (id, permission_key, name, permission_group, risk_level, status, created_at, updated_at)
VALUES
  (801001, '*', '全部权限', 'SYSTEM', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801002, 'dashboard:read', '查看运营总览', 'DASHBOARD', 'LOW', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801003, 'identity:user:read', '查询用户', 'IDENTITY', 'MEDIUM', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801004, 'identity:user:restrict', '限制用户账号', 'IDENTITY', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801005, 'identity:admin:manage', '管理管理员与角色', 'SYSTEM', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801006, 'commerce:read', '查看订单与权益', 'COMMERCE', 'MEDIUM', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801007, 'commerce.refund.confirm', '确认退款', 'COMMERCE', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801008, 'commerce:entitlement:adjust', '调整权益', 'COMMERCE', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801009, 'content:template:read', '查看目标模板', 'CONTENT', 'LOW', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801010, 'content:template:create', '创建目标模板', 'CONTENT', 'MEDIUM', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801011, 'content:template:submit', '提交目标模板', 'CONTENT', 'MEDIUM', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801012, 'content:template:review', '审核目标模板', 'CONTENT', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801013, 'content:template:retire', '下架目标模板', 'CONTENT', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801014, 'support.ticket.manage', '处理客服工单', 'SUPPORT', 'MEDIUM', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801015, 'config.release.create', '创建配置发布', 'CONFIG', 'MEDIUM', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801016, 'config.release.validate', '验证配置发布', 'CONFIG', 'MEDIUM', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801017, 'config.release.approve', '审批配置发布', 'CONFIG', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801018, 'config.release.publish', '灰度和发布配置', 'CONFIG', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801019, 'config.release.rollback', '回滚配置', 'CONFIG', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801020, 'safety:case:manage', '处理安全事件', 'SAFETY', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801021, 'message:campaign:manage', '管理消息触达', 'ENGAGEMENT', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801022, 'experiment:manage', '管理实验', 'ANALYTICS', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801023, 'compliance:manage', '管理版本与协议', 'COMPLIANCE', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801024, 'audit:read', '查看管理审计', 'AUDIT', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT IGNORE INTO id_admin_role_permission(id, role_id, permission_id, deleted, created_at)
SELECT id + 1000000, 800001, id, 0, UTC_TIMESTAMP(6)
FROM id_admin_permission WHERE id BETWEEN 801001 AND 801024;
