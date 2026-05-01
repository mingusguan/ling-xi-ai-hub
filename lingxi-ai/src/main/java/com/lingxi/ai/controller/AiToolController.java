package com.lingxi.ai.controller;

import com.lingxi.ai.domain.dto.DocumentWritingRequest;
import com.lingxi.ai.domain.dto.ReportInterpretRequest;
import com.lingxi.ai.domain.vo.DocumentWritingResult;
import com.lingxi.ai.domain.vo.ReportInterpretResult;
import com.lingxi.ai.service.IAiToolService;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.log.annotation.Log;
import com.lingxi.common.log.enums.BusinessType;
import com.lingxi.common.security.annotation.RequiresPermissions;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 工具控制器。
 */
@Validated
@RestController
@RequestMapping("/ai/tool")
public class AiToolController extends BaseController
{
    private final IAiToolService aiToolService;

    public AiToolController(IAiToolService aiToolService)
    {
        this.aiToolService = aiToolService;
    }

    /**
     * 公文写作与润色。
     *
     * @param request 请求参数
     * @return 结果
     */
    @RequiresPermissions("ai:document:generate")
    @Log(title = "AI公文助手", businessType = BusinessType.OTHER)
    @PostMapping("/document/write")
    public AjaxResult writeDocument(@Valid @org.springframework.web.bind.annotation.RequestBody DocumentWritingRequest request)
    {
        DocumentWritingResult result = aiToolService.writeDocument(request);
        return AjaxResult.success("生成成功", result);
    }

    /**
     * 报表解读。
     *
     * @param request 请求参数
     * @param file 报表文件
     * @return 结果
     */
    @RequiresPermissions("ai:report:analyze")
    @Log(title = "AI报表解读", businessType = BusinessType.OTHER)
    @PostMapping("/report/interpret")
    public AjaxResult interpretReport(@Valid @ModelAttribute ReportInterpretRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file)
    {
        ReportInterpretResult result = aiToolService.interpretReport(request, file);
        return AjaxResult.success("解读成功", result);
    }
}
