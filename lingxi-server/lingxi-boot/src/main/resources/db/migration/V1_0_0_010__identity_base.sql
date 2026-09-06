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
