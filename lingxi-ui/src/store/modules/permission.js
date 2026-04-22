import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes, oaRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'
import store from '@/store'

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
        getRouters(sysCode).then(res => {
          const addPathPrefix = (routes, prefix) => {
            routes.forEach(route => {
              if (route.path) {
                route.path = route.path.replace(new RegExp('^' + prefix.replace(/\//g, '\\/')), '')
                route.path = route.path.replace(/^\/?oa\//, '')
                route.path = route.path.replace(/^\/?knowledge\//, '')
                // 所有路由(包括children)都统一加前缀！
                route.path = prefix + route.path.replace(/^\//, '')
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
          const sidebarRoutes = filterAsyncRouter(sdata)
          const rewriteRoutes = filterAsyncRouter(rdata, false, true)
          const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
          rewriteRoutes.push({ path: '*', redirect: '/404', hidden: true })
          router.addRoutes(asyncRoutes)

          if (sysCode === 'oa' && rewriteRoutes.length <= 1) {
            const staticOaRoutes = JSON.parse(JSON.stringify(oaRoutes))
            const filteredRoutes = filterDynamicRoutes(staticOaRoutes)
            commit('SET_ROUTES', filteredRoutes)
            commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(filteredRoutes))
            commit('SET_DEFAULT_ROUTES', filteredRoutes)
            commit('SET_TOPBAR_ROUTES', filteredRoutes)
            resolve(filteredRoutes)
            return
          }

          commit('SET_ROUTES', rewriteRoutes)
          commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(sidebarRoutes))
          commit('SET_DEFAULT_ROUTES', sidebarRoutes)
          commit('SET_TOPBAR_ROUTES', sidebarRoutes)
          resolve(rewriteRoutes)
        }).catch(() => {
          if (sysCode === 'oa') {
            const staticOaRoutes = JSON.parse(JSON.stringify(oaRoutes))
            const filteredRoutes = filterDynamicRoutes(staticOaRoutes)
            commit('SET_ROUTES', filteredRoutes)
            commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(filteredRoutes))
            commit('SET_DEFAULT_ROUTES', filteredRoutes)
            commit('SET_TOPBAR_ROUTES', filteredRoutes)
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
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
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
    // 先过滤children
    if (route.children && route.children.length > 0) {
      // 递归过滤子路由
      const filteredChildren = []
      route.children.forEach(child => {
        // 子路由有permissions的话检查权限
        if (child.permissions) {
          if (auth.hasPermiOr(child.permissions)) {
            filteredChildren.push(child)
          }
        } else if (child.roles) {
          if (auth.hasRoleOr(child.roles)) {
            filteredChildren.push(child)
          }
        } else {
          // 没有权限要求的直接保留
          filteredChildren.push(child)
        }
      })
      // 更新过滤后的children
      route.children = filteredChildren
    }
    // 顶层路由有permissions的话检查权限，否则直接保留
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    } else {
      // 顶层路由没有权限要求（如父路由Layout），直接保留
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
