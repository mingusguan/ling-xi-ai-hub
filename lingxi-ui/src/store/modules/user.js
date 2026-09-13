import router from '@/router'
import { MessageBox, } from 'element-ui'
import { login, logout, getInfo, refreshToken } from '@/api/login'
import { getToken, setToken, setExpiresIn, removeToken, setSessionExpiresAt, removeSessionExpiresAt } from '@/utils/auth'
import { isEmpty } from "@/utils/validate"
import { filePreviewUrl } from '@/utils/appPath'
import defAva from '@/assets/images/profile.jpg'
import cache from '@/plugins/cache'

const user = {
  state: {
    token: getToken(),
    id: '',
    deptId: '',
    name: '',
    nickName: '',
    avatar: '',
    roles: [],
    permissions: [],
    sysCode: cache.session.get('sysCode') || ''
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_EXPIRES_IN: (state, time) => {
      state.expires_in = time
    },
    SET_ID: (state, id) => {
      state.id = id
    },
    SET_DEPT_ID: (state, deptId) => {
      state.deptId = deptId
    },
    SET_NAME: (state, name) => {
      state.name = name
    },
    SET_NICK_NAME: (state, nickName) => {
      state.nickName = nickName
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions
    },
    SET_SYS_CODE: (state, sysCode) => {
      state.sysCode = sysCode
      if (sysCode) {
        cache.session.set('sysCode', sysCode)
      } else {
        cache.session.remove('sysCode')
      }
    }
  },

  actions: {
    // 登录
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      const code = userInfo.code
      const uuid = userInfo.uuid
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid).then(res => {
          let data = res.data
          setToken(data.accessToken)
          commit('SET_TOKEN', data.accessToken)
          // 服务端 accessToken 有效期记为 UTC 时刻（以 `Z` 结尾），浏览器可直接据此判定本地会话是否过期
          const expiresAt = Date.parse(data.expiresAt)
          const expiresIn = Number.isNaN(expiresAt) ? -1 : Math.max(0, Math.floor((expiresAt - Date.now()) / 1000))
          setExpiresIn(expiresIn)
          setSessionExpiresAt(data.expiresAt)
          commit('SET_EXPIRES_IN', expiresIn)
          commit('SET_SYS_CODE', 'companion_admin')
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 获取用户信息
    GetInfo({ commit, state }) {
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.data
          const avatar = defAva
          if (user.roles && user.roles.length > 0) {
            commit('SET_ROLES', user.roles)
            commit('SET_PERMISSIONS', user.permissions || [])
          } else {
            commit('SET_ROLES', ['ROLE_DEFAULT'])
          }
          commit('SET_ID', user.adminId)
          commit('SET_DEPT_ID', '')
          commit('SET_NAME', user.username)
          commit('SET_NICK_NAME', user.displayName)
          commit('SET_AVATAR', avatar)
          commit('SET_SYS_CODE', 'companion_admin')
          resolve(res)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 刷新token
    RefreshToken({commit, state}) {
      return new Promise((resolve, reject) => {
        refreshToken(state.token).then(res => {
          setExpiresIn(res.data)
          commit('SET_EXPIRES_IN', res.data)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },
    
    // 退出系统
    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        // 本地会话清理必须无条件完成：会话已过期时退出接口本身返回 401，
        // 若此时不改状态（旧实现直接 reject），权限守卫的 GetInfo().catch() 会跳过跳转，用户卡在空页面
        const clearLocalSession = () => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          commit('SET_SYS_CODE', '')
          removeToken()
          removeSessionExpiresAt()
        }
        logout(state.token).then(() => {
          clearLocalSession()
          resolve()
        }).catch(error => {
          const status = error && error.response && error.response.status
          // 401 表示服务端会话已失效，本地清理后按成功处理；其他错误仍上报给调用方
          if (status === 401) {
            clearLocalSession()
            resolve()
            return
          }
          clearLocalSession()
          reject(error)
        })
      })
    },

    // 前端 登出
    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '')
        commit('SET_SYS_CODE', '')
        removeToken()
        removeSessionExpiresAt()
        resolve()
      })
    }
  }
}

export default user
