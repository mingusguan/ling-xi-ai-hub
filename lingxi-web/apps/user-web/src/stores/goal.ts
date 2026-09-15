import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type {
  AchievementResult,
  ActionExceptionResult,
  ActionInput,
  ActionResult,
  CheckInBody,
  CheckInResult,
  CheckInView,
  FocusSessionResult,
  GoalDefinitionBody,
  GoalQuotaResult,
  GoalResult,
  GoalTransition,
  MoodLevel,
  OccurrenceResult,
  PlanActionDraft,
  PlanMilestoneDraft,
  QuickNoteResult,
  RecurrenceType,
  TodayResult
} from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

/** 计划编辑表单中的行动草稿。 */
export interface ActionForm {
  clientKey: string;
  milestoneSequence: number | null;
  title: string;
  recurrenceType: RecurrenceType;
  weekdays: string[];
  /** 间隔重复的间隔天数；仅 recurrenceType=INTERVAL 时有值。 */
  intervalDays: number | null;
  startDate: string;
  endDate: string | null;
  localTime: string;
  timezone: string;
}

/** 目标、行动执行与成就状态。 */
export const useGoalStore = defineStore('goal', () => {
  const goals = ref<GoalResult[]>([]);
  const currentGoal = ref<GoalResult | null>(null);
  /** 计划版本标识；服务端 long 以字符串输出，禁止 Number 转换。 */
  const pendingPlanVersionId = ref<string | null>(null);
  const occurrences = ref<OccurrenceResult[]>([]);
  const achievements = ref<AchievementResult[]>([]);
  /** 成就总数为服务端 long，按字符串展示。 */
  const achievementTotal = ref<string>('0');
  const loading = ref(false);
  const errorMessage = ref<string | null>(null);
  /** 最近一次打卡新获得的成就，用于即时提示。 */
  const latestAchievements = ref<AchievementResult[]>([]);
  /** 活跃目标配额；用于在创建与激活计划前提示上限。 */
  const quota = ref<GoalQuotaResult | null>(null);
  /** 当前目标生效计划下的行动定义。 */
  const planActions = ref<ActionResult[]>([]);
  /** 当前目标的单次调整例外。 */
  const actionExceptions = ref<ActionExceptionResult[]>([]);
  /** 今日工作台聚合；今天、逾期、接下来几天在此一次性取回。 */
  const today = ref<TodayResult | null>(null);
  /** 进行中的专注会话；空闲时为 null。 */
  const activeFocus = ref<FocusSessionResult | null>(null);
  /** 最近的快速记录。 */
  const quickNotes = ref<QuickNoteResult[]>([]);

  const activeGoals = computed(() =>
    goals.value.filter((goal) => goal.status === 'ACTIVE' || goal.status === 'PENDING_CONFIRMATION')
  );
  /** 草稿、进行中与暂停的目标都属于「仍在推进」，归档与放弃归入历史。 */
  const openGoals = computed(() =>
    goals.value.filter((goal) => goal.status !== 'ARCHIVED' && goal.status !== 'ABANDONED')
  );
  const archivedGoals = computed(() =>
    goals.value.filter((goal) => goal.status === 'ARCHIVED' || goal.status === 'ABANDONED')
  );
  /** 当前能否再激活一个目标；配额未加载时不拦。 */
  const canActivateMore = computed(() => quota.value === null || quota.value.activeCount < quota.value.allowance);
  const todayOccurrences = computed(() => {
    const today = new Date().toISOString().slice(0, 10);
    return occurrences.value.filter((occurrence) => occurrence.localDate === today);
  });

  async function guard<T>(action: () => Promise<T>): Promise<T | null> {
    const session = useSessionStore();
    loading.value = true;
    errorMessage.value = null;
    try {
      await session.ensureFreshToken();
      return await action();
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '请求失败';
      return null;
    } finally {
      loading.value = false;
    }
  }

  /** 加载本人目标列表。 */
  async function loadGoals(): Promise<void> {
    const result = await guard(() => api.goals.listGoals());
    if (result) {
      goals.value = result;
    }
  }

  /** 加载目标详情。 */
  async function loadGoal(goalId: string): Promise<void> {
    const result = await guard(() => api.goals.getGoal(goalId));
    if (result) {
      currentGoal.value = result;
    }
  }

  /** 创建草稿目标；草稿不计入活跃上限。 */
  async function createGoal(definition: GoalDefinitionBody): Promise<GoalResult | null> {
    const created = await guard(() =>
      api.goals.createGoal(definition, api.http.newIdempotencyKey())
    );
    if (created) {
      goals.value = [created, ...goals.value];
    }
    return created;
  }

  /** 加载活跃目标配额。 */
  async function loadQuota(): Promise<void> {
    const result = await guard(() => api.goals.quota());
    if (result) {
      quota.value = result;
    }
  }

  /** 更新目标定义；服务端按乐观版本号保护写入。 */
  async function updateGoal(goalId: string, body: GoalDefinitionBody & { expectedVersion: string }): Promise<GoalResult | null> {
    const updated = await guard(() => api.goals.updateGoal(goalId, body));
    if (updated) {
      currentGoal.value = updated;
      goals.value = goals.value.map((item) => (item.goalId === updated.goalId ? updated : item));
    }
    return updated;
  }

  /**
   * 目标生命周期流转。
   *
   * 服务端按目标状态幂等，重复提交同一动作不会报错，因此不需要幂等键。
   */
  async function transitionGoal(
    goalId: string,
    transition: GoalTransition,
    options: { expectedVersion: string; expectedResumeDate?: string | null; abandonReason?: string | null }
  ): Promise<GoalResult | null> {
    const updated = await guard(() =>
      api.goals.transitionGoal(goalId, {
        transition,
        expectedVersion: options.expectedVersion,
        expectedResumeDate: options.expectedResumeDate ?? null,
        abandonReason: options.abandonReason ?? null
      })
    );
    if (updated) {
      currentGoal.value = updated;
      goals.value = goals.value.map((item) => (item.goalId === updated.goalId ? updated : item));
      // 暂停与恢复会改变活跃目标计数，顺手刷新配额提示。
      await loadQuota();
    }
    return updated;
  }

  /** 直接确认计划并激活目标；里程碑与行动在同一次请求内原子生效。 */
  async function confirmPlan(
    goal: GoalResult,
    milestones: PlanMilestoneDraft[],
    actions: PlanActionDraft[],
    adjustmentReason: string | null
  ): Promise<GoalResult | null> {
    const updated = await guard(() =>
      api.goals.confirmPlan(
        goal.goalId,
        {
          expectedGoalVersion: goal.version,
          planSnapshotJson: JSON.stringify({ source: 'PC_WEB', actionCount: actions.length }),
          adjustmentReason,
          milestones,
          actions
        },
        api.http.newIdempotencyKey()
      )
    );
    if (updated) {
      currentGoal.value = updated;
      goals.value = goals.value.map((item) => (item.goalId === updated.goalId ? updated : item));
    }
    return updated;
  }

  /** 保存待确认的计划草案。 */
  async function savePlanDraft(
    goal: GoalResult,
    milestones: PlanMilestoneDraft[],
    actions: PlanActionDraft[]
  ): Promise<string | null> {
    const draft = await guard(() =>
      api.goals.savePlanDraft(
        goal.goalId,
        {
          planSnapshotJson: JSON.stringify({ source: 'PC_WEB' }),
          adjustmentReason: null,
          source: 'PC_WEB',
          milestones,
          actions
        },
        api.http.newIdempotencyKey()
      )
    );
    if (draft) {
      pendingPlanVersionId.value = draft.planVersionId;
    }
    return draft?.planVersionId ?? null;
  }

  /** 激活已保存的草案。 */
  async function confirmDraft(goal: GoalResult): Promise<GoalResult | null> {
    const planVersionId = pendingPlanVersionId.value;
    if (planVersionId === null) {
      errorMessage.value = '当前没有待确认的计划草案';
      return null;
    }
    const updated = await guard(() =>
      api.goals.confirmPlanDraft(
        planVersionId,
        { expectedGoalVersion: goal.version },
        api.http.newIdempotencyKey()
      )
    );
    if (updated) {
      currentGoal.value = updated;
      pendingPlanVersionId.value = null;
      goals.value = goals.value.map((item) => (item.goalId === updated.goalId ? updated : item));
    }
    return updated;
  }

  /** 加载目标生效计划下的行动定义与单次调整例外。 */
  async function loadPlanActions(goalId: string): Promise<void> {
    const [actions, exceptions] = await Promise.all([
      guard(() => api.goals.listActions(goalId)),
      guard(() => api.goals.listActionExceptions(goalId))
    ]);
    if (actions) {
      planActions.value = actions.filter((item) => item.status !== 'CANCELLED');
    }
    if (exceptions) {
      actionExceptions.value = exceptions;
    }
  }

  /** 编辑行动定义，「本次及未来」整体生效。 */
  async function editAction(
    actionId: string,
    body: { expectedVersion: string; definition: ActionInput }
  ): Promise<ActionResult | null> {
    const updated = await guard(() => api.goals.editAction(actionId, body));
    if (updated) {
      planActions.value = planActions.value.map((item) =>
        item.actionId === updated.actionId ? updated : item
      );
    }
    return updated;
  }

  /** 取消行动；服务端会同时清掉尚未执行的实例。 */
  async function cancelAction(actionId: string, expectedVersion: string): Promise<ActionResult | null> {
    const cancelled = await guard(() => api.goals.cancelAction(actionId, expectedVersion));
    if (cancelled) {
      planActions.value = planActions.value.filter((item) => item.actionId !== cancelled.actionId);
    }
    return cancelled;
  }

  /** 复制行动为同计划内的新行动。 */
  async function copyAction(actionId: string, title: string): Promise<ActionResult | null> {
    const created = await guard(() =>
      api.goals.copyAction(
        actionId,
        { clientKey: `copy-${Date.now()}`, title },
        api.http.newIdempotencyKey()
      )
    );
    if (created) {
      planActions.value = [...planActions.value, created];
    }
    return created;
  }

  /** 平移行动开始日期，「本次及未来」整体生效。 */
  async function moveAction(
    actionId: string,
    expectedVersion: string,
    newStartDate: string
  ): Promise<ActionResult | null> {
    const moved = await guard(() =>
      api.goals.moveAction(actionId, { expectedVersion, newStartDate })
    );
    if (moved) {
      planActions.value = planActions.value.map((item) =>
        item.actionId === moved.actionId ? moved : item
      );
    }
    return moved;
  }

  /**
   * 单次调整：只跳过或改期这一次。
   *
   * 与「编辑行动」是两条独立路径：这里不改写行动定义，服务端只登记一条按日期唯一的例外。
   */
  async function adjustOccurrence(
    occurrenceId: string,
    type: 'SKIP' | 'RESCHEDULE',
    targetDate: string | null,
    reason: string | null
  ): Promise<OccurrenceResult | null> {
    const adjusted = await guard(() =>
      api.goals.adjustOccurrence(
        occurrenceId,
        { type, targetDate, reason },
        api.http.newIdempotencyKey()
      )
    );
    if (adjusted) {
      occurrences.value = occurrences.value
        .filter((item) => item.occurrenceId !== occurrenceId)
        .concat(adjusted.status === 'SCHEDULED' ? [adjusted] : []);
    }
    return adjusted;
  }

  /**
   * 加载今日工作台。
   *
   * 「今天」由服务端按传入时区判定：客户端时区与服务器不同时，仍能看到属于自己那一天的行动。
   */
  async function loadToday(upcomingDays = 7): Promise<void> {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai';
    const result = await guard(() => api.goals.today(zone, upcomingDays));
    if (result) {
      today.value = result;
      activeFocus.value = result.activeFocus;
      quickNotes.value = result.quickNotes;
      // 打卡与单次调整仍走实例接口，这里同步一份，避免两处数据不一致。
      occurrences.value = [...result.overdue, ...result.today, ...result.upcoming];
    }
  }

  /** 开始专注会话；可选关联到某个行动实例。 */
  async function startFocus(
    occurrenceId: string | null,
    plannedMinutes: number | null
  ): Promise<FocusSessionResult | null> {
    const session = await guard(() =>
      api.goals.startFocus({ occurrenceId, plannedMinutes }, api.http.newIdempotencyKey())
    );
    if (session) {
      activeFocus.value = session;
    }
    return session;
  }

  /** 专注计时流转：暂停、恢复、结束、作废。 */
  async function transitionFocus(
    action: 'PAUSE' | 'RESUME' | 'FINISH' | 'ABANDON',
    note: string | null = null
  ): Promise<FocusSessionResult | null> {
    const current = activeFocus.value;
    if (!current) {
      errorMessage.value = '当前没有进行中的专注会话';
      return null;
    }
    const session = await guard(() =>
      api.goals.transitionFocus(current.sessionId, {
        action,
        expectedVersion: current.version,
        note
      })
    );
    if (session) {
      activeFocus.value = session.active ? session : null;
    }
    return session;
  }

  /** 新建快速记录；不绑定行动。 */
  async function createQuickNote(content: string, moodLevel: MoodLevel | null): Promise<QuickNoteResult | null> {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai';
    const note = await guard(() =>
      api.goals.createQuickNote({ content, moodLevel, timezone: zone }, api.http.newIdempotencyKey())
    );
    if (note) {
      quickNotes.value = [note, ...quickNotes.value];
    }
    return note;
  }

  /** 删除快速记录。 */
  async function deleteQuickNote(noteId: string): Promise<void> {
    const done = await guard(async () => {
      await api.goals.deleteQuickNote(noteId);
      return true;
    });
    if (done) {
      quickNotes.value = quickNotes.value.filter((note) => note.noteId !== noteId);
    }
  }

  /** 加载日期窗口内的行动实例。 */
  async function loadOccurrences(fromDate: string, toDate: string): Promise<void> {    const result = await guard(() => api.goals.listOccurrences(fromDate, toDate));
    if (result) {
      occurrences.value = result;
    }
  }

  /** 打卡；响应会带上本次记录的内容与本次新获得的成就。 */
  async function checkIn(
    occurrenceId: string,
    detail: Omit<CheckInBody, 'correction'>,
    correction = false
  ): Promise<CheckInResult | null> {
    const checkIn = await guard(() =>
      api.goals.checkIn(
        occurrenceId,
        { ...detail, correction },
        api.http.newIdempotencyKey()
      )
    );
    if (checkIn) {
      latestAchievements.value = checkIn.newAchievements;
      occurrences.value = occurrences.value.map((occurrence) =>
        occurrence.occurrenceId === occurrenceId
          ? { ...occurrence, status: checkIn.occurrenceStatus }
          : occurrence
      );
      if (currentGoal.value) {
        currentGoal.value = { ...currentGoal.value, progress: checkIn.goalProgress };
      }
      if (checkIn.newAchievements.length > 0) {
        await loadAchievements();
      }
    }
    return checkIn;
  }

  /** 查询当前有效打卡记录，供「更正」入口预填；未打卡时返回 null。 */
  async function loadEffectiveCheckIn(occurrenceId: string): Promise<CheckInView | null> {
    return guard(() => api.goals.getEffectiveCheckIn(occurrenceId));
  }

  /** 加载本人成就。 */
  async function loadAchievements(goalId: string | null = null): Promise<void> {
    const page = await guard(() => api.achievements.list(goalId, 1, 50));
    if (page) {
      achievements.value = page.items;
      achievementTotal.value = page.total;
    }
  }

  function clearLatestAchievements(): void {
    latestAchievements.value = [];
  }

  return {
    goals,
    activeGoals,
    openGoals,
    archivedGoals,
    quota,
    canActivateMore,
    planActions,
    actionExceptions,
    today,
    activeFocus,
    quickNotes,
    currentGoal,
    pendingPlanVersionId,
    occurrences,
    todayOccurrences,
    achievements,
    achievementTotal,
    latestAchievements,
    loading,
    errorMessage,
    loadGoals,
    loadGoal,
    createGoal,
    loadQuota,
    updateGoal,
    transitionGoal,
    loadPlanActions,
    editAction,
    cancelAction,
    copyAction,
    moveAction,
    adjustOccurrence,
    confirmPlan,
    savePlanDraft,
    confirmDraft,
    loadOccurrences,
    loadEffectiveCheckIn,
    loadToday,
    startFocus,
    transitionFocus,
    createQuickNote,
    deleteQuickNote,
    checkIn,
    loadAchievements,
    clearLatestAchievements
  };
});
