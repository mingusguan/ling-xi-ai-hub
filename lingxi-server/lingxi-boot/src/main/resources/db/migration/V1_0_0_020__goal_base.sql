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
