package com.lingxi.ai.api.local;

import com.lingxi.ai.api.RemoteAiModelService;
import com.lingxi.ai.api.RemoteAiService;
import com.lingxi.ai.service.IAiModelService;
import com.lingxi.common.core.domain.R;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 单体模式下的本地 AI 模型服务调用。
 */
@Service
@RequiredArgsConstructor
public class LocalRemoteAiModelService implements RemoteAiModelService, RemoteAiService {

    private final IAiModelService aiModelService;

    @Override
    public R<String> generateSuggestion(Map<String, Object> context) {
        return analyzeAndSuggest(context);
    }

    @Override
    public R<String> analyzeRisk(Map<String, Object> formData) {
        return analyzeAndSuggest(formData);
    }

    @Override
    public R<String> generateTemplates(Map<String, Object> context) {
        return analyzeAndSuggest(context);
    }

    @Override
    public R<String> analyzeAndSuggest(Map<String, Object> context) {
        return R.ok(aiModelService.analyzeAndSuggest(context));
    }

    @Override
    public R<String> analyzeAndSuggest(Map<String, Object> context, String source) {
        return analyzeAndSuggest(context);
    }
}
