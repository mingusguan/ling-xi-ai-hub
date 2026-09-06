package com.lingxi.kernel;

/** 在业务事务内登记领域事件的发布端口。 */
public interface DomainEventPublisher {

  void publish(DomainEvent event);
}
