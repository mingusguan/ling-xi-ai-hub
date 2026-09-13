<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { useSessionStore } from '@/stores/session';
import { useEngagementStore } from '@/stores/engagement';

const session = useSessionStore();
const engagement = useEngagementStore();
const router = useRouter();
const route = useRoute();

/**
 * 导航按「今天 → 长期 → 支持」分组，14 项平铺时很难扫读；
 * 分组只影响展示，不改路由与权限。
 */
const navGroups = computed(() => [
  {
    title: '今天',
    items: [
      { name: 'home', label: '今日行动' },
      { name: 'goals', label: '我的目标' },
      { name: 'reviews', label: '周期复盘' }
    ]
  },
  {
    title: '成长',
    items: [
      { name: 'achievements', label: '我的成就' },
      { name: 'companion', label: '灵犀对话' },
      { name: 'memory', label: '记忆管理' }
    ]
  },
  {
    title: '协同',
    items: [
      { name: 'partners', label: '伙伴与分享', to: { name: 'partner' } },
      { name: 'notifications', label: '消息与同步' },
      { name: 'calendar', label: '日历同步' },
      { name: 'assets', label: '内容资产' }
    ]
  },
  {
    title: '账号',
    items: [
      { name: 'membership', label: '会员与权益' },
      { name: 'guardian', label: '监护' },
      { name: 'support', label: '客服工单' },
      { name: 'privacy', label: '隐私与账号' }
    ]
  }
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

const currentTitle = computed(() => (route.meta.title as string | undefined) ?? '');

onMounted(async () => {
  await engagement.loadNotifications();
});

function isActive(item: { name: string; to?: { name: string } }): boolean {
  const target = item.to?.name ?? item.name;
  return route.name === target;
}

function logout(): void {
  engagement.stopStream();
  session.logout();
  void router.push({ name: 'login' });
}
</script>

<template>
  <div class="shell">
    <aside class="shell__side">
      <div class="shell__brand">
        <span class="shell__brand-mark">灵</span>
        <span class="shell__brand-text">灵犀伴行</span>
      </div>

      <nav class="shell__nav-list">
        <div v-for="group in navGroups" :key="group.title" class="shell__group">
          <p class="shell__group-title">{{ group.title }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.name"
            class="shell__nav"
            :class="{ 'shell__nav--active': isActive(item) }"
            :to="item.to ?? { name: item.name }"
          >
            <span class="shell__nav-label">{{ item.label }}</span>
            <span
              v-if="item.name === 'notifications' && engagement.unreadCount > 0"
              class="shell__badge"
            >{{ engagement.unreadCount }}</span>
          </RouterLink>
        </div>
      </nav>
    </aside>

    <div class="shell__main">
      <header class="shell__header">
        <div class="shell__header-left">
          <h1 class="shell__title">{{ currentTitle }}</h1>
          <span class="lx-tag" :class="{ 'lx-tag--warn': session.needsGuardian }">{{ statusLabel }}</span>
        </div>
        <div class="shell__header-right">
          <span class="shell__cursor lx-muted">同步游标 {{ engagement.cursor }}</span>
          <button class="lx-button lx-button--ghost shell__logout" @click="logout">退出登录</button>
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

/* --------------------------------------------------------------------------
   侧边栏
   -------------------------------------------------------------------------- */
.shell__side {
  display: flex;
  flex-direction: column;
  gap: 26px;
  width: 236px;
  flex-shrink: 0;
  padding: 24px 16px;
  background: rgba(13, 20, 34, 0.94);
  border-right: 1px solid var(--lx-color-border);
  backdrop-filter: blur(18px);
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.shell__brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 6px;
}

.shell__brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #34C08D 0%, #1E9E73 100%);
  color: #07130E;
  font-size: 17px;
  font-weight: 700;
  box-shadow: 0 6px 16px rgba(52, 192, 141, 0.3);
}

.shell__brand-text {
  color: var(--lx-color-text);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.shell__nav-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.shell__group-title {
  margin: 0 0 8px 10px;
  color: #7C8CA3;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.4px;
  text-transform: uppercase;
}

.shell__group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.shell__nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 12px;
  border-radius: 9px;
  border-left: 3px solid transparent;
  color: #B7C4D6;
  font-size: 14.5px;
  transition: background 0.15s ease, color 0.15s ease;
}

.shell__nav:hover {
  background: rgba(52, 192, 141, 0.12);
  color: #FFFFFF;
}

.shell__nav--active {
  background: linear-gradient(90deg, rgba(52, 192, 141, 0.26) 0%, rgba(52, 192, 141, 0.08) 100%);
  border-left-color: var(--lx-color-primary);
  color: #FFFFFF;
  font-weight: 600;
}

.shell__nav-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shell__badge {
  min-width: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--lx-color-primary);
  color: var(--lx-color-text-inverse);
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}

/* --------------------------------------------------------------------------
   主区域
   -------------------------------------------------------------------------- */
.shell__main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.shell__header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 32px;
  background: rgba(13, 20, 34, 0.86);
  border-bottom: 1px solid var(--lx-color-border);
  backdrop-filter: blur(18px);
}

.shell__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.shell__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.shell__header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.shell__cursor {
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.shell__logout {
  padding: 7px 14px;
  font-size: 13.5px;
}

.shell__content {
  flex: 1;
  padding: 28px 32px 48px;
  min-width: 0;
}

@media (max-width: 1100px) {
  .shell__side {
    width: 208px;
  }

  .shell__header,
  .shell__content {
    padding-left: 22px;
    padding-right: 22px;
  }
}
</style>
