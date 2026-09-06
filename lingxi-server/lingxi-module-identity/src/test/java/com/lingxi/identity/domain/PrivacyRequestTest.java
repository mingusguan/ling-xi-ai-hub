package com.lingxi.identity.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.identity.api.*;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PrivacyRequestTest {
  @Test
  void shouldAdvanceRecoverableProgressAndComplete() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    PrivacyRequest request =
        PrivacyRequest.create(1, "key", "digest", 2, PrivacyRequestType.DELETE_DATA, "{}", now);
    request.start(now.plusMinutes(1));
    request.checkpoint(50, now.plusMinutes(2));
    request.complete(null, now.plusMinutes(3));
    assertThat(request.getStatus()).isEqualTo(PrivacyRequestStatus.COMPLETED);
    assertThat(request.getProgress()).isEqualTo(100);
    assertThat(request.getVersion()).isEqualTo(3);
  }

  @Test
  void shouldRejectProgressRollback() {
    LocalDateTime now = LocalDateTime.now();
    PrivacyRequest request =
        PrivacyRequest.create(1, "key", "digest", 2, PrivacyRequestType.EXPORT, "{}", now);
    request.start(now);
    request.checkpoint(80, now);
    assertThatThrownBy(() -> request.checkpoint(20, now))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("PRIVACY_INVALID_PROGRESS");
  }

  @Test
  void closeAccountCanOnlyBeCancelledBeforeProcessingStarts() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    PrivacyRequest request =
        PrivacyRequest.create(2, "close", "digest", 3, PrivacyRequestType.CLOSE_ACCOUNT, "{}", now);
    request.cancel(now.plusMinutes(1));
    assertThat(request.getStatus()).isEqualTo(PrivacyRequestStatus.CANCELLED);
  }

  @Test
  void correctionOnlyCompletesFromItsBoundManualTicket() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    PrivacyRequest request =
        PrivacyRequest.create(3, "correction", "digest", 4, PrivacyRequestType.CORRECTION, "{}", now);
    request.start(now.plusMinutes(1));
    request.awaitManual("support-ticket:88", now.plusMinutes(2));

    assertThatThrownBy(
            () -> request.completeManual("support-ticket:99", now.plusMinutes(3)))
        .isInstanceOf(BusinessException.class);

    request.completeManual("support-ticket:88", now.plusMinutes(4));
    assertThat(request.getStatus()).isEqualTo(PrivacyRequestStatus.COMPLETED);
  }
}
