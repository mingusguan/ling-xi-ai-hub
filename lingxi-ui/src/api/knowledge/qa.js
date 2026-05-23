import request from '@/utils/request'

export function sendChat(data) {
  return request({
    url: '/knowledge/qa/chat',
    method: 'post',
    data
  })
}

export function listHistory(userId) {
  return request({
    url: '/knowledge/qa/history',
    method: 'get',
    params: { userId }
  })
}

export function listSessions(userId) {
  return request({
    url: '/knowledge/qa/session/list',
    method: 'get',
    params: { userId }
  })
}

export function createSession(data) {
  return request({
    url: '/knowledge/qa/session/create',
    method: 'post',
    data
  })
}

export function deleteSession(sessionId) {
  return request({
    url: `/knowledge/qa/session/${sessionId}`,
    method: 'delete'
  })
}

export function getSessionConversations(sessionId) {
  return request({
    url: `/knowledge/qa/session/${sessionId}/conversations`,
    method: 'get'
  })
}

export function feedbackQa(data) {
  return request({
    url: '/knowledge/qa/feedback',
    method: 'post',
    data
  })
}
