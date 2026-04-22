package com.lingxi.system.api.factory;

import com.lingxi.common.core.domain.R;
import com.lingxi.system.api.RemoteMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消息服务降级处理
 *
 * @author lingxi
 */
@Component
public class RemoteMessageFallbackFactory implements FallbackFactory<RemoteMessageService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteMessageFallbackFactory.class);

    @Override
    public RemoteMessageService create(Throwable throwable) {
        log.error("消息服务调用失败:{}", throwable.getMessage());
        return new RemoteMessageService() {
            @Override
            public R<Boolean> sendMessage(Map<String, Object> messageInfo, String source) {
                return R.fail("发送消息失败:" + throwable.getMessage());
            }
            
            @Override
            public R<Map<String, Object>> getUnreadCount(String source) {
                Map<String, Object> empty = new java.util.HashMap<>();
                empty.put("count", 0);
                return R.ok(empty);
            }
        };
    }
}
