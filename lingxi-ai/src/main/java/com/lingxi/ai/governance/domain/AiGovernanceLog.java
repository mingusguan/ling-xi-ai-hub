package com.lingxi.ai.governance.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * AI治理审计日志，记录模型、智能体和知识问答等企业AI使用行为。
 */
@Data
@TableName("ai_governance_log")
public class AiGovernanceLog {

    /** 日志主键。 */
    @TableId(type = IdType.AUTO)
    private Long logId;

    /** 场景类型，如 CHAT、AI_TOOL、KNOWLEDGE_QA、AGENT。 */
    private String sceneType;

    /** 场景名称，如 xiaolinger、document_write。 */
    private String sceneName;

    /** 请求摘要，避免保存完整敏感上下文。 */
    private String requestSummary;

    /** 响应摘要，避免保存完整模型输出。 */
    private String responseSummary;

    /** 调用状态：SUCCESS 或 FAILURE。 */
    private String status;

    /** 调用耗时，单位毫秒。 */
    private Long costMillis;

    /** 估算Token数量。 */
    private Integer tokenCount;

    /** 风险等级：LOW、MEDIUM、HIGH。 */
    private String riskLevel;

    /** 是否命中敏感内容：1 是，0 否。 */
    private String sensitiveHit;

    /** 失败原因。 */
    private String errorMessage;

    /** 调用用户ID。 */
    private Long userId;

    /** 调用用户账号。 */
    private String username;

    /** 调用用户部门ID。 */
    private Long deptId;

    /** 租户ID。 */
    private String tenantId;

    /** 创建时间。 */
    private Date createTime;
}
