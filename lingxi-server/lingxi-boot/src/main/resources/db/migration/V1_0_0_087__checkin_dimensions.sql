-- 打卡维度补齐。
--
-- 字段来源：PRD「打卡与记录」要求记录实际耗时、主观难度、精力、情绪自评与附件，
-- 结果除完成/部分完成/跳过外还需要「失败」，失败必须记录原因供复盘定位阻塞。
--
-- 情绪自评只是一张五档中性量表，仅用于趋势观察，不产出诊断结论；
-- 风险识别由安全链路单独负责，两者不共用信号。
ALTER TABLE goal_check_in
    ADD COLUMN actual_minutes INT NULL AFTER evidence_reference,
    ADD COLUMN perceived_difficulty VARCHAR(16) NULL AFTER actual_minutes,
    ADD COLUMN energy_level VARCHAR(16) NULL AFTER perceived_difficulty,
    ADD COLUMN mood_level VARCHAR(16) NULL AFTER energy_level,
    ADD COLUMN failure_reason VARCHAR(500) NULL AFTER mood_level;

-- result 由 VARCHAR(24) 承载，FAILED 在长度以内，无需变更列定义。
-- 历史行均为完成/部分完成/跳过，新列保持 NULL 即可；领域层会把 NULL 视为「未填写」。
