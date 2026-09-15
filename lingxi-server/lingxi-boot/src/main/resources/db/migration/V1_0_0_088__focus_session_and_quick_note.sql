-- 今日工作台所需的专注会话与快速记录。
--
-- 背景：PRD「今日工作台」要求提供专注模式（开始计时、暂停、结束、结果记录）与快速记录入口。
-- 两者都不属于目标/计划/打卡任何既有聚合：专注会话是「一次执行过程」，快速记录是
-- 「不绑定行动的随手记录」，硬塞进打卡会让打卡事实变得含混，因此各建一张表。
--
-- 逾期不引入新状态：逾期由查询时按「未执行且计划日期早于今天」判定。
-- 若把逾期写成 MISSED 状态，用户想把昨天没做的挪到今天时会被状态守卫挡住，
-- 反而违背了「逾期仍可处理」的产品预期。

CREATE TABLE goal_focus_session (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    occurrence_id BIGINT NULL,
    action_id BIGINT NULL,
    status VARCHAR(16) NOT NULL,
    planned_minutes INT NULL,
    -- 累计专注秒数；暂停期间不累加，恢复时从 last_resumed_at 重新起算。
    accumulated_seconds INT NOT NULL DEFAULT 0,
    last_resumed_at DATETIME(3) NULL,
    started_at DATETIME(3) NOT NULL,
    ended_at DATETIME(3) NULL,
    note VARCHAR(1000) NULL,
    -- 进行中标记：RUNNING/PAUSED 时为 1，结束时置 NULL；
    -- 借助唯一键里 NULL 不参与冲突的语义，保证同一用户同时只有一个进行中的会话。
    active_key TINYINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_focus_active (user_id, active_key),
    KEY idx_goal_focus_owner (user_id, deleted, started_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专注会话';

CREATE TABLE goal_quick_note (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    -- 情绪自评复用打卡的五档中性量表；仅用于趋势观察，不产出诊断结论。
    mood_level VARCHAR(16) NULL,
    local_date DATE NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_goal_quick_note_owner (user_id, deleted, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='快速记录';

-- 逾期查询按「状态 + 计划时刻」扫描，补一个覆盖该访问路径的索引。
ALTER TABLE goal_action_occurrence
    ADD KEY idx_goal_occurrence_due (status, scheduled_at);
