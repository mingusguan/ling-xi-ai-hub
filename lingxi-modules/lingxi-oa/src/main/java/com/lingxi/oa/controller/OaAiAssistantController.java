package com.lingxi.oa.controller;

import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.oa.domain.OaSmartReminder;
import com.lingxi.oa.service.IOaAiOpinionService;
import com.lingxi.oa.service.IOaSmartReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
    private IOaSmartReminderService reminderService;

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
     * 获取智能提醒列表
     *
     * @param status 提醒状态（可选）
     * @return 分页的提醒列表
     */
    @GetMapping("/reminder/list")
    public AjaxResult getReminderList(@RequestParam(required = false) String status) {
        startPage();
        OaSmartReminder reminder = new OaSmartReminder();
        reminder.setUserId(SecurityUtils.getUserId());
        reminder.setStatus(status);
        List<OaSmartReminder> list = reminderService.selectOaSmartReminderList(reminder);
        return AjaxResult.success(getDataTable(list));
    }

    /**
     * 获取未读提醒数量
     *
     * @return 未读提醒数量
     */
    @GetMapping("/reminder/unread/count")
    public AjaxResult getUnreadCount() {
        int count = reminderService.countUnreadReminders(SecurityUtils.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return AjaxResult.success(result);
    }

    /**
     * 标记提醒为已读
     *
     * @param reminderId 提醒ID
     * @return 操作结果
     */
    @PutMapping("/reminder/{reminderId}/read")
    public AjaxResult markAsRead(@PathVariable Long reminderId) {
        boolean success = reminderService.markAsRead(reminderId, SecurityUtils.getUserId());
        if (!success) {
            return AjaxResult.error("提醒消息不存在或无权限");
        }
        return AjaxResult.success();
    }
}
