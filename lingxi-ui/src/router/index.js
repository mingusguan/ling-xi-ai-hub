import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

import Layout from '@/layout'

export const constantRoutes = [
  { path: '/redirect', component: Layout, hidden: true, children: [{ path: '/redirect/:path(.*)', component: () => import('@/views/redirect') }] },
  { path: '/login', component: () => import('@/views/login'), hidden: true },
  { path: '/register', component: () => import('@/views/register'), hidden: true },
  { path: '/404', component: () => import('@/views/error/404'), hidden: true },
  { path: '/401', component: () => import('@/views/error/401'), hidden: true },
  // 灵犀伴行后台为唯一入口：登录后与直接访问根路径都落到运营总览，不再进入旧版子系统选择页
  { path: '', redirect: '/admin/dashboard', hidden: true, alias: '/index' }
]

export const companionAdminRoutes = [
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin/dashboard',
    alwaysShow: true,
    meta: { title: '灵犀伴行后台', icon: 'dashboard', sysCode: 'companion_admin' },
    children: [
      { path: 'dashboard', component: () => import('@/views/companion-admin/index'), name: 'CompanionAdminDashboard', meta: { title: '运营总览', icon: 'dashboard', module: 'dashboard' }, permissions: ['dashboard:read'] },
      { path: 'users', component: () => import('@/views/companion-admin/index'), name: 'CompanionAdminUsers', meta: { title: '用户与会员', icon: 'user', module: 'users' }, permissions: ['identity:user:read', 'identity:risk:manage', 'identity:age-appeal:manage', 'relationship:guardian-dispute:manage'] },
      { path: 'templates', component: () => import('@/views/companion-admin/index'), name: 'CompanionAdminTemplates', meta: { title: '目标模板中心', icon: 'tree', module: 'templates' }, permissions: ['content:template:read'] },
      { path: 'ai-config', component: () => import('@/views/companion-admin/index'), name: 'CompanionAdminAiConfig', meta: { title: 'AI 配置中心', icon: 'edit', module: 'aiConfig' }, permissions: ['config.release.create', 'config.release.validate', 'config.release.approve', 'config.release.publish', 'config.release.rollback'] },
      { path: 'safety', component: () => import('@/views/companion-admin/index'), name: 'CompanionAdminSafety', meta: { title: '内容安全中心', icon: 'monitor', module: 'safety' }, permissions: ['safety:case:manage'] },
      { path: 'engagement', component: () => import('@/views/companion-admin/index'), name: 'CompanionAdminEngagement', meta: { title: '消息与触达', icon: 'bell', module: 'engagement' }, permissions: ['message:campaign:manage'] },
      { path: 'commerce', component: () => import('@/views/companion-admin/index'), name: 'CompanionAdminCommerce', meta: { title: '订单与权益', icon: 'money', module: 'commerce' }, permissions: ['commerce:read', 'commerce:catalog:manage', 'commerce:reconcile:manage', 'commerce:entitlement:adjust', 'commerce.refund.confirm'] },
      { path: 'support', component: () => import('@/views/companion-admin/index'), name: 'CompanionAdminSupport', meta: { title: '客服与反馈', icon: 'message', module: 'support' }, permissions: ['support.ticket.manage', 'support:ticket:reply'] },
      { path: 'analytics', component: () => import('@/views/companion-admin/index'), name: 'CompanionAdminAnalytics', meta: { title: '数据分析', icon: 'chart', module: 'analytics' }, permissions: ['analytics:read', 'experiment:manage'] },
      { path: 'system', component: () => import('@/views/companion-admin/index'), name: 'CompanionAdminSystem', meta: { title: '系统与合规', icon: 'setting', module: 'system' }, permissions: ['audit:read', 'identity:admin:manage', 'compliance:manage', 'app:release:manage', 'feature:flag:manage'] }
    ]
  }
]

export const knowledgeRoutes = [
  {
    path: '/knowledge',
    component: Layout,
    redirect: '/knowledge/document',
    hidden: true,
    children: [
      { path: 'category', component: () => import('@/views/knowledge/category/index'), name: 'KnowledgeCategory', meta: { title: '分类管理', icon: 'tree' }, permissions: ['knowledge:category:list'] },
      { path: 'document', component: () => import('@/views/knowledge/document/index'), name: 'KnowledgeDocument', meta: { title: '文档管理', icon: 'documentation' }, permissions: ['knowledge:document:list'] },
      { path: 'operation', component: () => import('@/views/knowledge/operation/index'), name: 'KnowledgeOperation', meta: { title: '知识运营中心', icon: 'chart' }, permissions: ['knowledge:operation:view'] }
    ]
  }
]

export const oaRoutes = [
  {
    path: '/oa',
    component: Layout,
    redirect: '/oa/dashboard',
    hidden: true,
    children: [
      { path: 'dashboard', component: () => import('@/views/oa/dashboard/index'), name: 'OaDashboard', meta: { title: 'OA工作台', icon: 'dashboard', sysCode: 'oa' }, permissions: ['oa:dashboard:view'] },
      { path: 'process', component: () => import('@/views/oa/process/index'), name: 'OaProcess', meta: { title: '模板管理', icon: 'tree', sysCode: 'oa' }, permissions: ['oa:process:list'] },
      { path: 'leave', component: () => import('@/views/oa/leave/index'), name: 'OaLeave', meta: { title: '请假管理', icon: 'date', sysCode: 'oa' }, permissions: ['oa:leave:list'] },
      { path: 'balance', component: () => import('@/views/oa/leaveQuota/balance'), name: 'OaLeaveBalance', meta: { title: '假期余额', icon: 'guide', sysCode: 'oa' }, permissions: ['oa:leave:balance'] },
      { path: 'rule', component: () => import('@/views/oa/leaveQuota/rule'), name: 'OaLeaveRule', meta: { title: '假期规则', icon: 'form', sysCode: 'oa' }, permissions: ['oa:leave:rule:list'] },
      { path: 'manage', component: () => import('@/views/oa/leaveQuota/manage'), name: 'OaLeaveQuotaManage', meta: { title: '额度管理', icon: 'edit', sysCode: 'oa' }, permissions: ['oa:leave:quota:list'] },
      { path: 'adjustment', component: () => import('@/views/oa/leaveQuota/adjustment'), name: 'OaLeaveAdjustment', meta: { title: '调整记录', icon: 'log', sysCode: 'oa' }, permissions: ['oa:leave:adjustment:list'] },
      { path: 'expense', component: () => import('@/views/oa/expense/index'), name: 'OaExpense', meta: { title: '报销管理', icon: 'money', sysCode: 'oa' }, permissions: ['oa:expense:list'] },
      { path: 'reminder', component: () => import('@/views/oa/reminder/index'), name: 'OaReminder', meta: { title: '消息中心', icon: 'bell', sysCode: 'oa' }, permissions: ['oa:reminder:list'] },
      { path: 'holiday', component: () => import('@/views/oa/holiday/index'), name: 'OaHoliday', meta: { title: '假期管理', icon: 'bell', sysCode: 'oa' }, permissions: ['oa:holiday:list'] }
    ]
  },
  { path: '/oa', redirect: '/oa/dashboard', hidden: true }
]

export const aiRoutes = [
  {
    path: '/ai',
    component: Layout,
    redirect: '/ai/document',
    hidden: true,
    children: [
      { path: 'document', component: () => import('@/views/ai/document/index'), name: 'AiDocument', meta: { title: 'AI公文助手', icon: 'documentation', sysCode: 'ai_tool' }, permissions: ['ai:document:view'] },
      { path: 'report', component: () => import('@/views/ai/report/index'), name: 'AiReport', meta: { title: 'AI报表解读', icon: 'chart', sysCode: 'ai_tool' }, permissions: ['ai:report:view'] },
      { path: 'mcp-market', component: () => import('@/views/ai/mcp-market/index'), name: 'AiMcpMarket', meta: { title: 'MCP工具市场', icon: 'list', sysCode: 'ai_tool' }, permissions: ['ai:mcp:market:list'] },
      { path: 'agent', component: () => import('@/views/ai/agent/index'), name: 'AiAgent', meta: { title: 'Agent编排中心', icon: 'tree', sysCode: 'ai_tool' }, permissions: ['ai:agent:list'] },
      { path: 'governance', component: () => import('@/views/ai/governance/index'), name: 'AiGovernance', meta: { title: 'AI治理审计', icon: 'monitor', sysCode: 'ai_tool' }, permissions: ['ai:governance:view'] }
    ]
  },
  { path: '/ai', redirect: '/ai/document', hidden: true }
]

export const mcpMarketRedirectRoutes = [
  { path: '/mcp-market', redirect: '/ai/mcp-market', hidden: true },
  { path: '/mcp-market/tools', redirect: '/ai/mcp-market', hidden: true }
]

export const dynamicRoutes = [
  { path: '/system/user-auth', component: Layout, hidden: true, permissions: ['system:user:edit'], children: [{ path: 'role/:userId(\\d+)', component: () => import('@/views/system/user/authRole'), name: 'AuthRole', meta: { title: '分配角色', activeMenu: '/system/user' } }] },
  { path: '/system/role-auth', component: Layout, hidden: true, permissions: ['system:role:edit'], children: [{ path: 'user/:roleId(\\d+)', component: () => import('@/views/system/role/authUser'), name: 'AuthUser', meta: { title: '分配用户', activeMenu: '/system/role' } }] },
  { path: '/system/dict-data', component: Layout, hidden: true, permissions: ['system:dict:list'], children: [{ path: 'index/:dictId(\\d+)', component: () => import('@/views/system/dict/data'), name: 'Data', meta: { title: '字典数据', activeMenu: '/system/dict' } }] },
  { path: '/monitor/job-log', component: Layout, hidden: true, permissions: ['monitor:job:list'], children: [{ path: 'index/:jobId(\\d+)', component: () => import('@/views/monitor/job/log'), name: 'JobLog', meta: { title: '调度日志', activeMenu: '/monitor/job' } }] },
  { path: '/tool/gen-edit', component: Layout, hidden: true, permissions: ['tool:gen:edit'], children: [{ path: 'index/:tableId(\\d+)', component: () => import('@/views/tool/gen/editTable'), name: 'GenEdit', meta: { title: '修改生成配置', activeMenu: '/tool/gen' } }] }
]

let routerPush = Router.prototype.push
let routerReplace = Router.prototype.replace
Router.prototype.push = function push(location) { return routerPush.call(this, location).catch(err => err) }
Router.prototype.replace = function replace(location) { return routerReplace.call(this, location).catch(err => err) }

const createRouter = () => new Router({
  mode: 'history',
  base: process.env.VUE_APP_PUBLIC_PATH || '/',
  scrollBehavior: () => ({ y: 0 }),
  routes: [...constantRoutes, ...companionAdminRoutes, ...knowledgeRoutes, ...oaRoutes, ...aiRoutes, ...mcpMarketRedirectRoutes]
})

const router = createRouter()

export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher
}

export default router
