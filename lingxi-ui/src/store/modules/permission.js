import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes, oaRoutes, aiRoutes, knowledgeRoutes, companionAdminRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'
import store from '@/store'
import { joinRoutePath } from '@/utils/validate'

const cloneRoutes = (routes) => {
  return routes.map(route => ({
    ...route,
    meta: route.meta ? { ...route.meta } : route.meta,
    children: route.children ? cloneRoutes(route.children) : route.children
  }))
}

const trimSlashes = (value) => String(value || '').replace(/^\/+|\/+$/g, '')

const normalizeWithPrefix = (path, prefix) => {
  const cleanPrefix = trimSlashes(prefix)
  const cleanPath = trimSlashes(path)
  const relativePath = cleanPath === cleanPrefix
    ? ''
    : cleanPath.replace(new RegExp(`^${cleanPrefix}/`), '')
  return relativePath ? `/${cleanPrefix}/${relativePath}` : `/${cleanPrefix}`
}

const hasRenderableRoute = (routes) => {
  return routes.some(route => {
    if (route.hidden) {
      return false
    }
    if (route.children && route.children.length) {
      return hasRenderableRoute(route.children)
    }
    return route.component && ![Layout, ParentView, InnerLink, 'Layout', 'ParentView', 'InnerLink'].includes(route.component)
  })
}

const staticRoutesBySysCode = (sysCode) => {
  if (sysCode === 'oa') {
    return oaRoutes
  }
  if (sysCode === 'knowledge') {
    return knowledgeRoutes
  }
  if (sysCode === 'ai_tool') {
    return aiRoutes
  }
  if (sysCode === 'companion_admin') {
    return companionAdminRoutes
  }
  return null
}

const commitRoutes = (commit, routes) => {
  ensureDirectoryRedirects(routes)
  commit('SET_ROUTES', routes)
  commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(routes))
  commit('SET_DEFAULT_ROUTES', routes)
  commit('SET_TOPBAR_ROUTES', routes)
}

const isNoRedirect = (redirect) => {
  return redirect === 'noRedirect' || redirect === 'noredirect'
}

const firstVisibleRoutePath = (route, parentPath = '') => {
  if (!route || route.hidden) {
    return ''
  }
  const currentPath = joinRoutePath(parentPath, route.path)
  if (route.children && route.children.length) {
    for (const child of route.children) {
      const childPath = firstVisibleRoutePath(child, currentPath)
      if (childPath) {
        return childPath
      }
    }
    return ''
  }
  return currentPath
}

const ensureDirectoryRedirects = (routes, parentPath = '') => {
  routes.forEach(route => {
    const currentPath = joinRoutePath(parentPath, route.path)
    if (route.children && route.children.length) {
      if (!route.redirect || isNoRedirect(route.redirect)) {
        route.redirect = firstVisibleRoutePath(route, parentPath) || route.redirect
      }
      ensureDirectoryRedirects(route.children, currentPath)
    }
  })
}

const permission = {
  state: {
    routes: [],
    addRoutes: [],
    defaultRoutes: [],
    topbarRouters: [],
    sidebarRouters: []
  },
  mutations: {
    SET_ROUTES: (state, routes) => {
      state.addRoutes = routes
      state.routes = constantRoutes.concat(routes)
    },
    SET_DEFAULT_ROUTES: (state, routes) => {
      state.defaultRoutes = constantRoutes.concat(routes)
    },
    SET_TOPBAR_ROUTES: (state, routes) => {
      state.topbarRouters = routes
    },
    SET_SIDEBAR_ROUTERS: (state, routes) => {
      state.sidebarRouters = routes
    }
  },
  actions: {
    GenerateRoutes({ commit }) {
      return new Promise(resolve => {
        const sysCode = store.getters.sysCode
        if (sysCode === 'companion_admin') {
          const filteredRoutes = filterDynamicRoutes(cloneRoutes(companionAdminRoutes))
          commitRoutes(commit, filteredRoutes)
          resolve(filteredRoutes)
          return
        }
        getRouters(sysCode).then(res => {
          const addPathPrefix = (routes, prefix) => {
            routes.forEach(route => {
              if (route.path) {
                route.path = normalizeWithPrefix(route.path, prefix)
              }
              if (route.children && route.children.length) {
                addPathPrefix(route.children, prefix)
              }
            })
          }
          const sdata = JSON.parse(JSON.stringify(res.data || []))
          const rdata = JSON.parse(JSON.stringify(res.data || []))
          if (sysCode === 'oa' || sysCode === 'knowledge') {
            const prefix = '/' + sysCode + '/'
            addPathPrefix(sdata, prefix)
            addPathPrefix(rdata, prefix)
          }
          if (sysCode === 'ai_tool') {
            const prefix = '/ai/'
            addPathPrefix(sdata, prefix)
            addPathPrefix(rdata, prefix)
          }
          const sidebarRoutes = filterAsyncRouter(sdata)
          const rewriteRoutes = filterAsyncRouter(rdata, false, true)
          const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
          router.addRoutes(asyncRoutes)

          const staticRoutes = staticRoutesBySysCode(sysCode)
          if (staticRoutes && !hasRenderableRoute(rewriteRoutes)) {
            const filteredRoutes = filterDynamicRoutes(cloneRoutes(staticRoutes))
            commitRoutes(commit, filteredRoutes)
            resolve(filteredRoutes)
            return
          }

          ensureDirectoryRedirects(rewriteRoutes)
          ensureDirectoryRedirects(sidebarRoutes)
          rewriteRoutes.push({ path: '*', redirect: '/404', hidden: true })
          commit('SET_ROUTES', rewriteRoutes)
          commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(sidebarRoutes))
          commit('SET_DEFAULT_ROUTES', sidebarRoutes)
          commit('SET_TOPBAR_ROUTES', sidebarRoutes)
          resolve(rewriteRoutes)
        }).catch(() => {
          const staticRoutes = staticRoutesBySysCode(sysCode)
          if (staticRoutes) {
            const filteredRoutes = filterDynamicRoutes(cloneRoutes(staticRoutes))
            commitRoutes(commit, filteredRoutes)
            resolve(filteredRoutes)
            return
          }
          resolve([])
        })
      })
    }
  }
}

function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route.children
      delete route.redirect
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  let children = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? joinRoutePath(lastRouter.path, el.path) : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.children && route.children.length > 0) {
      const filteredChildren = []
      route.children.forEach(child => {
        if (child.permissions) {
          if (auth.hasPermiOr(child.permissions)) {
            filteredChildren.push(child)
          }
        } else if (child.roles) {
          if (auth.hasRoleOr(child.roles)) {
            filteredChildren.push(child)
          }
        } else {
          filteredChildren.push(child)
        }
      })
      route.children = filteredChildren
    }
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    } else {
      res.push(route)
    }
  })
  return res
}

export const loadView = (view) => {
  if (process.env.NODE_ENV === 'development') {
    return (resolve) => require([`@/views/${view}`], resolve)
  }
  return () => import(`@/views/${view}`)
}

export default permission
