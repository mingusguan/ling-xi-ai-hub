import request from '@/utils/request'

export function listAgents(params) {
  return request({
    url: '/ai/agent/list',
    method: 'get',
    params
  })
}

export function getAgent(agentId) {
  return request({
    url: `/ai/agent/${agentId}`,
    method: 'get'
  })
}

export function addAgent(data) {
  return request({
    url: '/ai/agent',
    method: 'post',
    data
  })
}

export function updateAgent(data) {
  return request({
    url: '/ai/agent',
    method: 'put',
    data
  })
}

export function publishAgent(agentId) {
  return request({
    url: `/ai/agent/${agentId}/publish`,
    method: 'put'
  })
}

export function disableAgent(agentId) {
  return request({
    url: `/ai/agent/${agentId}/disable`,
    method: 'put'
  })
}

export function listAgentSteps(params) {
  return request({
    url: '/ai/agent/steps',
    method: 'get',
    params
  })
}

export function saveAgentStep(data) {
  return request({
    url: '/ai/agent/steps',
    method: 'post',
    data
  })
}

export function deleteAgentStep(stepId) {
  return request({
    url: `/ai/agent/steps/${stepId}`,
    method: 'delete'
  })
}
