import type { ApiClient, LongId, PageResult } from '../http';
import type {
  AchievementResult,
  CheckInResult,
  CheckInResultType,
  GoalResult,
  OccurrenceResult,
  PlanActionDraft,
  PlanDraftResult,
  PlanMilestoneDraft,
  ReviewResult
} from '../types';

/** 创建目标请求体，对应 GoalController.CreateGoalBody。 */
export interface CreateGoalBody {
  title: string;
  successCriteria: string;
}

/** 计划草案请求体，对应 GoalController.PlanDraftBody。 */
export interface PlanDraftBody {
  planSnapshotJson: string;
  adjustmentReason: string | null;
  source: string;
  milestones: PlanMilestoneDraft[];
  actions: PlanActionDraft[];
}

/** 确认已存在草案的请求体，对应 GoalController.ConfirmDraftBody。 */
export interface ConfirmDraftBody {
  expectedGoalVersion: LongId;
}

/** 直接确认计划版本的请求体，对应 GoalController.ConfirmPlanBody。 */
export interface ConfirmPlanBody {
  expectedGoalVersion: LongId;
  planSnapshotJson: string;
  adjustmentReason: string | null;
  milestones: PlanMilestoneDraft[];
  actions: PlanActionDraft[];
}

/** 打卡请求体，对应 GoalController.CheckInBody。 */
export interface CheckInBody {
  result: CheckInResultType;
  note: string | null;
  evidenceReference: string | null;
  correction: boolean;
}

/** 复盘完成请求体，对应 GoalController.ReviewBody。 */
export interface ReviewBody {
  conclusionJson: string;
}

/** 目标、计划、行动、打卡、复盘与成就接口客户端。 */
export class GoalApi {
  constructor(private readonly client: ApiClient) {}

  /** 创建 DRAFT 目标。 */
  createGoal(body: CreateGoalBody, idempotencyKey: string): Promise<GoalResult> {
    return this.client.send<GoalResult>('/api/v1/goals', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 查询本人目标。 */
  getGoal(goalId: LongId): Promise<GoalResult> {
    return this.client.send<GoalResult>(`/api/v1/goals/${goalId}`);
  }

  /** 查询本人全部目标，供多目标列表使用。 */
  listGoals(): Promise<GoalResult[]> {
    return this.client.send<GoalResult[]>('/api/v1/goals');
  }

  /** 保存待用户确认的计划草案，不改变当前生效计划。 */
  savePlanDraft(goalId: LongId, body: PlanDraftBody, idempotencyKey: string): Promise<PlanDraftResult> {
    return this.client.send<PlanDraftResult>(`/api/v1/goals/${goalId}/plan-drafts`, {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 原子激活既有计划草案。 */
  confirmPlanDraft(planVersionId: LongId, body: ConfirmDraftBody, idempotencyKey: string): Promise<GoalResult> {
    return this.client.send<GoalResult>(`/api/v1/plans/${planVersionId}/confirm`, {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 直接确认用户提交的计划版本。 */
  confirmPlan(goalId: LongId, body: ConfirmPlanBody, idempotencyKey: string): Promise<GoalResult> {
    return this.client.send<GoalResult>(`/api/v1/goals/${goalId}/plan-confirmations`, {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 查询日期窗口内的行动实例。 */
  listOccurrences(fromDate: string, toDate: string): Promise<OccurrenceResult[]> {
    return this.client.send<OccurrenceResult[]>('/api/v1/action-occurrences', {
      query: { from: fromDate, to: toDate }
    });
  }

  /** 幂等打卡；修正已有打卡时必须显式声明 correction。 */
  checkIn(occurrenceId: LongId, body: CheckInBody, idempotencyKey: string): Promise<CheckInResult> {
    return this.client.send<CheckInResult>(`/api/v1/action-occurrences/${occurrenceId}/check-ins`, {
      method: 'PUT',
      body,
      idempotencyKey
    });
  }

  /** 完成周期复盘。 */
  completeReview(reviewId: LongId, body: ReviewBody, idempotencyKey: string): Promise<ReviewResult> {
    return this.client.send<ReviewResult>(`/api/v1/reviews/${reviewId}/complete`, {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 分页查询本人复盘，可按目标过滤。 */
  listReviews(
    goalId: LongId | null = null,
    page = 1,
    pageSize = 20
  ): Promise<PageResult<ReviewResult>> {
    return this.client.send<PageResult<ReviewResult>>('/api/v1/reviews', {
      query: { goalId, page, pageSize }
    });
  }

  /** 查询单条复盘详情。 */
  getReview(reviewId: LongId): Promise<ReviewResult> {
    return this.client.send<ReviewResult>(`/api/v1/reviews/${reviewId}`);
  }

  /** 分页查询本人成就，可按目标过滤。 */
  listAchievements(goalId: LongId | null, page = 1, pageSize = 20): Promise<PageResult<AchievementResult>> {
    return this.client.send<PageResult<AchievementResult>>('/api/v1/achievements', {
      query: { goalId, page, pageSize }
    });
  }
}
