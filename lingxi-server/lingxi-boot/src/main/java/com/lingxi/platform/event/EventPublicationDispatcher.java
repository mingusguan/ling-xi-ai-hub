package com.lingxi.platform.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingxi.kernel.PublishedEventMessage;
import com.lingxi.platform.persistence.EventPublicationEntity;
import com.lingxi.platform.persistence.EventPublicationMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 多实例安全的持久化事件派发器。 */
@Slf4j
@Component
public class EventPublicationDispatcher {
  private static final int MAX_ATTEMPTS = 5;
  private static final int MAX_ERROR_LENGTH = 1000;

  private final EventPublicationMapper publicationMapper;
  private final EventDeliveryService deliveryService;
  private final int batchSize;
  private final int leaseSeconds;

  public EventPublicationDispatcher(
      EventPublicationMapper publicationMapper,
      EventDeliveryService deliveryService,
      @Value("${lingxi.platform.events.batch-size:50}") int batchSize,
      @Value("${lingxi.platform.events.lease-seconds:60}") int leaseSeconds) {
    this.publicationMapper = publicationMapper;
    this.deliveryService = deliveryService;
    this.batchSize = batchSize;
    this.leaseSeconds = leaseSeconds;
  }

  @Scheduled(fixedDelayString = "${lingxi.platform.events.poll-delay-ms:1000}")
  public void dispatchAvailableEvents() {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    List<EventPublicationEntity> candidates =
        publicationMapper.selectPage(Page.of(1, batchSize), availableEvents(now)).getRecords();
    for (EventPublicationEntity candidate : candidates) {
      dispatchOne(candidate.getId(), now);
    }
  }

  private LambdaQueryWrapper<EventPublicationEntity> availableEvents(LocalDateTime now) {
    return new LambdaQueryWrapper<EventPublicationEntity>()
        .and(
            wrapper ->
                wrapper
                    .and(
                        pending ->
                            pending
                                .eq(
                                    EventPublicationEntity::getStatus,
                                    EventPublicationStatus.PENDING.name())
                                .le(EventPublicationEntity::getNextRetryAt, now))
                    .or(
                        expired ->
                            expired
                                .eq(
                                    EventPublicationEntity::getStatus,
                                    EventPublicationStatus.RUNNING.name())
                                .le(EventPublicationEntity::getLeaseUntil, now)))
        .orderByAsc(EventPublicationEntity::getId);
  }

  private void dispatchOne(long publicationId, LocalDateTime now) {
    if (!claim(publicationId, now)) {
      return;
    }
    EventPublicationEntity publication = publicationMapper.selectById(publicationId);
    try {
      deliveryService.deliver(publication, toMessage(publication));
    } catch (Exception exception) {
      markFailed(publication, exception);
    }
  }

  private boolean claim(long publicationId, LocalDateTime now) {
    LambdaUpdateWrapper<EventPublicationEntity> update =
        new LambdaUpdateWrapper<EventPublicationEntity>()
            .eq(EventPublicationEntity::getId, publicationId)
            .and(
                wrapper ->
                    wrapper
                        .and(
                            pending ->
                                pending
                                    .eq(
                                        EventPublicationEntity::getStatus,
                                        EventPublicationStatus.PENDING.name())
                                    .le(EventPublicationEntity::getNextRetryAt, now))
                        .or(
                            expired ->
                                expired
                                    .eq(
                                        EventPublicationEntity::getStatus,
                                        EventPublicationStatus.RUNNING.name())
                                    .le(EventPublicationEntity::getLeaseUntil, now)))
            .set(EventPublicationEntity::getStatus, EventPublicationStatus.RUNNING.name())
            .set(EventPublicationEntity::getLeaseUntil, now.plusSeconds(leaseSeconds));
    return publicationMapper.update(null, update) == 1;
  }

  private void markFailed(EventPublicationEntity publication, Exception exception) {
    int attempts = publication.getAttemptCount() + 1;
    boolean exhausted = attempts >= MAX_ATTEMPTS;
    long retryDelaySeconds = Math.min(300, 1L << Math.min(attempts, 8));
    publicationMapper.update(
        null,
        new LambdaUpdateWrapper<EventPublicationEntity>()
            .eq(EventPublicationEntity::getId, publication.getId())
            .eq(EventPublicationEntity::getStatus, EventPublicationStatus.RUNNING.name())
            .set(
                EventPublicationEntity::getStatus,
                exhausted
                    ? EventPublicationStatus.DEAD.name()
                    : EventPublicationStatus.PENDING.name())
            .set(EventPublicationEntity::getAttemptCount, attempts)
            .set(EventPublicationEntity::getLeaseUntil, null)
            .set(
                EventPublicationEntity::getNextRetryAt,
                LocalDateTime.now(ZoneOffset.UTC).plusSeconds(retryDelaySeconds))
            .set(EventPublicationEntity::getLastError, abbreviate(exception.getMessage())));
    log.warn(
        "领域事件派发失败, eventId={}, type={}, attempts={}",
        publication.getEventId(),
        publication.getEventType(),
        attempts,
        exception);
  }

  private PublishedEventMessage toMessage(EventPublicationEntity publication) {
    Instant occurredAt = publication.getOccurredAt().toInstant(ZoneOffset.UTC);
    return new PublishedEventMessage(
        publication.getEventId(),
        publication.getEventType(),
        publication.getAggregateId(),
        publication.getAggregateVersion(),
        publication.getSchemaVersion(),
        occurredAt,
        publication.getPayloadJson());
  }

  private String abbreviate(String message) {
    if (message == null) {
      return "unknown error";
    }
    return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
  }
}
