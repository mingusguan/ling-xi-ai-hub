import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useSessionStore } from '@/stores/session';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/components/AppShell.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '今日行动' } },
      { path: 'goals', name: 'goals', component: () => import('@/views/GoalsView.vue'), meta: { title: '我的目标' } },
      {
        path: 'goals/:goalId',
        name: 'goal-detail',
        component: () => import('@/views/GoalDetailView.vue'),
        meta: { title: '目标详情' }
      },
      {
        path: 'achievements',
        name: 'achievements',
        component: () => import('@/views/AchievementsView.vue'),
        meta: { title: '我的成就' }
      },
      {
        path: 'companion',
        name: 'companion',
        component: () => import('@/views/CompanionView.vue'),
        meta: { title: '灵犀对话' }
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: () => import('@/views/NotificationsView.vue'),
        meta: { title: '消息与同步' }
      },
      {
        path: 'guardian',
        name: 'guardian',
        component: () => import('@/views/GuardianView.vue'),
        meta: { title: '监护与青少年模式' }
      },
      {
        path: 'privacy',
        name: 'privacy',
        component: () => import('@/views/PrivacyView.vue'),
        meta: { title: '隐私与账号' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
];

export const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const session = useSessionStore();
  if (!to.meta.public && !session.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (to.meta.public && session.isAuthenticated) {
    return { name: 'home' };
  }
  return true;
});

router.afterEach((to) => {
  const title = (to.meta.title as string | undefined) ?? '灵犀伴行';
  document.title = `${title} · 灵犀伴行`;
});
