package com.lingxi.platform.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.lingxi.kernel.*;
import com.lingxi.platform.persistence.*;
import java.time.*;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AsyncJobReliabilityTest {
  static {
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), "test"), AsyncJobEntity.class);
  }

  @Test
  void delayedJobPersistsNotBeforeAndIsIdempotent() {
    AsyncJobMapper mapper = mock(AsyncJobMapper.class);
    IdGenerator ids = mock(IdGenerator.class);
    when(ids.nextId()).thenReturn(9L);
    when(mapper.insert(any(AsyncJobEntity.class))).thenReturn(1);
    DatabaseAsyncJobScheduler scheduler = new DatabaseAsyncJobScheduler(mapper, ids);
    Instant notBefore = Instant.parse("2026-08-12T02:00:00Z");
    assertThat(scheduler.scheduleAt("privacy", "request-1", "{}", 5, notBefore)).isEqualTo(9L);
    ArgumentCaptor<AsyncJobEntity> captor = ArgumentCaptor.forClass(AsyncJobEntity.class);
    verify(mapper).insert(captor.capture());
    assertThat(captor.getValue().getNextRetryAt())
        .isEqualTo(LocalDateTime.ofInstant(notBefore, ZoneOffset.UTC));
    assertThat(captor.getValue().getStatus()).isEqualTo(AsyncJobStatus.PENDING.name());
  }

  @Test
  void batchSchedulingUsesOneInsertAndPreservesIdempotencyValidation() {
    AsyncJobMapper mapper = mock(AsyncJobMapper.class);
    IdGenerator ids = mock(IdGenerator.class);
    when(ids.nextId()).thenReturn(10L, 11L);
    when(mapper.selectByBusinessKeys(anyList()))
        .thenReturn(List.of())
        .thenAnswer(invocation -> invocation.getArgument(0));
    DatabaseAsyncJobScheduler scheduler = new DatabaseAsyncJobScheduler(mapper, ids);
    Instant now = Instant.parse("2026-08-12T02:00:00Z");

    scheduler.scheduleBatch(
        List.of(
            new AsyncJobRequest("order-close", "1", "{\"orderId\":1}", 5, now),
            new AsyncJobRequest("order-close", "2", "{\"orderId\":2}", 5, now)));

    verify(mapper)
        .insertBatch(
            argThat(
                jobs ->
                    jobs.size() == 2 && jobs.get(0).getId() == 10L && jobs.get(1).getId() == 11L));
  }

  @Test
  void recoveredLeaseExecutesOnlyForCurrentOwner() throws Exception {
    AsyncJobMapper mapper = mock(AsyncJobMapper.class);
    AsyncJobHandler handler = mock(AsyncJobHandler.class);
    AsyncJobEntity job = new AsyncJobEntity();
    job.setId(1L);
    job.setJobType("privacy");
    job.setBusinessKey("key");
    job.setPayloadJson("{}");
    job.setStatus(AsyncJobStatus.RUNNING.name());
    job.setLeaseOwner("node-a");
    job.setAttemptCount(0);
    job.setMaxAttempts(3);
    job.setCreatedAt(LocalDateTime.now());
    when(mapper.selectById(1L)).thenReturn(job);
    when(handler.supports("privacy")).thenReturn(true);
    when(handler.handle(any())).thenReturn("{}");
    AsyncJobExecutionService service = new AsyncJobExecutionService(mapper, List.of(handler));
    service.execute(1L, "node-b");
    verify(handler, never()).handle(any());
    service.execute(1L, "node-a");
    verify(handler).handle(any());
    verify(mapper).update(isNull(), any());
  }
}
