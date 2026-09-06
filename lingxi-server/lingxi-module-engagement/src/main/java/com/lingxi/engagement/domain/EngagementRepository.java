package com.lingxi.engagement.domain;

import com.lingxi.engagement.api.*;
import java.time.*;
import java.util.*;

/** 触达、日历与同步持久化端口。 */
public interface EngagementRepository {
  Optional<NotificationPreference> findPreference(long userId, String scene);

  void insertPreference(NotificationPreference preference);

  boolean updatePreference(NotificationPreference preference, long previousVersion);

  Optional<NotificationTask> findTaskByDedupeKey(String key);

  void insertTask(NotificationTask task);

  Optional<NotificationTask> findTask(long id);

  boolean updateTask(NotificationTask task, String expectedStatus);

  void insertDelivery(
      long id,
      long taskId,
      int attempt,
      String providerMessageId,
      String result,
      String errorCode,
      LocalDateTime now);

  void insertInbox(
      long id,
      long userId,
      String type,
      String resourceType,
      String resourceId,
      String summary,
      long cursor,
      LocalDateTime now);

  List<NotificationResult> findNotifications(long userId, long cursor, int limit);

  boolean markRead(long userId, long id, LocalDateTime now);

  Optional<CalendarBinding> findCalendarByUserProvider(long userId, String provider);

  Optional<CalendarBinding> findCalendar(long id);

  void insertCalendar(CalendarBinding binding);

  boolean updateCalendar(CalendarBinding binding, long previousVersion);

  Optional<CalendarEventProjection> findCalendarEvent(
      long bindingId, String resourceType, String resourceId);

  List<CalendarEventProjection> findCalendarEvents(long bindingId);

  void upsertCalendarEvent(CalendarEventProjection event);

  void deleteCalendarEvent(long id);

  void insertChange(
      long id,
      long userId,
      long sequence,
      String domain,
      String resourceType,
      String resourceId,
      long resourceVersion,
      String operation,
      String snapshotJson,
      LocalDateTime now);

  List<SyncChangeResult> findChanges(long userId, long cursor, List<String> domains, int limit);

  Optional<OfflineCommandResult> findCommand(long userId, String clientCommandId);

  Optional<String> findCommandDigest(long userId, String clientCommandId);

  void insertCommand(
      long id,
      OfflineSyncCommand command,
      String digest,
      OfflineCommandResult result,
      LocalDateTime now);
}
