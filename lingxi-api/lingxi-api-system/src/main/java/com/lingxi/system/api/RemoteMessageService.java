package com.lingxi.system.api;

import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * 消息服务
 *
 * @author lingxi
 */
public interface RemoteMessageService {
    
    /**
     * 发送系统消息
     *
     * @param messageInfo 消息信息（包含userId, sourceType, messageType, title, content等）
     * @param source 请求来源
     * @return 结果
     */
    @PostMapping("/system/message/inner/send")
    public R<Boolean> sendMessage(@RequestBody Map<String, Object> messageInfo, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
    
    /**
     * 获取用户未读消息数量
     *
     * @param source 请求来源
     * @return 未读数量
     */
    @GetMapping("/system/message/inner/unread/count")
    public R<Map<String, Object>> getUnreadCount(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
