import router from './router'
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
  if (path.startsWith('/oa')) {
    return 'oa'
  }
  if (path.startsWith('/system') || path.startsWith('/monitor') || path.startsWith('/tool') || path.startsWith('/index')) {
    return 'basic'
  }
  return ''
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
    if (to.path === '/login') {
      next({ path: '/subsystem' })
      NProgress.done()
    } else if (isWhiteList(to.path)) {
      next()
    } else {
      if (!store.getters.sysCode) {
        const resolvedSysCode = resolveSysCodeByPath(to.path)
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
            next({ path: '/subsystem' })
          }
        }).catch(err => {
          store.dispatch('LogOut').then(() => {
            Message.error(err)
            next({ path: '/' })
          })
        })
      } else if (store.getters.sysCode && store.getters.sidebarRouters.length === 0) {
        store.dispatch('GenerateRoutes').then(accessRoutes => {
          router.addRoutes(accessRoutes)
          next({ ...to, replace: true })
        })
      } else if (!store.getters.sysCode && to.path !== '/subsystem') {
        next({ path: '/subsystem' })
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
