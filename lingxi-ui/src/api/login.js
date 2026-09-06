import request from '@/utils/request'
import { adminLogin, adminLogout, adminProfile } from '@/api/companionAdmin'

// 登录方法
export function login(username, password, code, uuid) {
  let deviceId = localStorage.getItem('lingxi-admin-device-id')
  if (!deviceId) {
    deviceId = `admin-web-${Date.now()}-${Math.random().toString(36).slice(2)}`
    localStorage.setItem('lingxi-admin-device-id', deviceId)
  }
  return adminLogin({ username, password, deviceId })
}

// 注册方法
export function register(data) {
  return request({
    url: '/auth/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 刷新方法
export function refreshToken() {
  return Promise.reject(new Error('管理端会话不支持静默刷新，请重新登录'))
}

// 获取用户详细信息
export function getInfo() {
  return adminProfile()
}

// 退出方法
export function logout() {
  return adminLogout()
}

// 获取验证码
export function getCodeImg() {
  return Promise.resolve({ captchaEnabled: false })
}
