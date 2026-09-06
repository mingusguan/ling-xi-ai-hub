package com.lingxi.companion.interfaces;

import com.lingxi.companion.api.*;
import com.lingxi.companion.application.AgentEventStreamService;
import com.lingxi.kernel.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** PC Web 与 HarmonyOS 共用的 Companion Agent API。 */
@RestController
@RequestMapping("/api/v1")
public class CompanionController {
  private final CompanionFacade companion;
  private final AgentEventStreamService streams;

  public CompanionController(CompanionFacade companion, AgentEventStreamService streams) {
    this.companion = companion;
    this.streams = streams;
  }

  @PostMapping("/conversations")
  public ApiResponse<ConversationResult> createConversation(
      @RequestHeader("Idempotency-Key") String requestKey,
      @RequestBody ConversationBody body,
      HttpServletRequest request) {
    return ok(
        companion.createConversation(
            new CreateConversationCommand(requestKey, userId(), body.scene(), body.title())),
        request);
  }

  @PostMapping("/agent-runs")
  public ApiResponse<AgentRunResult> startRun(
      @RequestHeader("Idempotency-Key") String requestKey,
      @RequestBody RunBody body,
      HttpServletRequest request) {
    return ok(
        companion.startRun(
            new StartAgentRunCommand(
                requestKey,
                userId(),
                body.conversationId(),
                body.scene(),
                body.input(),
                body.attachmentFileIds())),
        request);
  }

  @GetMapping("/agent-runs/{runId}")
  public ApiResponse<AgentRunResult> run(@PathVariable long runId, HttpServletRequest request) {
    return ok(companion.getRun(userId(), runId), request);
  }

  @PostMapping("/agent-runs/{runId}/confirmations")
  public ApiResponse<AgentRunResult> confirm(
      @PathVariable long runId, @RequestBody ConfirmationBody body, HttpServletRequest request) {
    return ok(
        companion.confirm(
            new ConfirmProposalCommand(
                userId(),
                runId,
                body.proposalId(),
                body.decision(),
                body.digest(),
                body.expectedVersion(),
                ActorContextHolder.requireUser().recentAuthentication())),
        request);
  }

  @GetMapping("/agent-runs/{runId}/events")
  public SseEmitter events(
      @PathVariable long runId,
      @RequestHeader(value = "Last-Event-ID", defaultValue = "0") long cursor) {
    return streams.open(userId(), runId, cursor);
  }

  @GetMapping("/memories")
  public ApiResponse<List<MemoryResult>> memories(HttpServletRequest request) {
    return ok(companion.listMemories(userId()), request);
  }

  @PutMapping("/memories/{memoryId}")
  public ApiResponse<MemoryResult> updateMemory(
      @PathVariable long memoryId, @RequestBody MemoryBody body, HttpServletRequest request) {
    return ok(
        companion.updateMemory(
            new UpdateMemoryCommand(
                userId(), memoryId, body.contentText(), body.expectedVersion())),
        request);
  }

  @DeleteMapping("/memories/{memoryId}")
  public ApiResponse<MemoryResult> deleteMemory(
      @PathVariable long memoryId, @RequestParam long expectedVersion, HttpServletRequest request) {
    return ok(companion.deleteMemory(userId(), memoryId, expectedVersion), request);
  }

  private long userId() {
    return ActorContextHolder.requireUser().actorId();
  }

  private <T> ApiResponse<T> ok(T value, HttpServletRequest request) {
    return ApiResponse.success(
        value, String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record ConversationBody(String scene, String title) {}

  public record RunBody(
      long conversationId, String scene, String input, List<Long> attachmentFileIds) {}

  public record ConfirmationBody(
      long proposalId, String decision, String digest, long expectedVersion) {}

  public record MemoryBody(String contentText, long expectedVersion) {}
}
