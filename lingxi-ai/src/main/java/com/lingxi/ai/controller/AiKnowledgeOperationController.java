package com.lingxi.ai.controller;

import com.lingxi.ai.domain.dto.AiKnowledgeFeedbackRequest;
import com.lingxi.ai.service.IAiChatToolCallService;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.common.security.annotation.RequiresPermissions;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小灵儿知识库运营控制器。
 */
@RestController
@RequestMapping("/ai/knowledge/operation")
public class AiKnowledgeOperationController extends BaseController {

    private final IAiChatToolCallService toolCallService;

    public AiKnowledgeOperationController(IAiChatToolCallService toolCallService) {
        this.toolCallService = toolCallService;
    }

    /**
     * 查询机器人知识库调用统计。
     */
    @RequiresPermissions("knowledge:operation:view")
    @GetMapping("/stats")
    public AjaxResult stats() {
        return AjaxResult.success(toolCallService.getKnowledgeOperationStats());
    }

    /**
     * 分页查询机器人知识库调用明细。
     */
    @RequiresPermissions("knowledge:operation:view")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String noAnswer,
            @RequestParam(required = false) String feedback,
            @RequestParam(required = false) String confidenceLevel) {
        startPage();
        return getDataTable(toolCallService.listKnowledgeOperations(keyword, noAnswer, feedback, confidenceLevel));
    }

    /**
     * 保存机器人知识库调用反馈。
     */
    @RequiresPermissions("knowledge:operation:feedback")
    @PostMapping("/feedback")
    public AjaxResult feedback(@Valid @RequestBody AiKnowledgeFeedbackRequest request) {
        toolCallService.feedback(request);
        return AjaxResult.success();
    }
}
