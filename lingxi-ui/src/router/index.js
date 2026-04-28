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
  { path: '/subsystem', component: () => import('@/views/subsystem'), hidden: true, meta: { title: '子系统选择' } },
  { path: '', component: Layout, redirect: '/subsystem', hidden: true, children: [{ path: 'index', component: () => import('@/views/index'), name: 'Index', meta: { title: '首页', icon: 'dashboard' } }] },
  { path: '/user', component: Layout, hidden: true, redirect: 'noredirect', children: [{ path: 'profile', component: () => import('@/views/system/user/profile/index'), name: 'Profile', meta: { title: '个人中心', icon: 'user' } }] },
  { path: '/message', component: Layout, hidden: true, children: [{ path: 'center', component: () => import('@/views/message/center/index'), name: 'MessageCenter', meta: { title: '消息中心', icon: 'bell' } }] },
  { path: '/system', component: Layout, hidden: true, redirect: '/system/user', children: [
    { path: 'user', component: () => import('@/views/system/user/index'), name: 'SystemUser', meta: { title: '用户管理', activeMenu: '/system/user' } },
    { path: 'role', component: () => import('@/views/system/role/index'), name: 'SystemRole', meta: { title: '角色管理', activeMenu: '/system/role' } },
    { path: 'menu', component: () => import('@/views/system/menu/index'), name: 'SystemMenu', meta: { title: '菜单管理', activeMenu: '/system/menu' } },
    { path: 'dept', component: () => import('@/views/system/dept/index'), name: 'SystemDept', meta: { title: '部门管理', activeMenu: '/system/dept' } },
    { path: 'post', component: () => import('@/views/system/post/index'), name: 'SystemPost', meta: { title: '岗位管理', activeMenu: '/system/post' } },
    { path: 'dict', component: () => import('@/views/system/dict/index'), name: 'SystemDict', meta: { title: '字典管理', activeMenu: '/system/dict' } },
    { path: 'config', component: () => import('@/views/system/config/index'), name: 'SystemConfig', meta: { title: '参数设置', activeMenu: '/system/config' } },
    { path: 'notice', component: () => import('@/views/system/notice/index'), name: 'SystemNotice', meta: { title: '通知公告', activeMenu: '/system/notice' } },
    { path: 'operlog', component: () => import('@/views/system/operlog/index'), name: 'SystemOperlog', meta: { title: '操作日志', activeMenu: '/system/operlog' } },
    { path: 'logininfor', component: () => import('@/views/system/logininfor/index'), name: 'SystemLogininfor', meta: { title: '登录日志', activeMenu: '/system/logininfor' } }
  ] },
  { path: '/monitor', component: Layout, hidden: true, children: [
    { path: 'online', component: () => import('@/views/monitor/online/index'), name: 'MonitorOnline', meta: { title: '在线用户', activeMenu: '/monitor/online' } },
    { path: 'job', component: () => import('@/views/monitor/job/index'), name: 'MonitorJob', meta: { title: '定时任务', activeMenu: '/monitor/job' } }
  ] },
  { path: '/tool', component: Layout, hidden: true, children: [
    { path: 'gen', component: () => import('@/views/tool/gen/index'), name: 'ToolGen', meta: { title: '代码生成', activeMenu: '/tool/gen' } },
    { path: 'build', component: () => import('@/views/tool/build/index'), name: 'ToolBuild', meta: { title: '表单构建', activeMenu: '/tool/build' } }
  ] }
]

export const knowledgeRoutes = [
  {
    path: '/knowledge',
    component: Layout,
    redirect: 'noredirect',
    hidden: true,
    children: [
      { path: 'category', component: () => import('@/views/knowledge/category/index'), name: 'KnowledgeCategory', meta: { title: '分类管理', icon: 'tree' }, permissions: ['knowledge:category:list'] },
      { path: 'document', component: () => import('@/views/knowledge/document/index'), name: 'KnowledgeDocument', meta: { title: '文档管理', icon: 'documentation' }, permissions: ['knowledge:document:list'] }
    ]
  }
]

export const oaRoutes = [
  {
    path: '/oa',
    component: Layout,
    redirect: 'noredirect',
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

export default new Router({
  mode: 'history',
  scrollBehavior: () => ({ y: 0 }),
  routes: [...constantRoutes, ...knowledgeRoutes, ...oaRoutes]
})
