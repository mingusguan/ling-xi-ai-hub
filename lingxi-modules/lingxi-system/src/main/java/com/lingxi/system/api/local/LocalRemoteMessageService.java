package com.lingxi.system.api.local;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.RemoteMessageService;
import com.lingxi.system.domain.SysMessage;
import com.lingxi.system.service.ISysMessageService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 单体模式下的本地消息服务调用。
 */
@Service
public class LocalRemoteMessageService implements RemoteMessageService {

    private final ISysMessageService messageService;

    public LocalRemoteMessageService(ISysMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public R<Boolean> sendMessage(Map<String, Object> messageInfo, String source) {
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
        return R.ok(messageService.sendMessage(message));
    }

    @Override
    public R<Map<String, Object>> getUnreadCount(String source) {
        int count = messageService.countUnreadMessages(SecurityUtils.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return R.ok(result);
    }
}
