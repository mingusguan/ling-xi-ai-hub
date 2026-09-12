-- LingXi Companion V1.0.0 full database initialization script
-- Source: Flyway migrations V1_0_0_001 through V1_0_0_083
-- Database: MySQL 8.0+
-- Use only for initializing a new empty database. Existing databases must use Flyway migrations.
-- Business records use logical deletion: deleted=0 active, deleted=1 deleted.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET time_zone = '+00:00';

-- ============================================================================
-- V1_0_0_001__platform_base.sql
-- ============================================================================
CREATE TABLE plat_event_publication (
    id BIGINT NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    schema_version INT NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NOT NULL,
    lease_until DATETIME(3) NULL,
    last_error VARCHAR(1000) NULL,
    occurred_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plat_event_publication_event (event_id),
    KEY idx_plat_event_publication_dispatch (status, next_retry_at, lease_until, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='持久化领域事件发布记录';

CREATE TABLE plat_event_consumption (
    id BIGINT NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(128) NOT NULL,
    consumed_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plat_event_consumption (event_id, consumer_name),
    KEY idx_plat_event_consumption_time (consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='领域事件消费者幂等记录';

CREATE TABLE plat_async_job (
    id BIGINT NOT NULL,
    job_type VARCHAR(64) NOT NULL,
    business_key VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL,
    payload_json JSON NOT NULL,
    result_json JSON NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL,
    next_retry_at DATETIME(3) NOT NULL,
    lease_owner VARCHAR(128) NULL,
    lease_until DATETIME(3) NULL,
    progress INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plat_async_job_business (job_type, business_key),
    KEY idx_plat_async_job_claim (status, next_retry_at, lease_until, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='可恢复异步任务';


-- ============================================================================
-- V1_0_0_010__identity_base.sql
-- ============================================================================
CREATE TABLE id_user (
    id BIGINT NOT NULL,
    public_id VARCHAR(64) NOT NULL,
    registration_key VARCHAR(128) NOT NULL,
    registration_digest CHAR(64) NOT NULL,
    age_band VARCHAR(24) NOT NULL,
    status VARCHAR(32) NOT NULL,
    adult_transition_date DATE NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    authorization_version BIGINT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_id_user_public_id (public_id),
    UNIQUE KEY uk_id_user_registration_key (registration_key),
    KEY idx_id_user_age_transition (age_band, status, adult_transition_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='C端用户账号';

CREATE TABLE id_age_verification (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    method VARCHAR(32) NOT NULL,
    evidence_reference VARCHAR(255) NOT NULL,
    verified_birth_date DATE NOT NULL,
    result VARCHAR(24) NOT NULL,
    verified_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_id_age_verification_user (user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年龄验证结果';


-- ============================================================================
-- V1_0_0_012__identity_and_guardian_completion.sql
-- ============================================================================
CREATE TABLE id_login_identity (
    id BIGINT NOT NULL, user_id BIGINT NOT NULL, channel VARCHAR(32) NOT NULL,
    subject_hash CHAR(64) NOT NULL, verified_at DATETIME(3) NOT NULL, created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_id_login_identity_subject (channel, subject_hash),
    KEY idx_id_login_identity_user (user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已验证登录身份';

CREATE TABLE id_device (
    id BIGINT NOT NULL, user_id BIGINT NOT NULL, device_id VARCHAR(128) NOT NULL,
    first_seen_at DATETIME(3) NOT NULL, last_seen_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_id_device_owner (user_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户设备';

CREATE TABLE id_session (
    id BIGINT NOT NULL, family_id VARCHAR(64) NOT NULL, user_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL, access_token_hash CHAR(64) NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL, previous_refresh_token_hash CHAR(64) NULL,
    authorization_version BIGINT NOT NULL, access_expires_at DATETIME(3) NOT NULL,
    refresh_expires_at DATETIME(3) NOT NULL, revoked_at DATETIME(3) NULL,
    revoke_reason VARCHAR(128) NULL, version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_id_session_family (family_id),
    UNIQUE KEY uk_id_session_access (access_token_hash),
    KEY idx_id_session_user_active (user_id, revoked_at, refresh_expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Opaque Token会话';

CREATE TABLE id_user_consent (
    id BIGINT NOT NULL, user_id BIGINT NOT NULL, purpose VARCHAR(32) NOT NULL,
    document_version VARCHAR(64) NOT NULL, granted TINYINT(1) NOT NULL,
    evidence_reference VARCHAR(255) NOT NULL, recorded_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id), KEY idx_id_user_consent_history (user_id, purpose, recorded_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可覆盖协议证据';

CREATE TABLE id_privacy_request (
    id BIGINT NOT NULL, request_key VARCHAR(128) NOT NULL, request_digest CHAR(64) NOT NULL,
    user_id BIGINT NOT NULL, type VARCHAR(32) NOT NULL, scope_json JSON NOT NULL,
    status VARCHAR(24) NOT NULL, progress INT NOT NULL DEFAULT 0,
    deadline DATETIME(3) NOT NULL, result_reference VARCHAR(255) NULL,
    last_error VARCHAR(1000) NULL, version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_id_privacy_request_key (request_key),
    KEY idx_id_privacy_request_user (user_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='隐私权利请求';

CREATE TABLE id_privacy_export (
    request_id BIGINT NOT NULL, user_id BIGINT NOT NULL, nonce BINARY(12) NOT NULL,
    cipher_text LONGBLOB NOT NULL, expires_at DATETIME(3) NOT NULL, created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (request_id), KEY idx_id_privacy_export_expiry (user_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AES-GCM 加密隐私导出包';
CREATE TABLE rel_guardian_relation (
    id BIGINT NOT NULL, request_key VARCHAR(128) NOT NULL, request_digest CHAR(64) NOT NULL,
    teen_user_id BIGINT NOT NULL, guardian_user_id BIGINT NULL,
    invitation_token_hash CHAR(64) NOT NULL, requested_permissions_json JSON NOT NULL,
    status VARCHAR(24) NOT NULL, expires_at DATETIME(3) NOT NULL,
    verified_at DATETIME(3) NULL, revoked_at DATETIME(3) NULL, revoke_reason VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_rel_guardian_request (request_key),
    UNIQUE KEY uk_rel_guardian_token (invitation_token_hash),
    KEY idx_rel_guardian_teen_active (teen_user_id, status, id),
    KEY idx_rel_guardian_guardian (guardian_user_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监护关系';

CREATE TABLE rel_guardian_permission (
    id BIGINT NOT NULL, relation_id BIGINT NOT NULL, permission VARCHAR(64) NOT NULL,
    scope VARCHAR(32) NOT NULL, effective_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_rel_guardian_permission (relation_id, permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监护最小权限';


-- ============================================================================
-- V1_0_0_020__goal_base.sql
-- ============================================================================
CREATE TABLE goal_goal (
    id BIGINT NOT NULL,
    public_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    request_key VARCHAR(128) NOT NULL,
    request_digest CHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    success_criteria VARCHAR(1000) NOT NULL,
    status VARCHAR(24) NOT NULL,
    current_plan_version_id BIGINT NULL,
    progress INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_goal_public_id (public_id),
    UNIQUE KEY uk_goal_goal_request (request_key),
    KEY idx_goal_goal_owner (user_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目标聚合';

CREATE TABLE goal_plan_version (
    id BIGINT NOT NULL,
    goal_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    request_key VARCHAR(128) NOT NULL,
    request_digest CHAR(64) NOT NULL,
    snapshot_json JSON NOT NULL,
    adjustment_reason VARCHAR(500) NULL,
    activated_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_plan_version_no (goal_id, version_no),
    UNIQUE KEY uk_goal_plan_request (request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可变计划版本';

CREATE TABLE goal_milestone (
    id BIGINT NOT NULL, plan_version_id BIGINT NOT NULL, sequence_no INT NOT NULL,
    title VARCHAR(160) NOT NULL, success_criteria VARCHAR(1000) NOT NULL,
    created_at DATETIME(3) NOT NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_goal_milestone_sequence (plan_version_id, sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计划里程碑';

CREATE TABLE goal_action (
    id BIGINT NOT NULL, plan_version_id BIGINT NOT NULL, milestone_id BIGINT NULL,
    title VARCHAR(160) NOT NULL, repeat_rule VARCHAR(255) NULL, timezone VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL, PRIMARY KEY (id),
    KEY idx_goal_action_plan (plan_version_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行动定义';

CREATE TABLE goal_action_occurrence (
    id BIGINT NOT NULL, action_id BIGINT NOT NULL, scheduled_at DATETIME(3) NOT NULL,
    local_date DATE NOT NULL, timezone VARCHAR(64) NOT NULL, status VARCHAR(24) NOT NULL,
    created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_goal_occurrence_schedule (action_id, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行动发生实例';

CREATE TABLE goal_check_in (
    id BIGINT NOT NULL, occurrence_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
    request_key VARCHAR(128) NOT NULL, request_digest CHAR(64) NOT NULL,
    result VARCHAR(24) NOT NULL, note VARCHAR(1000) NULL, evidence_reference VARCHAR(255) NULL,
    effective TINYINT(1) NOT NULL DEFAULT 1, recorded_at DATETIME(3) NOT NULL, created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_goal_check_in_request (occurrence_id, request_key),
    UNIQUE KEY uk_goal_check_in_effective (occurrence_id, effective)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行动打卡事实';

CREATE TABLE goal_review (
    id BIGINT NOT NULL, goal_id BIGINT NOT NULL, period_key VARCHAR(64) NOT NULL,
    input_snapshot_json JSON NOT NULL, status VARCHAR(24) NOT NULL,
    conclusion_json JSON NULL, version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL, completed_at DATETIME(3) NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_goal_review_period (goal_id, period_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目标复盘';


-- ============================================================================
-- V1_0_0_021__goal_execution_completion.sql
-- ============================================================================
ALTER TABLE goal_plan_version
    ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' AFTER adjustment_reason,
    ADD COLUMN source VARCHAR(32) NOT NULL DEFAULT 'USER_CONFIRMED' AFTER status,
    ADD KEY idx_goal_plan_active (goal_id, status, version_no);

ALTER TABLE goal_action
    ADD COLUMN goal_id BIGINT NOT NULL AFTER id,
    ADD COLUMN client_key VARCHAR(64) NOT NULL AFTER milestone_id,
    ADD COLUMN recurrence_type VARCHAR(16) NOT NULL AFTER title,
    ADD COLUMN weekdays_json JSON NOT NULL AFTER recurrence_type,
    ADD COLUMN start_date DATE NOT NULL AFTER weekdays_json,
    ADD COLUMN end_date DATE NULL AFTER start_date,
    ADD COLUMN local_time TIME NOT NULL AFTER end_date,
    ADD UNIQUE KEY uk_goal_action_client (plan_version_id, client_key),
    ADD KEY idx_goal_action_goal (goal_id, status, id);

ALTER TABLE goal_check_in
    DROP INDEX uk_goal_check_in_effective,
    ADD COLUMN effective_key TINYINT NULL AFTER effective,
    ADD UNIQUE KEY uk_goal_check_in_effective (occurrence_id, effective_key);

ALTER TABLE goal_review
    ADD COLUMN completion_request_key VARCHAR(128) NULL AFTER conclusion_json,
    ADD COLUMN completion_request_digest CHAR(64) NULL AFTER completion_request_key,
    ADD UNIQUE KEY uk_goal_review_completion_request (completion_request_key);


-- ============================================================================
-- V1_0_0_030__engagement.sql
-- ============================================================================
CREATE TABLE eng_notification_rule (
 id VARCHAR(180) NOT NULL,user_id BIGINT NOT NULL,scene VARCHAR(64) NOT NULL,channels_json JSON NOT NULL,
 quiet_start TIME NULL,quiet_end TIME NULL,timezone VARCHAR(64) NOT NULL,version BIGINT NOT NULL DEFAULT 0,updated_at DATETIME(3) NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_eng_rule_user_scene(user_id,scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知偏好与规则';
CREATE TABLE eng_notification_task (
 id BIGINT NOT NULL,dedupe_key VARCHAR(180) NOT NULL,recipient_user_id BIGINT NOT NULL,channel VARCHAR(24) NOT NULL,
 scene VARCHAR(64) NOT NULL,resource_type VARCHAR(64) NULL,resource_id VARCHAR(128) NULL,payload_json JSON NOT NULL,
 scheduled_at DATETIME(3) NOT NULL,status VARCHAR(32) NOT NULL,attempt_count INT NOT NULL DEFAULT 0,last_error VARCHAR(1000) NULL,
 created_at DATETIME(3) NOT NULL,updated_at DATETIME(3) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_eng_task_dedupe(dedupe_key),
 KEY idx_eng_task_ready(status,scheduled_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知投递任务';
CREATE TABLE eng_notification_delivery (
 id BIGINT NOT NULL,task_id BIGINT NOT NULL,attempt_no INT NOT NULL,provider_message_id VARCHAR(180) NULL,result VARCHAR(32) NOT NULL,
 delivered_at DATETIME(3) NULL,error_code VARCHAR(128) NULL,created_at DATETIME(3) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_eng_delivery_attempt(task_id,attempt_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知渠道回执';
CREATE TABLE eng_inbox_message (
 id BIGINT NOT NULL,user_id BIGINT NOT NULL,type VARCHAR(64) NOT NULL,resource_type VARCHAR(64) NULL,resource_id VARCHAR(128) NULL,
 summary VARCHAR(500) NOT NULL,cursor_no BIGINT NOT NULL,read_at DATETIME(3) NULL,created_at DATETIME(3) NOT NULL,PRIMARY KEY(id),
 UNIQUE KEY uk_eng_inbox_cursor(user_id,cursor_no),KEY idx_eng_inbox_user(user_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨端站内消息';
CREATE TABLE eng_calendar_binding (
 id BIGINT NOT NULL,request_key VARCHAR(128) NOT NULL,user_id BIGINT NOT NULL,provider VARCHAR(32) NOT NULL,
 credential_reference VARCHAR(255) NOT NULL,status VARCHAR(24) NOT NULL,delete_created_events TINYINT(1) NOT NULL DEFAULT 0,
 version BIGINT NOT NULL DEFAULT 0,created_at DATETIME(3) NOT NULL,updated_at DATETIME(3) NOT NULL,PRIMARY KEY(id),
 UNIQUE KEY uk_eng_calendar_request(request_key),UNIQUE KEY uk_eng_calendar_user_provider(user_id,provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统日历授权';
CREATE TABLE eng_calendar_event_binding (
 id BIGINT NOT NULL,binding_id BIGINT NOT NULL,resource_type VARCHAR(64) NOT NULL,resource_id VARCHAR(128) NOT NULL,
 external_id VARCHAR(180) NOT NULL,external_version VARCHAR(128) NULL,sync_status VARCHAR(24) NOT NULL,updated_at DATETIME(3) NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_eng_calendar_resource(binding_id,resource_type,resource_id),UNIQUE KEY uk_eng_calendar_external(binding_id,external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='本地资源与外部日历映射';
CREATE TABLE eng_sync_cursor (
 id BIGINT NOT NULL,user_id BIGINT NOT NULL,sequence_no BIGINT NOT NULL,domain_name VARCHAR(64) NOT NULL,resource_type VARCHAR(64) NOT NULL,
 resource_id VARCHAR(128) NOT NULL,resource_version BIGINT NOT NULL,operation_type VARCHAR(24) NOT NULL,snapshot_json JSON NOT NULL,
 occurred_at DATETIME(3) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_eng_sync_sequence(user_id,sequence_no),KEY idx_eng_sync_query(user_id,sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨端增量变更';
CREATE TABLE eng_offline_command (
 id BIGINT NOT NULL,user_id BIGINT NOT NULL,device_id VARCHAR(128) NOT NULL,client_command_id VARCHAR(128) NOT NULL,
 command_type VARCHAR(64) NOT NULL,request_digest CHAR(64) NOT NULL,base_version BIGINT NOT NULL,status VARCHAR(24) NOT NULL,
 result_json JSON NULL,error_code VARCHAR(128) NULL,created_at DATETIME(3) NOT NULL,PRIMARY KEY(id),
 UNIQUE KEY uk_eng_offline_command(user_id,client_command_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='离线命令结果';


-- ============================================================================
-- V1_0_0_040__relationship_and_content.sql
-- ============================================================================
-- R06 伙伴关系、受控分享与内容资产
CREATE TABLE IF NOT EXISTS rel_partner_relation (
 id BIGINT PRIMARY KEY, request_key VARCHAR(128) NOT NULL, inviter_user_id BIGINT NOT NULL, invitee_user_id BIGINT NOT NULL,
 status VARCHAR(24) NOT NULL, version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_rel_partner_request(request_key), KEY idx_rel_partner_pair(inviter_user_id,invitee_user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rel_partner_grant (
 id BIGINT PRIMARY KEY, relation_id BIGINT NOT NULL, owner_user_id BIGINT NOT NULL, goal_id BIGINT NOT NULL, permissions_json JSON NOT NULL,
 expires_at DATETIME(6) NULL, status VARCHAR(24) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_rel_partner_grant(relation_id,goal_id), KEY idx_rel_grant_goal(goal_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rel_partner_interaction (
 id BIGINT PRIMARY KEY, relation_id BIGINT NOT NULL, grant_id BIGINT NOT NULL, actor_user_id BIGINT NOT NULL,
 interaction_type VARCHAR(24) NOT NULL, resource_id VARCHAR(64) NOT NULL, content_json JSON NULL, created_at DATETIME(6) NOT NULL,
 KEY idx_rel_interaction_relation(relation_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rel_share_link (
 id BIGINT PRIMARY KEY, request_key VARCHAR(128) NOT NULL, request_digest CHAR(64) NOT NULL, owner_user_id BIGINT NOT NULL,
 resource_type VARCHAR(32) NOT NULL, resource_id VARCHAR(64) NOT NULL, fields_json JSON NOT NULL, snapshot_json JSON NOT NULL,
 token_hash CHAR(64) NOT NULL, password_hash VARCHAR(512) NULL, expires_at DATETIME(6) NOT NULL,
 visit_limit INT NULL, visit_count INT NOT NULL DEFAULT 0, status VARCHAR(24) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_rel_share_request(request_key), UNIQUE KEY uk_rel_share_token(token_hash), KEY idx_rel_share_owner(owner_user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rel_block_relation (
 id BIGINT PRIMARY KEY, blocker_user_id BIGINT NOT NULL, blocked_user_id BIGINT NOT NULL, reason_code VARCHAR(32) NULL,
 created_at DATETIME(6) NOT NULL, UNIQUE KEY uk_rel_block(blocker_user_id,blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rel_report (
 id BIGINT PRIMARY KEY, reporter_user_id BIGINT NOT NULL, target_type VARCHAR(32) NOT NULL, target_id VARCHAR(64) NOT NULL,
 reason_code VARCHAR(32) NOT NULL, evidence_ref VARCHAR(255) NULL, status VARCHAR(24) NOT NULL, created_at DATETIME(6) NOT NULL,
 KEY idx_rel_report_target(target_type,target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS content_file (
 id BIGINT PRIMARY KEY, public_id VARCHAR(32) NOT NULL, request_key VARCHAR(128) NOT NULL, owner_user_id BIGINT NOT NULL,
 purpose VARCHAR(32) NOT NULL, original_name VARCHAR(255) NOT NULL, object_key VARCHAR(512) NOT NULL,
 content_hash VARCHAR(128) NOT NULL, size_bytes BIGINT NOT NULL, mime_type VARCHAR(128) NOT NULL,
 sensitivity VARCHAR(24) NOT NULL, status VARCHAR(24) NOT NULL, scan_result VARCHAR(512) NULL,
 version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_content_file_public(public_id), UNIQUE KEY uk_content_file_object(object_key),
 UNIQUE KEY uk_content_file_request(request_key), KEY idx_content_file_owner(owner_user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS content_file_reference (
 id BIGINT PRIMARY KEY, file_id BIGINT NOT NULL, owner_user_id BIGINT NOT NULL, resource_type VARCHAR(32) NOT NULL,
 resource_id VARCHAR(64) NOT NULL, created_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_content_file_ref(file_id,resource_type,resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS content_file_derivative (
 id BIGINT PRIMARY KEY, file_id BIGINT NOT NULL, derivative_type VARCHAR(32) NOT NULL, object_key VARCHAR(512) NOT NULL,
 status VARCHAR(24) NOT NULL, created_at DATETIME(6) NOT NULL, UNIQUE KEY uk_content_derivative(file_id,derivative_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS content_goal_template (
 id BIGINT PRIMARY KEY, template_key VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, status VARCHAR(24) NOT NULL,
 current_version INT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, UNIQUE KEY uk_content_template_key(template_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS content_template_version (
 id BIGINT PRIMARY KEY, template_id BIGINT NOT NULL, version_no INT NOT NULL, age_scope VARCHAR(24) NOT NULL,
 content_snapshot JSON NOT NULL, status VARCHAR(24) NOT NULL, reviewer_user_id BIGINT NULL, review_reason VARCHAR(512) NULL,
 published_at DATETIME(6) NULL, lock_version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_content_template_version(template_id,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS content_import_job (
 id BIGINT PRIMARY KEY, request_key VARCHAR(128) NOT NULL, user_id BIGINT NOT NULL, source_file_id BIGINT NOT NULL,
 format VARCHAR(24) NOT NULL, status VARCHAR(24) NOT NULL, preview_json JSON NULL, error_json JSON NULL,
 version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_content_import_request(request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS content_export_job (
 id BIGINT PRIMARY KEY, request_key VARCHAR(128) NOT NULL, user_id BIGINT NOT NULL, scope_json JSON NOT NULL,
 format VARCHAR(24) NOT NULL, status VARCHAR(24) NOT NULL, result_file_id BIGINT NULL, expires_at DATETIME(6) NULL,
 error_message VARCHAR(512) NULL, version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_content_export_request(request_key), KEY idx_content_export_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================================
-- V1_0_0_050__commerce_and_operations.sql
-- ============================================================================
-- R07 会员订单、权益与运营治理
CREATE TABLE IF NOT EXISTS pay_product (
 id BIGINT PRIMARY KEY, product_key VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, scene VARCHAR(64) NOT NULL DEFAULT 'ALL', billing_period VARCHAR(24) NOT NULL,
 age_policy VARCHAR(24) NOT NULL, entitlement_key VARCHAR(64) NOT NULL, entitlement_amount BIGINT NOT NULL,
 status VARCHAR(24) NOT NULL, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_pay_product_key(product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_price (
 id BIGINT PRIMARY KEY, product_id BIGINT NOT NULL, version_no INT NOT NULL, amount_minor BIGINT NOT NULL,
 currency CHAR(3) NOT NULL, status VARCHAR(24) NOT NULL, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_pay_price_version(product_id,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_order (
 id BIGINT PRIMARY KEY, order_no VARCHAR(64) NOT NULL, business_order_key VARCHAR(128) NOT NULL, user_id BIGINT NOT NULL,
 product_id BIGINT NOT NULL, price_id BIGINT NOT NULL, price_version INT NOT NULL, amount_minor BIGINT NOT NULL,
 currency CHAR(3) NOT NULL, channel VARCHAR(32) NOT NULL, status VARCHAR(32) NOT NULL, payment_reference VARCHAR(255) NULL,
 refunded_minor BIGINT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_order_no(order_no), UNIQUE KEY uk_pay_order_business(business_order_key), KEY idx_pay_order_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_order_item (
 id BIGINT PRIMARY KEY, order_id BIGINT NOT NULL, product_id BIGINT NOT NULL, price_id BIGINT NOT NULL,
 quantity INT NOT NULL, amount_minor BIGINT NOT NULL, snapshot_json JSON NOT NULL, UNIQUE KEY uk_pay_order_item(order_id,product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_transaction (
 id BIGINT PRIMARY KEY, order_id BIGINT NOT NULL, channel VARCHAR(32) NOT NULL, transaction_id VARCHAR(128) NOT NULL,
 amount_minor BIGINT NOT NULL, currency CHAR(3) NOT NULL, raw_digest CHAR(64) NOT NULL, verified_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_transaction(channel,transaction_id), KEY idx_pay_transaction_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_refund (
 id BIGINT PRIMARY KEY, order_id BIGINT NOT NULL, channel VARCHAR(32) NOT NULL, refund_transaction_id VARCHAR(128) NOT NULL,
 amount_minor BIGINT NOT NULL, reason VARCHAR(512) NOT NULL, status VARCHAR(24) NOT NULL, created_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_refund(channel,refund_transaction_id), KEY idx_pay_refund_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_subscription (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, channel VARCHAR(32) NOT NULL,
 channel_subscription_id VARCHAR(128) NOT NULL, status VARCHAR(32) NOT NULL, period_end DATETIME(6) NULL, cancel_mode VARCHAR(24) NULL,
 version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 last_reconciled_at DATETIME(6) NULL,
 UNIQUE KEY uk_pay_subscription_channel(channel,channel_subscription_id), KEY idx_pay_subscription_user(user_id,product_id,status),
 KEY idx_pay_subscription_reconcile(last_reconciled_at,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_entitlement (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, resource_key VARCHAR(64) NOT NULL, balance BIGINT NOT NULL,
 expires_at DATETIME(6) NULL, version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_entitlement(user_id,resource_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_entitlement_ledger (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, resource_key VARCHAR(64) NOT NULL, delta BIGINT NOT NULL,
 balance_after BIGINT NOT NULL, source_type VARCHAR(32) NOT NULL, source_id VARCHAR(128) NOT NULL,
 command_id VARCHAR(128) NOT NULL, created_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_ledger_command(command_id), KEY idx_pay_ledger_user(user_id,resource_key,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_channel_callback (
 id BIGINT PRIMARY KEY, channel VARCHAR(32) NOT NULL, callback_id VARCHAR(128) NOT NULL, digest CHAR(64) NOT NULL,
 status VARCHAR(24) NOT NULL, received_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_callback(channel,callback_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ops_support_ticket (
 id BIGINT PRIMARY KEY, ticket_no VARCHAR(64) NOT NULL, user_id BIGINT NOT NULL, category VARCHAR(32) NOT NULL,
 subject VARCHAR(200) NOT NULL, description TEXT NOT NULL, status VARCHAR(24) NOT NULL, priority VARCHAR(16) NOT NULL,
 assignee_admin_id BIGINT NULL, version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_ops_ticket_no(ticket_no), KEY idx_ops_ticket_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ops_feature_flag (
 id BIGINT PRIMARY KEY, flag_key VARCHAR(64) NOT NULL, current_release_id BIGINT NULL, mandatory_policy BOOLEAN NOT NULL DEFAULT FALSE,
 status VARCHAR(24) NOT NULL, updated_at DATETIME(6) NOT NULL, UNIQUE KEY uk_ops_flag_key(flag_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ops_experiment (
 id BIGINT PRIMARY KEY, experiment_key VARCHAR(64) NOT NULL, hypothesis VARCHAR(512) NOT NULL,
 audience_rule JSON NOT NULL, metrics_json JSON NOT NULL, status VARCHAR(24) NOT NULL, current_release_id BIGINT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, UNIQUE KEY uk_ops_experiment_key(experiment_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ops_config_release (
 id BIGINT PRIMARY KEY, release_key VARCHAR(128) NOT NULL, config_type VARCHAR(64) NOT NULL, version_no INT NOT NULL,
 content_ref VARCHAR(512) NOT NULL, content_digest CHAR(64) NOT NULL, gray_rule JSON NULL, status VARCHAR(32) NOT NULL,
 created_by BIGINT NOT NULL, approved_by BIGINT NULL, published_by BIGINT NULL, previous_release_id BIGINT NULL,
 failure_reason VARCHAR(512) NULL, lock_version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_ops_release_key(release_key), UNIQUE KEY uk_ops_release_version(config_type,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ops_audit_log (
 id BIGINT PRIMARY KEY, admin_id BIGINT NOT NULL, action VARCHAR(64) NOT NULL, object_type VARCHAR(64) NOT NULL,
 object_id VARCHAR(128) NOT NULL, reason VARCHAR(512) NOT NULL, ticket_no VARCHAR(64) NOT NULL,
 before_digest VARCHAR(128) NULL, after_digest VARCHAR(128) NULL, result VARCHAR(24) NOT NULL,
 request_id VARCHAR(128) NOT NULL, created_at DATETIME(6) NOT NULL, KEY idx_ops_audit_admin(admin_id,created_at),
 KEY idx_ops_audit_object(object_type,object_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================================
-- V1_0_0_060__companion_agent.sql
-- ============================================================================
-- R04 Companion Agent：对话、持久化运行、确认提案、事件、记忆和安全事实
CREATE TABLE IF NOT EXISTS ai_conversation (
 id BIGINT PRIMARY KEY, public_id VARCHAR(32) NOT NULL, request_key VARCHAR(128) NOT NULL,
 user_id BIGINT NOT NULL, scene VARCHAR(64) NOT NULL, title VARCHAR(200) NOT NULL,
 status VARCHAR(24) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_ai_conversation_public(public_id), UNIQUE KEY uk_ai_conversation_request(request_key),
 KEY idx_ai_conversation_user(user_id,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_message (
 id BIGINT PRIMARY KEY, conversation_id BIGINT NOT NULL, run_id BIGINT NULL, role VARCHAR(16) NOT NULL,
 content_text TEXT NOT NULL, content_digest CHAR(64) NOT NULL, ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
 created_at DATETIME(6) NOT NULL, KEY idx_ai_message_conversation(conversation_id,id),
 KEY idx_ai_message_run(run_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_agent_run (
 id BIGINT PRIMARY KEY, public_id VARCHAR(32) NOT NULL, request_key VARCHAR(128) NOT NULL,
 request_digest CHAR(64) NOT NULL, conversation_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 scene VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL, authorization_version BIGINT NOT NULL,
 model_version VARCHAR(64) NULL, prompt_version VARCHAR(64) NULL, result_text TEXT NULL,
 error_code VARCHAR(64) NULL, version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_ai_run_public(public_id), UNIQUE KEY uk_ai_run_request(request_key),
 KEY idx_ai_run_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_action_proposal (
 id BIGINT PRIMARY KEY, public_id VARCHAR(32) NOT NULL, run_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 tool_name VARCHAR(64) NOT NULL, risk_level VARCHAR(8) NOT NULL, arguments_json JSON NOT NULL,
 request_digest CHAR(64) NOT NULL, authorization_version BIGINT NOT NULL, expires_at DATETIME(6) NOT NULL,
 status VARCHAR(24) NOT NULL, decision VARCHAR(16) NULL, result_json JSON NULL,
 version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_ai_proposal_public(public_id), KEY idx_ai_proposal_run(run_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_run_event (
 id BIGINT PRIMARY KEY, run_id BIGINT NOT NULL, event_type VARCHAR(32) NOT NULL,
 safe_payload_json JSON NOT NULL, created_at DATETIME(6) NOT NULL, KEY idx_ai_event_run(run_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_memory (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, purpose VARCHAR(64) NOT NULL,
 content_text TEXT NOT NULL, source_ref VARCHAR(128) NOT NULL, sensitivity VARCHAR(24) NOT NULL,
 status VARCHAR(24) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 KEY idx_ai_memory_user(user_id,status,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_safety_event (
 id BIGINT PRIMARY KEY, run_id BIGINT NOT NULL, user_id BIGINT NOT NULL, risk_type VARCHAR(32) NOT NULL,
 risk_level VARCHAR(16) NOT NULL, disclosure_scope VARCHAR(32) NOT NULL, status VARCHAR(24) NOT NULL,
 created_at DATETIME(6) NOT NULL, handled_at DATETIME(6) NULL, KEY idx_ai_safety_run(run_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================================
-- V1_0_0_070__privacy_logical_deletion.sql
-- ============================================================================
-- 用户确认所有隐私删除统一采用业务不可恢复的逻辑删除。
ALTER TABLE id_user ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE id_age_verification ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE id_login_identity ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE id_device ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE id_session ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE id_user_consent ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE id_privacy_export ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE goal_goal ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE goal_plan_version ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE goal_milestone ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE goal_action ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE goal_action_occurrence ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE goal_check_in ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE goal_review ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE ai_conversation ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE ai_message ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE ai_agent_run ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE ai_action_proposal ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE ai_run_event ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE ai_memory ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE ai_safety_event ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE eng_notification_rule ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE eng_notification_task ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE eng_notification_delivery ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE eng_inbox_message ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE eng_calendar_binding ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE eng_calendar_event_binding ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE eng_sync_cursor ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE eng_offline_command ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE rel_guardian_relation ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rel_guardian_permission ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rel_partner_relation ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rel_partner_grant ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rel_partner_interaction ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rel_share_link ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rel_block_relation ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rel_report ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE content_file ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE content_file_reference ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE content_file_derivative ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE content_import_job ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE content_export_job ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE pay_order ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE pay_order_item ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE pay_transaction ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE pay_refund ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE pay_subscription ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE pay_entitlement ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE pay_entitlement_ledger ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE pay_channel_callback ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE ops_support_ticket ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE ops_audit_log ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;

CREATE TABLE id_privacy_deletion_audit (
    id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    module_name VARCHAR(32) NOT NULL,
    affected_rows INT NOT NULL,
    completed_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_id_privacy_deletion_audit_step (request_id, module_name),
    KEY idx_id_privacy_deletion_audit_user (user_id, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='隐私逻辑删除模块执行审计';


-- ============================================================================
-- V1_0_0_071__privacy_closure_and_active_uniques.sql
-- ============================================================================
-- 隐私人工工单与注销冷静期恢复所需的最小关联字段。
ALTER TABLE id_user
    ADD COLUMN closing_previous_status VARCHAR(32) NULL AFTER status;

ALTER TABLE ops_support_ticket
    ADD COLUMN privacy_request_id BIGINT NULL AFTER user_id,
    ADD KEY idx_ops_ticket_privacy_request (privacy_request_id);

-- MySQL 在普通联合唯一键中会保留已逻辑删除行；使用“仅活动行有值”的生成列，
-- 允许用户删除后重建同一自然关系，同时仍约束活动数据唯一。
ALTER TABLE eng_notification_rule
    DROP INDEX uk_eng_rule_user_scene,
    ADD COLUMN active_user_scene VARBINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN deleted = 0
          THEN UNHEX(SHA2(CONCAT(user_id, ':', scene), 256))
          ELSE NULL END
      ) STORED,
    ADD UNIQUE KEY uk_eng_rule_active_user_scene (active_user_scene);

ALTER TABLE eng_calendar_binding
    DROP INDEX uk_eng_calendar_user_provider,
    ADD COLUMN active_user_provider VARBINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN deleted = 0
          THEN UNHEX(SHA2(CONCAT(user_id, ':', provider), 256))
          ELSE NULL END
      ) STORED,
    ADD UNIQUE KEY uk_eng_calendar_active_user_provider (active_user_provider);

ALTER TABLE rel_block_relation
    DROP INDEX uk_rel_block,
    ADD COLUMN active_block_pair VARBINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN deleted = 0
          THEN UNHEX(SHA2(CONCAT(blocker_user_id, ':', blocked_user_id), 256))
          ELSE NULL END
      ) STORED,
    ADD UNIQUE KEY uk_rel_active_block (active_block_pair);

ALTER TABLE pay_entitlement
    DROP INDEX uk_pay_entitlement,
    ADD COLUMN active_user_resource VARBINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN deleted = 0
          THEN UNHEX(SHA2(CONCAT(user_id, ':', resource_key), 256))
          ELSE NULL END
      ) STORED,
    ADD UNIQUE KEY uk_pay_active_entitlement (active_user_resource);


-- ============================================================================
-- V1_0_0_080__admin_identity_and_governance.sql
-- ============================================================================
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


-- ============================================================================
-- V1_0_0_081__admin_business_closure.sql
-- ============================================================================
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


-- ============================================================================
-- V1_0_0_082__governance_runtime_safety_and_metrics.sql
-- ============================================================================
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

-- ============================================================================
-- V1_0_0_083__goal_achievement.sql
-- ============================================================================
CREATE TABLE goal_achievement (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    goal_id BIGINT NULL,
    achievement_type VARCHAR(32) NOT NULL,
    achievement_title VARCHAR(160) NOT NULL,
    achievement_description VARCHAR(512) NULL,
    reference_key VARCHAR(128) NOT NULL,
    achieved_at DATETIME(3) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_achievement_reference (reference_key),
    KEY idx_goal_achievement_owner (user_id, deleted, achieved_at, id),
    KEY idx_goal_achievement_goal (goal_id, achieved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户成就事实';

ALTER TABLE goal_check_in
    ADD KEY idx_goal_check_in_owner (user_id, effective_key, result, occurrence_id);
