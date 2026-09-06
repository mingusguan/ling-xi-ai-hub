package com.lingxi.kernel;

import java.time.Instant;

/**
 * 事件工作器交给模块消费者的稳定消息。
 *
 * @param eventId 事件唯一标识
 * @param eventType 事件类型
 * @param aggregateId 聚合标识
 * @param aggregateVersion 聚合版本
 * @param schemaVersion 载荷结构版本
 * @param occurredAt 业务发生时间
 * @param payloadJson JSON 载荷
 */
public record PublishedEventMessage(
    String eventId,
    String eventType,
    String aggregateId,
    long aggregateVersion,
    int schemaVersion,
    Instant occurredAt,
    String payloadJson) {}
