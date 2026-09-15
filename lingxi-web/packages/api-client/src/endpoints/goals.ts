import type { ApiClient, LongId, PageResult } from '../http';
import type {
  AchievementResult,
  ActionDifficulty,
  ActionExceptionResult,
  ActionExceptionType,
  ActionInput,
  ActionResult,
  CheckInResult,
  CheckInView,
  CheckInResultType,
  EnergyLevel,
  FocusSessionResult,
  MoodLevel,
  PriorityLevel,
  GoalPrivacyLevel,
  GoalClarificationStage,
  GoalQuotaResult,
  GoalResult,
  GoalTransition,
  GoalType,
  OccurrenceResult,
  PlanActionDraft,
  PlanDraftResult,
  PlanMilestoneDraft,
  QuickNoteResult,
  ReviewResult,
  StarterGoalTemplate,
  TodayResult
} from '../types';

/**
 * 目标定义请求体，创建与更新共用，对应 GoalController.CreateGoalBody 与 UpdateGoalBody。
 *
 * 除标题与完成标准外均可省略：服务端会为目标类型补齐「习惯养成」、
 * 为优先级补齐「常规」、为隐私级别补齐「仅自己」。
 */
export interface GoalDefinitionBody {
  title: string;
  description?: string | null;
  successCriteria: string;
  goalType?: GoalType | null;
  startDate?: string | null;
  targetEndDate?: string | null;
  priority?: PriorityLevel | null;
  weeklyAvailableMinutes?: number | null;
  resourceConstraints?: string | null;
  verifiableOutcomes?: string | null;
  privacyLevel?: GoalPrivacyLevel | null;
}

/** 创建目标请求体，对应 GoalController.CreateGoalBody。 */
export type CreateGoalBody = GoalDefinitionBody;

/** 更新目标定义请求体，对应 GoalController.UpdateGoalBody。 */
export interface UpdateGoalBody extends GoalDefinitionBody {
  expectedVersion: LongId;
}

/**
 * 目标生命周期流转请求体，对应 GoalController.TransitionBody。
 *
 * 该接口按目标状态幂等，不需要 Idempotency-Key。
 */
export interface TransitionGoalBody {
  transition: GoalTransition;
  expectedVersion: LongId;
  /** 预计恢复日期，仅 PAUSE 可填，格式 YYYY-MM-DD。 */
  expectedResumeDate?: string | null;
  /** 放弃原因，仅 ABANDON 必填。 */
  abandonReason?: string | null;
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

/**
 * 打卡请求体，对应 GoalController.CheckInBody。
 *
 * FAILED 必须给出 failureReason；非 FAILED 不允许携带 failureReason，服务端会拒绝自相矛盾的组合。
 */
export interface CheckInBody {
  result: CheckInResultType;
  note: string | null;
  evidenceReference: string | null;
  /** 实际耗时，单位分钟。 */
  actualMinutes?: number | null;
  perceivedDifficulty?: ActionDifficulty | null;
  energyLevel?: EnergyLevel | null;
  moodLevel?: MoodLevel | null;
  /** 失败原因；仅 result=FAILED 时允许填写。 */
  failureReason?: string | null;
  correction: boolean;
}

/** 复盘完成请求体，对应 GoalController.ReviewBody。 */
export interface ReviewBody {
  conclusionJson: string;
}

/** 计划外新增行动请求体，对应 GoalController.AddActionBody。 */
export interface AddActionBody {
  milestoneSequence: number | null;
  definition: ActionInput;
}

/** 编辑行动请求体，对应 GoalController.EditActionBody。 */
export interface EditActionBody {
  expectedVersion: LongId;
  definition: ActionInput;
}

/** 复制行动请求体，对应 GoalController.CopyActionBody。 */
export interface CopyActionBody {
  clientKey: string | null;
  title: string | null;
}

/** 移动行动请求体，对应 GoalController.MoveActionBody。 */
export interface MoveActionBody {
  expectedVersion: LongId;
  newStartDate: string;
}

/**
 * 单次调整请求体，对应 GoalController.AdjustOccurrenceBody。
 *
 * SKIP 不得携带 targetDate，RESCHEDULE 必须携带，服务端会拒绝自相矛盾的组合。
 */
export interface AdjustOccurrenceBody {
  type: ActionExceptionType;
  targetDate?: string | null;
  reason?: string | null;
}

/** 开始专注请求体，对应 GoalController.StartFocusBody。 */
export interface StartFocusBody {
  occurrenceId?: LongId | null;
  plannedMinutes?: number | null;
}

/** 专注计时流转动作。 */
export type FocusTransitionAction = 'PAUSE' | 'RESUME' | 'FINISH' | 'ABANDON';

/** 专注计时流转请求体，对应 GoalController.FocusTransitionBody。 */
export interface FocusTransitionBody {
  action: FocusTransitionAction;
  expectedVersion: LongId;
  note?: string | null;
}

/** 快速记录请求体，对应 GoalController.QuickNoteBody。 */
export interface QuickNoteBody {
  content: string;
  moodLevel?: MoodLevel | null;
  timezone: string;
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

  /** 查询当前活跃目标配额。 */
  quota(): Promise<GoalQuotaResult> {
    return this.client.send<GoalQuotaResult>('/api/v1/goal-quota');
  }

  /** 查询入门目标模板目录（PRD ONB-02 的模板入口）。 */
  listStarterTemplates(): Promise<StarterGoalTemplate[]> {
    return this.client.send<StarterGoalTemplate[]>('/api/v1/goal-starter-templates');
  }

  /** 按业务键读取单个入门模板。 */
  getStarterTemplate(templateKey: string): Promise<StarterGoalTemplate> {
    return this.client.send<StarterGoalTemplate>(
      `/api/v1/goal-starter-templates/${encodeURIComponent(templateKey)}`
    );
  }

  /**
   * 推进首目标引导澄清阶段。
   *
   * 由目标乐观版本号保护，重复推进到同一阶段是空操作，因此不要求 Idempotency-Key。
   */
  advanceClarification(
    goalId: LongId,
    stage: GoalClarificationStage,
    expectedVersion: LongId
  ): Promise<GoalResult> {
    return this.client.send<GoalResult>(`/api/v1/goals/${goalId}/clarification`, {
      method: 'POST',
      body: { stage, expectedVersion }
    });
  }

  /**
   * 更新目标定义。
   *
   * 写入由目标乐观版本号保护，重复提交同一份定义结果一致，因此不要求 Idempotency-Key。
   */
  updateGoal(goalId: LongId, body: UpdateGoalBody): Promise<GoalResult> {
    return this.client.send<GoalResult>(`/api/v1/goals/${goalId}`, {
      method: 'PUT',
      body
    });
  }

  /** 目标生命周期流转：暂停、恢复、放弃、归档；按目标状态幂等。 */
  transitionGoal(goalId: LongId, body: TransitionGoalBody): Promise<GoalResult> {
    return this.client.send<GoalResult>(`/api/v1/goals/${goalId}/transitions`, {
      method: 'POST',
      body
    });
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

  /** 查询目标当前生效计划下的全部行动。 */
  listActions(goalId: LongId): Promise<ActionResult[]> {
    return this.client.send<ActionResult[]>(`/api/v1/goals/${goalId}/actions`);
  }

  /** 计划外新增行动；追加到目标当前生效的计划版本上。 */
  addAction(goalId: LongId, body: AddActionBody, idempotencyKey: string): Promise<ActionResult> {
    return this.client.send<ActionResult>(`/api/v1/goals/${goalId}/actions`, {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /**
   * 编辑行动定义，「本次及未来」整体生效。
   *
   * 由行动乐观版本号保护，重复提交同一份定义结果一致，因此不要求 Idempotency-Key。
   */
  editAction(actionId: LongId, body: EditActionBody): Promise<ActionResult> {
    return this.client.send<ActionResult>(`/api/v1/actions/${actionId}`, {
      method: 'PUT',
      body
    });
  }

  /** 取消行动；重复调用是安全的空操作。 */
  cancelAction(actionId: LongId, expectedVersion: LongId): Promise<ActionResult> {
    return this.client.send<ActionResult>(
      `/api/v1/actions/${actionId}?expectedVersion=${expectedVersion}`,
      { method: 'DELETE' }
    );
  }

  /** 复制行动为同计划内的新行动。 */
  copyAction(actionId: LongId, body: CopyActionBody, idempotencyKey: string): Promise<ActionResult> {
    return this.client.send<ActionResult>(`/api/v1/actions/${actionId}/copies`, {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 平移行动开始日期，「本次及未来」整体生效。 */
  moveAction(actionId: LongId, body: MoveActionBody): Promise<ActionResult> {
    return this.client.send<ActionResult>(`/api/v1/actions/${actionId}/moves`, {
      method: 'POST',
      body
    });
  }

  /**
   * 单次调整：只跳过或改期这一次，不改写行动的重复规则。
   *
   * 与「编辑行动」构成 PRD 要求的「单次修改 / 修改未来全部」两条独立路径。
   */
  adjustOccurrence(
    occurrenceId: LongId,
    body: AdjustOccurrenceBody,
    idempotencyKey: string
  ): Promise<OccurrenceResult> {
    return this.client.send<OccurrenceResult>(
      `/api/v1/action-occurrences/${occurrenceId}/adjustments`,
      { method: 'POST', body, idempotencyKey }
    );
  }

  /** 查询目标下已登记的单次调整例外。 */
  listActionExceptions(goalId: LongId): Promise<ActionExceptionResult[]> {
    return this.client.send<ActionExceptionResult[]>(`/api/v1/goals/${goalId}/action-exceptions`);
  }

  // ---- 今日工作台 ----

  /**
   * 今日工作台聚合：今天、逾期、接下来几天分组返回，并带规则化提示与进行中的专注会话。
   *
   * timezone 决定「今天」的判定口径，必须传用户所在时区。
   */
  today(timezone: string, upcomingDays = 7): Promise<TodayResult> {
    return this.client.send<TodayResult>('/api/v1/today', {
      query: { timezone, upcomingDays }
    });
  }

  /** 开始专注会话；已有进行中会话时服务端会拒绝。 */
  startFocus(
    body: StartFocusBody,
    idempotencyKey: string
  ): Promise<FocusSessionResult> {
    return this.client.send<FocusSessionResult>('/api/v1/focus-sessions', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 查询当前进行中的专注会话；空闲时服务端返回 data 为 null。 */
  activeFocus(): Promise<FocusSessionResult | null> {
    return this.client.send<FocusSessionResult | null>('/api/v1/focus-sessions/active');
  }

  /** 专注计时流转：暂停、恢复、结束、作废；由会话乐观版本号保护。 */
  transitionFocus(
    sessionId: LongId,
    body: FocusTransitionBody
  ): Promise<FocusSessionResult> {
    return this.client.send<FocusSessionResult>(
      `/api/v1/focus-sessions/${sessionId}/transitions`,
      { method: 'POST', body }
    );
  }

  /** 新建快速记录；不绑定行动。 */
  createQuickNote(body: QuickNoteBody, idempotencyKey: string): Promise<QuickNoteResult> {
    return this.client.send<QuickNoteResult>('/api/v1/quick-notes', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 查询最近的快速记录，按创建时间倒序。 */
  listQuickNotes(limit = 20): Promise<QuickNoteResult[]> {
    return this.client.send<QuickNoteResult[]>('/api/v1/quick-notes', { query: { limit } });
  }

  /** 删除快速记录；服务端逻辑删除。 */
  deleteQuickNote(noteId: LongId): Promise<void> {
    return this.client.send<void>(`/api/v1/quick-notes/${noteId}`, { method: 'DELETE' });
  }

  /** 幂等打卡；修正已有打卡时必须显式声明 correction。 */
  checkIn(occurrenceId: LongId, body: CheckInBody, idempotencyKey: string): Promise<CheckInResult> {
    return this.client.send<CheckInResult>(`/api/v1/action-occurrences/${occurrenceId}/check-ins`, {
      method: 'PUT',
      body,
      idempotencyKey
    });
  }

  /**
   * 查询行动实例当前有效的打卡记录，用于「更正」入口预填。
   *
   * 未打卡时服务端返回 data 为 null，这是正常状态而不是错误。
   */
  getEffectiveCheckIn(occurrenceId: LongId): Promise<CheckInView | null> {
    return this.client.send<CheckInView | null>(
      `/api/v1/action-occurrences/${occurrenceId}/check-in`
    );
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
