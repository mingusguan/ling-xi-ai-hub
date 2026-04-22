import request from '@/utils/request'

// 生成智能审批意见建议（异步）
export function getOpinionSuggest(query) {
  return request({
    url: '/oa/ai/opinion/suggest',
    method: 'get',
    params: query,
    timeout: 30000  // 30秒超时
  })
}

// 查询AI建议状态
export function getOpinionStatus(query) {
  return request({
    url: '/oa/ai/opinion/status',
    method: 'get',
    params: query
  })
}

// 获取聊天会话列表
export function getChatSessions() {
  return request({
    url: '/ai/chat/sessions',
    method: 'get'
  })
}

// 创建新聊天会话
export function createChatSession() {
  return request({
    url: '/ai/chat/session',
    method: 'post'
  })
}

// 获取会话消息列表
export function getSessionMessages(sessionId) {
  return request({
    url: `/ai/chat/messages/${sessionId}`,
    method: 'get'
  })
}

// 删除聊天会话
export function deleteChatSession(sessionId) {
  return request({
    url: `/ai/chat/session/${sessionId}`,
    method: 'delete'
  })
}

// 发送聊天消息
export function sendChatMessage(data) {
  return request({
    url: '/ai/chat/send',
    method: 'post',
    data: data
  })
}

// 获取未读提醒数量（使用统一消息中心）
export function getUnreadReminderCount() {
  return request({
    url: '/system/message/unread/count',
    method: 'get'
  })
}
