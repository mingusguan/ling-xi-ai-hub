package com.lingxi.goal.api;

import java.util.List;

/** 保存 Agent 或用户生成的待确认计划快照。 */
public record SavePlanDraftCommand(
    String requestKey,
    long userId,
    long goalId,
    String planSnapshotJson,
    String adjustmentReason,
    String source,
    List<PlanMilestoneDraft> milestones,
    List<PlanActionDraft> actions) {
  public SavePlanDraftCommand {
    milestones = milestones == null ? List.of() : List.copyOf(milestones);
    actions = actions == null ? List.of() : List.copyOf(actions);
  }
}
