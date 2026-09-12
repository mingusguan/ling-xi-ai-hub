<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { useSessionStore } from '@/stores/session';
import { useEngagementStore } from '@/stores/engagement';

const session = useSessionStore();
const engagement = useEngagementStore();
const router = useRouter();
const route = useRoute();

const navItems = computed(() => [
  { name: 'home', label: '今日行动' },
  { name: 'goals', label: '我的目标' },
  { name: 'reviews', label: '周期复盘' },
  { name: 'achievements', label: '我的成就' },
  { name: 'companion', label: '灵犀对话' },
  { name: 'memory', label: '记忆管理' },
  { name: 'notifications', label: '消息与同步' },
  { name: 'calendar', label: '日历同步' },
  { name: 'partner', label: '伙伴与分享' },
  { name: 'assets', label: '内容资产' },
  { name: 'membership', label: '会员与权益' },
  { name: 'guardian', label: '监护' },
  { name: 'support', label: '客服工单' },
  { name: 'privacy', label: '隐私与账号' }
]);

const statusLabel = computed(() => {
  const status = session.profile?.accountStatus;
  if (status === 'PENDING_GUARDIAN') {
    return '待绑定监护人';
  }
  if (status === 'RESTRICTED') {
    return '账号受限';
  }
  return session.isTeenager ? '青少年模式' : '成人模式';
});

onMounted(async () => {
  await engagement.loadNotifications();
});

function logout(): void {
  engagement.stopStream();
  session.logout();
  void router.push({ name: 'login' });
}
</script>

<template>
  <div class="shell">
    <aside class="shell__side">
      <div class="shell__brand">灵犀伴行</div>
      <nav>
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          class="shell__nav"
          :class="{ 'shell__nav--active': route.name === item.name }"
          :to="{ name: item.name }"
        >
          {{ item.label }}
          <span v-if="item.name === 'notifications' && engagement.unreadCount > 0" class="lx-tag">
            {{ engagement.unreadCount }}
          </span>
        </RouterLink>
      </nav>
    </aside>
    <div class="shell__main">
      <header class="shell__header">
        <div>
          <strong>{{ route.meta.title }}</strong>
          <span class="lx-tag" :class="{ 'lx-tag--warn': session.needsGuardian }">{{ statusLabel }}</span>
        </div>
        <div class="lx-row">
          <span class="lx-muted">同步游标 {{ engagement.cursor }}</span>
          <button class="lx-button lx-button--ghost" @click="logout">退出登录</button>
        </div>
      </header>
      <main class="shell__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  min-height: 100vh;
}

.shell__side {
  background: var(--lx-color-surface);
  border-right: 1px solid var(--lx-color-border);
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-5);
  padding: var(--lx-space-5) var(--lx-space-4);
  width: 220px;
}

.shell__brand {
  font-size: 18px;
  font-weight: 700;
}

.shell__nav {
  align-items: center;
  border-radius: var(--lx-radius-sm);
  color: var(--lx-color-text);
  display: flex;
  justify-content: space-between;
  padding: 8px 10px;
}

.shell__nav--active {
  background: var(--lx-color-primary-soft);
  color: var(--lx-color-primary);
  font-weight: 600;
}

.shell__main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.shell__header {
  align-items: center;
  background: var(--lx-color-surface);
  border-bottom: 1px solid var(--lx-color-border);
  display: flex;
  justify-content: space-between;
  padding: var(--lx-space-4) var(--lx-space-5);
}

.shell__content {
  flex: 1;
  padding: var(--lx-space-5);
}
</style>
