-- 免打扰顺延所需的字段，并把遗留表名改为与领域一致。
--
-- 背景一（缺陷修复）：原来命中免打扰时段时，通知任务被直接置为 CANCELLED。
-- 产品语义应当是「换个时间再发」而不是「不发了」：用户把行动设在 22:00、
-- 免打扰设为 22:30–07:00 时，那条提醒应当顺延到 07:00。新增 deferred_until
-- 记录被推迟到的时刻，供后台区分「策略拒绝」与「延后发送」。
--
-- 背景二（遗留表清理）：eng_notification_rule 存的是「用户通知偏好」
-- （渠道、免打扰时段、时区），实体类一直叫 NotificationPreferenceEntity。
-- 表名与实体名不一致，是 R05 早期命名的遗留；该表仅被这一个实体引用，
-- 没有外键也没有视图，因此直接重命名，而不是保留一个名不副实的旧表。

ALTER TABLE eng_notification_task
    ADD COLUMN deferred_until DATETIME(3) NULL
        COMMENT '因免打扰被推迟到的时刻；NULL 表示未被推迟';

RENAME TABLE eng_notification_rule TO eng_notification_preference;
