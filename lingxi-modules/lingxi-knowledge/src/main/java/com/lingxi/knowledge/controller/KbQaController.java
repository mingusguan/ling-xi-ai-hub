package com.lingxi.knowledge.controller;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.common.security.annotation.Logical;
import com.lingxi.common.security.annotation.RequiresPermissions;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.knowledge.domain.KbQaSession;
import com.lingxi.knowledge.domain.dto.KbQaChatRequest;
import com.lingxi.knowledge.domain.dto.KbQaFeedbackRequest;
import com.lingxi.knowledge.service.IKbQaService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库问答与运营控制器。
 */
@Validated
@RestController
@RequestMapping("/knowledge/qa")
public class KbQaController extends BaseController {

    private final IKbQaService qaService;

    public KbQaController(IKbQaService qaService) {
        this.qaService = qaService;
    }

    /**
     * 知识库问答。
     */
    @RequiresPermissions(value = {"knowledge:qa:chat", "knowledge:document:list"}, logical = Logical.OR)
    @PostMapping("/chat")
    public R<?> chat(@Valid @RequestBody KbQaChatRequest request) {
        return R.ok(qaService.chat(request));
    }

    /**
     * 查询用户问答会话。
     */
    @RequiresPermissions(value = {"knowledge:qa:chat", "knowledge:document:list"}, logical = Logical.OR)
    @GetMapping("/session/list")
    public R<?> listSessions(@RequestParam(required = false) Long userId) {
        return R.ok(qaService.listSessions(resolveUserId(userId)));
    }

    /**
     * 创建问答会话。
     */
    @RequiresPermissions(value = {"knowledge:qa:chat", "knowledge:document:list"}, logical = Logical.OR)
    @PostMapping("/session/create")
    public R<?> createSession(@RequestBody(required = false) Map<String, Object> request) {
        Long userId = extractLong(request, "userId");
        Long deptId = extractLong(request, "deptId");
        KbQaSession session = qaService.createSession(resolveUserId(userId), deptId);
        return R.ok(session);
    }

    /**
     * 删除问答会话。
     */
    @RequiresPermissions(value = {"knowledge:qa:chat", "knowledge:document:list"}, logical = Logical.OR)
    @DeleteMapping("/session/{sessionId}")
    public R<?> deleteSession(@PathVariable Long sessionId) {
        qaService.deleteSession(sessionId, SecurityUtils.getUserId());
        return R.ok();
    }

    /**
     * 查询会话问答明细。
     */
    @RequiresPermissions(value = {"knowledge:qa:chat", "knowledge:document:list"}, logical = Logical.OR)
    @GetMapping("/session/{sessionId}/conversations")
    public R<?> listConversations(@PathVariable Long sessionId) {
        return R.ok(qaService.listConversations(sessionId, SecurityUtils.getUserId()));
    }

    /**
     * 查询历史问答，兼容旧前端接口。
     */
    @RequiresPermissions(value = {"knowledge:qa:chat", "knowledge:document:list"}, logical = Logical.OR)
    @GetMapping("/history")
    public R<?> history(@RequestParam(required = false) Long userId) {
        return R.ok(qaService.listSessions(resolveUserId(userId)));
    }

    /**
     * 查询知识运营统计。
     */
    @RequiresPermissions("knowledge:operation:view")
    @GetMapping("/operation/stats")
    public R<?> operationStats() {
        return R.ok(qaService.getOperationStats());
    }

    /**
     * 分页查询知识问答运营日志。
     */
    @RequiresPermissions("knowledge:operation:view")
    @GetMapping("/operation/list")
    public TableDataInfo operationList(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String noAnswer,
            @RequestParam(required = false) String feedback,
            @RequestParam(required = false) String confidenceLevel) {
        startPage();
        return getDataTable(qaService.listOperations(keyword, noAnswer, feedback, confidenceLevel));
    }

    /**
     * 提交问答反馈。
     */
    @RequiresPermissions(value = {"knowledge:operation:feedback", "knowledge:qa:chat"}, logical = Logical.OR)
    @PostMapping("/feedback")
    public R<?> feedback(@Valid @RequestBody KbQaFeedbackRequest request) {
        qaService.feedback(request);
        return R.ok();
    }

    private Long resolveUserId(Long requestUserId) {
        Long currentUserId = SecurityUtils.getUserId();
        return currentUserId == null ? requestUserId : currentUserId;
    }

    private Long extractLong(Map<String, Object> request, String key) {
        if (request == null || !request.containsKey(key)) {
            return null;
        }
        Object value = request.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
