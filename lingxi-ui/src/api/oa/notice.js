import request from '@/utils/request'

export function listNotice(params) {
  return request({ url: '/oa/notice/list', method: 'get', params })
}

export function saveNotice(data) {
  return request({ url: '/oa/notice', method: 'post', data })
}

export function delNotice(noticeId) {
  return request({ url: `/oa/notice/${noticeId}`, method: 'delete' })
}
