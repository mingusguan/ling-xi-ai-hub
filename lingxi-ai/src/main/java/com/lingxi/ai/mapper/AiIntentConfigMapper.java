package com.lingxi.ai.mapper;

import com.lingxi.ai.domain.AiIntentConfig;

import java.util.List;

/**
 * AI意图配置Mapper
 *
 * @author lingxi
 */
public interface AiIntentConfigMapper {

    /**
     * 查询启用的意图配置列表
     */
    List<AiIntentConfig> selectEnabledList();
}
