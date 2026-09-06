-- V1.0.0 后台治理整改：结构化资源发布关联、安全告警生命周期和指标写权限。
ALTER TABLE ops_message_campaign ADD COLUMN current_release_id BIGINT NULL AFTER status;
ALTER TABLE ops_app_release ADD COLUMN current_release_id BIGINT NULL AFTER status;
ALTER TABLE ops_compliance_document ADD COLUMN current_release_id BIGINT NULL AFTER status;

ALTER TABLE ops_safety_alert
  ADD COLUMN attempt_count INT NOT NULL DEFAULT 0 AFTER status,
  ADD COLUMN next_retry_at DATETIME(6) NULL AFTER attempt_count,
  ADD COLUMN acknowledged_at DATETIME(6) NULL AFTER next_retry_at,
  ADD COLUMN resolved_at DATETIME(6) NULL AFTER acknowledged_at,
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER failure_reason;

INSERT INTO id_admin_permission(id,permission_key,name,permission_group,risk_level,status,created_at,updated_at)
SELECT 801035,'analytics:write','运营指标内部写入','ANALYTICS','HIGH','ACTIVE',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
WHERE NOT EXISTS(SELECT 1 FROM id_admin_permission WHERE permission_key='analytics:write');

INSERT IGNORE INTO id_admin_role_permission(id,role_id,permission_id,deleted,created_at)
SELECT 1801035,800001,801035,0,UTC_TIMESTAMP(6)
WHERE EXISTS(SELECT 1 FROM id_admin_role WHERE id=800001 AND deleted=0)
  AND EXISTS(SELECT 1 FROM id_admin_permission WHERE id=801035);
