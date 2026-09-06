package com.lingxi.engagement.application;

import com.lingxi.engagement.api.*;
import java.io.IOException;
import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.*;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 为在线客户端持续补发增量变更；断线后仍以 Last-Event-ID 从数据库事实恢复。 */
@Service
public class SyncEventStreamService {
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
  private static final int HEARTBEAT_EVERY_POLLS = 8;
  private static final long CONNECTION_TIMEOUT_MILLIS = Duration.ofMinutes(5).toMillis();

  private final EngagementFacade engagement;
  private final TaskScheduler scheduler;

  public SyncEventStreamService(EngagementFacade engagement, TaskScheduler scheduler) {
    this.engagement = engagement;
    this.scheduler = scheduler;
  }

  public SseEmitter open(long userId, long lastEventId) {
    SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MILLIS);
    StreamState state = new StreamState(userId, lastEventId, emitter);
    emitter.onCompletion(state::close);
    emitter.onTimeout(state::close);
    emitter.onError(ignored -> state.close());
    scheduleNext(state, Duration.ZERO);
    return emitter;
  }

  private void scheduleNext(StreamState state, Duration delay) {
    if (state.closed.get()) return;
    scheduler.schedule(() -> poll(state), Instant.now().plus(delay));
  }

  private void poll(StreamState state) {
    if (state.closed.get()) return;
    try {
      SyncPageResult page = engagement.getChanges(state.userId, state.cursor.get(), List.of(), 200);
      for (SyncChangeResult change : page.changes()) {
        state.emitter.send(
            SseEmitter.event().id(Long.toString(change.sequence())).name("change").data(change));
        state.cursor.set(change.sequence());
      }
      if (state.pollCount.incrementAndGet() % HEARTBEAT_EVERY_POLLS == 0) {
        state.emitter.send(SseEmitter.event().comment("heartbeat"));
      }
      scheduleNext(state, POLL_INTERVAL);
    } catch (IOException | RuntimeException error) {
      state.close();
      state.emitter.completeWithError(error);
    }
  }

  private static final class StreamState {
    private final long userId;
    private final AtomicLong cursor;
    private final AtomicInteger pollCount = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final SseEmitter emitter;

    private StreamState(long userId, long cursor, SseEmitter emitter) {
      this.userId = userId;
      this.cursor = new AtomicLong(cursor);
      this.emitter = emitter;
    }

    private void close() {
      closed.set(true);
    }
  }
}
