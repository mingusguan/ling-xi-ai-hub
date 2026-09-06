-- 灵犀伴行 V1.0.0 完整数据库初始化脚本
-- 来源：Flyway V1_0_0_001 至 V1_0_0_071
-- 数据库：MySQL 8.0+
-- 用途：仅用于全新空库一次性初始化；已有数据库请继续使用 Flyway 增量迁移，禁止直接重复执行本脚本。
-- 时间约定：DATETIME 字段由应用按 UTC 写入，展示时转换为用户时区。
-- 删除约定：业务数据统一逻辑删除，deleted=0 表示有效，deleted=1 表示已删除。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET time_zone = '+00:00';

-- =============================================================================
-- V1_0_0_001__platform_base.sql
-- =============================================================================
CREATE TABLE plat_event_publication (
    id BIGINT NOT NULL COMMENT '主键ID',
    event_id VARCHAR(64) NOT NULL COMMENT '领域事件唯一ID',
    event_type VARCHAR(128) NOT NULL COMMENT '运行事件类型',
    aggregate_id VARCHAR(64) NOT NULL COMMENT '聚合根ID',
    aggregate_version BIGINT NOT NULL COMMENT '聚合版本',
    schema_version INT NOT NULL COMMENT '事件数据结构版本',
    payload_json JSON NOT NULL COMMENT '业务载荷JSON',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '已尝试次数',
    next_retry_at DATETIME(3) NOT NULL COMMENT '下次重试时间（UTC）',
    lease_until DATETIME(3) NULL COMMENT '租约到期时间（UTC）',
    last_error VARCHAR(1000) NULL COMMENT '最后一次错误信息',
    occurred_at DATETIME(3) NOT NULL COMMENT '事件发生时间（UTC）',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    completed_at DATETIME(3) NULL COMMENT '完成时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_plat_event_publication_event (event_id),
    KEY idx_plat_event_publication_dispatch (status, next_retry_at, lease_until, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='持久化领域事件发布记录';

CREATE TABLE plat_event_consumption (
    id BIGINT NOT NULL COMMENT '主键ID',
    event_id VARCHAR(64) NOT NULL COMMENT '领域事件唯一ID',
    consumer_name VARCHAR(128) NOT NULL COMMENT '事件消费者名称',
    consumed_at DATETIME(3) NOT NULL COMMENT '消费完成时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_plat_event_consumption (event_id, consumer_name),
    KEY idx_plat_event_consumption_time (consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='领域事件消费者幂等记录';

CREATE TABLE plat_async_job (
    id BIGINT NOT NULL COMMENT '主键ID',
    job_type VARCHAR(64) NOT NULL COMMENT '异步任务类型',
    business_key VARCHAR(128) NOT NULL COMMENT '任务业务唯一键',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    payload_json JSON NOT NULL COMMENT '业务载荷JSON',
    result_json JSON NULL COMMENT '执行结果JSON',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '已尝试次数',
    max_attempts INT NOT NULL COMMENT '最大尝试次数',
    next_retry_at DATETIME(3) NOT NULL COMMENT '下次重试时间（UTC）',
    lease_owner VARCHAR(128) NULL COMMENT '租约持有者',
    lease_until DATETIME(3) NULL COMMENT '租约到期时间（UTC）',
    progress INT NOT NULL DEFAULT 0 COMMENT '完成进度百分比',
    last_error VARCHAR(1000) NULL COMMENT '最后一次错误信息',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    completed_at DATETIME(3) NULL COMMENT '完成时间（UTC）',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_plat_async_job_business (job_type, business_key),
    KEY idx_plat_async_job_claim (status, next_retry_at, lease_until, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='可恢复异步任务';

-- =============================================================================
-- V1_0_0_010__identity_base.sql
-- =============================================================================
CREATE TABLE id_user (
    id BIGINT NOT NULL COMMENT '主键ID',
    public_id VARCHAR(64) NOT NULL COMMENT '对外公开ID',
    registration_key VARCHAR(128) NOT NULL COMMENT '注册幂等键',
    registration_digest CHAR(64) NOT NULL COMMENT '注册请求摘要',
    age_band VARCHAR(24) NOT NULL COMMENT '年龄分段',
    status VARCHAR(32) NOT NULL COMMENT '业务状态',
    adult_transition_date DATE NOT NULL COMMENT '成年转换日期',
    timezone VARCHAR(64) NOT NULL COMMENT 'IANA时区',
    authorization_version BIGINT NOT NULL DEFAULT 1 COMMENT '授权版本',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_id_user_public_id (public_id),
    UNIQUE KEY uk_id_user_registration_key (registration_key),
    KEY idx_id_user_age_transition (age_band, status, adult_transition_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='C端用户账号';

CREATE TABLE id_age_verification (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    method VARCHAR(32) NOT NULL COMMENT '验证方式',
    evidence_reference VARCHAR(255) NOT NULL COMMENT '证据安全引用',
    verified_birth_date DATE NOT NULL COMMENT '核验出生日期',
    result VARCHAR(24) NOT NULL COMMENT '处理结果',
    verified_at DATETIME(3) NOT NULL COMMENT '验证时间（UTC）',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_id_age_verification_user (user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年龄验证结果';

-- =============================================================================
-- V1_0_0_012__identity_and_guardian_completion.sql
-- =============================================================================
CREATE TABLE id_login_identity (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    channel VARCHAR(32) NOT NULL COMMENT '渠道',
    subject_hash CHAR(64) NOT NULL COMMENT '登录主体不可逆摘要',
    verified_at DATETIME(3) NOT NULL COMMENT '验证时间（UTC）',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_id_login_identity_subject (channel, subject_hash),
    KEY idx_id_login_identity_user (user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已验证登录身份';

CREATE TABLE id_device (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id VARCHAR(128) NOT NULL COMMENT '客户端设备标识',
    first_seen_at DATETIME(3) NOT NULL COMMENT '设备首次出现时间（UTC）',
    last_seen_at DATETIME(3) NOT NULL COMMENT '设备最后出现时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_id_device_owner (user_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户设备';

CREATE TABLE id_session (
    id BIGINT NOT NULL COMMENT '主键ID',
    family_id VARCHAR(64) NOT NULL COMMENT '会话族ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id VARCHAR(128) NOT NULL COMMENT '客户端设备标识',
    access_token_hash CHAR(64) NOT NULL COMMENT '访问令牌摘要',
    refresh_token_hash CHAR(64) NOT NULL COMMENT '刷新令牌摘要',
    previous_refresh_token_hash CHAR(64) NULL COMMENT '上一个刷新令牌摘要',
    authorization_version BIGINT NOT NULL COMMENT '授权版本',
    access_expires_at DATETIME(3) NOT NULL COMMENT '访问令牌到期时间（UTC）',
    refresh_expires_at DATETIME(3) NOT NULL COMMENT '刷新令牌到期时间（UTC）',
    revoked_at DATETIME(3) NULL COMMENT '撤销时间（UTC）',
    revoke_reason VARCHAR(128) NULL COMMENT '撤销原因',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_id_session_family (family_id),
    UNIQUE KEY uk_id_session_access (access_token_hash),
    KEY idx_id_session_user_active (user_id, revoked_at, refresh_expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Opaque Token会话';

CREATE TABLE id_user_consent (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    purpose VARCHAR(32) NOT NULL COMMENT '数据处理目的',
    document_version VARCHAR(64) NOT NULL COMMENT '协议文档版本',
    granted TINYINT(1) NOT NULL COMMENT '是否授权',
    evidence_reference VARCHAR(255) NOT NULL COMMENT '证据安全引用',
    recorded_at DATETIME(3) NOT NULL COMMENT '记录时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_id_user_consent_history (user_id, purpose, recorded_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户协议授权证据';

CREATE TABLE id_privacy_request (
    id BIGINT NOT NULL COMMENT '主键ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    request_digest CHAR(64) NOT NULL COMMENT '请求内容摘要',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(32) NOT NULL COMMENT '业务类型',
    scope_json JSON NOT NULL COMMENT '处理范围JSON',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    progress INT NOT NULL DEFAULT 0 COMMENT '完成进度百分比',
    deadline DATETIME(3) NOT NULL COMMENT '处理期限（UTC）',
    result_reference VARCHAR(255) NULL COMMENT '结果安全引用',
    last_error VARCHAR(1000) NULL COMMENT '最后一次错误信息',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_id_privacy_request_key (request_key),
    KEY idx_id_privacy_request_user (user_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='隐私权利请求';

CREATE TABLE id_privacy_export (
    request_id BIGINT NOT NULL COMMENT '请求追踪ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    nonce BINARY(12) NOT NULL COMMENT 'AES-GCM随机数',
    cipher_text LONGBLOB NOT NULL COMMENT '加密数据密文',
    expires_at DATETIME(3) NOT NULL COMMENT '到期时间（UTC）',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    PRIMARY KEY (request_id),
    KEY idx_id_privacy_export_expiry (user_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='加密隐私导出包';
CREATE TABLE rel_guardian_relation (
    id BIGINT NOT NULL COMMENT '主键ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    request_digest CHAR(64) NOT NULL COMMENT '请求内容摘要',
    teen_user_id BIGINT NOT NULL COMMENT '未成年用户ID',
    guardian_user_id BIGINT NULL COMMENT '监护人用户ID',
    invitation_token_hash CHAR(64) NOT NULL COMMENT '邀请令牌摘要',
    requested_permissions_json JSON NOT NULL COMMENT '申请的监护权限JSON',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    expires_at DATETIME(3) NOT NULL COMMENT '到期时间（UTC）',
    verified_at DATETIME(3) NULL COMMENT '验证时间（UTC）',
    revoked_at DATETIME(3) NULL COMMENT '撤销时间（UTC）',
    revoke_reason VARCHAR(500) NULL COMMENT '撤销原因',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rel_guardian_request (request_key),
    UNIQUE KEY uk_rel_guardian_token (invitation_token_hash),
    KEY idx_rel_guardian_teen_active (teen_user_id, status, id),
    KEY idx_rel_guardian_guardian (guardian_user_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监护关系';

CREATE TABLE rel_guardian_permission (
    id BIGINT NOT NULL COMMENT '主键ID',
    relation_id BIGINT NOT NULL COMMENT '关系ID',
    permission VARCHAR(64) NOT NULL COMMENT '权限编码',
    scope VARCHAR(32) NOT NULL COMMENT '权限范围',
    effective_at DATETIME(3) NOT NULL COMMENT '生效时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rel_guardian_permission (relation_id, permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监护关系最小权限';

-- =============================================================================
-- V1_0_0_020__goal_base.sql
-- =============================================================================
CREATE TABLE goal_goal (
    id BIGINT NOT NULL COMMENT '主键ID',
    public_id VARCHAR(64) NOT NULL COMMENT '对外公开ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    request_digest CHAR(64) NOT NULL COMMENT '请求内容摘要',
    title VARCHAR(160) NOT NULL COMMENT '标题',
    success_criteria VARCHAR(1000) NOT NULL COMMENT '成功标准',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    current_plan_version_id BIGINT NULL COMMENT '当前生效计划版本ID',
    progress INT NOT NULL DEFAULT 0 COMMENT '完成进度百分比',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_goal_public_id (public_id),
    UNIQUE KEY uk_goal_goal_request (request_key),
    KEY idx_goal_goal_owner (user_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目标聚合';

CREATE TABLE goal_plan_version (
    id BIGINT NOT NULL COMMENT '主键ID',
    goal_id BIGINT NOT NULL COMMENT '目标ID',
    version_no INT NOT NULL COMMENT '业务版本序号',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    request_digest CHAR(64) NOT NULL COMMENT '请求内容摘要',
    snapshot_json JSON NOT NULL COMMENT '业务快照JSON',
    adjustment_reason VARCHAR(500) NULL COMMENT '计划调整原因',
    activated_at DATETIME(3) NULL COMMENT '激活时间（UTC）',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_plan_version_no (goal_id, version_no),
    UNIQUE KEY uk_goal_plan_request (request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可变计划版本';

CREATE TABLE goal_milestone (
    id BIGINT NOT NULL COMMENT '主键ID',
    plan_version_id BIGINT NOT NULL COMMENT '计划版本ID',
    sequence_no INT NOT NULL COMMENT '业务序号',
    title VARCHAR(160) NOT NULL COMMENT '标题',
    success_criteria VARCHAR(1000) NOT NULL COMMENT '成功标准',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_milestone_sequence (plan_version_id, sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计划里程碑';

CREATE TABLE goal_action (
    id BIGINT NOT NULL COMMENT '主键ID',
    plan_version_id BIGINT NOT NULL COMMENT '计划版本ID',
    milestone_id BIGINT NULL COMMENT '里程碑ID',
    title VARCHAR(160) NOT NULL COMMENT '标题',
    repeat_rule VARCHAR(255) NULL COMMENT '重复规则原文',
    timezone VARCHAR(64) NOT NULL COMMENT 'IANA时区',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_goal_action_plan (plan_version_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行动定义';

CREATE TABLE goal_action_occurrence (
    id BIGINT NOT NULL COMMENT '主键ID',
    action_id BIGINT NOT NULL COMMENT 'goal_action_occurrence表的action_id业务字段',
    scheduled_at DATETIME(3) NOT NULL COMMENT '计划执行时间（UTC）',
    local_date DATE NOT NULL COMMENT '用户本地日期',
    timezone VARCHAR(64) NOT NULL COMMENT 'IANA时区',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_occurrence_schedule (action_id, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行动发生实例';

CREATE TABLE goal_check_in (
    id BIGINT NOT NULL COMMENT '主键ID',
    occurrence_id BIGINT NOT NULL COMMENT '行动实例ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    request_digest CHAR(64) NOT NULL COMMENT '请求内容摘要',
    result VARCHAR(24) NOT NULL COMMENT '处理结果',
    note VARCHAR(1000) NULL COMMENT '用户备注',
    evidence_reference VARCHAR(255) NULL COMMENT '证据安全引用',
    effective TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否为有效打卡',
    recorded_at DATETIME(3) NOT NULL COMMENT '记录时间（UTC）',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_check_in_request (occurrence_id, request_key),
    UNIQUE KEY uk_goal_check_in_effective (occurrence_id, effective)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行动打卡事实';

CREATE TABLE goal_review (
    id BIGINT NOT NULL COMMENT '主键ID',
    goal_id BIGINT NOT NULL COMMENT '目标ID',
    period_key VARCHAR(64) NOT NULL COMMENT 'goal_review表的period_key业务字段',
    input_snapshot_json JSON NOT NULL COMMENT '复盘输入快照JSON',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    conclusion_json JSON NULL COMMENT '复盘结论JSON',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    completed_at DATETIME(3) NULL COMMENT '完成时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_review_period (goal_id, period_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目标复盘';

-- =============================================================================
-- V1_0_0_021__goal_execution_completion.sql
-- =============================================================================
ALTER TABLE goal_plan_version
    ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' COMMENT '业务状态' AFTER adjustment_reason,
    ADD COLUMN source VARCHAR(32) NOT NULL DEFAULT 'USER_CONFIRMED' COMMENT '数据来源' AFTER status,
    ADD KEY idx_goal_plan_active (goal_id, status, version_no);

ALTER TABLE goal_action
    ADD COLUMN goal_id BIGINT NOT NULL COMMENT '目标ID' AFTER id,
    ADD COLUMN client_key VARCHAR(64) NOT NULL COMMENT '客户端稳定键' AFTER milestone_id,
    ADD COLUMN recurrence_type VARCHAR(16) NOT NULL COMMENT '重复周期类型' AFTER title,
    ADD COLUMN weekdays_json JSON NOT NULL COMMENT '每周执行日JSON' AFTER recurrence_type,
    ADD COLUMN start_date DATE NOT NULL COMMENT '开始日期' AFTER weekdays_json,
    ADD COLUMN end_date DATE NULL COMMENT '结束日期' AFTER start_date,
    ADD COLUMN local_time TIME NOT NULL COMMENT '用户本地执行时间' AFTER end_date,
    ADD UNIQUE KEY uk_goal_action_client (plan_version_id, client_key),
    ADD KEY idx_goal_action_goal (goal_id, status, id);

ALTER TABLE goal_check_in
    DROP INDEX uk_goal_check_in_effective,
    ADD COLUMN effective_key TINYINT NULL COMMENT '有效打卡唯一约束辅助键' AFTER effective,
    ADD UNIQUE KEY uk_goal_check_in_effective (occurrence_id, effective_key);

ALTER TABLE goal_review
    ADD COLUMN completion_request_key VARCHAR(128) NULL COMMENT '完成复盘幂等键' AFTER conclusion_json,
    ADD COLUMN completion_request_digest CHAR(64) NULL COMMENT '完成复盘请求摘要' AFTER completion_request_key,
    ADD UNIQUE KEY uk_goal_review_completion_request (completion_request_key);

-- =============================================================================
-- V1_0_0_030__engagement.sql
-- =============================================================================
CREATE TABLE eng_notification_rule (
    id VARCHAR(180) NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    scene VARCHAR(64) NOT NULL COMMENT '业务场景',
    channels_json JSON NOT NULL COMMENT '启用渠道JSON',
    quiet_start TIME NULL COMMENT '免打扰开始时间',
    quiet_end TIME NULL COMMENT '免打扰结束时间',
    timezone VARCHAR(64) NOT NULL COMMENT 'IANA时区',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY(id),
    UNIQUE KEY uk_eng_rule_user_scene(user_id,scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知偏好与规则';
CREATE TABLE eng_notification_task (
    id BIGINT NOT NULL COMMENT '主键ID',
    dedupe_key VARCHAR(180) NOT NULL COMMENT '投递去重键',
    recipient_user_id BIGINT NOT NULL COMMENT '接收用户ID',
    channel VARCHAR(24) NOT NULL COMMENT '渠道',
    scene VARCHAR(64) NOT NULL COMMENT '业务场景',
    resource_type VARCHAR(64) NULL COMMENT '关联资源类型',
    resource_id VARCHAR(128) NULL COMMENT '关联资源ID',
    payload_json JSON NOT NULL COMMENT '业务载荷JSON',
    scheduled_at DATETIME(3) NOT NULL COMMENT '计划执行时间（UTC）',
    status VARCHAR(32) NOT NULL COMMENT '业务状态',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '已尝试次数',
    last_error VARCHAR(1000) NULL COMMENT '最后一次错误信息',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY(id),
    UNIQUE KEY uk_eng_task_dedupe(dedupe_key),
    KEY idx_eng_task_ready(status,scheduled_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知投递任务';
CREATE TABLE eng_notification_delivery (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '通知任务ID',
    attempt_no INT NOT NULL COMMENT '投递尝试序号',
    provider_message_id VARCHAR(180) NULL COMMENT '渠道消息ID',
    result VARCHAR(32) NOT NULL COMMENT '处理结果',
    delivered_at DATETIME(3) NULL COMMENT '送达时间（UTC）',
    error_code VARCHAR(128) NULL COMMENT '错误码',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    PRIMARY KEY(id),
    UNIQUE KEY uk_eng_delivery_attempt(task_id,attempt_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知渠道投递回执';
CREATE TABLE eng_inbox_message (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(64) NOT NULL COMMENT '业务类型',
    resource_type VARCHAR(64) NULL COMMENT '关联资源类型',
    resource_id VARCHAR(128) NULL COMMENT '关联资源ID',
    summary VARCHAR(500) NOT NULL COMMENT '消息摘要',
    cursor_no BIGINT NOT NULL COMMENT '用户消息递增游标',
    read_at DATETIME(3) NULL COMMENT '阅读时间（UTC）',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    PRIMARY KEY(id),
    UNIQUE KEY uk_eng_inbox_cursor(user_id,cursor_no),
    KEY idx_eng_inbox_user(user_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨端站内消息';
CREATE TABLE eng_calendar_binding (
    id BIGINT NOT NULL COMMENT '主键ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    provider VARCHAR(32) NOT NULL COMMENT '外部服务提供方',
    credential_reference VARCHAR(255) NOT NULL COMMENT '加密凭据安全引用',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    delete_created_events TINYINT(1) NOT NULL DEFAULT 0 COMMENT '解绑时是否删除已创建事件',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY(id),
    UNIQUE KEY uk_eng_calendar_request(request_key),
    UNIQUE KEY uk_eng_calendar_user_provider(user_id,provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外部日历授权绑定';
CREATE TABLE eng_calendar_event_binding (
    id BIGINT NOT NULL COMMENT '主键ID',
    binding_id BIGINT NOT NULL COMMENT '日历绑定ID',
    resource_type VARCHAR(64) NOT NULL COMMENT '关联资源类型',
    resource_id VARCHAR(128) NOT NULL COMMENT '关联资源ID',
    external_id VARCHAR(180) NOT NULL COMMENT '外部资源ID',
    external_version VARCHAR(128) NULL COMMENT '外部资源版本',
    sync_status VARCHAR(24) NOT NULL COMMENT '同步状态',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY(id),
    UNIQUE KEY uk_eng_calendar_resource(binding_id,resource_type,resource_id),
    UNIQUE KEY uk_eng_calendar_external(binding_id,external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='本地资源与外部日历映射';
CREATE TABLE eng_sync_cursor (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    sequence_no BIGINT NOT NULL COMMENT '业务序号',
    domain_name VARCHAR(64) NOT NULL COMMENT '领域模块名称',
    resource_type VARCHAR(64) NOT NULL COMMENT '关联资源类型',
    resource_id VARCHAR(128) NOT NULL COMMENT '关联资源ID',
    resource_version BIGINT NOT NULL COMMENT '资源版本',
    operation_type VARCHAR(24) NOT NULL COMMENT '变更操作类型',
    snapshot_json JSON NOT NULL COMMENT '业务快照JSON',
    occurred_at DATETIME(3) NOT NULL COMMENT '事件发生时间（UTC）',
    PRIMARY KEY(id),
    UNIQUE KEY uk_eng_sync_sequence(user_id,sequence_no),
    KEY idx_eng_sync_query(user_id,sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨端增量变更游标';
CREATE TABLE eng_offline_command (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id VARCHAR(128) NOT NULL COMMENT '客户端设备标识',
    client_command_id VARCHAR(128) NOT NULL COMMENT '客户端命令ID',
    command_type VARCHAR(64) NOT NULL COMMENT '离线命令类型',
    request_digest CHAR(64) NOT NULL COMMENT '请求内容摘要',
    base_version BIGINT NOT NULL COMMENT '客户端基准版本',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    result_json JSON NULL COMMENT '执行结果JSON',
    error_code VARCHAR(128) NULL COMMENT '错误码',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    PRIMARY KEY(id),
    UNIQUE KEY uk_eng_offline_command(user_id,client_command_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='离线命令执行结果';

-- =============================================================================
-- V1_0_0_040__relationship_and_content.sql
-- =============================================================================
-- R06 伙伴关系、受控分享与内容资产
CREATE TABLE IF NOT EXISTS rel_partner_relation (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    inviter_user_id BIGINT NOT NULL COMMENT '邀请方用户ID',
    invitee_user_id BIGINT NOT NULL COMMENT '受邀方用户ID',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_rel_partner_request(request_key),
    KEY idx_rel_partner_pair(inviter_user_id,invitee_user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同行伙伴关系';
CREATE TABLE IF NOT EXISTS rel_partner_grant (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    relation_id BIGINT NOT NULL COMMENT '关系ID',
    owner_user_id BIGINT NOT NULL COMMENT '资源所有者用户ID',
    goal_id BIGINT NOT NULL COMMENT '目标ID',
    permissions_json JSON NOT NULL COMMENT '授权权限JSON',
    expires_at DATETIME(6) NULL COMMENT '到期时间（UTC）',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_rel_partner_grant(relation_id,goal_id),
    KEY idx_rel_grant_goal(goal_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伙伴目标访问授权';
CREATE TABLE IF NOT EXISTS rel_partner_interaction (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    relation_id BIGINT NOT NULL COMMENT '关系ID',
    grant_id BIGINT NOT NULL COMMENT '伙伴授权ID',
    actor_user_id BIGINT NOT NULL COMMENT '操作用户ID',
    interaction_type VARCHAR(24) NOT NULL COMMENT '互动类型',
    resource_id VARCHAR(64) NOT NULL COMMENT '关联资源ID',
    content_json JSON NULL COMMENT '互动内容JSON',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    KEY idx_rel_interaction_relation(relation_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伙伴互动记录';
CREATE TABLE IF NOT EXISTS rel_share_link (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    request_digest CHAR(64) NOT NULL COMMENT '请求内容摘要',
    owner_user_id BIGINT NOT NULL COMMENT '资源所有者用户ID',
    resource_type VARCHAR(32) NOT NULL COMMENT '关联资源类型',
    resource_id VARCHAR(64) NOT NULL COMMENT '关联资源ID',
    fields_json JSON NOT NULL COMMENT '允许分享字段JSON',
    snapshot_json JSON NOT NULL COMMENT '业务快照JSON',
    token_hash CHAR(64) NOT NULL COMMENT '分享令牌摘要',
    password_hash VARCHAR(512) NULL COMMENT '分享口令摘要',
    expires_at DATETIME(6) NOT NULL COMMENT '到期时间（UTC）',
    visit_limit INT NULL COMMENT '允许访问次数',
    visit_count INT NOT NULL DEFAULT 0 COMMENT '已访问次数',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_rel_share_request(request_key),
    UNIQUE KEY uk_rel_share_token(token_hash),
    KEY idx_rel_share_owner(owner_user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='受控分享链接';
CREATE TABLE IF NOT EXISTS rel_block_relation (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    blocker_user_id BIGINT NOT NULL COMMENT '拉黑发起用户ID',
    blocked_user_id BIGINT NOT NULL COMMENT '被拉黑用户ID',
    reason_code VARCHAR(32) NULL COMMENT '原因编码',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    UNIQUE KEY uk_rel_block(blocker_user_id,blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户拉黑关系';
CREATE TABLE IF NOT EXISTS rel_report (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    reporter_user_id BIGINT NOT NULL COMMENT '举报用户ID',
    target_type VARCHAR(32) NOT NULL COMMENT '举报目标类型',
    target_id VARCHAR(64) NOT NULL COMMENT '举报目标ID',
    reason_code VARCHAR(32) NOT NULL COMMENT '原因编码',
    evidence_ref VARCHAR(255) NULL COMMENT '举报证据引用',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    KEY idx_rel_report_target(target_type,target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户举报记录';
CREATE TABLE IF NOT EXISTS content_file (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    public_id VARCHAR(32) NOT NULL COMMENT '对外公开ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    owner_user_id BIGINT NOT NULL COMMENT '资源所有者用户ID',
    purpose VARCHAR(32) NOT NULL COMMENT '数据处理目的',
    original_name VARCHAR(255) NOT NULL COMMENT '文件原始名称',
    object_key VARCHAR(512) NOT NULL COMMENT '对象存储键',
    content_hash VARCHAR(128) NOT NULL COMMENT '文件内容摘要',
    size_bytes BIGINT NOT NULL COMMENT '文件大小（字节）',
    mime_type VARCHAR(128) NOT NULL COMMENT 'MIME类型',
    sensitivity VARCHAR(24) NOT NULL COMMENT '敏感等级',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    scan_result VARCHAR(512) NULL COMMENT '安全扫描结果',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_content_file_public(public_id),
    UNIQUE KEY uk_content_file_object(object_key),
    UNIQUE KEY uk_content_file_request(request_key),
    KEY idx_content_file_owner(owner_user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容文件元数据';
CREATE TABLE IF NOT EXISTS content_file_reference (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    file_id BIGINT NOT NULL COMMENT '文件ID',
    owner_user_id BIGINT NOT NULL COMMENT '资源所有者用户ID',
    resource_type VARCHAR(32) NOT NULL COMMENT '关联资源类型',
    resource_id VARCHAR(64) NOT NULL COMMENT '关联资源ID',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    UNIQUE KEY uk_content_file_ref(file_id,resource_type,resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件业务引用';
CREATE TABLE IF NOT EXISTS content_file_derivative (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    file_id BIGINT NOT NULL COMMENT '文件ID',
    derivative_type VARCHAR(32) NOT NULL COMMENT '衍生资源类型',
    object_key VARCHAR(512) NOT NULL COMMENT '对象存储键',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    UNIQUE KEY uk_content_derivative(file_id,derivative_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件衍生资源';
CREATE TABLE IF NOT EXISTS content_goal_template (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    template_key VARCHAR(64) NOT NULL COMMENT '模板唯一键',
    name VARCHAR(128) NOT NULL COMMENT '名称',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    current_version INT NULL COMMENT '当前模板版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_content_template_key(template_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目标模板';
CREATE TABLE IF NOT EXISTS content_template_version (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    version_no INT NOT NULL COMMENT '业务版本序号',
    age_scope VARCHAR(24) NOT NULL COMMENT '适用年龄范围',
    content_snapshot JSON NOT NULL COMMENT '模板内容快照JSON',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    reviewer_user_id BIGINT NULL COMMENT '审核管理员ID',
    review_reason VARCHAR(512) NULL COMMENT '审核原因',
    published_at DATETIME(6) NULL COMMENT '发布时间（UTC）',
    lock_version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_content_template_version(template_id,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目标模板版本';
CREATE TABLE IF NOT EXISTS content_import_job (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    source_file_id BIGINT NOT NULL COMMENT '源文件ID',
    format VARCHAR(24) NOT NULL COMMENT '文件格式',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    preview_json JSON NULL COMMENT '导入预览JSON',
    error_json JSON NULL COMMENT '结构化错误JSON',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_content_import_request(request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容导入任务';
CREATE TABLE IF NOT EXISTS content_export_job (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    scope_json JSON NOT NULL COMMENT '处理范围JSON',
    format VARCHAR(24) NOT NULL COMMENT '文件格式',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    result_file_id BIGINT NULL COMMENT '结果文件ID',
    expires_at DATETIME(6) NULL COMMENT '到期时间（UTC）',
    error_message VARCHAR(512) NULL COMMENT '错误信息',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_content_export_request(request_key),
    KEY idx_content_export_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容导出任务';

-- =============================================================================
-- V1_0_0_050__commerce_and_operations.sql
-- =============================================================================
-- R07 会员订单、权益与运营治理
CREATE TABLE IF NOT EXISTS pay_product (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    product_key VARCHAR(64) NOT NULL COMMENT '商品唯一键',
    name VARCHAR(128) NOT NULL COMMENT '名称',
    scene VARCHAR(64) NOT NULL DEFAULT 'ALL' COMMENT '业务场景',
    billing_period VARCHAR(24) NOT NULL COMMENT '计费周期',
    age_policy VARCHAR(24) NOT NULL COMMENT '年龄适用策略',
    entitlement_key VARCHAR(64) NOT NULL COMMENT '权益编码',
    entitlement_amount BIGINT NOT NULL COMMENT '权益数量',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_pay_product_key(product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员商品';
CREATE TABLE IF NOT EXISTS pay_price (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    version_no INT NOT NULL COMMENT '业务版本序号',
    amount_minor BIGINT NOT NULL COMMENT '最小货币单位金额',
    currency CHAR(3) NOT NULL COMMENT 'ISO 4217货币代码',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间（UTC）',
    UNIQUE KEY uk_pay_price_version(product_id,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品价格版本';
CREATE TABLE IF NOT EXISTS pay_order (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    business_order_key VARCHAR(128) NOT NULL COMMENT '业务订单幂等键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    price_id BIGINT NOT NULL COMMENT '价格版本ID',
    price_version INT NOT NULL COMMENT '价格版本号',
    amount_minor BIGINT NOT NULL COMMENT '最小货币单位金额',
    currency CHAR(3) NOT NULL COMMENT 'ISO 4217货币代码',
    channel VARCHAR(32) NOT NULL COMMENT '渠道',
    status VARCHAR(32) NOT NULL COMMENT '业务状态',
    payment_reference VARCHAR(255) NULL COMMENT '支付安全引用',
    refunded_minor BIGINT NOT NULL DEFAULT 0 COMMENT '已退款金额（最小货币单位）',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_pay_order_no(order_no),
    UNIQUE KEY uk_pay_order_business(business_order_key),
    KEY idx_pay_order_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单';
CREATE TABLE IF NOT EXISTS pay_order_item (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    price_id BIGINT NOT NULL COMMENT '价格版本ID',
    quantity INT NOT NULL COMMENT '购买数量',
    amount_minor BIGINT NOT NULL COMMENT '最小货币单位金额',
    snapshot_json JSON NOT NULL COMMENT '业务快照JSON',
    UNIQUE KEY uk_pay_order_item(order_id,product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单明细';
CREATE TABLE IF NOT EXISTS pay_transaction (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    channel VARCHAR(32) NOT NULL COMMENT '渠道',
    transaction_id VARCHAR(128) NOT NULL COMMENT '渠道交易号',
    amount_minor BIGINT NOT NULL COMMENT '最小货币单位金额',
    currency CHAR(3) NOT NULL COMMENT 'ISO 4217货币代码',
    raw_digest CHAR(64) NOT NULL COMMENT '渠道原始报文摘要',
    verified_at DATETIME(6) NOT NULL COMMENT '验证时间（UTC）',
    UNIQUE KEY uk_pay_transaction(channel,transaction_id),
    KEY idx_pay_transaction_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付交易流水';
CREATE TABLE IF NOT EXISTS pay_refund (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    channel VARCHAR(32) NOT NULL COMMENT '渠道',
    refund_transaction_id VARCHAR(128) NOT NULL COMMENT '渠道退款交易号',
    amount_minor BIGINT NOT NULL COMMENT '最小货币单位金额',
    reason VARCHAR(512) NOT NULL COMMENT '业务原因',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    UNIQUE KEY uk_pay_refund(channel,refund_transaction_id),
    KEY idx_pay_refund_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款记录';
CREATE TABLE IF NOT EXISTS pay_subscription (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    channel VARCHAR(32) NOT NULL COMMENT '渠道',
    channel_subscription_id VARCHAR(128) NOT NULL COMMENT '渠道订阅ID',
    status VARCHAR(32) NOT NULL COMMENT '业务状态',
    period_end DATETIME(6) NULL COMMENT '当前订阅周期结束时间（UTC）',
    cancel_mode VARCHAR(24) NULL COMMENT '订阅取消方式',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    last_reconciled_at DATETIME(6) NULL COMMENT '最后对账时间（UTC）',
    UNIQUE KEY uk_pay_subscription_channel(channel,channel_subscription_id),
    KEY idx_pay_subscription_user(user_id,product_id,status),
    KEY idx_pay_subscription_reconcile(last_reconciled_at,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员订阅';
CREATE TABLE IF NOT EXISTS pay_entitlement (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resource_key VARCHAR(64) NOT NULL COMMENT '权益资源编码',
    balance BIGINT NOT NULL COMMENT '当前权益余额',
    expires_at DATETIME(6) NULL COMMENT '到期时间（UTC）',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_pay_entitlement(user_id,resource_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户权益余额';
CREATE TABLE IF NOT EXISTS pay_entitlement_ledger (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resource_key VARCHAR(64) NOT NULL COMMENT '权益资源编码',
    delta BIGINT NOT NULL COMMENT '权益变动数量',
    balance_after BIGINT NOT NULL COMMENT '变动后余额',
    source_type VARCHAR(32) NOT NULL COMMENT '权益来源类型',
    source_id VARCHAR(128) NOT NULL COMMENT '权益来源ID',
    command_id VARCHAR(128) NOT NULL COMMENT '权益命令幂等ID',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    UNIQUE KEY uk_pay_ledger_command(command_id),
    KEY idx_pay_ledger_user(user_id,resource_key,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户权益变动流水';
CREATE TABLE IF NOT EXISTS pay_channel_callback (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    channel VARCHAR(32) NOT NULL COMMENT '渠道',
    callback_id VARCHAR(128) NOT NULL COMMENT '渠道回调ID',
    digest CHAR(64) NOT NULL COMMENT '报文摘要',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    received_at DATETIME(6) NOT NULL COMMENT '回调接收时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_pay_callback(channel,callback_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付渠道回调记录';
CREATE TABLE IF NOT EXISTS ops_support_ticket (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    ticket_no VARCHAR(64) NOT NULL COMMENT '客服工单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category VARCHAR(32) NOT NULL COMMENT '工单分类',
    subject VARCHAR(200) NOT NULL COMMENT '工单主题',
    description TEXT NOT NULL COMMENT '工单描述',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    priority VARCHAR(16) NOT NULL COMMENT '优先级',
    assignee_admin_id BIGINT NULL COMMENT '受理管理员ID',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_ops_ticket_no(ticket_no),
    KEY idx_ops_ticket_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服工单';
CREATE TABLE IF NOT EXISTS ops_feature_flag (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    flag_key VARCHAR(64) NOT NULL COMMENT '功能开关键',
    current_release_id BIGINT NULL COMMENT '当前发布版本ID',
    mandatory_policy BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为强制策略',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_ops_flag_key(flag_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='功能开关';
CREATE TABLE IF NOT EXISTS ops_experiment (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    experiment_key VARCHAR(64) NOT NULL COMMENT '实验唯一键',
    hypothesis VARCHAR(512) NOT NULL COMMENT '实验假设',
    audience_rule JSON NOT NULL COMMENT '受众规则JSON',
    metrics_json JSON NOT NULL COMMENT '实验指标JSON',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    current_release_id BIGINT NULL COMMENT '当前发布版本ID',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_ops_experiment_key(experiment_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运营实验';
CREATE TABLE IF NOT EXISTS ops_config_release (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    release_key VARCHAR(128) NOT NULL COMMENT '配置发布唯一键',
    config_type VARCHAR(64) NOT NULL COMMENT '配置类型',
    version_no INT NOT NULL COMMENT '业务版本序号',
    content_ref VARCHAR(512) NOT NULL COMMENT '配置内容安全引用',
    content_digest CHAR(64) NOT NULL COMMENT '内容摘要',
    gray_rule JSON NULL COMMENT '灰度规则JSON',
    status VARCHAR(32) NOT NULL COMMENT '业务状态',
    created_by BIGINT NOT NULL COMMENT '创建管理员ID',
    approved_by BIGINT NULL COMMENT '审批管理员ID',
    published_by BIGINT NULL COMMENT '发布管理员ID',
    previous_release_id BIGINT NULL COMMENT '上一发布版本ID',
    failure_reason VARCHAR(512) NULL COMMENT '发布失败原因',
    lock_version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_ops_release_key(release_key),
    UNIQUE KEY uk_ops_release_version(config_type,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运营配置发布记录';
CREATE TABLE IF NOT EXISTS ops_audit_log (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    admin_id BIGINT NOT NULL COMMENT '管理员ID',
    action VARCHAR(64) NOT NULL COMMENT '管理操作',
    object_type VARCHAR(64) NOT NULL COMMENT '操作对象类型',
    object_id VARCHAR(128) NOT NULL COMMENT '操作对象ID',
    reason VARCHAR(512) NOT NULL COMMENT '业务原因',
    ticket_no VARCHAR(64) NOT NULL COMMENT '客服工单号',
    before_digest VARCHAR(128) NULL COMMENT '变更前摘要',
    after_digest VARCHAR(128) NULL COMMENT '变更后摘要',
    result VARCHAR(24) NOT NULL COMMENT '处理结果',
    request_id VARCHAR(128) NOT NULL COMMENT '请求追踪ID',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    KEY idx_ops_audit_admin(admin_id,created_at),
    KEY idx_ops_audit_object(object_type,object_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台管理审计日志';

-- =============================================================================
-- V1_0_0_060__companion_agent.sql
-- =============================================================================
-- R04 Companion Agent：对话、持久化运行、确认提案、事件、记忆和安全事实
CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    public_id VARCHAR(32) NOT NULL COMMENT '对外公开ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    scene VARCHAR(64) NOT NULL COMMENT '业务场景',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_ai_conversation_public(public_id),
    UNIQUE KEY uk_ai_conversation_request(request_key),
    KEY idx_ai_conversation_user(user_id,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话会话';
CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    conversation_id BIGINT NOT NULL COMMENT '对话会话ID',
    run_id BIGINT NULL COMMENT 'Agent运行ID',
    role VARCHAR(16) NOT NULL COMMENT '消息角色',
    content_text TEXT NOT NULL COMMENT '文本内容',
    content_digest CHAR(64) NOT NULL COMMENT '内容摘要',
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否由AI生成',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    KEY idx_ai_message_conversation(conversation_id,id),
    KEY idx_ai_message_run(run_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话消息';
CREATE TABLE IF NOT EXISTS ai_agent_run (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    public_id VARCHAR(32) NOT NULL COMMENT '对外公开ID',
    request_key VARCHAR(128) NOT NULL COMMENT '幂等请求键',
    request_digest CHAR(64) NOT NULL COMMENT '请求内容摘要',
    conversation_id BIGINT NOT NULL COMMENT '对话会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    scene VARCHAR(64) NOT NULL COMMENT '业务场景',
    status VARCHAR(32) NOT NULL COMMENT '业务状态',
    authorization_version BIGINT NOT NULL COMMENT '授权版本',
    model_version VARCHAR(64) NULL COMMENT '模型版本',
    prompt_version VARCHAR(64) NULL COMMENT '提示词版本',
    result_text TEXT NULL COMMENT 'Agent最终文本结果',
    error_code VARCHAR(64) NULL COMMENT '错误码',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_ai_run_public(public_id),
    UNIQUE KEY uk_ai_run_request(request_key),
    KEY idx_ai_run_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent运行记录';
CREATE TABLE IF NOT EXISTS ai_action_proposal (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    public_id VARCHAR(32) NOT NULL COMMENT '对外公开ID',
    run_id BIGINT NOT NULL COMMENT 'Agent运行ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    tool_name VARCHAR(64) NOT NULL COMMENT '工具名称',
    risk_level VARCHAR(8) NOT NULL COMMENT '风险等级',
    arguments_json JSON NOT NULL COMMENT '工具参数JSON',
    request_digest CHAR(64) NOT NULL COMMENT '请求内容摘要',
    authorization_version BIGINT NOT NULL COMMENT '授权版本',
    expires_at DATETIME(6) NOT NULL COMMENT '到期时间（UTC）',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    decision VARCHAR(16) NULL COMMENT '用户决策',
    result_json JSON NULL COMMENT '执行结果JSON',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    UNIQUE KEY uk_ai_proposal_public(public_id),
    KEY idx_ai_proposal_run(run_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent操作提案';
CREATE TABLE IF NOT EXISTS ai_run_event (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    run_id BIGINT NOT NULL COMMENT 'Agent运行ID',
    event_type VARCHAR(32) NOT NULL COMMENT '运行事件类型',
    safe_payload_json JSON NOT NULL COMMENT '脱敏事件载荷JSON',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    KEY idx_ai_event_run(run_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent运行事件';
CREATE TABLE IF NOT EXISTS ai_memory (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    purpose VARCHAR(64) NOT NULL COMMENT '数据处理目的',
    content_text TEXT NOT NULL COMMENT '文本内容',
    source_ref VARCHAR(128) NOT NULL COMMENT '数据来源引用',
    sensitivity VARCHAR(24) NOT NULL COMMENT '敏感等级',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间（UTC）',
    KEY idx_ai_memory_user(user_id,status,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户AI记忆';
CREATE TABLE IF NOT EXISTS ai_safety_event (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    run_id BIGINT NOT NULL COMMENT 'Agent运行ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    risk_type VARCHAR(32) NOT NULL COMMENT '安全风险类型',
    risk_level VARCHAR(16) NOT NULL COMMENT '风险等级',
    disclosure_scope VARCHAR(32) NOT NULL COMMENT '风险披露范围',
    status VARCHAR(24) NOT NULL COMMENT '业务状态',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间（UTC）',
    handled_at DATETIME(6) NULL COMMENT '处置完成时间（UTC）',
    KEY idx_ai_safety_run(run_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI安全事件';

-- =============================================================================
-- V1_0_0_070__privacy_logical_deletion.sql
-- =============================================================================
-- 用户确认所有隐私删除统一采用业务不可恢复的逻辑删除。
ALTER TABLE id_user ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE id_age_verification ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE id_login_identity ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE id_device ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE id_session ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE id_user_consent ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE id_privacy_export ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';

ALTER TABLE goal_goal ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE goal_plan_version ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE goal_milestone ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE goal_action ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE goal_action_occurrence ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE goal_check_in ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE goal_review ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';

ALTER TABLE ai_conversation ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE ai_message ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE ai_agent_run ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE ai_action_proposal ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE ai_run_event ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE ai_memory ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE ai_safety_event ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';

ALTER TABLE eng_notification_rule ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE eng_notification_task ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE eng_notification_delivery ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE eng_inbox_message ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE eng_calendar_binding ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE eng_calendar_event_binding ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE eng_sync_cursor ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE eng_offline_command ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';

ALTER TABLE rel_guardian_relation ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE rel_guardian_permission ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE rel_partner_relation ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE rel_partner_grant ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE rel_partner_interaction ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE rel_share_link ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE rel_block_relation ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE rel_report ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';

ALTER TABLE content_file ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE content_file_reference ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE content_file_derivative ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE content_import_job ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE content_export_job ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';

ALTER TABLE pay_order ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE pay_order_item ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE pay_transaction ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE pay_refund ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE pay_subscription ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE pay_entitlement ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE pay_entitlement_ledger ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE pay_channel_callback ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';

ALTER TABLE ops_support_ticket ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';
ALTER TABLE ops_audit_log ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除';

CREATE TABLE id_privacy_deletion_audit (
    id BIGINT NOT NULL COMMENT '主键ID',
    request_id BIGINT NOT NULL COMMENT '请求追踪ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    module_name VARCHAR(32) NOT NULL COMMENT '执行模块名称',
    affected_rows INT NOT NULL COMMENT '影响数据行数',
    completed_at DATETIME(3) NOT NULL COMMENT '完成时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_id_privacy_deletion_audit_step (request_id, module_name),
    KEY idx_id_privacy_deletion_audit_user (user_id, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='隐私逻辑删除模块执行审计';

-- =============================================================================
-- V1_0_0_071__privacy_closure_and_active_uniques.sql
-- =============================================================================
-- 隐私人工工单与注销冷静期恢复所需的最小关联字段。
ALTER TABLE id_user
    ADD COLUMN closing_previous_status VARCHAR(32) NULL COMMENT '账号注销前状态' AFTER status;

ALTER TABLE ops_support_ticket
    ADD COLUMN privacy_request_id BIGINT NULL COMMENT '关联隐私请求ID' AFTER user_id,
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
      ) STORED COMMENT '活动通知规则唯一键',
    ADD UNIQUE KEY uk_eng_rule_active_user_scene (active_user_scene);

ALTER TABLE eng_calendar_binding
    DROP INDEX uk_eng_calendar_user_provider,
    ADD COLUMN active_user_provider VARBINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN deleted = 0
          THEN UNHEX(SHA2(CONCAT(user_id, ':', provider), 256))
          ELSE NULL END
      ) STORED COMMENT '活动日历绑定唯一键',
    ADD UNIQUE KEY uk_eng_calendar_active_user_provider (active_user_provider);

ALTER TABLE rel_block_relation
    DROP INDEX uk_rel_block,
    ADD COLUMN active_block_pair VARBINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN deleted = 0
          THEN UNHEX(SHA2(CONCAT(blocker_user_id, ':', blocked_user_id), 256))
          ELSE NULL END
      ) STORED COMMENT '活动拉黑关系唯一键',
    ADD UNIQUE KEY uk_rel_active_block (active_block_pair);

ALTER TABLE pay_entitlement
    DROP INDEX uk_pay_entitlement,
    ADD COLUMN active_user_resource VARBINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN deleted = 0
          THEN UNHEX(SHA2(CONCAT(user_id, ':', resource_key), 256))
          ELSE NULL END
      ) STORED COMMENT '活动权益唯一键',
    ADD UNIQUE KEY uk_pay_active_entitlement (active_user_resource);
