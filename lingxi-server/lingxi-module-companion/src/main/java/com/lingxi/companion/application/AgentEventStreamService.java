package com.lingxi.companion.application;

import com.lingxi.companion.api.*;
import java.io.IOException;
import java.time.*;
import java.util.concurrent.atomic.*;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 持续推送已持久化 Agent 事件，断线后按 Last-Event-ID 恢复。 */
@Service
public class AgentEventStreamService {
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
  private static final long TIMEOUT = Duration.ofMinutes(5).toMillis();
  private final CompanionFacade companion;
  private final TaskScheduler scheduler;

  public AgentEventStreamService(CompanionFacade companion, TaskScheduler scheduler) {
    this.companion = companion;
    this.scheduler = scheduler;
  }

  public SseEmitter open(long userId, long runId, long cursor) {
    SseEmitter emitter = new SseEmitter(TIMEOUT);
    StreamState state = new StreamState(userId, runId, cursor, emitter);
    emitter.onCompletion(state::close);
    emitter.onTimeout(state::close);
    emitter.onError(ignored -> state.close());
    schedule(state, Duration.ZERO);
    return emitter;
  }

  private void schedule(StreamState state, Duration delay) {
    if (!state.closed.get()) scheduler.schedule(() -> poll(state), Instant.now().plus(delay));
  }

  private void poll(StreamState state) {
    if (state.closed.get()) return;
    try {
      for (AgentEventResult event :
          companion.listEvents(state.userId, state.runId, state.cursor.get())) {
        state.emitter.send(
            SseEmitter.event()
                .id(Long.toString(event.eventId()))
                .name(event.eventType())
                .data(event));
        state.cursor.set(event.eventId());
      }
      if (state.polls.incrementAndGet() % 8 == 0) {
        state.emitter.send(SseEmitter.event().comment("heartbeat"));
      }
      schedule(state, POLL_INTERVAL);
    } catch (IOException | RuntimeException exception) {
      state.close();
      state.emitter.completeWithError(exception);
    }
  }

  private static final class StreamState {
    private final long userId, runId;
    private final AtomicLong cursor;
    private final AtomicInteger polls = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final SseEmitter emitter;

    private StreamState(long userId, long runId, long cursor, SseEmitter emitter) {
      this.userId = userId;
      this.runId = runId;
      this.cursor = new AtomicLong(cursor);
      this.emitter = emitter;
    }

    private void close() {
      closed.set(true);
    }
  }
}
