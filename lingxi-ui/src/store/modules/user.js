import router from '@/router'
import { MessageBox, } from 'element-ui'
import { login, logout, getInfo, refreshToken } from '@/api/login'
import { getToken, setToken, setExpiresIn, removeToken } from '@/utils/auth'
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
          const expiresIn = Math.max(0, Math.floor((new Date(data.expiresAt).getTime() - Date.now()) / 1000))
          setExpiresIn(expiresIn)
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
        logout(state.token).then(() => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          commit('SET_SYS_CODE', '')
          removeToken()
          resolve()
        }).catch(error => {
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
        resolve()
      })
    }
  }
}

export default user
