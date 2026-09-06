package com.lingxi.platform.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.DomainEvent;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.platform.persistence.EventPublicationEntity;
import com.lingxi.platform.persistence.EventPublicationMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/** 将领域事件与业务数据登记在同一个本地数据库事务中。 */
@Component
public class DatabaseDomainEventPublisher implements DomainEventPublisher {

  private final EventPublicationMapper publicationMapper;
  private final ObjectMapper objectMapper;
  private final IdGenerator idGenerator;

  public DatabaseDomainEventPublisher(
      EventPublicationMapper publicationMapper,
      ObjectMapper objectMapper,
      IdGenerator idGenerator) {
    this.publicationMapper = publicationMapper;
    this.objectMapper = objectMapper;
    this.idGenerator = idGenerator;
  }

  @Override
  public void publish(DomainEvent event) {
    EventPublicationEntity publication = new EventPublicationEntity();
    publication.setId(idGenerator.nextId());
    publication.setEventId(event.eventId());
    publication.setEventType(event.eventType());
    publication.setAggregateId(event.aggregateId());
    publication.setAggregateVersion(event.aggregateVersion());
    publication.setSchemaVersion(event.schemaVersion());
    publication.setPayloadJson(serialize(event));
    publication.setStatus(EventPublicationStatus.PENDING.name());
    publication.setAttemptCount(0);
    publication.setNextRetryAt(LocalDateTime.now(ZoneOffset.UTC));
    publication.setOccurredAt(LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC));
    publication.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
    if (publicationMapper.insert(publication) != 1) {
      throw new BusinessException("PLATFORM_EVENT_PERSIST_FAILED", "领域事件登记失败");
    }
  }

  private String serialize(DomainEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("PLATFORM_EVENT_SERIALIZE_FAILED", "领域事件序列化失败");
    }
  }
}
