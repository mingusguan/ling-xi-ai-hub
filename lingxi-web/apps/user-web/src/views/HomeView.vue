<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import type {
  ActionDifficulty,
  CheckInResultType,
  EnergyLevel,
  MoodLevel,
  TodayItem
} from '@lingxi/api-client';
import { useGoalStore } from '@/stores/goal';
import { useSessionStore } from '@/stores/session';
import { ACTION_DIFFICULTY_OPTIONS, ENERGY_OPTIONS, MOOD_OPTIONS } from '@/utils/goalOptions';

const goalStore = useGoalStore();
const session = useSessionStore();
const busyOccurrenceId = ref<string | null>(null);
/** 单次改期的目标日期，按实例标识暂存。 */
const rescheduleDates = ref<Record<string, string>>({});
/** 单次调整的即时提示。 */
const adjustError = ref<string | null>(null);

/** 记录表单：结果与各维度；同一份表单同时服务首次记录与更正。 */
const detailForm = reactive({
  result: 'COMPLETED' as CheckInResultType,
  note: '',
  actualMinutes: '',
  perceivedDifficulty: 'NORMAL' as ActionDifficulty,
  energyLevel: 'NORMAL' as EnergyLevel,
  moodLevel: 'NEUTRAL' as MoodLevel,
  failureReason: ''
});
const detailError = ref<string | null>(null);
/** 正在更正哪一个实例；为空表示不在更正模式。 */
const correctingOccurrenceId = ref<string | null>(null);

/** 快速记录输入与提示。 */
const quickNoteDraft = reactive({ content: '', moodLevel: '' as MoodLevel | '' });
const quickNoteError = ref<string | null>(null);

/** 专注计时：本地秒数用于渲染，每次操作后都用服务端返回值重新校准。 */
const focusSeconds = ref(0);
const focusPlannedMinutes = ref('25');
const focusNote = ref('');
const focusError = ref<string | null>(null);
let ticker: ReturnType<typeof setInterval> | null = null;

const workbench = computed(() => goalStore.today);
const suggestions = computed(() => workbench.value?.suggestions ?? []);
const overdueItems = computed(() => workbench.value?.overdue ?? []);
const todayItems = computed(() => workbench.value?.today ?? []);
const upcomingItems = computed(() => workbench.value?.upcoming ?? []);
const activeFocus = computed(() => goalStore.activeFocus);

onMounted(async () => {
  await goalStore.loadGoals();
  await goalStore.loadToday();
  await goalStore.loadAchievements();
  startTicker();
});

onUnmounted(() => {
  if (ticker !== null) {
    clearInterval(ticker);
  }
});

/** 每秒推进一步专注秒数；暂停时不增长，与服务端语义保持一致。 */
function startTicker(): void {
  if (ticker !== null) {
    clearInterval(ticker);
  }
  ticker = setInterval(() => {
    if (activeFocus.value?.status === 'RUNNING') {
      focusSeconds.value += 1;
    }
  }, 1000);
}

/** 用服务端返回的秒数校准本地计时。 */
function syncFocusSeconds(): void {
  focusSeconds.value = activeFocus.value?.accumulatedSeconds ?? 0;
}

/** 秒数格式化为 mm:ss（超过一小时显示 h:mm:ss）。 */
function formatDuration(totalSeconds: number): string {
  const seconds = Math.max(0, Math.floor(totalSeconds));
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = seconds % 60;
  const mm = String(minutes).padStart(2, '0');
  const ss = String(rest).padStart(2, '0');
  return hours > 0 ? `${hours}:${mm}:${ss}` : `${mm}:${ss}`;
}

async function startFocus(item: TodayItem | null): Promise<void> {
  focusError.value = null;
  const planned = focusPlannedMinutes.value ? Number(focusPlannedMinutes.value) : null;
  const started = await goalStore.startFocus(item ? item.occurrenceId : null, planned);
  if (!started) {
    focusError.value = goalStore.errorMessage;
    return;
  }
  syncFocusSeconds();
}

async function transitionFocus(action: 'PAUSE' | 'RESUME' | 'FINISH' | 'ABANDON'): Promise<void> {
  focusError.value = null;
  const result = await goalStore.transitionFocus(action, action === 'FINISH' ? focusNote.value || null : null);
  if (!result) {
    focusError.value = goalStore.errorMessage;
    return;
  }
  if (action === 'FINISH') {
    focusNote.value = '';
  }
  syncFocusSeconds();
  if (action === 'FINISH' || action === 'ABANDON') {
    await goalStore.loadToday();
  }
}

async function submitQuickNote(): Promise<void> {
  quickNoteError.value = null;
  if (!quickNoteDraft.content.trim()) {
    quickNoteError.value = '写一句再提交';
    return;
  }
  const created = await goalStore.createQuickNote(
    quickNoteDraft.content.trim(),
    quickNoteDraft.moodLevel === '' ? null : quickNoteDraft.moodLevel
  );
  if (!created) {
    quickNoteError.value = goalStore.errorMessage;
    return;
  }
  quickNoteDraft.content = '';
  quickNoteDraft.moodLevel = '';
}

/** 结果与维度必须自洽：失败要原因，非失败不能带原因。 */
function validateDetail(): string | null {
  if (detailForm.result === 'FAILED' && !detailForm.failureReason.trim()) {
    return '记录为失败时必须填写失败原因';
  }
  if (detailForm.result !== 'FAILED' && detailForm.failureReason.trim()) {
    return '只有结果为「失败」时才填写失败原因';
  }
  if (detailForm.actualMinutes && Number(detailForm.actualMinutes) <= 0) {
    return '实际耗时必须是大于 0 的分钟数';
  }
  return null;
}

/**
 * 结果从「失败」改回其他值时清空失败原因。
 *
 * 失败原因输入框只在结果为失败时渲染；不清空的话残留值会触发一条用户看不见也改不掉的校验错误。
 */
watch(
  () => detailForm.result,
  (next) => {
    if (next !== 'FAILED') {
      detailForm.failureReason = '';
    }
  }
);

function resetDetailForm(): void {
  detailForm.result = 'COMPLETED';
  detailForm.note = '';
  detailForm.actualMinutes = '';
  detailForm.failureReason = '';
  detailForm.perceivedDifficulty = 'NORMAL';
  detailForm.energyLevel = 'NORMAL';
  detailForm.moodLevel = 'NEUTRAL';
}

/**
 * 提交记录或更正。
 *
 * correction 为 true 时服务端不覆盖原记录：原记录被标记失效并保留为历史，
 * 因此「用户改过什么」始终可追溯。
 */
async function submitCheckIn(occurrenceId: string, correction: boolean): Promise<void> {
  detailError.value = null;
  const invalid = validateDetail();
  if (invalid !== null) {
    detailError.value = invalid;
    return;
  }
  busyOccurrenceId.value = occurrenceId;
  const checkIn = await goalStore.checkIn(
    occurrenceId,
    {
      result: detailForm.result,
      note: detailForm.note.trim() || null,
      evidenceReference: null,
      actualMinutes: detailForm.actualMinutes ? Number(detailForm.actualMinutes) : null,
      perceivedDifficulty: detailForm.perceivedDifficulty,
      energyLevel: detailForm.energyLevel,
      moodLevel: detailForm.moodLevel,
      failureReason: detailForm.result === 'FAILED' ? detailForm.failureReason.trim() : null
    },
    correction
  );
  busyOccurrenceId.value = null;
  if (!checkIn) {
    detailError.value = goalStore.errorMessage;
    return;
  }
  correctingOccurrenceId.value = null;
  resetDetailForm();
  await goalStore.loadToday();
}

/** 进入更正模式，并用当前有效记录预填表单，避免用户盲改。 */
async function startCorrection(occurrenceId: string): Promise<void> {
  detailError.value = null;
  const current = await goalStore.loadEffectiveCheckIn(occurrenceId);
  if (!current) {
    detailError.value = goalStore.errorMessage ?? '没有可更正的记录';
    return;
  }
  correctingOccurrenceId.value = occurrenceId;
  detailForm.result = current.result;
  detailForm.note = current.note ?? '';
  detailForm.actualMinutes = current.actualMinutes === null ? '' : String(current.actualMinutes);
  detailForm.perceivedDifficulty = current.perceivedDifficulty ?? 'NORMAL';
  detailForm.energyLevel = current.energyLevel ?? 'NORMAL';
  detailForm.moodLevel = current.moodLevel ?? 'NEUTRAL';
  detailForm.failureReason = current.failureReason ?? '';
}

function cancelCorrection(): void {
  correctingOccurrenceId.value = null;
  detailError.value = null;
  resetDetailForm();
}

/**
 * 单次跳过：只让这一次不发生，重复规则保持不变。
 *
 * 与「打卡为跳过」的区别：打卡是执行结果记录，单次跳过是计划层面的调整，
 * 服务端会登记一条按日期唯一的例外并在重算实例时叠加。
 */
async function skipOnce(occurrenceId: string): Promise<void> {
  adjustError.value = null;
  busyOccurrenceId.value = occurrenceId;
  const adjusted = await goalStore.adjustOccurrence(occurrenceId, 'SKIP', null, '用户单次跳过');
  busyOccurrenceId.value = null;
  if (!adjusted) {
    adjustError.value = goalStore.errorMessage;
    return;
  }
  await goalStore.loadToday();
}

/** 单次改期：把这一次挪到选择的日期，重复规则保持不变。 */
async function rescheduleOnce(occurrenceId: string): Promise<void> {
  adjustError.value = null;
  const target = rescheduleDates.value[occurrenceId];
  if (!target) {
    adjustError.value = '请先选择改期后的日期';
    return;
  }
  busyOccurrenceId.value = occurrenceId;
  const adjusted = await goalStore.adjustOccurrence(occurrenceId, 'RESCHEDULE', target, '用户单次改期');
  busyOccurrenceId.value = null;
  if (!adjusted) {
    adjustError.value = goalStore.errorMessage;
    return;
  }
  rescheduleDates.value = { ...rescheduleDates.value, [occurrenceId]: '' };
  await goalStore.loadToday();
}

/** 某条行动为什么排在这里：把排序依据明确告诉用户。 */
function reasonLabel(item: TodayItem): string {
  const parts: string[] = [];
  if (item.overdue) {
    parts.push('已逾期');
  }
  if (item.unlocksOthers > 0) {
    parts.push(`是另外 ${item.unlocksOthers} 个行动的前置`);
  }
  if (item.estimatedMinutes !== null) {
    parts.push(`预计 ${item.estimatedMinutes} 分钟`);
  }
  parts.push(item.priority === 'HIGH' ? '高优先级' : item.priority === 'LOW' ? '低优先级' : '常规优先级');
  return parts.join(' · ');
}
</script>

<template>
  <section v-if="session.needsGuardian" class="lx-card">
    <h2>绑定监护人后才能使用核心功能</h2>
    <p class="lx-muted">
      当前账号为 14—17 周岁待监护状态，需要监护人接受邀请后才能创建目标、打卡与使用灵犀对话。
    </p>
    <RouterLink class="lx-button" :to="{ name: 'guardian' }">前往监护管理</RouterLink>
  </section>

  <template v-else>
    <section v-if="goalStore.latestAchievements.length > 0" class="lx-card achievement-banner">
      <strong>获得新成就</strong>
      <ul>
        <li v-for="item in goalStore.latestAchievements" :key="item.achievementId">
          {{ item.title }} · {{ item.description }}
        </li>
      </ul>
      <button class="lx-button lx-button--ghost" @click="goalStore.clearLatestAchievements()">
        知道了
      </button>
    </section>

    <section class="lx-card focus">
      <header class="lx-row lx-row--between">
        <h2>专注模式</h2>
        <span class="lx-tag">{{ activeFocus ? activeFocus.status : 'IDLE' }}</span>
      </header>
      <p class="lx-muted">
        计时不依赖后台心跳：暂停期间不计入时长，即使中途关闭页面，累计时长也不会丢。
      </p>
      <div class="focus__display">{{ formatDuration(focusSeconds) }}</div>
      <div class="focus__controls">
        <template v-if="!activeFocus">
          <input v-model="focusPlannedMinutes" class="lx-input" type="number" min="1" placeholder="计划分钟数" />
          <button class="lx-button" @click="startFocus(null)">开始专注</button>
        </template>
        <template v-else>
          <button
            v-if="activeFocus.status === 'RUNNING'"
            class="lx-button lx-button--ghost"
            @click="transitionFocus('PAUSE')"
          >
            暂停
          </button>
          <button v-else class="lx-button" @click="transitionFocus('RESUME')">继续</button>
          <input v-model="focusNote" class="lx-input" placeholder="结束备注（可选）" />
          <button class="lx-button" @click="transitionFocus('FINISH')">结束并记录</button>
          <button class="lx-button lx-button--ghost" @click="transitionFocus('ABANDON')">作废</button>
        </template>
      </div>
      <p v-if="focusError" class="lx-error">{{ focusError }}</p>
    </section>

    <section v-if="suggestions.length > 0" class="lx-card suggestions">
      <h3>提示</h3>
      <p class="lx-muted">
        当前提示由可核对的规则产生（来源 RULE），不是模型判断；接入模型后同一位置会展示带依据的结论。
      </p>
      <ul>
        <li v-for="item in suggestions" :key="item.type + (item.relatedOccurrenceId ?? '')">
          <strong>{{ item.title }}</strong>
          <span class="lx-tag">{{ item.source }}</span>
          <div class="lx-muted">依据：{{ item.basis }}</div>
        </li>
      </ul>
    </section>

    <section class="lx-card">
      <header class="lx-row lx-row--between">
        <h2>今日工作台</h2>
        <RouterLink class="lx-button lx-button--ghost" :to="{ name: 'goals' }">管理目标</RouterLink>
      </header>

      <section class="lx-card record">
        <h3>{{ correctingOccurrenceId ? '更正记录' : '记录执行情况' }}</h3>
        <p class="lx-muted">
          结果用于判断计划是否可行；失败要写原因，复盘才能定位阻塞，而不是把「没做成」当成不努力。
          更正不会覆盖原记录：旧记录作为历史保留。
        </p>
        <div class="record__grid">
          <select v-model="detailForm.result" class="lx-select">
            <option value="COMPLETED">完成</option>
            <option value="PARTIAL">部分完成</option>
            <option value="SKIPPED">跳过</option>
            <option value="FAILED">失败</option>
          </select>
          <input v-model="detailForm.actualMinutes" class="lx-input" type="number" min="1" placeholder="实际耗时（分钟）" />
          <select v-model="detailForm.perceivedDifficulty" class="lx-select">
            <option v-for="option in ACTION_DIFFICULTY_OPTIONS" :key="option.value" :value="option.value">
              主观难度：{{ option.label }}
            </option>
          </select>
          <select v-model="detailForm.energyLevel" class="lx-select">
            <option v-for="option in ENERGY_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <select v-model="detailForm.moodLevel" class="lx-select">
            <option v-for="option in MOOD_OPTIONS" :key="option.value" :value="option.value">
              情绪：{{ option.label }}
            </option>
          </select>
          <input v-model="detailForm.note" class="lx-input" placeholder="备注（可选）" />
          <input
            v-if="detailForm.result === 'FAILED'"
            v-model="detailForm.failureReason"
            class="lx-input"
            placeholder="失败原因（必填）"
          />
        </div>
        <p v-if="detailError" class="lx-error">{{ detailError }}</p>
        <div class="lx-row">
          <button
            v-if="correctingOccurrenceId"
            class="lx-button"
            :disabled="busyOccurrenceId === correctingOccurrenceId"
            @click="submitCheckIn(correctingOccurrenceId, true)"
          >
            保存更正
          </button>
          <button v-if="correctingOccurrenceId" class="lx-button lx-button--ghost" @click="cancelCorrection">
            取消更正
          </button>
        </div>
      </section>

      <section class="lx-card quick-note">
        <h3>快速记录</h3>
        <p class="lx-muted">不绑定行动，随手写一句即可；情绪自评可选，仅用于观察趋势。</p>
        <div class="quick-note__form">
          <input v-model="quickNoteDraft.content" class="lx-input" placeholder="现在想记下的一句话" />
          <select v-model="quickNoteDraft.moodLevel" class="lx-select">
            <option value="">不填情绪</option>
            <option v-for="option in MOOD_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <button class="lx-button lx-button--ghost" @click="submitQuickNote">记下来</button>
        </div>
        <p v-if="quickNoteError" class="lx-error">{{ quickNoteError }}</p>
        <ul v-if="goalStore.quickNotes.length > 0" class="quick-note__list">
          <li v-for="note in goalStore.quickNotes" :key="note.noteId">
            <span>{{ note.localDate }} · {{ note.content }}</span>
            <button class="lx-button lx-button--ghost" @click="goalStore.deleteQuickNote(note.noteId)">
              删除
            </button>
          </li>
        </ul>
      </section>

      <p v-if="goalStore.errorMessage" class="lx-error">{{ goalStore.errorMessage }}</p>

      <div v-if="!workbench" class="lx-empty">正在加载今日工作台…</div>

      <template v-else>
        <h3 v-if="overdueItems.length > 0" class="group-title group-title--overdue">
          已逾期（{{ overdueItems.length }}）
        </h3>
        <ul v-if="overdueItems.length > 0" class="occurrence-list">
          <li v-for="item in overdueItems" :key="item.occurrenceId" class="occurrence occurrence--overdue">
            <div>
              <strong>{{ item.actionTitle }}</strong>
              <div class="lx-muted">{{ item.localDate }} · 原定 {{ item.localTime.slice(0, 5) }} · {{ reasonLabel(item) }}</div>
            </div>
            <div v-if="item.status === 'SCHEDULED'" class="lx-row">
              <button class="lx-button" @click="startFocus(item)">用专注模式做</button>
            </div>
            <div v-if="item.status === 'SCHEDULED'" class="occurrence__adjust">
              <button class="lx-button lx-button--ghost" @click="skipOnce(item.occurrenceId)">仅跳过这一次</button>
              <input v-model="rescheduleDates[item.occurrenceId]" class="lx-input" type="date" />
              <button class="lx-button lx-button--ghost" @click="rescheduleOnce(item.occurrenceId)">仅改期这一次</button>
            </div>
          </li>
        </ul>

        <h3 class="group-title">今天（{{ workbench.todayFinished }} / {{ workbench.todayTotal }} 已记录）</h3>
        <div v-if="todayItems.length === 0" class="lx-empty">今天没有安排行动。</div>
        <ul v-else class="occurrence-list">
          <li v-for="item in todayItems" :key="item.occurrenceId" class="occurrence">
            <div>
              <strong>{{ item.actionTitle }}</strong>
              <div class="lx-muted">
                {{ item.localTime.slice(0, 5) }} · {{ reasonLabel(item) }} ·
                <span :class="{ 'lx-tag--warn': item.status === 'SCHEDULED' }" class="lx-tag">{{ item.status }}</span>
                <span v-if="item.exceptionType" class="lx-tag">{{ item.exceptionType }}</span>
              </div>
              <div v-if="item.actionDescription" class="lx-muted">{{ item.actionDescription }}</div>
            </div>
            <template v-if="item.status === 'SCHEDULED'">
              <div class="lx-row">
                <button class="lx-button" @click="startFocus(item)">用专注模式做</button>
                <button
                  class="lx-button lx-button--ghost"
                  :disabled="busyOccurrenceId === item.occurrenceId"
                  @click="submitCheckIn(item.occurrenceId, false)"
                >
                  用上面的表单记录
                </button>
              </div>
              <div class="occurrence__adjust">
                <button class="lx-button lx-button--ghost" @click="skipOnce(item.occurrenceId)">仅跳过这一次</button>
                <input v-model="rescheduleDates[item.occurrenceId]" class="lx-input" type="date" />
                <button class="lx-button lx-button--ghost" @click="rescheduleOnce(item.occurrenceId)">仅改期这一次</button>
              </div>
            </template>
            <div v-else class="occurrence__recorded">
              <span class="lx-tag">已记录</span>
              <button class="lx-button lx-button--ghost" @click="startCorrection(item.occurrenceId)">更正记录</button>
            </div>
          </li>
        </ul>

        <h3 v-if="upcomingItems.length > 0" class="group-title">接下来几天（{{ upcomingItems.length }}）</h3>
        <ul v-if="upcomingItems.length > 0" class="occurrence-list">
          <li v-for="item in upcomingItems" :key="item.occurrenceId" class="occurrence occurrence--upcoming">
            <div>
              <strong>{{ item.actionTitle }}</strong>
              <div class="lx-muted">{{ item.localDate }} {{ item.localTime.slice(0, 5) }} · {{ reasonLabel(item) }}</div>
            </div>
          </li>
        </ul>
      </template>

      <p v-if="adjustError" class="lx-error">{{ adjustError }}</p>
      <p class="lx-muted">
        排序规则：逾期优先，其次是需要先做（能解锁其他行动）、用户设定的时刻、预计时长；未填预计时长的排在最后。
      </p>
      <p class="lx-muted">
        「跳过 / 失败」记录的是执行结果；「仅跳过这一次 / 仅改期这一次」调整的是计划本身，
        只影响这一天的实例，不会改动行动的正常重复规则。
      </p>
    </section>
  </template>
</template>

<style scoped>
.achievement-banner {
  background: var(--lx-color-primary-soft);
  margin-bottom: var(--lx-space-5);
}

.group-title {
  margin: var(--lx-space-4) 0 var(--lx-space-2);
}

.group-title--overdue {
  color: var(--lx-color-danger);
}

.occurrence-list {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-3);
  list-style: none;
  margin: 0;
  padding: 0;
}

.occurrence {
  align-items: center;
  border: 1px solid var(--lx-color-border);
  border-radius: var(--lx-radius-sm);
  display: flex;
  flex-wrap: wrap;
  gap: var(--lx-space-2);
  justify-content: space-between;
  padding: var(--lx-space-3);
}

.occurrence--overdue {
  border-color: var(--lx-color-danger-soft);
}

.occurrence--upcoming {
  opacity: 0.85;
}

.occurrence__adjust {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--lx-space-2);
  width: 100%;
}

.occurrence__adjust .lx-input {
  max-width: 170px;
}

.occurrence__recorded {
  align-items: center;
  display: flex;
  gap: var(--lx-space-2);
}

.record {
  margin-bottom: var(--lx-space-4);
}

.record__grid {
  display: grid;
  gap: var(--lx-space-2);
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  margin: var(--lx-space-3) 0;
}

.focus {
  margin-bottom: var(--lx-space-4);
}

.focus__display {
  font-family: var(--lx-font-mono);
  font-size: 40px;
  font-weight: 600;
  letter-spacing: 0.04em;
  margin: var(--lx-space-2) 0;
}

.focus__controls {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--lx-space-2);
}

.suggestions {
  margin-bottom: var(--lx-space-4);
}

.suggestions ul {
  display: grid;
  gap: var(--lx-space-2);
  list-style: none;
  margin: 0;
  padding: 0;
}

.quick-note {
  margin-bottom: var(--lx-space-4);
}

.quick-note__form {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--lx-space-2);
  margin: var(--lx-space-2) 0;
}

.quick-note__list {
  display: grid;
  gap: var(--lx-space-2);
  list-style: none;
  margin: 0;
  padding: 0;
}

.quick-note__list li {
  align-items: center;
  display: flex;
  gap: var(--lx-space-2);
  justify-content: space-between;
}

.lx-row--between {
  justify-content: space-between;
}
</style>
