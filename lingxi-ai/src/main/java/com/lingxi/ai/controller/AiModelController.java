package com.lingxi.ai.controller;

import com.lingxi.ai.service.IAiModelService;
import com.lingxi.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI模型控制器（供Feign调用）
 *
 * @author lingxi
 */
@RestController
@RequestMapping("/ai/model")
public class AiModelController {

    @Autowired
    private IAiModelService aiModelService;

    /**
     * 完整审批分析
     */
    @PostMapping("/analyze")
    public R<String> analyzeAndSuggest(@RequestBody Map<String, Object> context) {
        String result = aiModelService.analyzeAndSuggest(context);
        return R.ok(result);
    }
}
