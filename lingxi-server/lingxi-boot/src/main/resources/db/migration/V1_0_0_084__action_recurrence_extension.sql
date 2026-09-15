-- 行动重复规则扩展：支持「工作日」与「间隔重复」两种形式。
-- 背景：V1.0.0 原本只有 ONCE/DAILY/WEEKLY，PRD「行动与任务」要求支持
-- 按天、周、工作日、自定义星期、间隔重复五种形式；其中工作日与间隔重复缺少承载字段。
ALTER TABLE goal_action
    ADD COLUMN interval_days INT NULL AFTER weekdays_json;

-- recurrence_type 由 VARCHAR(16) 承载，WEEKDAYS / INTERVAL 均在长度以内，无需变更列定义。
-- 历史数据回填：既有行均为 ONCE/DAILY/WEEKLY，interval_days 保持 NULL 即可，
-- 领域层对非 INTERVAL 规则会强制丢弃该字段，避免出现两套事实。
