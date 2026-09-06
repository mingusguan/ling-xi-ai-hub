package com.lingxi.operations.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.kernel.PublishedEventMessage;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OperationsMetricProjectionEventHandlerTest {

  @Test
  void shouldProjectTotalAndBusinessDimension() {
    JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
    IdGenerator idGenerator = org.mockito.Mockito.mock(IdGenerator.class);
    when(idGenerator.nextId()).thenReturn(101L, 102L);
    OperationsMetricProjectionEventHandler handler =
        new OperationsMetricProjectionEventHandler(
            new ObjectMapper(), jdbcTemplate, idGenerator);
    PublishedEventMessage message =
        new PublishedEventMessage(
            "event-1",
            "identity.user-registered",
            "7",
            0,
            1,
            Instant.parse("2026-08-10T01:02:03Z"),
            "{\"ageBand\":\"TEEN\"}");

    handler.handle(message);

    assertThat(handler.supports("identity.user-registered")).isTrue();
    verify(jdbcTemplate, atLeast(2)).update(anyString(), any(Object[].class));
  }
}
