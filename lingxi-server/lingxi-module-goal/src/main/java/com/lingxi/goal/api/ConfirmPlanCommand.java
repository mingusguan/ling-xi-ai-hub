package com.lingxi.goal.api;

import java.util.List;

/** 确认不可变计划版本命令。 */
public record ConfirmPlanCommand(
    String requestKey,
    long userId,
    long goalId,
    long expectedGoalVersion,
    String planSnapshotJson,
    String adjustmentReason,
    List<PlanMilestoneDraft> milestones,
    List<PlanActionDraft> actions) {
  public ConfirmPlanCommand {
    milestones = milestones == null ? List.of() : List.copyOf(milestones);
    actions = actions == null ? List.of() : List.copyOf(actions);
  }

  public ConfirmPlanCommand(
      String requestKey,
      long userId,
      long goalId,
      long expectedGoalVersion,
      String planSnapshotJson,
      String adjustmentReason) {
    this(
        requestKey,
        userId,
        goalId,
        expectedGoalVersion,
        planSnapshotJson,
        adjustmentReason,
        List.of(),
        List.of());
  }
}
