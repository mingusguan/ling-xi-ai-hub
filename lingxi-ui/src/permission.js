import router, { resetRouter } from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

const resolveSysCodeByPath = (path) => {
  if (!path) {
    return ''
  }
  if (path.startsWith('/knowledge')) {
    return 'knowledge'
  }
  if (path.startsWith('/ai') || path.startsWith('/mcp-market')) {
    return 'ai_tool'
  }
  if (path.startsWith('/oa')) {
    return 'oa'
  }
  if (path.startsWith('/admin')) {
    return 'companion_admin'
  }
  return ''
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
    if (to.path === '/login') {
      next({ path: '/admin/dashboard' })
      NProgress.done()
    } else if (isWhiteList(to.path)) {
      next()
    } else {
      const resolvedSysCode = resolveSysCodeByPath(to.path)
      if (resolvedSysCode && resolvedSysCode !== store.getters.sysCode) {
        resetRouter()
        store.commit('SET_SYS_CODE', resolvedSysCode)
        store.commit('SET_SIDEBAR_ROUTERS', [])
      } else if (!store.getters.sysCode) {
        if (resolvedSysCode) {
          store.commit('SET_SYS_CODE', resolvedSysCode)
        }
      }
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        store.dispatch('GetInfo').then(() => {
          isRelogin.show = false
          if (store.getters.sysCode) {
            store.dispatch('GenerateRoutes').then(accessRoutes => {
              router.addRoutes(accessRoutes)
              next({ ...to, replace: true })
            })
          } else {
            next({ path: '/admin/dashboard' })
          }
        }).catch(err => {
          // 会话失效时请求拦截器已提示并跳转登录页，这里只做兜底清理与跳转。
          // token 无效时后端不会签发新会话（否则未登录用户可借此拿 token），因此不尝试登录页接口。
          store.dispatch('LogOut').catch(() => {}).then(() => {
            if (!err || !(err.response && err.response.status === 401)) {
              Message.error(typeof err === 'string' ? err : (err && err.message) || '获取用户信息失败')
            }
            next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
          })
        })
      } else if (store.getters.sysCode && store.getters.sidebarRouters.length === 0) {
        store.dispatch('GenerateRoutes').then(accessRoutes => {
          router.addRoutes(accessRoutes)
          next({ ...to, replace: true })
        })
      } else if (!store.getters.sysCode && to.path !== '/admin/dashboard') {
        next({ path: '/admin/dashboard' })
      } else {
        next()
      }
    }
  } else {
    if (isWhiteList(to.path)) {
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
