package com.lingxi.kernel;

import java.time.Instant;

/** 批量异步任务请求。 */
public record AsyncJobRequest(
    String jobType, String businessKey, String payloadJson, int maxAttempts, Instant notBefore) {}
