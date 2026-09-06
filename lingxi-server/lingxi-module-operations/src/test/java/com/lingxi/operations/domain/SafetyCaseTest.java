package com.lingxi.operations.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SafetyCaseTest {

  @Test
  void shouldRequireResolutionWhenResolvingCase() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 10, 0);
    SafetyCase safetyCase = SafetyCase.rehydrate(
        1L, "SC-001", "SELF_HARM", "HIGH", "REVIEWING", null, null, 3L, now);

    assertThatThrownBy(() -> safetyCase.transition(9L, "RESOLVED", " ", 3L, now.plusMinutes(1)))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("OPS_SAFETY_RESOLUTION_REQUIRED");

    safetyCase.transition(9L, "RESOLVED", "已完成人工复核并通知用户", 3L, now.plusMinutes(2));

    assertThat(safetyCase.getStatus()).isEqualTo(SafetyCase.Status.RESOLVED);
    assertThat(safetyCase.getReviewerAdminId()).isEqualTo(9L);
    assertThat(safetyCase.getVersion()).isEqualTo(4L);
  }

  @Test
  void shouldRejectStaleVersionAndClosedCaseMutation() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 10, 0);
    SafetyCase reviewing = SafetyCase.rehydrate(
        1L, "SC-001", "ABUSE", "MEDIUM", "REVIEWING", null, null, 2L, now);

    assertThatThrownBy(() -> reviewing.transition(9L, "ESCALATED", null, 1L, now.plusMinutes(1)))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("OPS_SAFETY_CASE_CONFLICT");

    SafetyCase closed = SafetyCase.rehydrate(
        2L, "SC-002", "ABUSE", "LOW", "CLOSED", 9L, "已关闭", 1L, now);
    assertThatThrownBy(() -> closed.transition(9L, "REVIEWING", null, 1L, now.plusMinutes(1)))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("OPS_SAFETY_STATE_INVALID");
  }
}
