-- Enterprise AI features: knowledge operations, agent orchestration, and AI governance.

CREATE TABLE IF NOT EXISTS `ai_governance_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '治理日志ID',
  `scene_type` varchar(40) NOT NULL DEFAULT 'UNKNOWN' COMMENT '场景类型',
  `scene_name` varchar(100) DEFAULT NULL COMMENT '场景名称',
  `request_summary` varchar(500) DEFAULT NULL COMMENT '请求摘要',
  `response_summary` varchar(500) DEFAULT NULL COMMENT '响应摘要',
  `status` varchar(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '调用状态',
  `cost_millis` bigint DEFAULT 0 COMMENT '调用耗时毫秒',
  `token_count` int DEFAULT 0 COMMENT '估算Token数',
  `risk_level` varchar(20) DEFAULT 'LOW' COMMENT '风险等级',
  `sensitive_hit` char(1) DEFAULT '0' COMMENT '是否命中敏感内容',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(64) DEFAULT NULL COMMENT '用户名',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_ai_governance_time` (`create_time`),
  KEY `idx_ai_governance_scene` (`scene_type`, `scene_name`),
  KEY `idx_ai_governance_risk` (`risk_level`, `sensitive_hit`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI治理审计日志';

CREATE TABLE IF NOT EXISTS `ai_agent_definition` (
  `agent_id` bigint NOT NULL AUTO_INCREMENT COMMENT '智能体ID',
  `agent_code` varchar(80) NOT NULL COMMENT '智能体编码',
  `agent_name` varchar(100) NOT NULL COMMENT '智能体名称',
  `business_scene` varchar(80) DEFAULT NULL COMMENT '业务场景',
  `description` varchar(500) DEFAULT NULL COMMENT '职责说明',
  `system_prompt` text COMMENT '系统提示词',
  `guardrails` text COMMENT '安全与输出边界',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `owner_team` varchar(100) DEFAULT NULL COMMENT '归属团队',
  `audit_enabled` char(1) NOT NULL DEFAULT '1' COMMENT '是否开启审计',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`agent_id`),
  UNIQUE KEY `uk_ai_agent_code` (`agent_code`),
  KEY `idx_ai_agent_status` (`status`),
  KEY `idx_ai_agent_scene` (`business_scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI智能体定义';

CREATE TABLE IF NOT EXISTS `ai_agent_step` (
  `step_id` bigint NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
  `agent_code` varchar(80) NOT NULL COMMENT '智能体编码',
  `step_order` int NOT NULL DEFAULT 1 COMMENT '步骤顺序',
  `step_name` varchar(100) NOT NULL COMMENT '步骤名称',
  `step_type` varchar(30) NOT NULL COMMENT '步骤类型',
  `tool_name` varchar(120) DEFAULT NULL COMMENT '工具名称',
  `instruction` text COMMENT '步骤指令',
  `config_json` text COMMENT '步骤配置',
  `failure_policy` varchar(30) NOT NULL DEFAULT 'STOP' COMMENT '失败策略',
  `enabled` char(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`step_id`),
  KEY `idx_ai_agent_step_code_order` (`agent_code`, `step_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI智能体编排步骤';

CREATE TABLE IF NOT EXISTS `kb_qa_session` (
  `session_id` bigint NOT NULL AUTO_INCREMENT COMMENT '问答会话ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `title` varchar(120) NOT NULL COMMENT '会话标题',
  `message_count` int NOT NULL DEFAULT 0 COMMENT '消息数量',
  `last_message_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后消息时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`session_id`),
  KEY `idx_kb_qa_session_user` (`user_id`, `last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库问答会话';

CREATE TABLE IF NOT EXISTS `kb_qa_conversation` (
  `conversation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '问答记录ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `question` text NOT NULL COMMENT '用户问题',
  `answer` text COMMENT 'AI答案',
  `source_chunks` longtext COMMENT '引用片段JSON',
  `top_score` decimal(10,4) DEFAULT 0 COMMENT '最高命中分',
  `confidence_level` varchar(20) DEFAULT 'LOW' COMMENT '置信度等级',
  `no_answer` char(1) NOT NULL DEFAULT '0' COMMENT '是否无答案',
  `feedback` varchar(20) DEFAULT NULL COMMENT '反馈',
  `feedback_remark` varchar(500) DEFAULT NULL COMMENT '反馈备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`conversation_id`),
  KEY `idx_kb_qa_conversation_session` (`session_id`, `create_time`),
  KEY `idx_kb_qa_conversation_operation` (`create_time`, `no_answer`, `feedback`, `confidence_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库问答明细';

-- Root directories are inserted only when the subsystem menu has not been initialized.
INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `route_name`, `is_frame`, `is_cache`, `menu_type`,
 `visible`, `status`, `perms`, `icon`, `sys_code`, `remark`, `create_by`, `create_time`)
SELECT '知识库', 0, 20, 'knowledge', 'Layout', 'Knowledge', '1', '0', 'M',
       '0', '0', '', 'documentation', 'knowledge', '知识库子系统', 'system', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'knowledge' AND `sys_code` = 'knowledge'
);

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `route_name`, `is_frame`, `is_cache`, `menu_type`,
 `visible`, `status`, `perms`, `icon`, `sys_code`, `remark`, `create_by`, `create_time`)
SELECT 'AI工具', 0, 30, 'ai', 'Layout', 'Ai', '1', '0', 'M',
       '0', '0', '', 'magic-stick', 'ai_tool', 'AI工具子系统', 'system', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'ai' AND `sys_code` = 'ai_tool'
);

SET @knowledge_parent_id := (
  SELECT `menu_id` FROM `sys_menu`
  WHERE `sys_code` = 'knowledge' AND `parent_id` = 0
  ORDER BY `menu_id` ASC LIMIT 1
);

SET @ai_parent_id := (
  SELECT `menu_id` FROM `sys_menu`
  WHERE `sys_code` = 'ai_tool' AND `parent_id` = 0
  ORDER BY `menu_id` ASC LIMIT 1
);

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `route_name`, `is_frame`, `is_cache`, `menu_type`,
 `visible`, `status`, `perms`, `icon`, `sys_code`, `remark`, `create_by`, `create_time`)
SELECT '知识问答', @knowledge_parent_id, 30, 'qa', 'knowledge/qa/index', 'KnowledgeQa', '1', '0', 'C',
       '0', '0', 'knowledge:qa:chat', 'message', 'knowledge', '知识库问答入口', 'system', NOW()
WHERE @knowledge_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `path` = 'qa' AND `sys_code` = 'knowledge'
  );

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `route_name`, `is_frame`, `is_cache`, `menu_type`,
 `visible`, `status`, `perms`, `icon`, `sys_code`, `remark`, `create_by`, `create_time`)
SELECT '知识运营中心', @knowledge_parent_id, 40, 'operation', 'knowledge/operation/index', 'KnowledgeOperation',
       '1', '0', 'C', '0', '0', 'knowledge:operation:view', 'chart', 'knowledge', '知识问答运营分析',
       'system', NOW()
WHERE @knowledge_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `path` = 'operation' AND `sys_code` = 'knowledge'
  );

SET @knowledge_operation_id := (
  SELECT `menu_id` FROM `sys_menu`
  WHERE `path` = 'operation' AND `sys_code` = 'knowledge'
  ORDER BY `menu_id` ASC LIMIT 1
);

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `route_name`, `is_frame`, `is_cache`, `menu_type`,
 `visible`, `status`, `perms`, `icon`, `sys_code`, `remark`, `create_by`, `create_time`)
SELECT '问答反馈', @knowledge_operation_id, 1, '#', '', NULL, '1', '0', 'F',
       '0', '0', 'knowledge:operation:feedback', '#', 'knowledge', '知识问答反馈权限', 'system', NOW()
WHERE @knowledge_operation_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `perms` = 'knowledge:operation:feedback'
  );

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `route_name`, `is_frame`, `is_cache`, `menu_type`,
 `visible`, `status`, `perms`, `icon`, `sys_code`, `remark`, `create_by`, `create_time`)
SELECT 'Agent编排中心', @ai_parent_id, 40, 'agent', 'ai/agent/index', 'AiAgent', '1', '0', 'C',
       '0', '0', 'ai:agent:list', 'tree', 'ai_tool', '企业Agent编排配置', 'system', NOW()
WHERE @ai_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `path` = 'agent' AND `sys_code` = 'ai_tool'
  );

SET @ai_agent_id := (
  SELECT `menu_id` FROM `sys_menu`
  WHERE `path` = 'agent' AND `sys_code` = 'ai_tool'
  ORDER BY `menu_id` ASC LIMIT 1
);

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `route_name`, `is_frame`, `is_cache`, `menu_type`,
 `visible`, `status`, `perms`, `icon`, `sys_code`, `remark`, `create_by`, `create_time`)
SELECT 'Agent维护', @ai_agent_id, 1, '#', '', NULL, '1', '0', 'F',
       '0', '0', 'ai:agent:edit', '#', 'ai_tool', '企业Agent新增编辑发布权限', 'system', NOW()
WHERE @ai_agent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `perms` = 'ai:agent:edit'
  );

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `route_name`, `is_frame`, `is_cache`, `menu_type`,
 `visible`, `status`, `perms`, `icon`, `sys_code`, `remark`, `create_by`, `create_time`)
SELECT 'AI治理审计', @ai_parent_id, 50, 'governance', 'ai/governance/index', 'AiGovernance', '1', '0', 'C',
       '0', '0', 'ai:governance:view', 'monitor', 'ai_tool', 'AI调用治理审计', 'system', NOW()
WHERE @ai_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `path` = 'governance' AND `sys_code` = 'ai_tool'
  );
