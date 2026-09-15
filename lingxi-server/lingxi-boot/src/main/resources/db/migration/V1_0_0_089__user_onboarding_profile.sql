-- 新手引导基础画像（PRD 8.2 ONB-01）。
--
-- 背景：ONB-01 要求用户可配置称呼、作息与每周可用时间、提醒时段与免打扰时段、
-- AI 沟通风格、主动程度、常见阻塞原因。当前 id_user 只有账号状态机字段（年龄档、
-- 账号状态、时区、授权版本），没有任何画像字段，引导流程无处落库。
--
-- 为什么不建独立画像表：
-- 画像与账号是 1:1、无独立生命周期、无独立并发场景，且所有读取路径都已经加载账号行。
-- 单开一张表只会给每次读取增加一次 join，不带来任何隔离收益。
--
-- 为什么全部可空且不设默认值：
-- PRD 明确要求 ONB-01 可跳过，并且禁止强制收集真实姓名、身份证、职业单位、精确位置
-- 等非必需信息。因此「用户没填」必须以 NULL 落库，不能写默认值——否则服务端将无法
-- 区分「用户明确选了温和」和「用户从没选过，服务端按简洁兜底」。默认值只在读取时兜底。

ALTER TABLE id_user
    -- 用户自定义称呼（昵称），不做实名校验，长度上限由领域层校验。
    ADD COLUMN nickname VARCHAR(64) NULL COMMENT '用户自定义称呼（昵称，非实名）',
    ADD COLUMN remind_window_start TIME NULL COMMENT '偏好提醒时段开始，本地时间',
    ADD COLUMN remind_window_end TIME NULL COMMENT '偏好提醒时段结束，本地时间',
    -- 免打扰时段允许跨天（例如 23:00-07:00），因此不校验结束晚于开始。
    ADD COLUMN quiet_hours_start TIME NULL COMMENT '免打扰时段开始，本地时间，允许跨天',
    ADD COLUMN quiet_hours_end TIME NULL COMMENT '免打扰时段结束，本地时间，允许跨天',
    ADD COLUMN communication_style VARCHAR(16) NULL COMMENT 'AI 沟通风格 CONCISE/GENTLE/DIRECT/COACHING',
    ADD COLUMN proactivity_level VARCHAR(16) NULL COMMENT 'AI 主动程度 LOW/MEDIUM/HIGH',
    -- 作息：睡觉与起床时刻，用于判断「今天还来不来得及」以及避免在不合适的时间打扰。
    ADD COLUMN sleep_time TIME NULL COMMENT '通常入睡时刻，本地时间',
    ADD COLUMN wake_time TIME NULL COMMENT '通常起床时刻，本地时间',
    -- 每周可用时间：画像级别的整体预算，目标级别的同名字段是单个目标的预算。
    ADD COLUMN weekly_available_minutes INT NULL COMMENT '每周可用时间（分钟），可跳过',
    -- 常见阻塞原因以 JSON 数组存储；空集合与「未填写」用 NULL 区分。
    ADD COLUMN common_blockers_json VARCHAR(255) NULL COMMENT '常见阻塞原因 JSON 数组，可为空',
    -- 引导完成时间：NULL 表示还没走完引导。用时间戳而不是布尔值，
    -- 这样「已跳过」（时间戳有值、画像为空）与「从未进入引导」可以区分开。
    ADD COLUMN onboarding_completed_at DATETIME(3) NULL COMMENT '新手引导完成时间，NULL 表示未完成';
