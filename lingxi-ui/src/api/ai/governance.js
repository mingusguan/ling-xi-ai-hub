import request from '@/utils/request'

export function getGovernanceStats() {
  return request({
    url: '/ai/governance/stats',
    method: 'get'
  })
}

export function listGovernanceLogs(params) {
  return request({
    url: '/ai/governance/logs',
    method: 'get',
    params
  })
}
