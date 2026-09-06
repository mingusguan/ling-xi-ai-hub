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
