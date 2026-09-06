package com.lingxi.ai.agent.tools;

/**
 * 小灵儿会话工具调用上下文。
 */
public final class AiChatToolCallContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private AiChatToolCallContext() {
    }

    public static void set(Context context) {
        HOLDER.set(context);
    }

    public static Context get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前机器人消息触发工具调用时需要携带的业务标识。
     */
    public record Context(Long sessionId, Long userMessageId, Long userId, String username, Long deptId,
            String tenantId, String agentCode) {
    }
}
