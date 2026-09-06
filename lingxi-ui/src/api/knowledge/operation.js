import request from '@/utils/request'

export function getKnowledgeOperationStats() {
  return request({
    url: '/ai/knowledge/operation/stats',
    method: 'get'
  })
}

export function listKnowledgeOperations(params) {
  return request({
    url: '/ai/knowledge/operation/list',
    method: 'get',
    params
  })
}

export function feedbackKnowledgeQa(data) {
  return request({
    url: '/ai/knowledge/operation/feedback',
    method: 'post',
    data
  })
}
