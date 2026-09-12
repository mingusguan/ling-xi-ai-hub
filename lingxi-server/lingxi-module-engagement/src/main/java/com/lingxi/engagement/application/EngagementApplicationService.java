package com.lingxi.engagement.application;

import com.fasterxml.jackson.databind.*;
import com.lingxi.engagement.api.*;
import com.lingxi.engagement.domain.*;
import com.lingxi.goal.api.*;
import com.lingxi.identity.api.*;
import com.lingxi.kernel.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 通知偏好、消息、日历绑定和离线命令应用服务。 */
@Service
public class EngagementApplicationService implements EngagementFacade {
  private final EngagementRepository repository;
  private final IdentityFacade identity;
  private final GoalFacade goals;
  private final AsyncJobScheduler jobs;
  private final IdGenerator ids;
  private final ObjectMapper mapper;

  public EngagementApplicationService(
      EngagementRepository repository,
      IdentityFacade identity,
      GoalFacade goals,
      AsyncJobScheduler jobs,
      IdGenerator ids,
      ObjectMapper mapper) {
    this.repository = repository;
    this.identity = identity;
    this.goals = goals;
    this.jobs = jobs;
    this.ids = ids;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public NotificationPreferenceResult updatePreference(UpdateNotificationPreferenceCommand c) {
    validate(c);
    AccessProfile profile = identity.getAccessProfile(c.userId());
    boolean teen = profile.ageBand() == AgeBand.TEEN;
    LocalDateTime now = now();
    NotificationPreference p = repository.findPreference(c.userId(), c.scene()).orElse(null);
    if (p == null) {
      p =
          NotificationPreference.create(
              c.userId(),
              c.scene(),
              c.channels(),
              c.quietStart(),
              c.quietEnd(),
              c.timezone(),
              teen,
              now);
      repository.insertPreference(p);
    } else {
      long previous = p.getVersion();
      p.update(
          c.channels(), c.quietStart(), c.quietEnd(), c.timezone(), teen, c.expectedVersion(), now);
      if (!repository.updatePreference(p, previous)) {
        throw new BusinessException("ENG_PREFERENCE_CONFLICT", "通知偏好已变化");
      }
    }
    recordChange(
        c.userId(),
        "notification-preference",
        c.scene(),
        p.getVersion(),
        "UPSERT",
        json(result(p)));
    return result(p);
  }

  @Override
  @Transactional
  public long scheduleNotification(ScheduleNotificationCommand c) {
    NotificationTask existing = repository.findTaskByDedupeKey(c.dedupeKey()).orElse(null);
    if (existing != null) {
      return existing.getId();
    }
    NotificationTask task = NotificationTask.schedule(ids.nextId(), c, now());
    repository.insertTask(task);
    jobs.scheduleAt(
        NotificationJobHandler.JOB_TYPE,
        Long.toString(task.getId()),
        "{\"taskId\":" + task.getId() + "}",
        5,
        c.scheduledAt());
    return task.getId();
  }

  @Override
  @Transactional(readOnly = true)
  public List<NotificationResult> listNotifications(long userId, long cursor, int limit) {
    requireAccess(userId);
    return repository.findNotifications(userId, cursor, limit);
  }

  @Override
  @Transactional
  public void markNotificationRead(long userId, long messageId) {
    requireAccess(userId);
    if (repository.markRead(userId, messageId, now())) {
      recordChange(
          userId, "notification", Long.toString(messageId), 1, "UPDATED", "{\"read\":true}");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<CalendarBindingResult> listCalendarBindings(long userId) {
    requireAccess(userId);
    return repository.findCalendarsByUser(userId).stream().map(this::calendar).toList();
  }

  @Override
  @Transactional
  public CalendarBindingResult bindCalendar(CalendarBindingCommand c) {
    requireAccess(c.userId());
    requireRecentAuthentication(c.recentAuthentication());
    CalendarBinding existing =
        repository.findCalendarByUserProvider(c.userId(), c.provider()).orElse(null);
    if (existing != null) {
      if (existing.getStatus() == CalendarBinding.Status.ACTIVE) {
        return calendar(existing);
      }
      long previous = existing.getVersion();
      existing.reactivate(c.credentialReference(), previous, now());
      if (!repository.updateCalendar(existing, previous)) {
        throw new BusinessException("ENG_CALENDAR_CONFLICT", "日历绑定已变化");
      }
      recordChange(
          c.userId(),
          "calendar-binding",
          Long.toString(existing.getId()),
          existing.getVersion(),
          "REACTIVATED",
          json(calendar(existing)));
      return calendar(existing);
    }
    CalendarBinding binding =
        CalendarBinding.activate(
            ids.nextId(), c.requestKey(), c.userId(), c.provider(), c.credentialReference(), now());
    repository.insertCalendar(binding);
    recordChange(
        c.userId(),
        "calendar-binding",
        Long.toString(binding.getId()),
        binding.getVersion(),
        "CREATED",
        json(calendar(binding)));
    return calendar(binding);
  }

  @Override
  @Transactional
  public long projectCalendar(CalendarProjectionCommand c) {
    requireAccess(c.userId());
    CalendarBinding binding =
        repository
            .findCalendar(c.bindingId())
            .orElseThrow(() -> new BusinessException("ENG_CALENDAR_NOT_FOUND", "日历绑定不存在"));
    if (binding.getUserId() != c.userId() || binding.getStatus() != CalendarBinding.Status.ACTIVE) {
      throw new BusinessException("ENG_CALENDAR_REVOKED", "日历绑定不可写");
    }
    if (c.requestKey() == null
        || c.resourceType() == null
        || c.resourceId() == null
        || !("UPSERT".equals(c.operation()) || "DELETE".equals(c.operation()))) {
      throw new BusinessException("ENG_INVALID_CALENDAR_PROJECTION", "日历投影参数不完整");
    }
    return jobs.schedule(CalendarProjectionJobHandler.JOB_TYPE, c.requestKey(), json(c), 5);
  }

  @Override
  @Transactional
  public CalendarBindingResult revokeCalendar(RevokeCalendarCommand command) {
    requireAccess(command.userId());
    requireRecentAuthentication(command.recentAuthentication());
    CalendarBinding b =
        repository
            .findCalendar(command.bindingId())
            .orElseThrow(() -> new BusinessException("ENG_CALENDAR_NOT_FOUND", "日历绑定不存在"));
    if (b.getUserId() != command.userId()) {
      throw new BusinessException("ENG_CALENDAR_FORBIDDEN", "无权操作日历绑定");
    }
    long previous = b.getVersion();
    b.revoke(previous, command.deleteCreatedEvents(), now());
    if (!repository.updateCalendar(b, previous)) {
      throw new BusinessException("ENG_CALENDAR_CONFLICT", "日历绑定已变化");
    }
    if (command.deleteCreatedEvents()) {
      List<AsyncJobRequest> cleanupJobs = new ArrayList<>();
      for (CalendarEventProjection event : repository.findCalendarEvents(command.bindingId())) {
        CalendarProjectionCommand cleanup =
            new CalendarProjectionCommand(
                "calendar-cleanup:" + command.bindingId() + ":" + event.id(),
                command.userId(),
                command.bindingId(),
                event.resourceType(),
                event.resourceId(),
                0,
                null,
                null,
                null,
                "DELETE");
        cleanupJobs.add(
            new AsyncJobRequest(
                CalendarProjectionJobHandler.JOB_TYPE,
                cleanup.requestKey(),
                json(cleanup),
                5,
                Instant.now()));
      }
      jobs.scheduleBatch(cleanupJobs);
    }
    recordChange(
        command.userId(),
        "calendar-binding",
        Long.toString(command.bindingId()),
        b.getVersion(),
        "REVOKED",
        json(calendar(b)));
    return calendar(b);
  }

  private void requireRecentAuthentication(boolean recentAuthentication) {
    if (!recentAuthentication) {
      throw new BusinessException("AUTH_RECENT_AUTHENTICATION_REQUIRED", "日历授权变更需要近期认证");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public SyncPageResult getChanges(long userId, long cursor, List<String> domains, int limit) {
    requireAccess(userId);
    List<SyncChangeResult> changes = repository.findChanges(userId, cursor, domains, limit);
    long next = changes.isEmpty() ? cursor : changes.get(changes.size() - 1).sequence();
    return new SyncPageResult(changes, next, false);
  }

  @Override
  @Transactional
  public OfflineCommandResult applyOfflineCommand(OfflineSyncCommand c) {
    validate(c);
    requireAccess(c.userId());
    String digest = hash(c.commandType() + "|" + c.payloadJson() + "|" + c.baseVersion());
    OfflineCommandResult existed =
        repository.findCommand(c.userId(), c.clientCommandId()).orElse(null);
    if (existed != null) {
      String previous = repository.findCommandDigest(c.userId(), c.clientCommandId()).orElse("");
      if (!previous.equals(digest)) {
        throw new BusinessException("ENG_COMMAND_IDEMPOTENCY_CONFLICT", "同一离线命令标识不能提交不同内容");
      }
      return existed;
    }
    OfflineCommandResult applied = applySupportedCommand(c);
    repository.insertCommand(ids.nextId(), c, digest, applied, now());
    return applied;
  }

  private OfflineCommandResult applySupportedCommand(OfflineSyncCommand c) {
    try {
      JsonNode payload = mapper.readTree(c.payloadJson());
      return switch (c.commandType()) {
        case "GOAL_CREATE" -> {
          GoalResult g =
              goals.createGoal(
                  new CreateGoalCommand(
                      c.clientCommandId(),
                      c.userId(),
                      payload.path("title").asText(),
                      payload.path("successCriteria").asText()));
          yield success(c, json(g));
        }
        case "CHECK_IN" -> {
          CheckInResult x =
              goals.checkIn(
                  new CheckInCommand(
                      c.clientCommandId(),
                      c.userId(),
                      payload.path("occurrenceId").asLong(),
                      CheckInResultType.valueOf(payload.path("result").asText()),
                      payload.path("note").asText(null),
                      payload.path("evidenceReference").asText(null),
                      payload.path("correction").asBoolean(false)));
          yield success(c, json(x));
        }
        case "MARK_NOTIFICATION_READ" -> {
          markNotificationRead(c.userId(), payload.path("messageId").asLong());
          yield success(c, "{}");
        }
        default ->
            new OfflineCommandResult(
                c.clientCommandId(), "REJECTED", null, "ENG_OFFLINE_COMMAND_NOT_ALLOWED");
      };
    } catch (BusinessException e) {
      return new OfflineCommandResult(
          c.clientCommandId(),
          e.getCode().endsWith("CONFLICT") ? "CONFLICT" : "REJECTED",
          null,
          e.getCode());
    } catch (Exception e) {
      throw new BusinessException("ENG_INVALID_COMMAND_PAYLOAD", "离线命令内容不合法");
    }
  }

  private void recordChange(
      long userId,
      String type,
      String resourceId,
      long version,
      String operation,
      String snapshot) {
    long cursor = ids.nextId();
    repository.insertChange(
        ids.nextId(),
        userId,
        cursor,
        "engagement",
        type,
        resourceId,
        version,
        operation,
        snapshot,
        now());
  }

  private OfflineCommandResult success(OfflineSyncCommand c, String json) {
    return new OfflineCommandResult(c.clientCommandId(), "APPLIED", json, null);
  }

  private NotificationPreferenceResult result(NotificationPreference p) {
    return new NotificationPreferenceResult(
        p.getUserId(),
        p.getScene(),
        p.getChannels(),
        p.getQuietStart(),
        p.getQuietEnd(),
        p.getTimezone(),
        p.getVersion());
  }

  private CalendarBindingResult calendar(CalendarBinding b) {
    return new CalendarBindingResult(
        b.getId(), b.getProvider(), b.getStatus().name(), b.getVersion());
  }

  private void requireAccess(long user) {
    if (!identity.getAccessProfile(user).coreFeaturesAllowed()) {
      throw new BusinessException("ENG_ACCESS_DENIED", "当前账号不可使用触达功能");
    }
  }

  private void validate(UpdateNotificationPreferenceCommand c) {
    if (c == null || c.userId() <= 0 || c.scene() == null || c.timezone() == null) {
      throw new BusinessException("ENG_INVALID_PREFERENCE", "通知偏好参数不完整");
    }
  }

  private void validate(OfflineSyncCommand c) {
    if (c == null
        || c.userId() <= 0
        || c.clientCommandId() == null
        || c.deviceId() == null
        || c.commandType() == null
        || c.payloadJson() == null) {
      throw new BusinessException("ENG_INVALID_COMMAND", "离线命令参数不完整");
    }
  }

  private String json(Object v) {
    try {
      return mapper.writeValueAsString(v);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String hash(String v) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }
}
