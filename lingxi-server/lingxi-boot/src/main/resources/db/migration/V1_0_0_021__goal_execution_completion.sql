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
