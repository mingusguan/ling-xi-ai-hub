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
