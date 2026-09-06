package com.lingxi.companion.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.kernel.BusinessException;
import java.time.*;
import org.junit.jupiter.api.Test;

class AgentDomainTest {
  private final LocalDateTime now = LocalDateTime.of(2026, 8, 6, 1, 0);

  @Test
  void runCannotSkipWaitingConfirmation() {
    AgentRun run = AgentRun.create(1, "r", "key", "digest", 2, 3, "GOAL", 4, now);
    run.start(0, now);
    run.waitForConfirmation("model", "prompt", 1, now);
    assertThatThrownBy(() -> run.succeed("fake", "model", "prompt", 2, now))
        .isInstanceOf(BusinessException.class);
    run.resumeAfterConfirmation(2, now);
    run.succeed("verified", "model", "prompt", 3, now);
    assertThat(run.getStatus()).isEqualTo(AgentRun.Status.SUCCEEDED);
  }

  @Test
  void proposalBindsDigestOwnerAndAuthorizationVersion() {
    ActionProposal proposal =
        ActionProposal.create(
            1,
            "p",
            2,
            3,
            "goal.confirm",
            "T2",
            "{}",
            "digest",
            9,
            Instant.parse("2026-08-06T02:00:00Z"),
            now);
    assertThatThrownBy(
            () ->
                proposal.decide(
                    4, 9, "digest", true, 0, Instant.parse("2026-08-06T01:00:00Z"), now))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                proposal.decide(
                    3, 10, "digest", true, 0, Instant.parse("2026-08-06T01:00:00Z"), now))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                proposal.decide(3, 9, "other", true, 0, Instant.parse("2026-08-06T01:00:00Z"), now))
        .isInstanceOf(BusinessException.class);
    proposal.decide(3, 9, "digest", true, 0, Instant.parse("2026-08-06T01:00:00Z"), now);
    assertThat(proposal.getStatus()).isEqualTo(ActionProposal.Status.EXECUTING);
  }

  @Test
  void proposalExpiresBeforeExecution() {
    ActionProposal proposal =
        ActionProposal.create(
            1,
            "p",
            2,
            3,
            "goal.confirm",
            "T2",
            "{}",
            "digest",
            9,
            Instant.parse("2026-08-06T00:30:00Z"),
            now);
    proposal.decide(3, 9, "digest", true, 0, Instant.parse("2026-08-06T01:00:00Z"), now);
    assertThat(proposal.getStatus()).isEqualTo(ActionProposal.Status.EXPIRED);
  }

  @Test
  void memoryDeletionImmediatelyRemovesReadableContent() {
    Memory memory =
        Memory.rehydrate(
            1, 2, "PREFERENCE", "喜欢早起", "run:1", "PRIVATE", Memory.Status.ACTIVE, 0, now, now);
    memory.beginDeletion(2, 0, now);
    assertThat(memory.getStatus()).isEqualTo(Memory.Status.DELETING);
    assertThat(memory.getContentText()).isEmpty();
    memory.completeDeletion(1, now);
    assertThat(memory.getStatus()).isEqualTo(Memory.Status.DELETED);
  }
}
