package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.engagement.api.*;
import com.lingxi.engagement.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

/** R05 MyBatis-Plus 仓储实现。 */
@Repository
public class MybatisEngagementRepository implements EngagementRepository {
  private final NotificationPreferenceMapper preferenceMapper;
  private final NotificationTaskMapper taskMapper;
  private final InboxMessageMapper inboxMapper;
  private final NotificationDeliveryMapper deliveryMapper;
  private final CalendarBindingMapper calendarMapper;
  private final CalendarEventBindingMapper calendarEventMapper;
  private final SyncChangeMapper changeMapper;
  private final OfflineCommandMapper commandMapper;
  private final ObjectMapper objectMapper;

  public MybatisEngagementRepository(
      NotificationPreferenceMapper preferenceMapper,
      NotificationTaskMapper taskMapper,
      InboxMessageMapper inboxMapper,
      NotificationDeliveryMapper deliveryMapper,
      CalendarBindingMapper calendarMapper,
      CalendarEventBindingMapper calendarEventMapper,
      SyncChangeMapper changeMapper,
      OfflineCommandMapper commandMapper,
      ObjectMapper objectMapper) {
    this.preferenceMapper = preferenceMapper;
    this.taskMapper = taskMapper;
    this.inboxMapper = inboxMapper;
    this.deliveryMapper = deliveryMapper;
    this.calendarMapper = calendarMapper;
    this.calendarEventMapper = calendarEventMapper;
    this.changeMapper = changeMapper;
    this.commandMapper = commandMapper;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<NotificationPreference> findPreference(long userId, String scene) {
    return Optional.ofNullable(
            preferenceMapper.selectOne(
                Wrappers.<NotificationPreferenceEntity>lambdaQuery()
                    .eq(NotificationPreferenceEntity::getUserId, userId)
                    .eq(NotificationPreferenceEntity::getScene, scene)
                    .last("LIMIT 1")))
        .map(this::preference);
  }

  @Override
  public void insertPreference(NotificationPreference p) {
    preferenceMapper.insert(preferenceEntity(p));
  }

  @Override
  public boolean updatePreference(NotificationPreference p, long previous) {
    // 显式乐观并发：WHERE 带 previous 版本并在 SET 写入领域对象的新版本。
    // 必须传 null 实体，否则 MyBatis-Plus 的 @Version 优化锁会再自增一次并追加重复版本条件
    // （生成 `... AND version = ? AND version = ?`），导致永远匹配不到行。
    return preferenceMapper.update(
            null,
            Wrappers.<NotificationPreferenceEntity>lambdaUpdate()
                .eq(NotificationPreferenceEntity::getUserId, p.getUserId())
                .eq(NotificationPreferenceEntity::getScene, p.getScene())
                .eq(NotificationPreferenceEntity::getVersion, previous)
                .set(NotificationPreferenceEntity::getChannelsJson, write(p.getChannels()))
                .set(NotificationPreferenceEntity::getQuietStart, p.getQuietStart())
                .set(NotificationPreferenceEntity::getQuietEnd, p.getQuietEnd())
                .set(NotificationPreferenceEntity::getTimezone, p.getTimezone())
                .set(NotificationPreferenceEntity::getVersion, p.getVersion())
                .set(NotificationPreferenceEntity::getUpdatedAt, p.getUpdatedAt()))
        == 1;
  }

  @Override
  public Optional<NotificationTask> findTaskByDedupeKey(String key) {
    return Optional.ofNullable(
            taskMapper.selectOne(
                Wrappers.<NotificationTaskEntity>lambdaQuery()
                    .eq(NotificationTaskEntity::getDedupeKey, key)
                    .last("LIMIT 1")))
        .map(this::task);
  }

  @Override
  public void insertTask(NotificationTask task) {
    taskMapper.insert(taskEntity(task));
  }

  @Override
  public Optional<NotificationTask> findTask(long id) {
    return Optional.ofNullable(taskMapper.selectById(id)).map(this::task);
  }

  @Override
  public boolean updateTask(NotificationTask t, String expected) {
    return taskMapper.update(
            null,
            Wrappers.<NotificationTaskEntity>lambdaUpdate()
                .eq(NotificationTaskEntity::getId, t.getId())
                .eq(NotificationTaskEntity::getStatus, expected)
                .set(NotificationTaskEntity::getStatus, t.getStatus().name())
                .set(NotificationTaskEntity::getAttemptCount, t.getAttemptCount())
                .set(NotificationTaskEntity::getLastError, t.getLastError())
                .set(NotificationTaskEntity::getUpdatedAt, t.getUpdatedAt()))
        == 1;
  }

  @Override
  public void insertDelivery(
      long id,
      long taskId,
      int attempt,
      String providerMessageId,
      String result,
      String errorCode,
      LocalDateTime now) {
    NotificationDeliveryEntity e = new NotificationDeliveryEntity();
    e.setId(id);
    e.setTaskId(taskId);
    e.setAttemptNo(attempt);
    e.setProviderMessageId(providerMessageId);
    e.setResult(result);
    e.setErrorCode(errorCode);
    e.setDeliveredAt("SENT".equals(result) ? now : null);
    e.setCreatedAt(now);
    deliveryMapper.insert(e);
  }

  @Override
  public void insertInbox(
      long id,
      long userId,
      String type,
      String resourceType,
      String resourceId,
      String summary,
      long cursor,
      LocalDateTime now) {
    InboxMessageEntity e = new InboxMessageEntity();
    e.setId(id);
    e.setUserId(userId);
    e.setType(type);
    e.setResourceType(resourceType);
    e.setResourceId(resourceId);
    e.setSummary(summary);
    e.setCursorNo(cursor);
    e.setCreatedAt(now);
    inboxMapper.insert(e);
  }

  @Override
  public List<NotificationResult> findNotifications(long userId, long cursor, int limit) {
    return inboxMapper
        .selectList(
            Wrappers.<InboxMessageEntity>lambdaQuery()
                .eq(InboxMessageEntity::getUserId, userId)
                .gt(InboxMessageEntity::getCursorNo, cursor)
                .orderByAsc(InboxMessageEntity::getCursorNo)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 200)))
        .stream()
        .map(
            e ->
                new NotificationResult(
                    e.getId(),
                    e.getType(),
                    e.getResourceType(),
                    e.getResourceId(),
                    e.getSummary(),
                    e.getReadAt() == null ? "UNREAD" : "READ",
                    e.getCreatedAt().toInstant(ZoneOffset.UTC),
                    e.getReadAt() == null ? null : e.getReadAt().toInstant(ZoneOffset.UTC),
                    e.getCursorNo()))
        .toList();
  }

  @Override
  public boolean markRead(long userId, long id, LocalDateTime now) {
    return inboxMapper.update(
            null,
            Wrappers.<InboxMessageEntity>lambdaUpdate()
                .eq(InboxMessageEntity::getId, id)
                .eq(InboxMessageEntity::getUserId, userId)
                .isNull(InboxMessageEntity::getReadAt)
                .set(InboxMessageEntity::getReadAt, now))
        == 1;
  }

  @Override
  public Optional<CalendarBinding> findCalendarByUserProvider(long userId, String provider) {
    return Optional.ofNullable(
            calendarMapper.selectOne(
                Wrappers.<CalendarBindingEntity>lambdaQuery()
                    .eq(CalendarBindingEntity::getUserId, userId)
                    .eq(CalendarBindingEntity::getProvider, provider)
                    .last("LIMIT 1")))
        .map(this::calendar);
  }

  @Override
  public List<CalendarBinding> findCalendarsByUser(long userId) {
    return calendarMapper
        .selectList(
            Wrappers.<CalendarBindingEntity>lambdaQuery()
                .eq(CalendarBindingEntity::getUserId, userId)
                .orderByAsc(CalendarBindingEntity::getProvider))
        .stream()
        .map(this::calendar)
        .toList();
  }

  @Override
  public Optional<CalendarBinding> findCalendar(long id) {
    return Optional.ofNullable(calendarMapper.selectById(id)).map(this::calendar);
  }

  @Override
  public void insertCalendar(CalendarBinding b) {
    calendarMapper.insert(calendarEntity(b));
  }

  @Override
  public boolean updateCalendar(CalendarBinding b, long previous) {
    // 与通知偏好同理：显式版本条件配合领域对象的新版本，不能传实体，否则 @Version 会重复自增与追加条件。
    return calendarMapper.update(
            null,
            Wrappers.<CalendarBindingEntity>lambdaUpdate()
                .eq(CalendarBindingEntity::getId, b.getId())
                .eq(CalendarBindingEntity::getVersion, previous)
                .set(CalendarBindingEntity::getStatus, b.getStatus().name())
                .set(CalendarBindingEntity::getCredentialReference, b.getCredentialReference())
                .set(CalendarBindingEntity::getDeleteCreatedEvents, b.isDeleteCreatedEvents())
                .set(CalendarBindingEntity::getVersion, b.getVersion())
                .set(CalendarBindingEntity::getUpdatedAt, b.getUpdatedAt()))
        == 1;
  }

  @Override
  public Optional<CalendarEventProjection> findCalendarEvent(
      long bindingId, String type, String resourceId) {
    return Optional.ofNullable(
            calendarEventMapper.selectOne(
                Wrappers.<CalendarEventBindingEntity>lambdaQuery()
                    .eq(CalendarEventBindingEntity::getBindingId, bindingId)
                    .eq(CalendarEventBindingEntity::getResourceType, type)
                    .eq(CalendarEventBindingEntity::getResourceId, resourceId)
                    .last("LIMIT 1")))
        .map(
            e ->
                new CalendarEventProjection(
                    e.getId(),
                    e.getBindingId(),
                    e.getResourceType(),
                    e.getResourceId(),
                    e.getExternalId(),
                    e.getExternalVersion(),
                    e.getSyncStatus(),
                    e.getUpdatedAt()));
  }

  @Override
  public List<CalendarEventProjection> findCalendarEvents(long bindingId) {
    return calendarEventMapper
        .selectList(
            Wrappers.<CalendarEventBindingEntity>lambdaQuery()
                .eq(CalendarEventBindingEntity::getBindingId, bindingId))
        .stream()
        .map(
            e ->
                new CalendarEventProjection(
                    e.getId(),
                    e.getBindingId(),
                    e.getResourceType(),
                    e.getResourceId(),
                    e.getExternalId(),
                    e.getExternalVersion(),
                    e.getSyncStatus(),
                    e.getUpdatedAt()))
        .toList();
  }

  @Override
  public void upsertCalendarEvent(CalendarEventProjection event) {
    CalendarEventBindingEntity e = new CalendarEventBindingEntity();
    e.setId(event.id());
    e.setBindingId(event.bindingId());
    e.setResourceType(event.resourceType());
    e.setResourceId(event.resourceId());
    e.setExternalId(event.externalId());
    e.setExternalVersion(event.externalVersion());
    e.setSyncStatus(event.syncStatus());
    e.setUpdatedAt(event.updatedAt());
    if (calendarEventMapper.selectById(event.id()) == null) calendarEventMapper.insert(e);
    else calendarEventMapper.updateById(e);
  }

  @Override
  public void deleteCalendarEvent(long id) {
    calendarEventMapper.deleteById(id);
  }

  @Override
  public void insertChange(
      long id,
      long userId,
      long sequence,
      String domain,
      String resourceType,
      String resourceId,
      long resourceVersion,
      String operation,
      String snapshot,
      LocalDateTime now) {
    SyncChangeEntity e = new SyncChangeEntity();
    e.setId(id);
    e.setUserId(userId);
    e.setSequenceNo(sequence);
    e.setDomainName(domain);
    e.setResourceType(resourceType);
    e.setResourceId(resourceId);
    e.setResourceVersion(resourceVersion);
    e.setOperationType(operation);
    e.setSnapshotJson(snapshot);
    e.setOccurredAt(now);
    changeMapper.insert(e);
  }

  @Override
  public List<SyncChangeResult> findChanges(
      long userId, long cursor, List<String> domains, int limit) {
    var q =
        Wrappers.<SyncChangeEntity>lambdaQuery()
            .eq(SyncChangeEntity::getUserId, userId)
            .gt(SyncChangeEntity::getSequenceNo, cursor);
    if (domains != null && !domains.isEmpty()) {
      q.in(SyncChangeEntity::getDomainName, domains);
    }
    return changeMapper
        .selectList(
            q.orderByAsc(SyncChangeEntity::getSequenceNo)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 500)))
        .stream()
        .map(
            e ->
                new SyncChangeResult(
                    e.getSequenceNo(),
                    e.getDomainName(),
                    e.getResourceType(),
                    e.getResourceId(),
                    e.getResourceVersion(),
                    e.getOperationType(),
                    e.getSnapshotJson(),
                    e.getOccurredAt().toInstant(ZoneOffset.UTC)))
        .toList();
  }

  @Override
  public Optional<OfflineCommandResult> findCommand(long userId, String clientId) {
    return Optional.ofNullable(
            commandMapper.selectOne(
                Wrappers.<OfflineCommandEntity>lambdaQuery()
                    .eq(OfflineCommandEntity::getUserId, userId)
                    .eq(OfflineCommandEntity::getClientCommandId, clientId)
                    .last("LIMIT 1")))
        .map(
            e ->
                new OfflineCommandResult(
                    e.getClientCommandId(), e.getStatus(), e.getResultJson(), e.getErrorCode()));
  }

  @Override
  public Optional<String> findCommandDigest(long userId, String clientId) {
    OfflineCommandEntity entity =
        commandMapper.selectOne(
            Wrappers.<OfflineCommandEntity>lambdaQuery()
                .select(OfflineCommandEntity::getRequestDigest)
                .eq(OfflineCommandEntity::getUserId, userId)
                .eq(OfflineCommandEntity::getClientCommandId, clientId)
                .last("LIMIT 1"));
    return Optional.ofNullable(entity).map(OfflineCommandEntity::getRequestDigest);
  }

  @Override
  public void insertCommand(
      long id, OfflineSyncCommand c, String digest, OfflineCommandResult r, LocalDateTime now) {
    OfflineCommandEntity e = new OfflineCommandEntity();
    e.setId(id);
    e.setUserId(c.userId());
    e.setDeviceId(c.deviceId());
    e.setClientCommandId(c.clientCommandId());
    e.setCommandType(c.commandType());
    e.setRequestDigest(digest);
    e.setBaseVersion(c.baseVersion());
    e.setStatus(r.status());
    e.setResultJson(r.resultJson());
    e.setErrorCode(r.errorCode());
    e.setCreatedAt(now);
    commandMapper.insert(e);
  }

  private NotificationPreference preference(NotificationPreferenceEntity e) {
    return NotificationPreference.rehydrate(
        e.getUserId(),
        e.getScene(),
        readChannels(e.getChannelsJson()),
        e.getQuietStart(),
        e.getQuietEnd(),
        e.getTimezone(),
        e.getVersion(),
        e.getUpdatedAt());
  }

  private NotificationPreferenceEntity preferenceEntity(NotificationPreference p) {
    NotificationPreferenceEntity e = new NotificationPreferenceEntity();
    e.setId(p.getUserId() + ":" + p.getScene());
    e.setUserId(p.getUserId());
    e.setScene(p.getScene());
    e.setChannelsJson(write(p.getChannels()));
    e.setQuietStart(p.getQuietStart());
    e.setQuietEnd(p.getQuietEnd());
    e.setTimezone(p.getTimezone());
    e.setVersion(p.getVersion());
    e.setUpdatedAt(p.getUpdatedAt());
    return e;
  }

  private NotificationTask task(NotificationTaskEntity e) {
    return NotificationTask.rehydrate(
        e.getId(),
        e.getDedupeKey(),
        e.getRecipientUserId(),
        NotificationChannel.valueOf(e.getChannel()),
        e.getScene(),
        e.getResourceType(),
        e.getResourceId(),
        e.getPayloadJson(),
        e.getScheduledAt().toInstant(ZoneOffset.UTC),
        NotificationTaskStatus.valueOf(e.getStatus()),
        e.getAttemptCount(),
        e.getLastError(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private NotificationTaskEntity taskEntity(NotificationTask t) {
    NotificationTaskEntity e = new NotificationTaskEntity();
    e.setId(t.getId());
    e.setDedupeKey(t.getDedupeKey());
    e.setRecipientUserId(t.getRecipientUserId());
    e.setChannel(t.getChannel().name());
    e.setScene(t.getScene());
    e.setResourceType(t.getResourceType());
    e.setResourceId(t.getResourceId());
    e.setPayloadJson(t.getPayloadJson());
    e.setScheduledAt(LocalDateTime.ofInstant(t.getScheduledAt(), ZoneOffset.UTC));
    e.setStatus(t.getStatus().name());
    e.setAttemptCount(t.getAttemptCount());
    e.setLastError(t.getLastError());
    e.setCreatedAt(t.getCreatedAt());
    e.setUpdatedAt(t.getUpdatedAt());
    return e;
  }

  private CalendarBinding calendar(CalendarBindingEntity e) {
    return CalendarBinding.rehydrate(
        e.getId(),
        e.getRequestKey(),
        e.getUserId(),
        e.getProvider(),
        e.getCredentialReference(),
        Boolean.TRUE.equals(e.getDeleteCreatedEvents()),
        CalendarBinding.Status.valueOf(e.getStatus()),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private CalendarBindingEntity calendarEntity(CalendarBinding b) {
    CalendarBindingEntity e = new CalendarBindingEntity();
    e.setId(b.getId());
    e.setRequestKey(b.getRequestKey());
    e.setUserId(b.getUserId());
    e.setProvider(b.getProvider());
    e.setCredentialReference(b.getCredentialReference());
    e.setStatus(b.getStatus().name());
    e.setDeleteCreatedEvents(b.isDeleteCreatedEvents());
    e.setVersion(b.getVersion());
    e.setCreatedAt(b.getCreatedAt());
    e.setUpdatedAt(b.getUpdatedAt());
    return e;
  }

  private Set<NotificationChannel> readChannels(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Set<NotificationChannel>>() {});
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
