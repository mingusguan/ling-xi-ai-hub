-- 隐私人工工单与注销冷静期恢复所需的最小关联字段。
ALTER TABLE id_user
    ADD COLUMN closing_previous_status VARCHAR(32) NULL AFTER status;

ALTER TABLE ops_support_ticket
    ADD COLUMN privacy_request_id BIGINT NULL AFTER user_id,
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
      ) STORED,
    ADD UNIQUE KEY uk_eng_rule_active_user_scene (active_user_scene);

ALTER TABLE eng_calendar_binding
    DROP INDEX uk_eng_calendar_user_provider,
    ADD COLUMN active_user_provider VARBINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN deleted = 0
          THEN UNHEX(SHA2(CONCAT(user_id, ':', provider), 256))
          ELSE NULL END
      ) STORED,
    ADD UNIQUE KEY uk_eng_calendar_active_user_provider (active_user_provider);

ALTER TABLE rel_block_relation
    DROP INDEX uk_rel_block,
    ADD COLUMN active_block_pair VARBINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN deleted = 0
          THEN UNHEX(SHA2(CONCAT(blocker_user_id, ':', blocked_user_id), 256))
          ELSE NULL END
      ) STORED,
    ADD UNIQUE KEY uk_rel_active_block (active_block_pair);

ALTER TABLE pay_entitlement
    DROP INDEX uk_pay_entitlement,
    ADD COLUMN active_user_resource VARBINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN deleted = 0
          THEN UNHEX(SHA2(CONCAT(user_id, ':', resource_key), 256))
          ELSE NULL END
      ) STORED,
    ADD UNIQUE KEY uk_pay_active_entitlement (active_user_resource);
