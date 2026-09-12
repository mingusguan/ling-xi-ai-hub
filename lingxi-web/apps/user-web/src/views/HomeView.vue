<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { useGoalStore } from '@/stores/goal';
import { useSessionStore } from '@/stores/session';

const goalStore = useGoalStore();
const session = useSessionStore();
const note = ref('');
const busyOccurrenceId = ref<string | null>(null);

const today = new Date().toISOString().slice(0, 10);
const weekEnd = new Date(Date.now() + 6 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

const pendingOccurrences = computed(() =>
  goalStore.occurrences.filter((occurrence) => occurrence.status === 'SCHEDULED')
);
const finishedOccurrences = computed(() =>
  goalStore.occurrences.filter((occurrence) => occurrence.status !== 'SCHEDULED')
);

onMounted(async () => {
  await goalStore.loadGoals();
  await goalStore.loadOccurrences(today, weekEnd);
  await goalStore.loadAchievements();
});

async function checkIn(occurrenceId: string, result: 'COMPLETED' | 'PARTIAL' | 'SKIPPED'): Promise<void> {
  busyOccurrenceId.value = occurrenceId;
  await goalStore.checkIn(occurrenceId, result, note.value || null);
  busyOccurrenceId.value = null;
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

    <section class="lx-card">
      <header class="lx-row lx-row--between">
        <h2>未来 7 天行动</h2>
        <RouterLink class="lx-button lx-button--ghost" :to="{ name: 'goals' }">管理目标</RouterLink>
      </header>

      <div class="lx-field">
        <label>打卡备注（可选）</label>
        <input v-model="note" class="lx-input" placeholder="记录今天的执行感受" />
      </div>

      <p v-if="goalStore.errorMessage" class="lx-error">{{ goalStore.errorMessage }}</p>

      <div v-if="goalStore.occurrences.length === 0" class="lx-empty">
        还没有行动实例。请先在目标页面确认计划，系统会按行动规则滚动生成实例。
      </div>

      <ul v-else class="occurrence-list">
        <li v-for="occurrence in goalStore.occurrences" :key="occurrence.occurrenceId" class="occurrence">
          <div>
            <strong>{{ occurrence.actionTitle }}</strong>
            <div class="lx-muted">
              {{ occurrence.localDate }} · {{ occurrence.timezone }} ·
              <span :class="{ 'lx-tag--warn': occurrence.status === 'SCHEDULED' }" class="lx-tag">
                {{ occurrence.status }}
              </span>
            </div>
          </div>
          <div v-if="occurrence.status === 'SCHEDULED'" class="lx-row">
            <button
              class="lx-button"
              :disabled="busyOccurrenceId === occurrence.occurrenceId"
              @click="checkIn(occurrence.occurrenceId, 'COMPLETED')"
            >
              完成
            </button>
            <button
              class="lx-button lx-button--ghost"
              :disabled="busyOccurrenceId === occurrence.occurrenceId"
              @click="checkIn(occurrence.occurrenceId, 'PARTIAL')"
            >
              部分完成
            </button>
            <button
              class="lx-button lx-button--ghost"
              :disabled="busyOccurrenceId === occurrence.occurrenceId"
              @click="checkIn(occurrence.occurrenceId, 'SKIPPED')"
            >
              跳过
            </button>
          </div>
          <span v-else class="lx-tag">已记录</span>
        </li>
      </ul>

      <p class="lx-muted">
        已记录 {{ finishedOccurrences.length }} 项，待执行 {{ pendingOccurrences.length }} 项；打卡结果会即时返回新获得的成就。
      </p>
    </section>
  </template>
</template>

<style scoped>
.achievement-banner {
  background: var(--lx-color-primary-soft);
  margin-bottom: var(--lx-space-5);
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
  justify-content: space-between;
  padding: var(--lx-space-3);
}

.lx-row--between {
  justify-content: space-between;
}
</style>
