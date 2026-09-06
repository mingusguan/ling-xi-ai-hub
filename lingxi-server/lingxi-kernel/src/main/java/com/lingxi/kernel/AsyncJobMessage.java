package com.lingxi.kernel;

import java.time.Instant;

/**
 * 异步任务执行消息。
 *
 * @param jobId 任务标识
 * @param jobType 任务类型
 * @param businessKey 业务唯一键
 * @param payloadJson 任务载荷
 * @param attempt 当前执行次数
 * @param createdAt 创建时间
 */
public record AsyncJobMessage(
    long jobId,
    String jobType,
    String businessKey,
    String payloadJson,
    int attempt,
    Instant createdAt) {}
