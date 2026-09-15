-- 目标定义字段与生命周期：补齐 PRD「目标管理」要求的目标定义字段，并支持暂停/恢复/放弃/归档。
--
-- 字段来源：PRD「目标名称、描述与完成标准；目标类型；开始日期与期望完成日期；优先级；
-- 每周可用时间；资源和限制条件；可验证成果；隐私级别，默认仅自己」。
-- 目标类型取值与 PRD「支持的目标类型」十类一一对应；优先级为低/常规/高三档；
-- 隐私级别只有「仅自己」与「允许对伙伴可见」两种，后者仍以按目标授权为前置。
ALTER TABLE goal_goal
    ADD COLUMN description VARCHAR(1000) NULL AFTER title,
    ADD COLUMN goal_type VARCHAR(32) NOT NULL DEFAULT 'HABIT' AFTER description,
    ADD COLUMN start_date DATE NULL AFTER success_criteria,
    ADD COLUMN target_end_date DATE NULL AFTER start_date,
    ADD COLUMN priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL' AFTER target_end_date,
    ADD COLUMN weekly_available_minutes INT NULL AFTER priority,
    ADD COLUMN resource_constraints VARCHAR(1000) NULL AFTER weekly_available_minutes,
    ADD COLUMN verifiable_outcomes VARCHAR(1000) NULL AFTER resource_constraints,
    ADD COLUMN privacy_level VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' AFTER verifiable_outcomes,
    ADD COLUMN pause_resume_at DATE NULL AFTER privacy_level,
    ADD COLUMN abandon_reason VARCHAR(500) NULL AFTER pause_resume_at;

-- 历史行没有目标类型，按「习惯养成」归类，与领域层 GoalDefinition.of 的兜底口径保持一致。
UPDATE goal_goal SET goal_type = 'HABIT' WHERE goal_type IS NULL OR goal_type = '';

-- 目标列表按用户与状态过滤，并需要统计活跃目标数以校验配额。
-- 既有 idx_goal_goal_owner(user_id, status, id) 已能覆盖该访问路径，无需新增索引。
