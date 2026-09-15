-- 行动定义细节与单次调整例外。
--
-- 字段来源：PRD「行动与任务」要求行动具备说明、优先级、难度、完成标准、预计时长、
-- 时间段、前置行动与行动级提醒策略；这些字段不参与「行动在哪天发生」的判定，
-- 但决定今日工作台排序、打卡对比基线与复盘的偏差分析。
ALTER TABLE goal_action
    ADD COLUMN description VARCHAR(1000) NULL AFTER title,
    ADD COLUMN priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL' AFTER description,
    ADD COLUMN difficulty VARCHAR(16) NOT NULL DEFAULT 'NORMAL' AFTER priority,
    ADD COLUMN completion_criteria VARCHAR(1000) NULL AFTER difficulty,
    ADD COLUMN estimated_minutes INT NULL AFTER completion_criteria,
    ADD COLUMN end_local_time TIME NULL AFTER local_time,
    ADD COLUMN prerequisite_action_id BIGINT NULL AFTER end_local_time,
    ADD COLUMN reminder_policy VARCHAR(255) NULL AFTER prerequisite_action_id;

-- 历史数据没有难度列，统一按「一般」归类，与领域层 ActionDetail 的默认口径一致。
UPDATE goal_action SET difficulty = 'NORMAL' WHERE difficulty IS NULL OR difficulty = '';
UPDATE goal_action SET priority = 'NORMAL' WHERE priority IS NULL OR priority = '';

-- 单次调整例外：PRD 要求「单次修改」与「修改未来全部」分离。
-- 单次调整不改写行动定义，只记录按「行动 + 原日期」唯一的一条例外，
-- 由实例生成器展开重复规则时叠加，因此一次临时改期不会污染整个重复规则。
CREATE TABLE goal_action_exception (
    id BIGINT NOT NULL,
    action_id BIGINT NOT NULL,
    local_date DATE NOT NULL,
    exception_type VARCHAR(16) NOT NULL,
    rescheduled_date DATE NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    -- 同一行动的同一天只能有一条例外；唯一键同时承担按行动查询例外的访问路径。
    UNIQUE KEY uk_goal_action_exception_slot (action_id, local_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行动单次调整例外';
