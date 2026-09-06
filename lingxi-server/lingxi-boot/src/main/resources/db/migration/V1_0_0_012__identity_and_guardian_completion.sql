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
