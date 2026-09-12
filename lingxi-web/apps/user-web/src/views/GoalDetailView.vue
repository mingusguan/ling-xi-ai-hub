<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import type { PlanActionDraft, PlanMilestoneDraft } from '@lingxi/api-client';
import { useGoalStore } from '@/stores/goal';

const goalStore = useGoalStore();
const route = useRoute();
const goalId = String(route.params.goalId);

const milestoneForm = reactive({ title: '', successCriteria: '' });
const milestones = ref<PlanMilestoneDraft[]>([]);
const actionForm = reactive({
  title: '',
  recurrenceType: 'DAILY' as 'ONCE' | 'DAILY' | 'WEEKLY',
  localTime: '21:00',
  startDate: new Date().toISOString().slice(0, 10),
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai',
  milestoneSequence: '' as string
});
const actions = ref<PlanActionDraft[]>([]);
const savedDraftId = ref<string | null>(null);

onMounted(async () => {
  await goalStore.loadGoal(goalId);
});

/** 生成稳定的行动客户端键，服务端按 clientKey 去重。 */
function nextClientKey(): string {
  return `pc-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
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
  if (!actionForm.title.trim()) {
    return;
  }
  actions.value = [
    ...actions.value,
    {
      clientKey: nextClientKey(),
      milestoneSequence: actionForm.milestoneSequence ? Number(actionForm.milestoneSequence) : null,
      title: actionForm.title.trim(),
      recurrenceType: actionForm.recurrenceType,
      weekdays: [],
      startDate: actionForm.startDate,
      endDate: null,
      localTime: `${actionForm.localTime}:00`,
      timezone: actionForm.timezone
    }
  ];
  actionForm.title = '';
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
</script>

<template>
  <section v-if="goalStore.currentGoal" class="lx-card">
    <header class="lx-row detail__head">
      <div>
        <h2>{{ goalStore.currentGoal.title }}</h2>
        <p class="lx-muted">{{ goalStore.currentGoal.successCriteria }}</p>
      </div>
      <div class="lx-row">
        <span class="lx-tag">{{ goalStore.currentGoal.status }}</span>
        <span class="lx-tag">进度 {{ goalStore.currentGoal.progress }}%</span>
        <span class="lx-tag">版本 {{ goalStore.currentGoal.version }}</span>
      </div>
    </header>
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
        <option value="ONCE">一次性</option>
        <option value="DAILY">每天</option>
        <option value="WEEKLY">每周</option>
      </select>
      <input v-model="actionForm.localTime" class="lx-input" type="time" />
      <input v-model="actionForm.startDate" class="lx-input" type="date" />
      <select v-model="actionForm.milestoneSequence" class="lx-select">
        <option value="">不归属里程碑</option>
        <option v-for="milestone in milestones" :key="milestone.sequenceNo" :value="String(milestone.sequenceNo)">
          归属里程碑 {{ milestone.sequenceNo }}
        </option>
      </select>
      <button class="lx-button lx-button--ghost" @click="addAction">添加行动</button>
    </div>
    <ul class="plan__list">
      <li v-for="action in actions" :key="action.clientKey">
        {{ action.title }} · {{ action.recurrenceType }} · {{ action.startDate }} {{ action.localTime }} ·
        {{ action.timezone }}
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
</template>

<style scoped>
.detail__head {
  justify-content: space-between;
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
</style>
