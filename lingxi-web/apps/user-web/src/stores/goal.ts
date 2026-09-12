import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type {
  AchievementResult,
  CheckInResult,
  CheckInResultType,
  GoalResult,
  OccurrenceResult,
  PlanActionDraft,
  PlanMilestoneDraft
} from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

/** 计划编辑表单中的行动草稿。 */
export interface ActionForm {
  clientKey: string;
  milestoneSequence: number | null;
  title: string;
  recurrenceType: 'ONCE' | 'DAILY' | 'WEEKLY';
  weekdays: string[];
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

  const activeGoals = computed(() =>
    goals.value.filter((goal) => goal.status === 'ACTIVE' || goal.status === 'PENDING_CONFIRMATION')
  );
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

  /** 创建草稿目标。 */
  async function createGoal(title: string, successCriteria: string): Promise<GoalResult | null> {
    const created = await guard(() =>
      api.goals.createGoal({ title, successCriteria }, api.http.newIdempotencyKey())
    );
    if (created) {
      goals.value = [created, ...goals.value];
    }
    return created;
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

  /** 加载日期窗口内的行动实例。 */
  async function loadOccurrences(fromDate: string, toDate: string): Promise<void> {
    const result = await guard(() => api.goals.listOccurrences(fromDate, toDate));
    if (result) {
      occurrences.value = result;
    }
  }

  /** 打卡；响应会带上本次新获得的成就。 */
  async function checkIn(
    occurrenceId: string,
    result: CheckInResultType,
    note: string | null,
    correction = false
  ): Promise<CheckInResult | null> {
    const checkIn = await guard(() =>
      api.goals.checkIn(
        occurrenceId,
        { result, note, evidenceReference: null, correction },
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
    confirmPlan,
    savePlanDraft,
    confirmDraft,
    loadOccurrences,
    checkIn,
    loadAchievements,
    clearLatestAchievements
  };
});
