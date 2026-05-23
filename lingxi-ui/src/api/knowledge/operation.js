import request from '@/utils/request'

export function getKnowledgeOperationStats() {
  return request({
    url: '/knowledge/qa/operation/stats',
    method: 'get'
  })
}

export function listKnowledgeOperations(params) {
  return request({
    url: '/knowledge/qa/operation/list',
    method: 'get',
    params
  })
}

export function feedbackKnowledgeQa(data) {
  return request({
    url: '/knowledge/qa/feedback',
    method: 'post',
    data
  })
}
