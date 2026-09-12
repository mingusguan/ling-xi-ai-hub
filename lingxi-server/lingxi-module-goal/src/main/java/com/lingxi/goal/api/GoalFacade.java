package com.lingxi.goal.api;

import java.time.LocalDate;
import java.util.List;

/** 目标模块对其他模块公开的应用门面。 */
public interface GoalFacade {
  /** 创建草稿目标。 */
  GoalResult createGoal(CreateGoalCommand command);

  /** 保存待用户确认的计划草案，不改变当前生效计划。 */
  PlanDraftResult savePlanDraft(SavePlanDraftCommand command);

  /** 原子激活既有计划草案。 */
  GoalResult confirmPlanDraft(ConfirmPlanDraftCommand command);

  /** 原子确认用户直接提交的计划版本、里程碑和行动。 */
  GoalResult confirmPlan(ConfirmPlanCommand command);

  /** 查询用户自己的目标。 */
  GoalResult getGoal(long userId, long goalId);

  /** 查询用户全部目标，供 PC Web 与 HarmonyOS 的多目标列表使用。 */
  List<GoalResult> listGoals(long userId);

  /** 为行动按本地时间语义生成发生实例。 */
  int generateOccurrences(long actionId, LocalDate fromDate, LocalDate toDate);

  /** 查询用户行动实例。 */
  List<OccurrenceResult> listOccurrences(long userId, LocalDate fromDate, LocalDate toDate);

  /** 幂等打卡或显式修正已有打卡。 */
  CheckInResult checkIn(CheckInCommand command);

  /** 完成周期复盘。 */
  ReviewResult completeReview(CompleteReviewCommand command);
}
