package com.lingxi.operations.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lingxi.engagement.api.EngagementFacade;
import com.lingxi.operations.domain.SafetyAlertDeliveryRepository;
import com.lingxi.operations.domain.SafetyAlertDeliveryRepository.SafetyAlertDelivery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SafetyAlertDispatchServiceTest {

  @Mock private SafetyAlertDeliveryRepository repository;
  @Mock private EngagementFacade engagement;
  private SafetyAlertDispatchService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new SafetyAlertDispatchService(repository, engagement);
  }

  @Test
  void shouldDeliverGuardianAlertWithMinimumDisclosure() {
    SafetyAlertDelivery alert =
        new SafetyAlertDelivery(11, 22, "GUARDIAN_INBOX", "USER:33", "PENDING", 0, null, null, 4);
    when(repository.findDue(any(), any(), anyInt())).thenReturn(List.of(alert));
    when(repository.claim(eq(11L), eq(4L), any(), any())).thenReturn(true);
    when(engagement.scheduleNotification(any())).thenReturn(99L);

    service.dispatch();

    var command =
        ArgumentCaptor.forClass(com.lingxi.engagement.api.ScheduleNotificationCommand.class);
    verify(engagement).scheduleNotification(command.capture());
    org.assertj.core.api.Assertions.assertThat(command.getValue().recipientUserId()).isEqualTo(33);
    org.assertj.core.api.Assertions.assertThat(command.getValue().payloadJson())
        .isEqualTo("{\"disclosure\":\"MINIMUM\"}");
    verify(repository)
        .markSent(org.mockito.ArgumentMatchers.eq(11L), org.mockito.ArgumentMatchers.eq(5L), any());
  }

  @Test
  void shouldIgnoreDeliveryCompletionAfterLeaseWasReclaimed() {
    SafetyAlertDelivery alert =
        new SafetyAlertDelivery(11, 22, "GUARDIAN_INBOX", "USER:33", "PENDING", 0, null, null, 4);
    when(repository.findDue(any(), any(), anyInt())).thenReturn(List.of(alert));
    when(repository.claim(eq(11L), eq(4L), any(), any())).thenReturn(true);
    when(engagement.scheduleNotification(any())).thenReturn(99L);
    when(repository.markSent(
            org.mockito.ArgumentMatchers.eq(11L), org.mockito.ArgumentMatchers.eq(5L), any()))
        .thenReturn(false);

    service.dispatch();

    verify(repository, never()).markFailed(anyLong(), anyLong(), any(), any(), any());
  }
}
