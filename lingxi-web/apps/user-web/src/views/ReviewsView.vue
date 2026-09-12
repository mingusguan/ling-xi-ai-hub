<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useGoalStore } from '@/stores/goal';
import { useRecordStore } from '@/stores/record';

const recordStore = useRecordStore();
const goalStore = useGoalStore();
const selectedReviewId = ref<string | null>(null);
const conclusion = ref('');

const selectedReview = computed(() => recordStore.currentReview);

onMounted(async () => {
  await goalStore.loadGoals();
  await recordStore.loadReviews();
});

function goalTitle(goalId: string): string {
  return goalStore.goals.find((goal) => goal.goalId === goalId)?.title ?? '目标已删除';
}

function formatTime(value: string | null): string {
  return value ? new Date(value).toLocaleString('zh-CN') : '—';
}

/** 快照是生成复盘时的进度与打卡统计，只做只读展示。 */
function snapshotSummary(json: string | null): string {
  if (!json) {
    return '无快照';
  }
  try {
    const parsed = JSON.parse(json) as Record<string, unknown>;
    return Object.entries(parsed)
      .map(([key, value]) => `${key}=${String(value)}`)
      .join(' · ');
  } catch {
    return json;
  }
}

async function openReview(reviewId: string): Promise<void> {
  selectedReviewId.value = reviewId;
  conclusion.value = '';
  await recordStore.loadReview(reviewId);
}

async function submitConclusion(): Promise<void> {
  if (!selectedReviewId.value || !conclusion.value.trim()) {
    return;
  }
  await recordStore.completeReview(selectedReviewId.value, conclusion.value.trim());
  conclusion.value = '';
}

async function reload(): Promise<void> {
  await recordStore.loadReviews();
}
</script>

<template>
  <section class="lx-card">
    <header class="lx-row reviews__head">
      <div>
        <h2>周期复盘</h2>
        <p class="lx-muted">
          复盘由服务端按周期自动生成；完成后写入结论。结论一经完成即作为历史记录保留，修改需通过新的复盘周期。
        </p>
      </div>
      <span class="lx-tag">共 {{ recordStore.reviewTotal }} 条</span>
    </header>

    <div class="lx-row reviews__filters">
      <label class="lx-muted">
        目标筛选
        <select v-model="recordStore.reviewGoalFilter" class="lx-input" @change="reload">
          <option value="ALL">全部目标</option>
          <option v-for="goal in goalStore.goals" :key="goal.goalId" :value="goal.goalId">
            {{ goal.title }}
          </option>
        </select>
      </label>
      <button class="lx-button lx-button--ghost" @click="reload">刷新</button>
    </div>

    <p v-if="recordStore.errorMessage" class="lx-error">{{ recordStore.errorMessage }}</p>
    <div v-if="recordStore.reviews.length === 0" class="lx-empty">
      还没有复盘记录，计划生效并跨过第一个周期后会自动生成。
    </div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>周期</th>
          <th>目标</th>
          <th>状态</th>
          <th>完成时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="review in recordStore.reviews" :key="review.reviewId">
          <td>{{ review.periodKey }}</td>
          <td>{{ goalTitle(review.goalId) }}</td>
          <td>
            <span class="lx-tag" :class="{ 'lx-tag--warn': review.status !== 'COMPLETED' }">
              {{ review.status === 'COMPLETED' ? '已完成' : '待完成' }}
            </span>
          </td>
          <td>{{ formatTime(review.completedAt) }}</td>
          <td>
            <button class="lx-button lx-button--ghost" @click="openReview(review.reviewId)">
              查看
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <article v-if="selectedReview" class="lx-card reviews__detail">
      <h3>复盘详情 · {{ selectedReview.periodKey }}</h3>
      <p class="lx-muted">生成时间：{{ formatTime(selectedReview.createdAt) }}</p>
      <p class="lx-muted">输入快照：{{ snapshotSummary(selectedReview.inputSnapshotJson) }}</p>
      <pre v-if="selectedReview.conclusionJson" class="reviews__conclusion">{{
        selectedReview.conclusionJson
      }}</pre>
      <p v-else class="lx-empty">这条复盘还没有填写结论。</p>

      <div v-if="selectedReview.status !== 'COMPLETED'" class="reviews__form">
        <textarea
          v-model="conclusion"
          class="lx-input"
          rows="3"
          placeholder="写下本周期总结与下一周期要调整的地方"
        />
        <button class="lx-button" :disabled="!conclusion.trim()" @click="submitConclusion">
          完成复盘
        </button>
      </div>
    </article>
  </section>
</template>

<style scoped>
.reviews__head {
  justify-content: space-between;
}

.reviews__filters {
  align-items: flex-end;
  flex-wrap: wrap;
  justify-content: space-between;
  margin: var(--lx-space-4) 0;
}

.reviews__detail {
  margin-top: var(--lx-space-5);
}

.reviews__conclusion {
  background: var(--lx-color-primary-soft);
  border-radius: var(--lx-radius-sm);
  padding: var(--lx-space-3);
  white-space: pre-wrap;
}

.reviews__form {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-3);
  margin-top: var(--lx-space-3);
}
</style>
