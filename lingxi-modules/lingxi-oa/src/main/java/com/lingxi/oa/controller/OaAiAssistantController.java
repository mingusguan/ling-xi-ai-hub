package com.lingxi.oa.controller;

import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.RemoteMessageService;
import com.lingxi.oa.service.IOaAiOpinionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OA AI助手控制器
 * 提供AI对话、智能意见生成、智能提醒等功能
 *
 * @author lingxi
 */
@RestController
@RequestMapping("/oa/ai")
public class OaAiAssistantController extends BaseController {

    @Autowired
    private IOaAiOpinionService opinionService;

    @Autowired
    private RemoteMessageService remoteMessageService;

    /**
     * 生成智能审批意见建议
     *
     * @param businessType 业务类型（如：leave-请假，expense-报销）
     * @param businessId 业务单据ID
     * @param taskId 流程任务ID
     * @return AI生成的审批意见
     */
    @GetMapping("/opinion/suggest")
    public AjaxResult generateOpinionSuggestion(
            @RequestParam String businessType,
            @RequestParam Long businessId,
            @RequestParam String taskId) {
        return opinionService.generateSuggestion(businessType, businessId, taskId, SecurityUtils.getUserId());
    }

    /**
     * 查询AI建议生成状态
     *
     * @param businessType 业务类型
     * @param businessId 业务单据ID
     * @return AI建议结果
     */
    @GetMapping("/opinion/status")
    public AjaxResult getOpinionStatus(
            @RequestParam String businessType,
            @RequestParam Long businessId) {
        return opinionService.getSuggestionStatus(businessType, businessId);
    }

    /**
     * 获取未读消息数量（从统一消息中心）
     *
     * @return 未读消息数量
     */
    @GetMapping("/reminder/unread/count")
    public AjaxResult getUnreadCount() {
        R<Map<String, Object>> result = remoteMessageService.getUnreadCount(SecurityConstants.INNER);
        if (result != null && result.getData() != null) {
            return AjaxResult.success(result.getData());
        }
        Map<String, Object> empty = new HashMap<>();
        empty.put("count", 0);
        return AjaxResult.success(empty);
    }
}
