import request from '@/utils/request'

// 获取消息列表
export function getMessageList(query) {
  return request({
    url: '/system/message/list',
    method: 'get',
    params: query
  })
}

// 获取未读消息数量
export function getUnreadMessageCount() {
  return request({
    url: '/system/message/unread/count',
    method: 'get'
  })
}

// 标记消息为已读
export function markMessageAsRead(messageId) {
  return request({
    url: `/system/message/${messageId}/read`,
    method: 'put'
  })
}

// 删除消息
export function deleteMessage(messageId) {
  return request({
    url: `/system/message/${messageId}`,
    method: 'delete'
  })
}
