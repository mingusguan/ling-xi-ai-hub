import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'

const ExpiresInKey = 'Admin-Expires-In'

// 会话绝对到期时间（毫秒时间戳）的本地存储键。
// 服务端签发的 accessToken 记为 UTC 时刻并以 `Z` 结尾，浏览器解析后即为真实到期时刻；
// 仅依赖 Cookie 的 maxAge 无法主动登出（token 过期后 Cookie 仍在），因此额外持久化到期时间。
const ExpiresAtKey = 'lingxi-admin-expires-at'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}

export function getExpiresIn() {
  return Cookies.get(ExpiresInKey) || -1
}

export function setExpiresIn(time) {
  return Cookies.set(ExpiresInKey, time)
}

export function removeExpiresIn() {
  return Cookies.remove(ExpiresInKey)
}

export function setSessionExpiresAt(expiresAtIso) {
  const at = Date.parse(expiresAtIso)
  if (Number.isNaN(at)) {
    localStorage.removeItem(ExpiresAtKey)
    return
  }
  localStorage.setItem(ExpiresAtKey, String(at))
}

export function getSessionExpiresAt() {
  const raw = localStorage.getItem(ExpiresAtKey)
  const at = Number(raw)
  return Number.isFinite(at) && at > 0 ? at : 0
}

export function removeSessionExpiresAt() {
  localStorage.removeItem(ExpiresAtKey)
}

// 服务端会话是否已到期。缺少到期时间信息时返回 false，退化为依赖接口 401 判定。
export function isSessionExpired() {
  const at = getSessionExpiresAt()
  return at > 0 && Date.now() >= at
}
