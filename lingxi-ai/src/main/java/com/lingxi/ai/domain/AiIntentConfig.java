package com.lingxi.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI意图配置实体
 *
 * @author lingxi
 */
@Data
@TableName("ai_intent_config")
public class AiIntentConfig {

    /** 配置ID */
    @TableId(type = IdType.AUTO)
    private Long configId;

    /** 意图代码 */
    private String intentCode;

    /** 意图名称 */
    private String intentName;

    /** 示例问题（JSON数组） */
    private String sampleQuestions;

    /** 是否启用（0否 1是） */
    private String isEnabled;

    /** 排序 */
    private Integer sortOrder;
}
