<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { AchievementType } from '@lingxi/api-client';
import { useGoalStore } from '@/stores/goal';

const goalStore = useGoalStore();
const filter = ref<AchievementType | 'ALL'>('ALL');

const typeLabels: Record<AchievementType, string> = {
  GOAL_COMPLETED: '目标达成',
  MILESTONE_COMPLETED: '里程碑完成',
  CHECK_IN_STREAK: '连续打卡'
};

const visible = computed(() =>
  filter.value === 'ALL'
    ? goalStore.achievements
    : goalStore.achievements.filter((item) => item.type === filter.value)
);

onMounted(() => {
  void goalStore.loadAchievements();
});

function formatDate(value: string): string {
  return new Date(value).toLocaleString('zh-CN');
}
</script>

<template>
  <section class="lx-card">
    <header class="lx-row achievements__head">
      <div>
        <h2>我的成就</h2>
        <p class="lx-muted">
          成就只由确定性领域事实触发：目标达成、里程碑完成、连续打卡达到阈值；授予后不可撤销。
        </p>
      </div>
      <span class="lx-tag">共 {{ goalStore.achievementTotal }} 项</span>
    </header>

    <div class="lx-row achievements__filters">
      <button
        v-for="item in (['ALL', 'GOAL_COMPLETED', 'MILESTONE_COMPLETED', 'CHECK_IN_STREAK'] as const)"
        :key="item"
        class="lx-button"
        :class="{ 'lx-button--ghost': filter !== item }"
        @click="filter = item"
      >
        {{ item === 'ALL' ? '全部' : typeLabels[item] }}
      </button>
    </div>

    <p v-if="goalStore.errorMessage" class="lx-error">{{ goalStore.errorMessage }}</p>
    <div v-if="visible.length === 0" class="lx-empty">还没有成就，完成目标或连续打卡即可获得。</div>
    <div v-else class="lx-grid">
      <article v-for="achievement in visible" :key="achievement.achievementId" class="lx-card achievement">
        <span class="lx-tag">{{ typeLabels[achievement.type] }}</span>
        <h3>{{ achievement.title }}</h3>
        <p class="lx-muted">{{ achievement.description }}</p>
        <p class="lx-muted">达成时间：{{ formatDate(achievement.achievedAt) }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.achievements__head {
  justify-content: space-between;
}

.achievements__filters {
  flex-wrap: wrap;
  margin: var(--lx-space-4) 0;
}

.achievement {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-2);
}
</style>
