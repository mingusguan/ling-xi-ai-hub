-- 新手引导首目标引导（PRD 8.2 ONB-02）所需的入门模板目录与首行动约束。
--
-- 背景：ONB-02 要求首目标支持「文字、语音、模板」三种入口，并且创建成功后直接生成一个
-- 5～30 分钟的首行动，以降低启动门槛。
--
-- 为什么新开一张 goal_starter_template 而不是复用 content_goal_template：
-- 后者是运营治理资产，模板必须经后台创建、提交、审核（reviewer_admin_id 非空）才能发布，
-- 而入门模板是产品自带的引导数据，由版本迁移随代码一起交付，没有也不应有审核人。
-- 把两者混在一张表里，要么给迁移伪造一条审核记录，要么让治理链在入门模板上开洞。
--
-- 首行动之所以要单独建模（而不是复用普通行动字段）：
-- 「首个行动」在业务上是一次性的启动门槛，只在引导阶段出现一次，因此用 goal_clarification
-- 记录当前进度、用 goal_action.is_first 标记这次引导生成的首行动，两者都不影响正式计划。

CREATE TABLE goal_starter_template (
    id BIGINT NOT NULL,
    -- 稳定业务键，客户端引用模板时使用；不使用自增主键，保证跨环境一致。
    template_key VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    summary VARCHAR(255) NULL,
    goal_type VARCHAR(32) NOT NULL,
    default_success_criteria VARCHAR(255) NULL,
    -- 分类标签，用于按类别推荐；JSON 数组，可为空。
    tags_json VARCHAR(255) NULL,
    -- 首行动候选：JSON 数组，每项含 title / estimatedMinutes / priority /
    -- difficulty / completionCriteria。estimatedMinutes 必须落在 5—30 之间，
    -- 迁移作为权威来源，服务端写入时由领域层再次校验。
    first_actions_json JSON NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_starter_template_key (template_key),
    KEY idx_goal_starter_template_list (enabled, deleted, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='新手引导入门目标模板';

-- 引导澄清阶段（PRD ONB-02「AI 每次最多提出一个关键澄清问题」）。
-- AWAITING_GOAL     等待用户用自己的话说想达成什么；
-- AWAITING_CRITERIA 已拿到目标，等待用户确认怎样算达成；
-- AWAITING_FIRST_ACTION 已确认达成标准，等待生成/确认首行动；
-- COMPLETE          引导阶段结束，后续走目标详情页的正式计划编排。
ALTER TABLE goal_goal
    ADD COLUMN clarification_stage VARCHAR(32) NULL
        COMMENT 'ONB-02 引导阶段；NULL 表示不处于首目标引导中';

-- 首行动标记：同一次引导生成的首行动。用 client_key 记录（而不是主键），
-- 便于在目标创建事务内先按客户端键定位，不依赖自增标识回填。
ALTER TABLE goal_action
    ADD COLUMN is_first TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为新手引导生成的首行动';

-- 内置入门模板：覆盖 PRD 十类目标中最常见的六类，每类给一个可在 5—30 分钟内完成的起步动作。
INSERT INTO goal_starter_template
    (id, template_key, name, summary, goal_type, default_success_criteria, tags_json, first_actions_json, display_order, enabled, deleted, created_at, updated_at)
VALUES
    (9000000000000000001, 'starter-reading-habit', '每天读一点书', '把「想读书」变成每天固定的一小段阅读时间。', 'READING',
     '连续四周每周至少完成 5 次阅读',
     '["阅读","习惯"]',
     '[{"title":"读 10 分钟手边这本纸质书","estimatedMinutes":10,"priority":"NORMAL","difficulty":"EASY","completionCriteria":"读完 10 分钟并合上书"},{"title":"读 15 分钟并划出一句印象最深的话","estimatedMinutes":15,"priority":"NORMAL","difficulty":"EASY","completionCriteria":"读完并记下那一句话"}]',
     10, 1, 0, NOW(3), NOW(3)),

    (9000000000000000002, 'starter-fitness-routine', '把作息和运动固定下来', '先固定一个最小可执行的运动与起床时间。', 'FITNESS',
     '连续两周每周至少 3 次运动',
     '["运动","作息"]',
     '[{"title":"做 10 分钟拉伸或快走","estimatedMinutes":10,"priority":"HIGH","difficulty":"EASY","completionCriteria":"完成 10 分钟并确认身体无不适"},{"title":"做 20 分钟自重训练","estimatedMinutes":20,"priority":"NORMAL","difficulty":"NORMAL","completionCriteria":"完成一组训练动作"}]',
     20, 1, 0, NOW(3), NOW(3)),

    (9000000000000000003, 'starter-exam-prep', '开始准备一场考试', '先把复习范围收窄到一个可执行的最小单元。', 'LEARNING_EXAM',
     '按计划完成全部复习单元',
     '["学习","考试"]',
     '[{"title":"整理这科今天要复习的 1 个知识点","estimatedMinutes":25,"priority":"HIGH","difficulty":"NORMAL","completionCriteria":"写出一页知识要点"},{"title":"做完 5 道历年真题并订正","estimatedMinutes":30,"priority":"HIGH","difficulty":"HARD","completionCriteria":"5 道题都写出错因"}]',
     30, 1, 0, NOW(3), NOW(3)),

    (9000000000000000004, 'starter-creative-output', '开始持续输出作品', '用一小步把「想写/想做」变成已完成的第一件作品。', 'CREATIVE',
     '完成 4 件可展示的作品',
     '["创作","输出"]',
     '[{"title":"写下这篇内容的 3 个要点提纲","estimatedMinutes":15,"priority":"NORMAL","difficulty":"EASY","completionCriteria":"列出 3 条要点"},{"title":"写 300 字初稿，不求好只求有","estimatedMinutes":30,"priority":"NORMAL","difficulty":"NORMAL","completionCriteria":"产出 300 字连续文本"}]',
     40, 1, 0, NOW(3), NOW(3)),

    (9000000000000000005, 'starter-career-skill', '推进一项职业技能', '把一个模糊的「提升自己」拆到今天能做完的动作。', 'CAREER',
     '完成一个可交付的学习成果',
     '["职业","技能"]',
     '[{"title":"看 15 分钟官方文档并记下 3 个要点","estimatedMinutes":15,"priority":"NORMAL","difficulty":"EASY","completionCriteria":"写下 3 个要点"},{"title":"动手跑通一个 20 分钟的最小示例","estimatedMinutes":20,"priority":"HIGH","difficulty":"NORMAL","completionCriteria":"示例能运行并看到结果"}]',
     50, 1, 0, NOW(3), NOW(3)),

    (9000000000000000006, 'starter-emotion-record', '观察自己的情绪节奏', '用最短的动作开始记录情绪，不做任何评判。', 'EMOTION',
     '连续记录三周情绪并完成一次复盘',
     '["情绪","自我观察"]',
     '[{"title":"写下今天最强烈的一次情绪和它的触发点","estimatedMinutes":5,"priority":"NORMAL","difficulty":"EASY","completionCriteria":"写出一句话描述情绪与触发点"},{"title":"记录今天的精力与情绪自评","estimatedMinutes":5,"priority":"LOW","difficulty":"EASY","completionCriteria":"完成两条自评"}]',
     60, 1, 0, NOW(3), NOW(3));
