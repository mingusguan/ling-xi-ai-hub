<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import type { ActionResult, PlanActionDraft, PlanMilestoneDraft } from '@lingxi/api-client';
import { useGoalStore } from '@/stores/goal';
import {
  RECURRENCE_OPTIONS,
  WEEKDAY_OPTIONS,
  normalizeRecurrence,
  recurrenceLabel,
  validateRecurrence
} from '@/utils/recurrence';
import { ACTION_DIFFICULTY_OPTIONS, PRIORITY_OPTIONS } from '@/utils/goalOptions';

const goalStore = useGoalStore();
const route = useRoute();
const goalId = String(route.params.goalId);

const milestoneForm = reactive({ title: '', successCriteria: '' });
const milestones = ref<PlanMilestoneDraft[]>([]);
const actionForm = reactive({
  title: '',
  description: '',
  recurrenceType: 'DAILY' as PlanActionDraft['recurrenceType'],
  weekdays: [] as string[],
  intervalDays: 3 as number | null,
  localTime: '21:00',
  endLocalTime: '' as string,
  startDate: new Date().toISOString().slice(0, 10),
  endDate: null as string | null,
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai',
  priority: 'NORMAL' as PlanActionDraft['priority'],
  difficulty: 'NORMAL' as PlanActionDraft['difficulty'],
  completionCriteria: '',
  estimatedMinutes: '' as string,
  milestoneSequence: '' as string
});
const actions = ref<PlanActionDraft[]>([]);
const savedDraftId = ref<string | null>(null);
/** 行动表单的即时校验提示，避免把服务端错误码直接抛给用户。 */
const actionError = ref<string | null>(null);
/** 生命周期流转的输入与提示。 */
const resumeDate = ref('');
const abandonReason = ref('');
const lifecycleError = ref<string | null>(null);
const transitioning = ref(false);
/** 生效计划的行动管理：编辑/取消/复制/移动。 */
const actionPlanError = ref<string | null>(null);
const editingActionId = ref<string | null>(null);
const moveDates = reactive<Record<string, string>>({});
const editForm = reactive({
  title: '',
  priority: 'NORMAL' as PlanActionDraft['priority'],
  difficulty: 'NORMAL' as PlanActionDraft['difficulty'],
  completionCriteria: '',
  estimatedMinutes: '' as string,
  localTime: '21:00',
  /** 时间段结束时刻；必须与开始时刻一起可编辑，否则调整开始时刻后会卡在校验错误上。 */
  endLocalTime: ''
});

onMounted(async () => {
  await goalStore.loadGoal(goalId);
  await goalStore.loadPlanActions(goalId);
});

/** 目标是否已生效计划；只有生效后才允许管理行动。 */
const hasActivePlan = computed(() => goalStore.currentGoal?.currentPlanVersionId !== null);

/** 进行中或等待确认计划的目标可以暂停。 */
const canPause = computed(
  () =>
    goalStore.currentGoal?.status === 'ACTIVE' ||
    goalStore.currentGoal?.status === 'PENDING_CONFIRMATION'
);
/** 只有已暂停的目标可以恢复。 */
const canResume = computed(() => goalStore.currentGoal?.status === 'PAUSED');
/** 已结束的目标才允许归档，与领域规则一致：进行中与等待确认计划的目标必须先结束。 */
const canArchive = computed(() => {
  const status = goalStore.currentGoal?.status;
  return status === 'PAUSED' || status === 'COMPLETED' || status === 'DRAFT' || status === 'ABANDONED';
});
/** 已完成、已放弃、已归档的目标不再提供放弃入口。 */
const canAbandon = computed(() => {
  const status = goalStore.currentGoal?.status;
  return status !== 'ABANDONED' && status !== 'ARCHIVED' && status !== 'COMPLETED';
});

/** 执行一次生命周期流转；服务端按目标状态幂等，重复点击是安全的。 */
async function runTransition(
  transition: 'PAUSE' | 'RESUME' | 'ABANDON' | 'ARCHIVE'
): Promise<void> {
  const goal = goalStore.currentGoal;
  if (!goal) {
    return;
  }
  lifecycleError.value = null;
  if (transition === 'ABANDON' && !abandonReason.value.trim()) {
    lifecycleError.value = '放弃目标必须填写原因';
    return;
  }
  transitioning.value = true;
  const updated = await goalStore.transitionGoal(goal.goalId, transition, {
    expectedVersion: goal.version,
    expectedResumeDate: transition === 'PAUSE' ? resumeDate.value || null : null,
    abandonReason: transition === 'ABANDON' ? abandonReason.value.trim() : null
  });
  transitioning.value = false;
  if (!updated) {
    lifecycleError.value = goalStore.errorMessage;
    return;
  }
  if (transition === 'ABANDON') {
    abandonReason.value = '';
  }
  if (transition === 'PAUSE') {
    resumeDate.value = '';
  }
}

/** 生成稳定的行动客户端键，服务端按 clientKey 去重。 */
function nextClientKey(): string {
  return `pc-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
}

/** 仅在「每周（自选星期）」下展示星期勾选，其他重复形式不保留星期集合。 */
const needsWeekdays = computed(() => actionForm.recurrenceType === 'WEEKLY');
/** 仅在「间隔重复」下展示间隔天数输入。 */
const needsIntervalDays = computed(() => actionForm.recurrenceType === 'INTERVAL');

/** 表单当前的重复规则摘要，供用户在提交前确认。 */
const recurrenceSummary = computed(() =>
  recurrenceLabel({
    recurrenceType: actionForm.recurrenceType,
    weekdays: actionForm.weekdays,
    intervalDays: actionForm.intervalDays
  })
);

function toggleWeekday(value: string): void {
  actionForm.weekdays = actionForm.weekdays.includes(value)
    ? actionForm.weekdays.filter((item) => item !== value)
    : [...actionForm.weekdays, value];
}

function addMilestone(): void {
  if (!milestoneForm.title.trim()) {
    return;
  }
  milestones.value = [
    ...milestones.value,
    {
      sequenceNo: milestones.value.length + 1,
      title: milestoneForm.title.trim(),
      successCriteria: milestoneForm.successCriteria.trim() || '按计划完成本阶段行动'
    }
  ];
  milestoneForm.title = '';
  milestoneForm.successCriteria = '';
}

function addAction(): void {
  actionError.value = null;
  if (!actionForm.title.trim()) {
    actionError.value = '请先填写行动标题';
    return;
  }
  const rule = {
    recurrenceType: actionForm.recurrenceType,
    weekdays: actionForm.weekdays,
    intervalDays: actionForm.intervalDays
  };
  const invalid = validateRecurrence(rule);
  if (invalid !== null) {
    actionError.value = invalid;
    return;
  }
  const normalized = normalizeRecurrence(rule);
  actions.value = [
    ...actions.value,
    {
      clientKey: nextClientKey(),
      milestoneSequence: actionForm.milestoneSequence ? Number(actionForm.milestoneSequence) : null,
      title: actionForm.title.trim(),
      description: actionForm.description.trim() || null,
      recurrenceType: normalized.recurrenceType,
      weekdays: normalized.weekdays,
      intervalDays: normalized.intervalDays,
      startDate: actionForm.startDate,
      endDate: actionForm.endDate,
      localTime: `${actionForm.localTime}:00`,
      endLocalTime: actionForm.endLocalTime ? `${actionForm.endLocalTime}:00` : null,
      timezone: actionForm.timezone,
      priority: actionForm.priority,
      difficulty: actionForm.difficulty,
      completionCriteria: actionForm.completionCriteria.trim() || null,
      estimatedMinutes: actionForm.estimatedMinutes ? Number(actionForm.estimatedMinutes) : null,
      prerequisiteClientKey: null,
      reminderPolicy: null
    }
  ];
  actionForm.title = '';
  actionForm.description = '';
  actionForm.completionCriteria = '';
  actionForm.weekdays = [];
  actionForm.intervalDays = 3;
  actionForm.estimatedMinutes = '';
}

function removeAction(clientKey: string): void {
  actions.value = actions.value.filter((item) => item.clientKey !== clientKey);
}

/** 行动在草稿列表中的重复规则摘要。 */
function actionRuleLabel(action: PlanActionDraft): string {
  return recurrenceLabel({
    recurrenceType: action.recurrenceType,
    weekdays: action.weekdays,
    intervalDays: action.intervalDays
  });
}

async function saveDraft(): Promise<void> {
  if (!goalStore.currentGoal) {
    return;
  }
  savedDraftId.value = await goalStore.savePlanDraft(goalStore.currentGoal, milestones.value, actions.value);
}

async function confirm(): Promise<void> {
  if (!goalStore.currentGoal) {
    return;
  }
  await goalStore.confirmPlan(goalStore.currentGoal, milestones.value, actions.value, null);
}

async function confirmSavedDraft(): Promise<void> {
  if (!goalStore.currentGoal) {
    return;
  }
  await goalStore.confirmDraft(goalStore.currentGoal);
}

/** 生效计划下的行动按重复规则展示摘要。 */
function planActionRule(action: ActionResult): string {
  return recurrenceLabel({
    recurrenceType: action.recurrenceType,
    weekdays: action.weekdays,
    intervalDays: action.intervalDays
  });
}

/** 打开某个行动的编辑表单，并用当前值预填。 */
function startEditAction(action: ActionResult): void {
  actionPlanError.value = null;
  editingActionId.value = action.actionId;
  editForm.title = action.title;
  editForm.priority = action.priority;
  editForm.difficulty = action.difficulty;
  editForm.completionCriteria = action.completionCriteria ?? '';
  editForm.estimatedMinutes = action.estimatedMinutes === null ? '' : String(action.estimatedMinutes);
  editForm.localTime = action.localTime.slice(0, 5);
  editForm.endLocalTime = action.endLocalTime === null ? '' : action.endLocalTime.slice(0, 5);
}

function cancelEditAction(): void {
  editingActionId.value = null;
}

/** 保存编辑：只改定义细节与时刻，重复规则沿用原值，避免误改周期。 */
async function saveEditAction(action: ActionResult): Promise<void> {
  actionPlanError.value = null;
  if (!editForm.title.trim()) {
    actionPlanError.value = '行动标题不能为空';
    return;
  }
  const updated = await goalStore.editAction(action.actionId, {
    expectedVersion: action.version,
    definition: {
      title: editForm.title.trim(),
      description: action.description,
      recurrenceType: action.recurrenceType,
      weekdays: action.weekdays,
      intervalDays: action.intervalDays,
      startDate: action.startDate,
      endDate: action.endDate,
      localTime: `${editForm.localTime}:00`,
      endLocalTime: editForm.endLocalTime ? `${editForm.endLocalTime}:00` : null,
      timezone: action.timezone,
      priority: editForm.priority,
      difficulty: editForm.difficulty,
      completionCriteria: editForm.completionCriteria.trim() || null,
      estimatedMinutes: editForm.estimatedMinutes ? Number(editForm.estimatedMinutes) : null,
      prerequisiteActionId: action.prerequisiteActionId,
      reminderPolicy: action.reminderPolicy
    }
  });
  if (!updated) {
    actionPlanError.value = goalStore.errorMessage;
    return;
  }
  editingActionId.value = null;
}

/** 取消行动：只停止后续生成，历史打卡保留。 */
async function cancelPlanAction(action: ActionResult): Promise<void> {
  actionPlanError.value = null;
  const cancelled = await goalStore.cancelAction(action.actionId, action.version);
  if (!cancelled) {
    actionPlanError.value = goalStore.errorMessage;
  }
}

/** 复制行动为同计划内的新行动。 */
async function copyPlanAction(action: ActionResult): Promise<void> {
  actionPlanError.value = null;
  const copied = await goalStore.copyAction(action.actionId, `${action.title}（副本）`);
  if (!copied) {
    actionPlanError.value = goalStore.errorMessage;
  }
}

/** 平移行动开始日期，影响当前及未来全部实例。 */
async function movePlanAction(action: ActionResult): Promise<void> {
  actionPlanError.value = null;
  const target = moveDates[action.actionId];
  if (!target) {
    actionPlanError.value = '请先选择新的开始日期';
    return;
  }
  const moved = await goalStore.moveAction(action.actionId, action.version, target);
  if (!moved) {
    actionPlanError.value = goalStore.errorMessage;
    return;
  }
  moveDates[action.actionId] = '';
}
</script>

<template>
  <section v-if="goalStore.currentGoal" class="lx-card">
    <header class="lx-row detail__head">
      <div>
        <h2>{{ goalStore.currentGoal.title }}</h2>
        <p class="lx-muted">
          {{ goalStore.currentGoal.description || goalStore.currentGoal.successCriteria }}
        </p>
      </div>
      <div class="lx-row">
        <span class="lx-tag">{{ goalStore.currentGoal.status }}</span>
        <span class="lx-tag">进度 {{ goalStore.currentGoal.progress }}%</span>
        <span class="lx-tag">版本 {{ goalStore.currentGoal.version }}</span>
      </div>
    </header>

    <dl class="detail__meta">
      <div><dt>目标类型</dt><dd>{{ goalStore.currentGoal.goalType }}</dd></div>
      <div><dt>优先级</dt><dd>{{ goalStore.currentGoal.priority }}</dd></div>
      <div><dt>完成标准</dt><dd>{{ goalStore.currentGoal.successCriteria }}</dd></div>
      <div><dt>开始日期</dt><dd>{{ goalStore.currentGoal.startDate || '未设置' }}</dd></div>
      <div><dt>期望完成</dt><dd>{{ goalStore.currentGoal.targetEndDate || '未设置' }}</dd></div>
      <div>
        <dt>每周可用时间</dt>
        <dd>
          {{
            goalStore.currentGoal.weeklyAvailableMinutes
              ? `${Math.round(goalStore.currentGoal.weeklyAvailableMinutes / 60)} 小时`
              : '未设置'
          }}
        </dd>
      </div>
      <div><dt>可验证成果</dt><dd>{{ goalStore.currentGoal.verifiableOutcomes || '未填写' }}</dd></div>
      <div><dt>资源和限制</dt><dd>{{ goalStore.currentGoal.resourceConstraints || '未填写' }}</dd></div>
      <div><dt>隐私级别</dt><dd>{{ goalStore.currentGoal.privacyLevel }}</dd></div>
    </dl>
    <p v-if="goalStore.currentGoal.status === 'PAUSED' && goalStore.currentGoal.pauseResumeAt" class="lx-muted">
      预计 {{ goalStore.currentGoal.pauseResumeAt }} 恢复。
    </p>
    <p v-if="goalStore.currentGoal.abandonReason" class="lx-muted">
      放弃原因：{{ goalStore.currentGoal.abandonReason }}
    </p>

    <h3>目标状态</h3>
    <div class="detail__lifecycle">
      <div v-if="canPause" class="detail__lifecycle-row">
        <label>预计恢复日期（可选）</label>
        <input v-model="resumeDate" class="lx-input" type="date" />
        <button class="lx-button lx-button--ghost" :disabled="transitioning" @click="runTransition('PAUSE')">
          暂停目标
        </button>
      </div>
      <div v-if="canResume" class="detail__lifecycle-row">
        <button class="lx-button" :disabled="transitioning" @click="runTransition('RESUME')">
          恢复目标
        </button>
        <span class="lx-muted">
          恢复后请重新评估截止日期与剩余任务：暂停期间不会生成新的行动实例。
        </span>
      </div>
      <div v-if="canAbandon" class="detail__lifecycle-row">
        <label>放弃原因（必填）</label>
        <input v-model="abandonReason" class="lx-input" placeholder="例如 时间安排冲突，改到明年" />
        <button class="lx-button lx-button--ghost" :disabled="transitioning" @click="runTransition('ABANDON')">
          放弃目标
        </button>
      </div>
      <div v-if="canArchive" class="detail__lifecycle-row">
        <button class="lx-button lx-button--ghost" :disabled="transitioning" @click="runTransition('ARCHIVE')">
          归档目标
        </button>
      </div>
    </div>
    <p v-if="lifecycleError" class="lx-error">{{ lifecycleError }}</p>
  </section>

  <section class="lx-card plan">
    <h3>里程碑</h3>
    <div class="plan__form">
      <input v-model="milestoneForm.title" class="lx-input" placeholder="里程碑标题" />
      <input v-model="milestoneForm.successCriteria" class="lx-input" placeholder="阶段成功标准" />
      <button class="lx-button lx-button--ghost" @click="addMilestone">添加里程碑</button>
    </div>
    <ol class="plan__list">
      <li v-for="milestone in milestones" :key="milestone.sequenceNo">
        {{ milestone.sequenceNo }}. {{ milestone.title }} — {{ milestone.successCriteria }}
      </li>
    </ol>

    <h3>行动</h3>
    <div class="plan__form">
      <input v-model="actionForm.title" class="lx-input" placeholder="行动标题，例如 阅读 20 分钟" />
      <select v-model="actionForm.recurrenceType" class="lx-select">
        <option v-for="option in RECURRENCE_OPTIONS" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
      <input v-model="actionForm.localTime" class="lx-input" type="time" />
      <input v-model="actionForm.startDate" class="lx-input" type="date" />
      <select v-model="actionForm.milestoneSequence" class="lx-select">
        <option value="">不归属里程碑</option>
        <option v-for="milestone in milestones" :key="milestone.sequenceNo" :value="String(milestone.sequenceNo)">
          归属里程碑 {{ milestone.sequenceNo }}
        </option>
      </select>
    </div>

    <div v-if="needsWeekdays" class="plan__weekdays">
      <span class="lx-muted">每周重复的星期：</span>
      <button
        v-for="day in WEEKDAY_OPTIONS"
        :key="day.value"
        type="button"
        class="lx-button lx-button--ghost plan__weekday"
        :class="{ 'plan__weekday--on': actionForm.weekdays.includes(day.value) }"
        @click="toggleWeekday(day.value)"
      >
        {{ day.label }}
      </button>
    </div>

    <div v-if="needsIntervalDays" class="plan__interval">
      <span class="lx-muted">每隔</span>
      <input v-model.number="actionForm.intervalDays" class="lx-input" type="number" min="1" max="365" />
      <span class="lx-muted">天重复一次</span>
    </div>

    <div class="plan__form">
      <input v-model="actionForm.description" class="lx-input" placeholder="行动说明（可选）" />
      <input v-model="actionForm.completionCriteria" class="lx-input" placeholder="完成标准（可选）" />
      <select v-model="actionForm.priority" class="lx-select">
        <option v-for="option in PRIORITY_OPTIONS" :key="option.value" :value="option.value">
          优先级：{{ option.label }}
        </option>
      </select>
      <select v-model="actionForm.difficulty" class="lx-select">
        <option v-for="option in ACTION_DIFFICULTY_OPTIONS" :key="option.value" :value="option.value">
          难度：{{ option.label }}
        </option>
      </select>
      <input v-model="actionForm.estimatedMinutes" class="lx-input" type="number" min="1" placeholder="预计时长（分钟）" />
      <input v-model="actionForm.endLocalTime" class="lx-input" type="time" title="时间段结束时刻（可选）" />
    </div>

    <p class="lx-muted">本次行动的重复规则：{{ recurrenceSummary }}</p>
    <p v-if="actionError" class="lx-error">{{ actionError }}</p>
    <button class="lx-button lx-button--ghost" @click="addAction">添加行动</button>

    <ul class="plan__list">
      <li v-for="action in actions" :key="action.clientKey" class="plan__action">
        <span>
          {{ action.title }} · {{ actionRuleLabel(action) }} · {{ action.startDate }} {{ action.localTime }}
          <template v-if="action.estimatedMinutes"> · {{ action.estimatedMinutes }} 分钟</template>
          <template v-if="action.completionCriteria"> · 完成标准：{{ action.completionCriteria }}</template>
        </span>
        <button class="lx-button lx-button--ghost" type="button" @click="removeAction(action.clientKey)">
          移除
        </button>
      </li>
    </ul>

    <p class="lx-muted">
      计划确认会在一个事务内写入计划版本、里程碑与行动并激活目标；激活后服务端按行动规则滚动生成实例。
    </p>
    <p v-if="goalStore.errorMessage" class="lx-error">{{ goalStore.errorMessage }}</p>
    <div class="lx-row">
      <button class="lx-button" :disabled="actions.length === 0" @click="confirm">直接确认计划</button>
      <button class="lx-button lx-button--ghost" :disabled="actions.length === 0" @click="saveDraft">
        仅保存草案
      </button>
      <button
        class="lx-button lx-button--ghost"
        :disabled="savedDraftId === null"
        @click="confirmSavedDraft"
      >
        激活已保存草案{{ savedDraftId ? ` #${savedDraftId}` : '' }}
      </button>
    </div>
  </section>

  <section v-if="hasActivePlan" class="lx-card plan-manage">
    <h3>已生效计划的行动</h3>
    <p class="lx-muted">
      这里的改动对「本次及未来」生效：修改会立即重算后续行动实例，已打卡的历史记录不会被改动。
      只调整某一天请到「今日行动」使用单次跳过或改期。
    </p>
    <p v-if="actionPlanError" class="lx-error">{{ actionPlanError }}</p>
    <div v-if="goalStore.planActions.length === 0" class="lx-empty">当前计划下还没有行动。</div>
    <ul v-else class="plan-manage__list">
      <li v-for="action in goalStore.planActions" :key="action.actionId" class="plan-manage__item">
        <div class="plan-manage__head">
          <strong>{{ action.title }}</strong>
          <span class="lx-tag">{{ planActionRule(action) }}</span>
          <span class="lx-tag">{{ action.priority }}</span>
          <span class="lx-tag">{{ action.difficulty }}</span>
          <span class="lx-muted">版本 {{ action.version }}</span>
        </div>
        <p class="lx-muted">
          {{ action.startDate }} {{ action.localTime.slice(0, 5) }}
          <template v-if="action.endLocalTime"> — {{ action.endLocalTime.slice(0, 5) }}</template>
          <template v-if="action.estimatedMinutes"> · 预计 {{ action.estimatedMinutes }} 分钟</template>
          <template v-if="action.completionCriteria"> · 完成标准：{{ action.completionCriteria }}</template>
        </p>
        <p v-if="action.description" class="lx-muted">{{ action.description }}</p>

        <div v-if="editingActionId === action.actionId" class="plan-manage__edit">
          <input v-model="editForm.title" class="lx-input" placeholder="行动标题" />
          <input v-model="editForm.localTime" class="lx-input" type="time" title="开始时刻" />
          <input v-model="editForm.endLocalTime" class="lx-input" type="time" title="结束时刻（可选）" />
          <input v-model="editForm.estimatedMinutes" class="lx-input" type="number" min="1" placeholder="预计时长（分钟）" />
          <input v-model="editForm.completionCriteria" class="lx-input" placeholder="完成标准" />
          <select v-model="editForm.priority" class="lx-select">
            <option v-for="option in PRIORITY_OPTIONS" :key="option.value" :value="option.value">
              优先级：{{ option.label }}
            </option>
          </select>
          <select v-model="editForm.difficulty" class="lx-select">
            <option v-for="option in ACTION_DIFFICULTY_OPTIONS" :key="option.value" :value="option.value">
              难度：{{ option.label }}
            </option>
          </select>
          <div class="lx-row">
            <button class="lx-button" @click="saveEditAction(action)">保存（本次及未来）</button>
            <button class="lx-button lx-button--ghost" @click="cancelEditAction">取消编辑</button>
          </div>
        </div>

        <div v-else class="plan-manage__actions">
          <button class="lx-button lx-button--ghost" @click="startEditAction(action)">编辑（本次及未来）</button>
          <button class="lx-button lx-button--ghost" @click="copyPlanAction(action)">复制行动</button>
          <input v-model="moveDates[action.actionId]" class="lx-input" type="date" />
          <button class="lx-button lx-button--ghost" @click="movePlanAction(action)">移动开始日期</button>
          <button class="lx-button lx-button--ghost" @click="cancelPlanAction(action)">取消行动</button>
        </div>
      </li>
    </ul>

    <div v-if="goalStore.actionExceptions.length > 0">
      <h4>单次调整记录</h4>
      <ul class="plan-manage__exceptions">
        <li v-for="item in goalStore.actionExceptions" :key="item.exceptionId">
          {{ item.localDate }} ·
          {{ item.type === 'SKIP' ? '已跳过' : `已改期至 ${item.rescheduledDate}` }}
          <template v-if="item.reason"> · {{ item.reason }}</template>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.detail__head {
  justify-content: space-between;
}

.detail__meta {
  display: grid;
  gap: var(--lx-space-2);
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  margin: var(--lx-space-3) 0;
}

.detail__meta div {
  display: flex;
  gap: var(--lx-space-2);
}

.detail__meta dt {
  color: var(--lx-color-text-muted);
  flex: 0 0 auto;
}

.detail__meta dd {
  margin: 0;
}

.detail__lifecycle {
  display: grid;
  gap: var(--lx-space-2);
}

.detail__lifecycle-row {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--lx-space-2);
}

.plan {
  margin-top: var(--lx-space-5);
}

.plan__form {
  display: grid;
  gap: var(--lx-space-2);
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  margin-bottom: var(--lx-space-3);
}

.plan__list {
  margin: 0 0 var(--lx-space-4);
  padding-left: var(--lx-space-5);
}

.plan__weekdays,
.plan__interval {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--lx-space-2);
  margin-bottom: var(--lx-space-2);
}

.plan__weekday {
  min-width: 44px;
  padding: var(--lx-space-1) var(--lx-space-2);
}

.plan__weekday--on {
  background: var(--lx-color-primary-soft);
  border-color: var(--lx-color-primary-border);
  color: var(--lx-color-text);
}

.plan__interval .lx-input {
  max-width: 120px;
}

.plan__action {
  align-items: center;
  display: flex;
  gap: var(--lx-space-3);
  justify-content: space-between;
}

.plan-manage {
  margin-top: var(--lx-space-5);
}

.plan-manage__list,
.plan-manage__exceptions {
  display: grid;
  gap: var(--lx-space-3);
  list-style: none;
  margin: 0;
  padding: 0;
}

.plan-manage__item {
  border: 1px solid var(--lx-color-primary-border);
  border-radius: var(--lx-radius-sm);
  padding: var(--lx-space-3);
}

.plan-manage__head {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--lx-space-2);
}

.plan-manage__edit,
.plan-manage__actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--lx-space-2);
  margin-top: var(--lx-space-2);
}
</style>
