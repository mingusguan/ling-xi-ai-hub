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
