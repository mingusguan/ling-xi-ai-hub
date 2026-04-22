-- 消息中心改造：重命名表并添加新字段
-- 执行时间：2026-04-22

-- 1. 重命名表
RENAME TABLE oa_smart_reminder TO sys_message;

-- 2. 重命名字段
ALTER TABLE sys_message CHANGE COLUMN reminder_id message_id BIGINT COMMENT '消息ID';
ALTER TABLE sys_message CHANGE COLUMN reminder_type message_type VARCHAR(50) COMMENT '消息类型（timeout:超时,approval:待审批,complete:流程完成,cc:抄送,notice:通知等）';

-- 3. 添加新字段
ALTER TABLE sys_message ADD COLUMN user_name VARCHAR(64) DEFAULT NULL COMMENT '用户名称' AFTER user_id;
ALTER TABLE sys_message ADD COLUMN source_type VARCHAR(50) DEFAULT 'oa' COMMENT '消息来源（oa:OA系统,system:系统通知,knowledge:知识库,ai:AI助手）' AFTER user_name;
ALTER TABLE sys_message ADD COLUMN process_instance_id VARCHAR(64) DEFAULT NULL COMMENT '流程实例ID' AFTER business_id;
ALTER TABLE sys_message ADD COLUMN task_id VARCHAR(64) DEFAULT NULL COMMENT '任务ID' AFTER process_instance_id;
ALTER TABLE sys_message ADD COLUMN channel VARCHAR(20) DEFAULT 'system' COMMENT '渠道（system:系统,wechat:微信,sms:短信）' AFTER priority;

-- 4. 调整字段顺序和注释
ALTER TABLE sys_message MODIFY COLUMN message_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID';
ALTER TABLE sys_message MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '用户ID';
ALTER TABLE sys_message MODIFY COLUMN source_type VARCHAR(50) DEFAULT 'oa' COMMENT '消息来源（oa:OA系统,system:系统通知,knowledge:知识库,ai:AI助手）';
ALTER TABLE sys_message MODIFY COLUMN message_type VARCHAR(50) DEFAULT NULL COMMENT '消息类型（timeout:超时,approval:待审批,complete:流程完成,cc:抄送,notice:通知等）';
ALTER TABLE sys_message MODIFY COLUMN title VARCHAR(200) NOT NULL COMMENT '消息标题';
ALTER TABLE sys_message MODIFY COLUMN content TEXT COMMENT '消息内容';
ALTER TABLE sys_message MODIFY COLUMN business_type VARCHAR(50) DEFAULT NULL COMMENT '业务类型';
ALTER TABLE sys_message MODIFY COLUMN business_id VARCHAR(64) DEFAULT NULL COMMENT '业务ID';
ALTER TABLE sys_message MODIFY COLUMN process_instance_id VARCHAR(64) DEFAULT NULL COMMENT '流程实例ID';
ALTER TABLE sys_message MODIFY COLUMN task_id VARCHAR(64) DEFAULT NULL COMMENT '任务ID';
ALTER TABLE sys_message MODIFY COLUMN priority INT DEFAULT 2 COMMENT '优先级（1:低,2:中,3:高）';
ALTER TABLE sys_message MODIFY COLUMN channel VARCHAR(20) DEFAULT 'system' COMMENT '渠道（system:系统,wechat:微信,sms:短信）';
ALTER TABLE sys_message MODIFY COLUMN status CHAR(1) DEFAULT '0' COMMENT '状态（0:未读,1:已读）';
ALTER TABLE sys_message MODIFY COLUMN send_time DATETIME DEFAULT NULL COMMENT '发送时间';
ALTER TABLE sys_message MODIFY COLUMN read_time DATETIME DEFAULT NULL COMMENT '阅读时间';

-- 5. 更新现有数据的 source_type 为 'oa'
UPDATE sys_message SET source_type = 'oa' WHERE source_type IS NULL OR source_type = '';

-- 6. 添加索引优化查询性能
CREATE INDEX idx_user_id ON sys_message(user_id);
CREATE INDEX idx_status ON sys_message(status);
CREATE INDEX idx_source_type ON sys_message(source_type);
CREATE INDEX idx_message_type ON sys_message(message_type);
CREATE INDEX idx_send_time ON sys_message(send_time DESC);
