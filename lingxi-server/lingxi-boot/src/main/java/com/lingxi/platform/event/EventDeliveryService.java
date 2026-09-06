package com.lingxi.platform.event;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.kernel.PublishedEventHandler;
import com.lingxi.kernel.PublishedEventMessage;
import com.lingxi.platform.persistence.EventConsumptionEntity;
import com.lingxi.platform.persistence.EventConsumptionMapper;
import com.lingxi.platform.persistence.EventPublicationEntity;
import com.lingxi.platform.persistence.EventPublicationMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在同一事务中完成模块消费者业务写入与消费凭证登记。 */
@Service
public class EventDeliveryService {
  private final EventPublicationMapper publicationMapper;
  private final EventConsumptionMapper consumptionMapper;
  private final List<PublishedEventHandler> handlers;
  private final IdGenerator idGenerator;

  public EventDeliveryService(
      EventPublicationMapper publicationMapper,
      EventConsumptionMapper consumptionMapper,
      List<PublishedEventHandler> handlers,
      IdGenerator idGenerator) {
    this.publicationMapper = publicationMapper;
    this.consumptionMapper = consumptionMapper;
    this.handlers = handlers;
    this.idGenerator = idGenerator;
  }

  @Transactional
  public void deliver(EventPublicationEntity publication, PublishedEventMessage message)
      throws Exception {
    for (PublishedEventHandler handler : handlers) {
      if (!handler.supports(publication.getEventType())
          || hasConsumed(publication.getEventId(), handler.consumerName())) {
        continue;
      }
      handler.handle(message);
      markConsumed(publication.getEventId(), handler.consumerName());
    }
    publicationMapper.update(
        null,
        Wrappers.<EventPublicationEntity>lambdaUpdate()
            .eq(EventPublicationEntity::getId, publication.getId())
            .eq(EventPublicationEntity::getStatus, EventPublicationStatus.RUNNING.name())
            .set(EventPublicationEntity::getStatus, EventPublicationStatus.COMPLETED.name())
            .set(EventPublicationEntity::getLeaseUntil, null)
            .set(EventPublicationEntity::getCompletedAt, LocalDateTime.now(ZoneOffset.UTC)));
  }

  private boolean hasConsumed(String eventId, String consumerName) {
    return consumptionMapper.selectCount(
            Wrappers.<EventConsumptionEntity>lambdaQuery()
                .eq(EventConsumptionEntity::getEventId, eventId)
                .eq(EventConsumptionEntity::getConsumerName, consumerName))
        > 0;
  }

  private void markConsumed(String eventId, String consumerName) {
    EventConsumptionEntity consumption = new EventConsumptionEntity();
    consumption.setId(idGenerator.nextId());
    consumption.setEventId(eventId);
    consumption.setConsumerName(consumerName);
    consumption.setConsumedAt(LocalDateTime.now(ZoneOffset.UTC));
    try {
      consumptionMapper.insert(consumption);
    } catch (DuplicateKeyException ignored) {
      // 唯一键兜底并发确认；消费者业务本身仍须按 eventId 保持幂等。
    }
  }
}
