package com.lingxi.operations.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SupportTicketTest {
  private final LocalDateTime now = LocalDateTime.of(2026, 8, 10, 8, 0);

  @Test
  void shouldFreezeSlaDeadlineFromPriority() {
    SupportTicket urgent = SupportTicket.create(1, "TK1", 2, "SAFETY", "主题", "描述", "URGENT", now);
    SupportTicket normal = SupportTicket.create(3, "TK3", 2, "ACCOUNT", "主题", "描述", "NORMAL", now);

    assertThat(urgent.getSlaDueAt()).isEqualTo(now.plusHours(1));
    assertThat(normal.getSlaDueAt()).isEqualTo(now.plusHours(24));
  }

  @Test
  void shouldRejectUnknownPriority() {
    assertThatThrownBy(() -> SupportTicket.create(
        1, "TK1", 2, "ACCOUNT", "主题", "描述", "UNKNOWN", now))
        .hasMessageContaining("优先级");
  }
}
