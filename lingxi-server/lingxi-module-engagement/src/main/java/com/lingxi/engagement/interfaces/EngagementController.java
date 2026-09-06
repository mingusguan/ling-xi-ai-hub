package com.lingxi.engagement.interfaces;

import com.lingxi.engagement.api.*;
import com.lingxi.engagement.application.SyncEventStreamService;
import com.lingxi.kernel.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalTime;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** PC Web 与 HarmonyOS 共用的同步、通知和日历接口。 */
@RestController
@RequestMapping("/api/v1")
public class EngagementController {
  private final EngagementFacade facade;
  private final SyncEventStreamService streams;

  public EngagementController(EngagementFacade facade, SyncEventStreamService streams) {
    this.facade = facade;
    this.streams = streams;
  }

  @GetMapping("/notifications")
  public ApiResponse<List<NotificationResult>> notifications(
      @RequestParam(defaultValue = "0") long cursor,
      @RequestParam(defaultValue = "50") int limit,
      HttpServletRequest request) {
    return ok(facade.listNotifications(userId(), cursor, limit), request);
  }

  @PutMapping("/notifications/{id}/read")
  public ApiResponse<Void> read(@PathVariable long id, HttpServletRequest request) {
    facade.markNotificationRead(userId(), id);
    return ok(null, request);
  }

  @PutMapping("/notification-preferences")
  public ApiResponse<NotificationPreferenceResult> preference(
      @RequestBody PreferenceBody body, HttpServletRequest request) {
    return ok(
        facade.updatePreference(
            new UpdateNotificationPreferenceCommand(
                userId(),
                body.scene(),
                body.channels(),
                body.quietStart(),
                body.quietEnd(),
                body.timezone(),
                body.expectedVersion())),
        request);
  }

  @PostMapping("/calendar/bindings")
  public ApiResponse<CalendarBindingResult> bind(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody CalendarBody body,
      HttpServletRequest request) {
    return ok(
        facade.bindCalendar(
            new CalendarBindingCommand(
                key,
                userId(),
                body.provider(),
                body.credentialReference(),
                ActorContextHolder.requireUser().recentAuthentication())),
        request);
  }

  @DeleteMapping("/calendar/bindings/{id}")
  public ApiResponse<CalendarBindingResult> revoke(
      @PathVariable long id,
      @RequestParam(defaultValue = "false") boolean deleteCreatedEvents,
      HttpServletRequest request) {
    return ok(
        facade.revokeCalendar(
            new RevokeCalendarCommand(
                userId(),
                id,
                deleteCreatedEvents,
                ActorContextHolder.requireUser().recentAuthentication())),
        request);
  }

  @GetMapping("/sync/changes")
  public ApiResponse<SyncPageResult> changes(
      @RequestParam(defaultValue = "0") long cursor,
      @RequestParam(required = false) List<String> domains,
      @RequestParam(defaultValue = "100") int limit,
      HttpServletRequest request) {
    return ok(facade.getChanges(userId(), cursor, domains, limit), request);
  }

  @PostMapping("/sync/commands")
  public ApiResponse<OfflineCommandResult> command(
      @RequestBody CommandBody body, HttpServletRequest request) {
    return ok(
        facade.applyOfflineCommand(
            new OfflineSyncCommand(
                body.clientCommandId(),
                userId(),
                ActorContextHolder.requireUser().deviceId(),
                body.commandType(),
                body.payloadJson(),
                body.baseVersion())),
        request);
  }

  @GetMapping("/sync/events")
  public SseEmitter events(
      @RequestHeader(value = "Last-Event-ID", defaultValue = "0") long cursor) {
    return streams.open(userId(), cursor);
  }

  private long userId() {
    return ActorContextHolder.requireUser().actorId();
  }

  private <T> ApiResponse<T> ok(T value, HttpServletRequest request) {
    return ApiResponse.success(
        value, String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  /** 通知偏好请求。 */
  public record PreferenceBody(
      String scene,
      Set<NotificationChannel> channels,
      LocalTime quietStart,
      LocalTime quietEnd,
      String timezone,
      long expectedVersion) {}

  /** 日历绑定请求。 */
  public record CalendarBody(String provider, String credentialReference) {}

  /** 离线命令请求。 */
  public record CommandBody(
      String clientCommandId, String commandType, String payloadJson, long baseVersion) {}
}
