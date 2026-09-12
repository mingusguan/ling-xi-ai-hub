<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { useGoalStore } from '@/stores/goal';

const goalStore = useGoalStore();
const router = useRouter();
const form = reactive({ title: '', successCriteria: '' });
const creating = ref(false);
const createError = ref<string | null>(null);

onMounted(() => {
  void goalStore.loadGoals();
});

async function create(): Promise<void> {
  createError.value = null;
  if (!form.title.trim() || !form.successCriteria.trim()) {
    createError.value = '目标标题与成功标准都不能为空';
    return;
  }
  creating.value = true;
  const created = await goalStore.createGoal(form.title.trim(), form.successCriteria.trim());
  creating.value = false;
  if (created) {
    form.title = '';
    form.successCriteria = '';
    await router.push({ name: 'goal-detail', params: { goalId: created.goalId } });
  } else {
    createError.value = goalStore.errorMessage;
  }
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
      <label>成功标准</label>
      <input v-model="form.successCriteria" class="lx-input" placeholder="例如 连续 30 天完成阅读" />
    </div>
    <p v-if="createError" class="lx-error">{{ createError }}</p>
    <button class="lx-button" :disabled="creating" @click="create">
      {{ creating ? '创建中…' : '创建草稿目标' }}
    </button>
  </section>

  <section class="goal-section">
    <h2>我的目标</h2>
    <p v-if="goalStore.errorMessage" class="lx-error">{{ goalStore.errorMessage }}</p>
    <div v-if="goalStore.goals.length === 0" class="lx-empty">还没有目标，先创建一个吧。</div>
    <div v-else class="lx-grid">
      <article v-for="goal in goalStore.goals" :key="goal.goalId" class="lx-card goal-card">
        <header class="lx-row goal-card__head">
          <strong>{{ goal.title }}</strong>
          <span class="lx-tag">{{ goal.status }}</span>
        </header>
        <p class="lx-muted">{{ goal.successCriteria }}</p>
        <div class="goal-card__progress">
          <div class="goal-card__bar" :style="{ width: `${goal.progress}%` }" />
        </div>
        <p class="lx-muted">进度 {{ goal.progress }}% · 版本 {{ goal.version }}</p>
        <RouterLink class="lx-button" :to="{ name: 'goal-detail', params: { goalId: goal.goalId } }">
          查看与编辑计划
        </RouterLink>
      </article>
    </div>
  </section>
</template>

<style scoped>
.goal-section {
  margin-top: var(--lx-space-5);
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
