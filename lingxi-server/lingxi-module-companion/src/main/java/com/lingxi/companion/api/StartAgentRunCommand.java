package com.lingxi.companion.api;

import java.util.List;

/** Agent 运行请求；附件仅传已完成授权的文件标识。 */
public record StartAgentRunCommand(
    String requestKey,
    long userId,
    long conversationId,
    String scene,
    String input,
    List<Long> attachmentFileIds) {}
