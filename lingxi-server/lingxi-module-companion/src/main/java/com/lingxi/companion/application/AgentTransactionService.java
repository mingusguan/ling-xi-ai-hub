package com.lingxi.companion.application;

import com.lingxi.companion.api.SafetyRiskDetectedEvent;
import com.lingxi.companion.domain.*;
import com.lingxi.identity.api.AccessProfile;
import com.lingxi.kernel.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Agent 本地状态、消息和事件的短事务边界。 */
@Service
public class AgentTransactionService {
  private final AgentRepository repository;
  private final IdGenerator ids;
  private final DomainEventPublisher events;

  public AgentTransactionService(
      AgentRepository repository, IdGenerator ids, DomainEventPublisher events) {
    this.repository = repository;
    this.ids = ids;
    this.events = events;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AgentRepository.ConversationSnapshot createConversation(
      String requestKey, long userId, String scene, String title) {
    AgentRepository.ConversationSnapshot existing =
        repository.findConversationByRequestKey(requestKey).orElse(null);
    if (existing != null) {
      if (existing.userId() != userId
          || !existing.scene().equals(scene)
          || !existing.title().equals(title)) {
        throw new BusinessException("AGENT_IDEMPOTENCY_CONFLICT", "幂等键对应不同会话");
      }
      return existing;
    }
    LocalDateTime now = now();
    AgentRepository.ConversationSnapshot conversation =
        new AgentRepository.ConversationSnapshot(
            ids.nextId(), publicId(), requestKey, userId, scene, title, "ACTIVE", now, now);
    repository.insertConversation(conversation);
    return conversation;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AgentRun createRun(
      String requestKey,
      String requestDigest,
      long conversationId,
      long userId,
      String scene,
      long authorizationVersion,
      String input,
      String attachmentContext) {
    AgentRun existing = repository.findRunByRequestKey(requestKey).orElse(null);
    if (existing != null) {
      if (existing.getUserId() != userId
          || existing.getConversationId() != conversationId
          || !existing.getRequestDigest().equals(requestDigest)) {
        throw new BusinessException("AGENT_IDEMPOTENCY_CONFLICT", "幂等键对应不同 Agent 请求");
      }
      return existing;
    }
    LocalDateTime now = now();
    AgentRun run =
        AgentRun.create(
            ids.nextId(),
            publicId(),
            requestKey,
            requestDigest,
            conversationId,
            userId,
            scene,
            authorizationVersion,
            now);
    repository.insertRun(run);
    repository.insertMessage(
        ids.nextId(),
        conversationId,
        run.getId(),
        "USER",
        input,
        Digests.sha256(input),
        false,
        now);
    repository.insertMessage(
        ids.nextId(),
        conversationId,
        run.getId(),
        "CONTEXT",
        attachmentContext,
        Digests.sha256(attachmentContext),
        false,
        now);
    event(run.getId(), "QUEUED", "{\"status\":\"QUEUED\"}");
    return run;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AgentRun beginRun(long runId) {
    AgentRun run = run(runId);
    if (run.getStatus() == AgentRun.Status.RUNNING) return run;
    long previous = run.getVersion();
    run.start(previous, now());
    updateRun(run, previous);
    event(runId, "RUNNING", "{\"status\":\"RUNNING\"}");
    return run;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ActionProposal createProposal(
      AgentRun current,
      String toolName,
      String riskLevel,
      String argumentsJson,
      String digest,
      Instant expiresAt,
      String modelVersion,
      String promptVersion) {
    AgentRun run = run(current.getId());
    ActionProposal existing = repository.findPendingProposal(run.getId()).orElse(null);
    if (existing != null) return existing;
    LocalDateTime now = now();
    ActionProposal proposal =
        ActionProposal.create(
            ids.nextId(),
            publicId(),
            run.getId(),
            run.getUserId(),
            toolName,
            riskLevel,
            argumentsJson,
            digest,
            run.getAuthorizationVersion(),
            expiresAt,
            now);
    repository.insertProposal(proposal);
    long previous = run.getVersion();
    run.waitForConfirmation(modelVersion, promptVersion, previous, now);
    updateRun(run, previous);
    event(
        run.getId(),
        "CONFIRMATION_REQUIRED",
        "{\"proposalId\":" + proposal.getId() + ",\"riskLevel\":\"" + riskLevel + "\"}");
    return proposal;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ClaimResult claimProposal(
      long proposalId,
      long userId,
      String decision,
      String digest,
      long expectedVersion,
      AccessProfile profile,
      boolean recentAuthentication) {
    ActionProposal proposal = proposal(proposalId);
    AgentRun run = run(proposal.getRunId());
    run.assertOwner(userId);
    boolean accepted = "ACCEPTED".equals(decision);
    if (!accepted && !"REJECTED".equals(decision)) {
      throw new BusinessException("AGENT_DECISION_INVALID", "确认决定不合法");
    }
    if ("T3".equals(proposal.getRiskLevel()) && !recentAuthentication) {
      throw new BusinessException("AGENT_RECENT_AUTH_REQUIRED", "T3 操作需要近期认证");
    }
    if (proposal.getStatus() == ActionProposal.Status.EXECUTED) {
      proposal.assertExecutionClaim(userId, profile.authorizationVersion(), digest);
      return new ClaimResult(run, proposal, true, true);
    }
    if (proposal.getStatus() == ActionProposal.Status.EXECUTING) {
      proposal.assertExecutionClaim(userId, profile.authorizationVersion(), digest);
      return new ClaimResult(run, proposal, true, false);
    }
    long proposalPrevious = proposal.getVersion();
    proposal.decide(
        userId,
        profile.authorizationVersion(),
        digest,
        accepted,
        expectedVersion,
        Instant.now(),
        now());
    updateProposal(proposal, proposalPrevious);
    long runPrevious = run.getVersion();
    if (proposal.getStatus() == ActionProposal.Status.EXPIRED) {
      run.fail("AGENT_PROPOSAL_EXPIRED", false, runPrevious, now());
      updateRun(run, runPrevious);
      event(run.getId(), "PROPOSAL_EXPIRED", "{\"proposalId\":" + proposalId + "}");
      accepted = false;
    } else if (accepted) {
      run.resumeAfterConfirmation(runPrevious, now());
      updateRun(run, runPrevious);
      event(run.getId(), "PROPOSAL_ACCEPTED", "{\"proposalId\":" + proposalId + "}");
    } else {
      run.fail("AGENT_PROPOSAL_REJECTED", false, runPrevious, now());
      updateRun(run, runPrevious);
      event(run.getId(), "PROPOSAL_REJECTED", "{\"proposalId\":" + proposalId + "}");
    }
    return new ClaimResult(run, proposal, accepted, false);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AgentRun completeProposal(
      long proposalId,
      String resultJson,
      String userMessage,
      String modelVersion,
      String promptVersion) {
    ActionProposal proposal = proposal(proposalId);
    AgentRun run = run(proposal.getRunId());
    if (proposal.getStatus() == ActionProposal.Status.EXECUTED
        && run.getStatus() == AgentRun.Status.SUCCEEDED) return run;
    long proposalPrevious = proposal.getVersion();
    proposal.complete(resultJson, proposalPrevious, now());
    updateProposal(proposal, proposalPrevious);
    return succeed(run, userMessage, modelVersion, promptVersion);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AgentRun completeDirect(
      long runId, String resultText, String modelVersion, String promptVersion) {
    return succeed(run(runId), resultText, modelVersion, promptVersion);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AgentRun completeSafety(long runId, AgentSafetyPolicy.SafetyDecision decision) {
    AgentRun run = run(runId);
    long safetyEventId = ids.nextId();
    repository.insertSafetyEvent(
        safetyEventId,
        runId,
        run.getUserId(),
        decision.riskType(),
        decision.riskLevel(),
        decision.disclosureScope(),
        now());
    events.publish(
        new SafetyRiskDetectedEvent(
            java.util.UUID.randomUUID().toString(),
            safetyEventId,
            runId,
            run.getUserId(),
            decision.riskType(),
            decision.riskLevel(),
            decision.disclosureScope(),
            Instant.now()));
    event(runId, "SAFETY_INTERRUPTED", "{\"riskLevel\":\"" + decision.riskLevel() + "\"}");
    return succeed(run, decision.safeResponse(), "SAFETY_POLICY", "LOCAL_V1");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void fail(long runId, String code, boolean retryable) {
    AgentRun run = run(runId);
    if (run.getStatus() == AgentRun.Status.SUCCEEDED
        || run.getStatus() == AgentRun.Status.FAILED_FINAL) return;
    long previous = run.getVersion();
    run.fail(code, retryable, previous, now());
    updateRun(run, previous);
    event(
        runId,
        retryable ? "FAILED_RETRYABLE" : "FAILED_FINAL",
        "{\"errorCode\":\"" + safeCode(code) + "\"}");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Memory updateMemory(long userId, long memoryId, String content, long expectedVersion) {
    Memory memory = memory(memoryId);
    long previous = memory.getVersion();
    memory.update(userId, content, expectedVersion, now());
    updateMemory(memory, previous);
    return memory;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Memory beginMemoryDeletion(long userId, long memoryId, long expectedVersion) {
    Memory memory = memory(memoryId);
    memory.assertOwner(userId);
    if (memory.getStatus() == Memory.Status.DELETING || memory.getStatus() == Memory.Status.DELETED)
      return memory;
    long previous = memory.getVersion();
    memory.beginDeletion(userId, expectedVersion, now());
    updateMemory(memory, previous);
    return memory;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Memory completeMemoryDeletion(long memoryId) {
    Memory memory = memory(memoryId);
    if (memory.getStatus() == Memory.Status.DELETED) return memory;
    long previous = memory.getVersion();
    memory.completeDeletion(previous, now());
    updateMemory(memory, previous);
    return memory;
  }

  private AgentRun succeed(AgentRun run, String text, String modelVersion, String promptVersion) {
    if (run.getStatus() == AgentRun.Status.SUCCEEDED) return run;
    long previous = run.getVersion();
    run.succeed(text, modelVersion, promptVersion, previous, now());
    updateRun(run, previous);
    repository.insertMessage(
        ids.nextId(),
        run.getConversationId(),
        run.getId(),
        "ASSISTANT",
        text,
        Digests.sha256(text),
        true,
        now());
    event(run.getId(), "SUCCEEDED", "{\"status\":\"SUCCEEDED\"}");
    return run;
  }

  private AgentRun run(long id) {
    return repository
        .findRun(id)
        .orElseThrow(() -> new BusinessException("AGENT_RUN_NOT_FOUND", "Agent 运行不存在"));
  }

  private ActionProposal proposal(long id) {
    return repository
        .findProposal(id)
        .orElseThrow(() -> new BusinessException("AGENT_PROPOSAL_NOT_FOUND", "提案不存在"));
  }

  private Memory memory(long id) {
    return repository
        .findMemory(id)
        .orElseThrow(() -> new BusinessException("AGENT_MEMORY_NOT_FOUND", "记忆不存在"));
  }

  private void updateMemory(Memory memory, long previous) {
    if (!repository.updateMemory(memory, previous)) conflict();
  }

  private void updateRun(AgentRun run, long previous) {
    if (!repository.updateRun(run, previous)) conflict();
  }

  private void updateProposal(ActionProposal proposal, long previous) {
    if (!repository.updateProposal(proposal, previous)) conflict();
  }

  private void event(long runId, String type, String payload) {
    repository.insertEvent(ids.nextId(), runId, type, payload, now());
  }

  private void conflict() {
    throw new BusinessException("AGENT_CONCURRENT_UPDATE", "Agent 状态已被并发修改");
  }

  private String publicId() {
    return java.util.UUID.randomUUID().toString().replace("-", "");
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private String safeCode(String code) {
    return code == null ? "AGENT_FAILED" : code.replaceAll("[^A-Z0-9_]", "");
  }

  public record ClaimResult(
      AgentRun run, ActionProposal proposal, boolean accepted, boolean completed) {}

  static final class Digests {
    static String sha256(String value) {
      try {
        byte[] digest =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(String.valueOf(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest);
      } catch (java.security.NoSuchAlgorithmException exception) {
        throw new IllegalStateException(exception);
      }
    }
  }
}
