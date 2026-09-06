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
