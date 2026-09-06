package com.lingxi.engagement.infrastructure.privacy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.engagement.application.CalendarProviderAdapter;
import com.lingxi.engagement.infrastructure.persistence.*;
import com.lingxi.identity.api.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 导出或逻辑删除用户通知、日历、离线命令和同步游标。 */
@Component
public class EngagementPrivacyDataContributor implements PrivacyDataContributor {
  private final CalendarBindingMapper calendarMapper;
  private final CalendarEventBindingMapper eventMapper;
  private final NotificationPreferenceMapper preferenceMapper;
  private final NotificationTaskMapper taskMapper;
  private final NotificationDeliveryMapper deliveryMapper;
  private final InboxMessageMapper inboxMapper;
  private final OfflineCommandMapper commandMapper;
  private final SyncChangeMapper syncMapper;
  private final List<CalendarProviderAdapter> calendarProviders;
  private final ObjectMapper objectMapper;

  public EngagementPrivacyDataContributor(
      CalendarBindingMapper calendarMapper, CalendarEventBindingMapper eventMapper,
      NotificationPreferenceMapper preferenceMapper, NotificationTaskMapper taskMapper,
      NotificationDeliveryMapper deliveryMapper, InboxMessageMapper inboxMapper,
      OfflineCommandMapper commandMapper, SyncChangeMapper syncMapper,
      List<CalendarProviderAdapter> calendarProviders, ObjectMapper objectMapper) {
    this.calendarMapper = calendarMapper;
    this.eventMapper = eventMapper;
    this.preferenceMapper = preferenceMapper;
    this.taskMapper = taskMapper;
    this.deliveryMapper = deliveryMapper;
    this.inboxMapper = inboxMapper;
    this.commandMapper = commandMapper;
    this.syncMapper = syncMapper;
    this.calendarProviders = List.copyOf(calendarProviders);
    this.objectMapper = objectMapper;
  }

  @Override public String moduleName() { return "engagement"; }

  @Override
  @Transactional
  public PrivacyContribution process(PrivacyProcessingContext context) {
    long userId = context.userId();
    PrivacyRequestType type = context.type();
    if (type == PrivacyRequestType.EXPORT) {
      return PrivacyContribution.exported(export(userId));
    }
    if (type != PrivacyRequestType.DELETE_DATA && type != PrivacyRequestType.CLOSE_ACCOUNT) {
      return PrivacyContribution.unchanged();
    }
    List<CalendarBindingEntity> bindings = calendarMapper.selectList(
        Wrappers.<CalendarBindingEntity>lambdaQuery()
            .select(
                CalendarBindingEntity::getId,
                CalendarBindingEntity::getCredentialReference)
            .eq(CalendarBindingEntity::getUserId, userId));
    List<Long> bindingIds = bindings.stream().map(CalendarBindingEntity::getId).toList();
    disconnectExternalCalendars(context, bindings);
    List<Long> taskIds = taskMapper.selectList(
            Wrappers.<NotificationTaskEntity>lambdaQuery().select(NotificationTaskEntity::getId)
                .eq(NotificationTaskEntity::getRecipientUserId, userId))
        .stream().map(NotificationTaskEntity::getId).toList();
    int affectedRows = 0;
    if (!bindingIds.isEmpty()) {
      affectedRows += eventMapper.delete(Wrappers.<CalendarEventBindingEntity>lambdaQuery()
          .in(CalendarEventBindingEntity::getBindingId, bindingIds));
      affectedRows += calendarMapper.deleteByIds(bindingIds);
    }
    if (!taskIds.isEmpty()) {
      affectedRows += deliveryMapper.delete(Wrappers.<NotificationDeliveryEntity>lambdaQuery()
          .in(NotificationDeliveryEntity::getTaskId, taskIds));
      affectedRows += taskMapper.deleteByIds(taskIds);
    }
    affectedRows += preferenceMapper.delete(Wrappers.<NotificationPreferenceEntity>lambdaQuery()
        .eq(NotificationPreferenceEntity::getUserId, userId));
    affectedRows += inboxMapper.delete(Wrappers.<InboxMessageEntity>lambdaQuery()
        .eq(InboxMessageEntity::getUserId, userId));
    affectedRows += commandMapper.delete(Wrappers.<OfflineCommandEntity>lambdaQuery()
        .eq(OfflineCommandEntity::getUserId, userId));
    affectedRows += syncMapper.delete(Wrappers.<SyncChangeEntity>lambdaQuery()
        .eq(SyncChangeEntity::getUserId, userId));
    return PrivacyContribution.deleted(affectedRows);
  }

  private void disconnectExternalCalendars(
      PrivacyProcessingContext context, List<CalendarBindingEntity> bindings) {
    List<String> credentialReferences =
        bindings.stream()
            .map(CalendarBindingEntity::getCredentialReference)
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .toList();
    for (CalendarProviderAdapter provider : calendarProviders) {
      try {
        String receipt =
            provider.logicallyDisconnectUserData(
                context.requestId(), context.userId(), credentialReferences);
        if (receipt == null || receipt.isBlank()) {
          throw new IllegalStateException("日历供应商未返回隔离凭证: " + provider.provider());
        }
      } catch (Exception exception) {
        throw new IllegalStateException("外部日历授权撤销失败: " + provider.provider(), exception);
      }
    }
  }

  private String export(long userId) {
    Map<String, Object> data = new LinkedHashMap<>();
    List<CalendarBindingEntity> calendars = calendarMapper.selectList(
        Wrappers.<CalendarBindingEntity>lambdaQuery()
            .eq(CalendarBindingEntity::getUserId, userId));
    List<Long> bindingIds = calendars.stream().map(CalendarBindingEntity::getId).toList();
    List<NotificationTaskEntity> notifications = taskMapper.selectList(
        Wrappers.<NotificationTaskEntity>lambdaQuery()
            .eq(NotificationTaskEntity::getRecipientUserId, userId));
    List<Long> taskIds = notifications.stream().map(NotificationTaskEntity::getId).toList();
    data.put("calendars", calendars);
    data.put("calendarEvents", bindingIds.isEmpty() ? List.of() : eventMapper.selectList(
        Wrappers.<CalendarEventBindingEntity>lambdaQuery()
            .in(CalendarEventBindingEntity::getBindingId, bindingIds)));
    data.put("preferences", preferenceMapper.selectList(Wrappers.<NotificationPreferenceEntity>lambdaQuery()
        .eq(NotificationPreferenceEntity::getUserId, userId)));
    data.put("notifications", notifications);
    data.put("deliveries", taskIds.isEmpty() ? List.of() : deliveryMapper.selectList(
        Wrappers.<NotificationDeliveryEntity>lambdaQuery()
            .in(NotificationDeliveryEntity::getTaskId, taskIds)));
    data.put("inbox", inboxMapper.selectList(Wrappers.<InboxMessageEntity>lambdaQuery()
        .eq(InboxMessageEntity::getUserId, userId)));
    data.put("offlineCommands", commandMapper.selectList(Wrappers.<OfflineCommandEntity>lambdaQuery()
        .eq(OfflineCommandEntity::getUserId, userId)));
    data.put("syncChanges", syncMapper.selectList(Wrappers.<SyncChangeEntity>lambdaQuery()
        .eq(SyncChangeEntity::getUserId, userId)));
    try { return objectMapper.writeValueAsString(data); }
    catch (JsonProcessingException e) { throw new IllegalStateException("触达数据导出失败", e); }
  }
}
