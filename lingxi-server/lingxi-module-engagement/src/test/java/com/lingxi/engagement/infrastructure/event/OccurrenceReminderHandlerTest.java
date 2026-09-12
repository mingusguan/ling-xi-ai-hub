package com.lingxi.engagement.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.engagement.api.EngagementFacade;
import com.lingxi.engagement.api.NotificationChannel;
import com.lingxi.engagement.api.ScheduleNotificationCommand;
import com.lingxi.engagement.application.NotificationTaskLifecycleService;
import com.lingxi.engagement.domain.EngagementRepository;
import com.lingxi.engagement.domain.NotificationPreference;
import com.lingxi.engagement.domain.NotificationTask;
import com.lingxi.kernel.PublishedEventMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 行动提醒事件处理器的行为验证。 */
class OccurrenceReminderHandlerTest {

  private static final String FUTURE = "2099-01-01T09:00:00Z";
  private static final String PAST = "2020-01-01T09:00:00Z";

  private final EngagementFacade engagement = mock(EngagementFacade.class);
  private final EngagementRepository repository = mock(EngagementRepository.class);
  private final NotificationTaskLifecycleService lifecycle =
      mock(NotificationTaskLifecycleService.class);
  private final ObjectMapper mapper = new ObjectMapper();

  private OccurrenceReminderHandler handler(boolean enabled, long leadMinutes) {
    return new OccurrenceReminderHandler(
        engagement, repository, lifecycle, mapper, enabled, leadMinutes);
  }

  @Test
  void supportsOnlyOccurrenceEvents() {
    OccurrenceReminderHandler handler = handler(true, 0);
    assertThat(handler.supports("goal.occurrence-scheduled")).isTrue();
    assertThat(handler.supports("goal.action-checked-in")).isTrue();
    assertThat(handler.supports("goal.created")).isFalse();
    assertThat(handler.supports("identity.user-registered")).isFalse();
  }

  @Test
  void createsInboxReminderWhenUserHasNoPreference() {
    when(repository.findPreference(42L, OccurrenceReminderHandler.SCENE))
        .thenReturn(Optional.empty());

    handler(true, 0)
        .handle(
            message(
                "goal.occurrence-scheduled",
                "{\"userId\":42,\"goalId\":99,\"actionId\":7,\"occurrenceId\":555,"
                    + "\"actionTitle\":\"晨跑\",\"scheduledAt\":\""
                    + FUTURE
                    + "\"}"));

    ArgumentCaptor<ScheduleNotificationCommand> captor =
        ArgumentCaptor.forClass(ScheduleNotificationCommand.class);
    verify(engagement).scheduleNotification(captor.capture());
    ScheduleNotificationCommand command = captor.getValue();
    assertThat(command.dedupeKey()).isEqualTo("goal-reminder:555:INBOX");
    assertThat(command.channel()).isEqualTo(NotificationChannel.INBOX);
    assertThat(command.scene()).isEqualTo(OccurrenceReminderHandler.SCENE);
    assertThat(command.resourceType()).isEqualTo(OccurrenceReminderHandler.RESOURCE_TYPE);
    assertThat(command.resourceId()).isEqualTo("555");
    assertThat(command.scheduledAt()).isEqualTo(Instant.parse(FUTURE));
    assertThat(command.payloadJson()).contains("晨跑").contains("\"occurrenceId\":555");
  }

  @Test
  void usesUserPreferredChannels() {
    when(repository.findPreference(42L, OccurrenceReminderHandler.SCENE))
        .thenReturn(
            Optional.of(
                NotificationPreference.create(
                    42,
                    OccurrenceReminderHandler.SCENE,
                    Set.of(NotificationChannel.PUSH),
                    null,
                    null,
                    "Asia/Shanghai",
                    false,
                    LocalDateTime.now())));

    handler(true, 0)
        .handle(
            message(
                "goal.occurrence-scheduled",
                "{\"userId\":42,\"goalId\":99,\"occurrenceId\":1,\"scheduledAt\":\""
                    + FUTURE
                    + "\"}"));

    ArgumentCaptor<ScheduleNotificationCommand> captor =
        ArgumentCaptor.forClass(ScheduleNotificationCommand.class);
    verify(engagement).scheduleNotification(captor.capture());
    assertThat(captor.getValue().channel()).isEqualTo(NotificationChannel.PUSH);
    assertThat(captor.getValue().dedupeKey()).isEqualTo("goal-reminder:1:PUSH");
  }

  @Test
  void appliesLeadMinutes() {
    when(repository.findPreference(42L, OccurrenceReminderHandler.SCENE))
        .thenReturn(Optional.empty());

    handler(true, 30)
        .handle(
            message(
                "goal.occurrence-scheduled",
                "{\"userId\":42,\"goalId\":99,\"occurrenceId\":1,\"scheduledAt\":\""
                    + FUTURE
                    + "\"}"));

    ArgumentCaptor<ScheduleNotificationCommand> captor =
        ArgumentCaptor.forClass(ScheduleNotificationCommand.class);
    verify(engagement).scheduleNotification(captor.capture());
    assertThat(captor.getValue().scheduledAt())
        .isEqualTo(Instant.parse(FUTURE).minusSeconds(1800));
  }

  @Test
  void skipsAlreadyDueOccurrence() {
    handler(true, 0)
        .handle(
            message(
                "goal.occurrence-scheduled",
                "{\"userId\":42,\"goalId\":99,\"occurrenceId\":1,\"scheduledAt\":\""
                    + PAST
                    + "\"}"));

    verify(engagement, never()).scheduleNotification(any());
  }

  @Test
  void skipsWhenReminderDisabled() {
    handler(false, 0)
        .handle(
            message(
                "goal.occurrence-scheduled",
                "{\"userId\":42,\"goalId\":99,\"occurrenceId\":1,\"scheduledAt\":\""
                    + FUTURE
                    + "\"}"));

    verify(engagement, never()).scheduleNotification(any());
  }

  @Test
  void skipsEventWithoutUser() {
    handler(true, 0)
        .handle(
            message(
                "goal.occurrence-scheduled",
                "{\"goalId\":99,\"occurrenceId\":1,\"scheduledAt\":\"" + FUTURE + "\"}"));

    verify(engagement, never()).scheduleNotification(any());
  }

  @Test
  void cancelsPendingRemindersOnCheckIn() {
    NotificationTask pending =
        NotificationTask.schedule(
            9,
            new ScheduleNotificationCommand(
                "goal-reminder:555:INBOX",
                42,
                NotificationChannel.INBOX,
                OccurrenceReminderHandler.SCENE,
                OccurrenceReminderHandler.RESOURCE_TYPE,
                "555",
                "{}",
                Instant.parse(FUTURE)),
            LocalDateTime.now());
    when(repository.findTaskByDedupeKey("goal-reminder:555:INBOX"))
        .thenReturn(Optional.of(pending));

    handler(true, 0)
        .handle(
            message(
                "goal.action-checked-in",
                "{\"userId\":42,\"goalId\":99,\"occurrenceId\":555,\"result\":\"COMPLETED\"}"));

    verify(lifecycle).cancelPending(eq(pending), anyString());
  }

  @Test
  void doesNotTouchRemindersWhenNothingScheduled() {
    when(repository.findTaskByDedupeKey(anyString())).thenReturn(Optional.empty());

    handler(true, 0)
        .handle(
            message(
                "goal.action-checked-in",
                "{\"userId\":42,\"goalId\":99,\"occurrenceId\":555,\"result\":\"SKIPPED\"}"));

    verify(lifecycle, never()).cancelPending(any(), anyString());
  }

  private PublishedEventMessage message(String type, String payloadJson) {
    return new PublishedEventMessage("evt-1", type, "99", 1, 1, Instant.now(), payloadJson);
  }

  /** 载荷中的 occurrenceId 在 JSON 中按字符串序列化时也必须可解析。 */
  @Test
  void readsOccurrenceIdFromStringPayload() throws Exception {
    JsonNode node =
        mapper.readTree(
            "{\"userId\":42,\"goalId\":99,\"occurrenceId\":\"555\",\"scheduledAt\":\""
                + FUTURE
                + "\"}");
    assertThat(node.path("occurrenceId").asLong()).isEqualTo(555L);
  }
}
