package com.lingxi.kernel;

/** 持久化领域事件消费者。实现必须以 eventId 保持业务幂等。 */
public interface PublishedEventHandler {

  /** 稳定消费者名称，用于记录独立消费进度。 */
  String consumerName();

  boolean supports(String eventType);

  void handle(PublishedEventMessage event);
}
