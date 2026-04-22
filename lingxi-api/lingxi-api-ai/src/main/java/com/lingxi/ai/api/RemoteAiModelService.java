package com.lingxi.ai.api;

import com.lingxi.ai.api.factory.RemoteAiModelFallbackFactory;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.constant.ServiceNameConstants;
import com.lingxi.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * AI模型服务远程调用接口
 *
 * @author lingxi
 */
@FeignClient(contextId = "remoteAiModelService", value = ServiceNameConstants.AI_SERVICE,
             fallbackFactory = RemoteAiModelFallbackFactory.class)
public interface RemoteAiModelService {

    /**
     * 一次性生成完整审批分析
     *
     * @param context 审批上下文
     * @param source 请求来源
     * @return JSON格式的完整分析结果
     */
    @PostMapping("/ai/model/analyze")
    R<String> analyzeAndSuggest(@RequestBody Map<String, Object> context,
                                @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
