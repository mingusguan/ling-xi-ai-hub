package com.lingxi.system.controller;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.domain.SysMessage;
import com.lingxi.system.service.ISysMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统消息控制器
 *
 * @author lingxi
 */
@RestController
@RequestMapping("/message")
public class SysMessageController extends BaseController {

    @Autowired
    private ISysMessageService messageService;

    /**
     * 获取消息列表
     *
     * @param messageType 消息类型（可选）
     * @param sourceType 消息来源（可选）
     * @param status 消息状态（可选）
     * @return 分页的消息列表
     */
    @GetMapping("/list")
    public TableDataInfo getMessageList(
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String status) {
        startPage();
        SysMessage message = new SysMessage();
        message.setUserId(SecurityUtils.getUserId());
        message.setMessageType(messageType);
        message.setSourceType(sourceType);
        message.setStatus(status);
        List<SysMessage> list = messageService.selectSysMessageList(message);
        return getDataTable(list);
    }

    /**
     * 获取未读消息数量
     *
     * @return 未读消息数量
     */
    @GetMapping("/unread/count")
    public AjaxResult getUnreadCount() {
        int count = messageService.countUnreadMessages(SecurityUtils.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return AjaxResult.success(result);
    }

    /**
     * 标记消息为已读
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @PutMapping("/{messageId}/read")
    public AjaxResult markAsRead(@PathVariable Long messageId) {
        boolean success = messageService.markAsRead(messageId, SecurityUtils.getUserId());
        if (!success) {
            return AjaxResult.error("消息不存在或无权限");
        }
        return AjaxResult.success();
    }

    /**
     * 删除消息
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @DeleteMapping("/{messageId}")
    public AjaxResult deleteMessage(@PathVariable Long messageId) {
        return toAjax(messageService.deleteSysMessageById(messageId));
    }

    /**
     * 内部接口：发送系统消息（供其他微服务调用）
     *
     * @param messageInfo 消息信息
     * @return 操作结果
     */
    @PostMapping("/inner/send")
    public R<Boolean> sendMessageInternal(@RequestBody Map<String, Object> messageInfo) {
        SysMessage message = new SysMessage();
        message.setUserId(((Number) messageInfo.get("userId")).longValue());
        message.setUserName((String) messageInfo.get("userName"));
        message.setSourceType((String) messageInfo.get("sourceType"));
        message.setMessageType((String) messageInfo.get("messageType"));
        message.setTitle((String) messageInfo.get("title"));
        message.setContent((String) messageInfo.get("content"));
        message.setBusinessType((String) messageInfo.get("businessType"));
        message.setBusinessId((String) messageInfo.get("businessId"));
        message.setProcessInstanceId((String) messageInfo.get("processInstanceId"));
        message.setTaskId((String) messageInfo.get("taskId"));
        if (messageInfo.get("priority") != null) {
            message.setPriority(((Number) messageInfo.get("priority")).intValue());
        }
        message.setChannel((String) messageInfo.get("channel"));
        
        boolean success = messageService.sendMessage(message);
        return R.ok(success);
    }

    /**
     * 内部接口：获取未读消息数量（供其他微服务调用）
     *
     * @return 未读消息数量
     */
    @GetMapping("/inner/unread/count")
    public AjaxResult getUnreadCountInternal() {
        int count = messageService.countUnreadMessages(SecurityUtils.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return AjaxResult.success(result);
    }
}
