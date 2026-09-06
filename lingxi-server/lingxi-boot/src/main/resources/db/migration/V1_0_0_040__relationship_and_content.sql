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