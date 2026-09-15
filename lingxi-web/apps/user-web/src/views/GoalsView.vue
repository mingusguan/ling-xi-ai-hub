<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import type { PriorityLevel, GoalPrivacyLevel, GoalType } from '@lingxi/api-client';
import { useGoalStore } from '@/stores/goal';
import { PRIORITY_OPTIONS, GOAL_TYPE_OPTIONS, GOAL_PRIVACY_OPTIONS } from '@/utils/goalOptions';

const goalStore = useGoalStore();
const router = useRouter();
const form = reactive({
  title: '',
  description: '',
  successCriteria: '',
  goalType: 'HABIT' as GoalType,
  startDate: new Date().toISOString().slice(0, 10),
  targetEndDate: '',
  priority: 'NORMAL' as PriorityLevel,
  resourceConstraints: '',
  verifiableOutcomes: '',
  privacyLevel: 'PRIVATE' as GoalPrivacyLevel
});
/** 每周可用时间按小时填写更直观，提交时换算为服务端要求的分钟。 */
const weeklyHoursBudget = ref('');
const creating = ref(false);
const createError = ref<string | null>(null);
const showAdvanced = ref(false);
/** 列表按「推进中 / 历史」分组展示，归档与放弃归入历史。 */
const showArchived = ref(false);

onMounted(() => {
  void goalStore.loadGoals();
  void goalStore.loadQuota();
});

async function create(): Promise<void> {
  createError.value = null;
  if (!form.title.trim() || !form.successCriteria.trim()) {
    createError.value = '目标标题与完成标准都不能为空';
    return;
  }
  if (form.startDate && form.targetEndDate && form.targetEndDate < form.startDate) {
    createError.value = '期望完成日期不能早于开始日期';
    return;
  }
  creating.value = true;
  const created = await goalStore.createGoal({
    title: form.title.trim(),
    description: form.description.trim() || null,
    successCriteria: form.successCriteria.trim(),
    goalType: form.goalType,
    startDate: form.startDate || null,
    targetEndDate: form.targetEndDate || null,
    priority: form.priority,
    weeklyAvailableMinutes: weeklyHoursBudget.value ? Number(weeklyHoursBudget.value) * 60 : null,
    resourceConstraints: form.resourceConstraints.trim() || null,
    verifiableOutcomes: form.verifiableOutcomes.trim() || null,
    privacyLevel: form.privacyLevel
  });
  creating.value = false;
  if (created) {
    form.title = '';
    form.description = '';
    form.successCriteria = '';
    form.verifiableOutcomes = '';
    form.resourceConstraints = '';
    weeklyHoursBudget.value = '';
    await router.push({ name: 'goal-detail', params: { goalId: created.goalId } });
  } else {
    createError.value = goalStore.errorMessage;
  }
}

function goalTypeLabel(value: GoalType): string {
  return GOAL_TYPE_OPTIONS.find((item) => item.value === value)?.label ?? value;
}
</script>

<template>
  <section class="lx-card">
    <h2>创建目标</h2>
    <div class="lx-field">
      <label>目标标题</label>
      <input v-model="form.title" class="lx-input" placeholder="例如 每天阅读 20 分钟" />
    </div>
    <div class="lx-field">
      <label>目标描述（可选）</label>
      <input v-model="form.description" class="lx-input" placeholder="一句话说明为什么要做这件事" />
    </div>
    <div class="lx-field">
      <label>完成标准</label>
      <input v-model="form.successCriteria" class="lx-input" placeholder="例如 连续 30 天完成阅读" />
    </div>
    <div class="lx-field">
      <label>目标类型</label>
      <select v-model="form.goalType" class="lx-select">
        <option v-for="option in GOAL_TYPE_OPTIONS" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
    </div>

    <button class="lx-button lx-button--ghost" type="button" @click="showAdvanced = !showAdvanced">
      {{ showAdvanced ? '收起更多设置' : '展开更多设置（日期、优先级、可用时间、隐私）' }}
    </button>

    <div v-if="showAdvanced" class="goal-form__advanced">
      <div class="lx-field">
        <label>开始日期</label>
        <input v-model="form.startDate" class="lx-input" type="date" />
      </div>
      <div class="lx-field">
        <label>期望完成日期</label>
        <input v-model="form.targetEndDate" class="lx-input" type="date" />
      </div>
      <div class="lx-field">
        <label>优先级</label>
        <select v-model="form.priority" class="lx-select">
          <option v-for="option in PRIORITY_OPTIONS" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>
      <div class="lx-field">
        <label>每周可用时间（小时）</label>
        <input v-model="weeklyHoursBudget" class="lx-input" type="number" min="1" placeholder="例如 5" />
      </div>
      <div class="lx-field">
        <label>可验证成果（可选）</label>
        <input v-model="form.verifiableOutcomes" class="lx-input" placeholder="例如 12 篇读书笔记" />
      </div>
      <div class="lx-field">
        <label>资源和限制条件（可选）</label>
        <input v-model="form.resourceConstraints" class="lx-input" placeholder="例如 只有通勤时间可用" />
      </div>
      <div class="lx-field">
        <label>隐私级别</label>
        <select v-model="form.privacyLevel" class="lx-select">
          <option v-for="option in GOAL_PRIVACY_OPTIONS" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>
    </div>

    <p v-if="createError" class="lx-error">{{ createError }}</p>
    <button class="lx-button" :disabled="creating" @click="create">
      {{ creating ? '创建中…' : '创建草稿目标' }}
    </button>
    <p class="lx-muted">
      草稿目标不计入活跃目标上限。确认计划并激活后才会占用配额。
      <template v-if="goalStore.quota">
        当前活跃 {{ goalStore.quota.activeCount }} / {{ goalStore.quota.allowance }} 个。
      </template>
    </p>
  </section>

  <section class="goal-section">
    <header class="lx-row goal-section__head">
      <h2>我的目标</h2>
      <div class="lx-row">
        <span v-if="goalStore.quota" class="lx-tag">
          活跃 {{ goalStore.quota.activeCount }} / {{ goalStore.quota.allowance }}
        </span>
        <button class="lx-button lx-button--ghost" type="button" @click="showArchived = !showArchived">
          {{ showArchived ? '隐藏历史目标' : `查看历史目标（${goalStore.archivedGoals.length}）` }}
        </button>
      </div>
    </header>
    <p v-if="goalStore.errorMessage" class="lx-error">{{ goalStore.errorMessage }}</p>
    <div v-if="goalStore.openGoals.length === 0" class="lx-empty">还没有进行中的目标，先创建一个吧。</div>
    <div v-else class="lx-grid">
      <article v-for="goal in goalStore.openGoals" :key="goal.goalId" class="lx-card goal-card">
        <header class="lx-row goal-card__head">
          <strong>{{ goal.title }}</strong>
          <span class="lx-tag">{{ goal.status }}</span>
        </header>
        <p class="lx-muted">{{ goal.description || goal.successCriteria }}</p>
        <p class="lx-muted">
          {{ goalTypeLabel(goal.goalType) }} · 优先级 {{ goal.priority }}
          <template v-if="goal.targetEndDate"> · 截止 {{ goal.targetEndDate }}</template>
        </p>
        <div class="goal-card__progress">
          <div class="goal-card__bar" :style="{ width: `${goal.progress}%` }" />
        </div>
        <p class="lx-muted">进度 {{ goal.progress }}% · 版本 {{ goal.version }}</p>
        <p v-if="goal.status === 'PAUSED' && goal.pauseResumeAt" class="lx-muted">
          预计 {{ goal.pauseResumeAt }} 恢复
        </p>
        <RouterLink class="lx-button" :to="{ name: 'goal-detail', params: { goalId: goal.goalId } }">
          查看与编辑计划
        </RouterLink>
      </article>
    </div>

    <div v-if="showArchived" class="goal-section__archived">
      <h3>历史目标</h3>
      <div v-if="goalStore.archivedGoals.length === 0" class="lx-empty">还没有归档或放弃的目标。</div>
      <ul v-else class="goal-section__list">
        <li v-for="goal in goalStore.archivedGoals" :key="goal.goalId">
          <RouterLink :to="{ name: 'goal-detail', params: { goalId: goal.goalId } }">
            {{ goal.title }}
          </RouterLink>
          <span class="lx-tag">{{ goal.status }}</span>
          <span v-if="goal.abandonReason" class="lx-muted">原因：{{ goal.abandonReason }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.goal-section {
  margin-top: var(--lx-space-5);
}

.goal-section__head {
  justify-content: space-between;
}

.goal-section__archived {
  margin-top: var(--lx-space-5);
}

.goal-section__list {
  display: grid;
  gap: var(--lx-space-2);
  list-style: none;
  margin: 0;
  padding: 0;
}

.goal-section__list li {
  align-items: center;
  display: flex;
  gap: var(--lx-space-2);
}

.goal-form__advanced {
  display: grid;
  gap: var(--lx-space-3);
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  margin: var(--lx-space-3) 0;
}

.goal-card__head {
  justify-content: space-between;
}

.goal-card__progress {
  background: var(--lx-color-surface-muted);
  border-radius: 999px;
  height: 8px;
  margin: var(--lx-space-3) 0;
  overflow: hidden;
}

.goal-card__bar {
  background: var(--lx-color-primary);
  height: 100%;
}
</style>
