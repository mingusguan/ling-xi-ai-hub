-- V1.0.0 后台管理业务闭环：用户风险、青年治理、内容质量、商业化、客服、运营与分析。

ALTER TABLE pay_product
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN deleted_at DATETIME(6) NULL;

ALTER TABLE pay_price
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN deleted_at DATETIME(6) NULL,
  ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE content_goal_template
  ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN deleted_at DATETIME(6) NULL;

ALTER TABLE content_template_version
  ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN deleted_at DATETIME(6) NULL;

ALTER TABLE ops_feature_flag
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN deleted_at DATETIME(6) NULL;

ALTER TABLE ops_experiment
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN deleted_at DATETIME(6) NULL;

ALTER TABLE ops_support_ticket
  ADD COLUMN sla_due_at DATETIME(6) NULL,
  ADD COLUMN follow_up_at DATETIME(6) NULL;

ALTER TABLE ops_message_campaign
  ADD COLUMN scheduled_at DATETIME(6) NULL,
  ADD COLUMN sent_count BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN success_count BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN failed_count BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN unsubscribe_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE pay_product
  DROP INDEX uk_pay_product_key,
  ADD COLUMN active_product_key VARBINARY(32) GENERATED ALWAYS AS (
    CASE WHEN deleted=0 THEN UNHEX(SHA2(product_key,256)) ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_pay_active_product_key(active_product_key);

ALTER TABLE pay_price
  DROP INDEX uk_pay_price_version,
  ADD COLUMN active_product_version VARBINARY(32) GENERATED ALWAYS AS (
    CASE WHEN deleted=0 THEN UNHEX(SHA2(CONCAT(product_id,':',version_no),256)) ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_pay_active_price_version(active_product_version);

ALTER TABLE content_goal_template
  DROP INDEX uk_content_template_key,
  ADD COLUMN active_template_key VARBINARY(32) GENERATED ALWAYS AS (
    CASE WHEN deleted=0 THEN UNHEX(SHA2(template_key,256)) ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_content_active_template_key(active_template_key);

ALTER TABLE content_template_version
  DROP INDEX uk_content_template_version,
  ADD COLUMN active_template_version VARBINARY(32) GENERATED ALWAYS AS (
    CASE WHEN deleted=0 THEN UNHEX(SHA2(CONCAT(template_id,':',version_no),256)) ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_content_active_template_version(active_template_version);

ALTER TABLE ops_feature_flag
  DROP INDEX uk_ops_flag_key,
  ADD COLUMN active_flag_key VARBINARY(32) GENERATED ALWAYS AS (
    CASE WHEN deleted=0 THEN UNHEX(SHA2(flag_key,256)) ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_ops_active_flag_key(active_flag_key);

ALTER TABLE ops_experiment
  DROP INDEX uk_ops_experiment_key,
  ADD COLUMN active_experiment_key VARBINARY(32) GENERATED ALWAYS AS (
    CASE WHEN deleted=0 THEN UNHEX(SHA2(experiment_key,256)) ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_ops_active_experiment_key(active_experiment_key);

ALTER TABLE ops_message_campaign
  DROP INDEX uk_ops_message_campaign_key,
  ADD COLUMN active_campaign_key VARBINARY(32) GENERATED ALWAYS AS (
    CASE WHEN deleted=0 THEN UNHEX(SHA2(campaign_key,256)) ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_ops_active_campaign_key(active_campaign_key);

ALTER TABLE ops_app_release
  DROP INDEX uk_ops_app_release,
  ADD COLUMN active_platform_version VARBINARY(32) GENERATED ALWAYS AS (
    CASE WHEN deleted=0 THEN UNHEX(SHA2(CONCAT(platform,':',version_code),256)) ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_ops_active_app_release(active_platform_version);

ALTER TABLE ops_compliance_document
  DROP INDEX uk_ops_compliance_document,
  ADD COLUMN active_document_version VARBINARY(32) GENERATED ALWAYS AS (
    CASE WHEN deleted=0 THEN UNHEX(SHA2(CONCAT(document_type,':',version_no),256)) ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_ops_active_compliance_document(active_document_version);

CREATE UNIQUE INDEX uk_ops_safety_source ON ops_safety_case(source_type, source_id);

CREATE TABLE IF NOT EXISTS id_login_risk_event (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  device_id VARCHAR(128) NULL,
  risk_level VARCHAR(16) NOT NULL,
  risk_type VARCHAR(48) NOT NULL,
  summary VARCHAR(512) NOT NULL,
  status VARCHAR(24) NOT NULL,
  reviewer_admin_id BIGINT NULL,
  resolution VARCHAR(512) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  KEY idx_id_login_risk_queue(status, risk_level, created_at),
  KEY idx_id_login_risk_user(user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS id_age_appeal (
  id BIGINT PRIMARY KEY,
  appeal_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  claimed_birth_date DATE NOT NULL,
  evidence_ref VARCHAR(512) NOT NULL,
  status VARCHAR(24) NOT NULL,
  reviewer_admin_id BIGINT NULL,
  resolution VARCHAR(512) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_id_age_appeal_no(appeal_no),
  KEY idx_id_age_appeal_queue(status, created_at),
  KEY idx_id_age_appeal_user(user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rel_guardian_dispute (
  id BIGINT PRIMARY KEY,
  dispute_no VARCHAR(64) NOT NULL,
  relation_id BIGINT NOT NULL,
  teen_user_id BIGINT NOT NULL,
  guardian_user_id BIGINT NOT NULL,
  reason VARCHAR(512) NOT NULL,
  status VARCHAR(24) NOT NULL,
  reviewer_admin_id BIGINT NULL,
  resolution VARCHAR(512) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_rel_guardian_dispute_no(dispute_no),
  KEY idx_rel_guardian_dispute_queue(status, created_at),
  KEY idx_rel_guardian_dispute_teen(teen_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS content_template_review (
  id BIGINT PRIMARY KEY,
  template_id BIGINT NOT NULL,
  template_version BIGINT NOT NULL,
  review_dimension VARCHAR(24) NOT NULL,
  decision VARCHAR(24) NOT NULL,
  comment VARCHAR(512) NOT NULL,
  reviewer_admin_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_content_template_review(template_id, template_version, review_dimension),
  KEY idx_content_template_review_template(template_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS content_template_metric_daily (
  id BIGINT PRIMARY KEY,
  metric_date DATE NOT NULL,
  template_id BIGINT NOT NULL,
  usage_count BIGINT NOT NULL DEFAULT 0,
  completion_count BIGINT NOT NULL DEFAULT 0,
  negative_feedback_count BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_content_template_metric(metric_date, template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pay_promotion (
  id BIGINT PRIMARY KEY,
  promotion_key VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  promotion_type VARCHAR(24) NOT NULL,
  rule_json JSON NOT NULL,
  audience_rule JSON NOT NULL,
  starts_at DATETIME(6) NOT NULL,
  ends_at DATETIME(6) NOT NULL,
  status VARCHAR(24) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_pay_promotion_key(promotion_key),
  KEY idx_pay_promotion_status(status, starts_at, ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE pay_promotion
  DROP INDEX uk_pay_promotion_key,
  ADD COLUMN active_promotion_key VARBINARY(32) GENERATED ALWAYS AS (
    CASE WHEN deleted=0 THEN UNHEX(SHA2(promotion_key,256)) ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_pay_active_promotion_key(active_promotion_key);

CREATE TABLE IF NOT EXISTS pay_reconciliation_case (
  id BIGINT PRIMARY KEY,
  case_no VARCHAR(64) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  business_type VARCHAR(32) NOT NULL,
  business_id VARCHAR(128) NOT NULL,
  expected_minor BIGINT NOT NULL,
  actual_minor BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  difference_reason VARCHAR(512) NULL,
  status VARCHAR(24) NOT NULL,
  reviewer_admin_id BIGINT NULL,
  resolution VARCHAR(512) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_pay_reconciliation_case_no(case_no),
  KEY idx_pay_reconciliation_queue(status, channel, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_support_ticket_message (
  id BIGINT PRIMARY KEY,
  ticket_id BIGINT NOT NULL,
  sender_type VARCHAR(24) NOT NULL,
  sender_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  internal_note TINYINT(1) NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  KEY idx_ops_ticket_message(ticket_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_support_ticket_history (
  id BIGINT PRIMARY KEY,
  ticket_id BIGINT NOT NULL,
  action VARCHAR(48) NOT NULL,
  from_status VARCHAR(24) NULL,
  to_status VARCHAR(24) NULL,
  operator_type VARCHAR(24) NOT NULL,
  operator_id BIGINT NOT NULL,
  detail VARCHAR(512) NULL,
  created_at DATETIME(6) NOT NULL,
  KEY idx_ops_ticket_history(ticket_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_safety_alert (
  id BIGINT PRIMARY KEY,
  safety_case_id BIGINT NOT NULL,
  alert_channel VARCHAR(24) NOT NULL,
  recipient_ref VARCHAR(128) NOT NULL,
  status VARCHAR(24) NOT NULL,
  failure_reason VARCHAR(512) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_ops_safety_alert(safety_case_id, alert_channel, recipient_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_metric_daily (
  id BIGINT PRIMARY KEY,
  metric_date DATE NOT NULL,
  metric_key VARCHAR(96) NOT NULL,
  dimension_type VARCHAR(32) NOT NULL,
  dimension_value VARCHAR(96) NOT NULL,
  metric_value DECIMAL(20,4) NOT NULL,
  sample_count BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_ops_metric_daily(metric_date, metric_key, dimension_type, dimension_value),
  KEY idx_ops_metric_query(metric_key, metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO id_admin_permission
  (id, permission_key, name, permission_group, risk_level, status, created_at, updated_at)
VALUES
  (801025, 'identity:risk:manage', '处理登录风险', 'IDENTITY', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801026, 'identity:age-appeal:manage', '处理年龄申诉', 'IDENTITY', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801027, 'relationship:guardian-dispute:manage', '处理监护争议', 'YOUTH', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801028, 'commerce:catalog:manage', '管理商品价格与促销', 'COMMERCE', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801029, 'commerce:reconcile:manage', '处理渠道对账', 'COMMERCE', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801030, 'support:ticket:reply', '回复客服工单', 'SUPPORT', 'MEDIUM', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801031, 'analytics:read', '查看匿名化运营分析', 'ANALYTICS', 'MEDIUM', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801032, 'app:release:manage', '管理客户端版本', 'COMPLIANCE', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801033, 'feature:flag:manage', '管理功能开关', 'CONFIG', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (801034, 'content:template:quality', '管理模板质量评审', 'CONTENT', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT IGNORE INTO id_admin_role_permission(id, role_id, permission_id, deleted, created_at)
SELECT id + 1000000, 800001, id, 0, UTC_TIMESTAMP(6)
FROM id_admin_permission WHERE id BETWEEN 801025 AND 801034;
