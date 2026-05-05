import request from '@/utils/request'

export function listMarketTools(params) {
  return request({
    url: '/ai/mcp/market/tools',
    method: 'get',
    params
  })
}

export function getMarketTool(toolId) {
  return request({
    url: `/ai/mcp/market/tools/${toolId}`,
    method: 'get'
  })
}

export function addMarketTool(data) {
  return request({
    url: '/ai/mcp/market/tools',
    method: 'post',
    data
  })
}

export function updateMarketTool(data) {
  return request({
    url: '/ai/mcp/market/tools',
    method: 'put',
    data
  })
}

export function publishMarketTool(toolId) {
  return request({
    url: `/ai/mcp/market/tools/${toolId}/publish`,
    method: 'put'
  })
}

export function disableMarketTool(toolId) {
  return request({
    url: `/ai/mcp/market/tools/${toolId}/disable`,
    method: 'put'
  })
}

export function deprecateMarketTool(toolId) {
  return request({
    url: `/ai/mcp/market/tools/${toolId}/deprecate`,
    method: 'put'
  })
}

export function applyMarketTool(data) {
  return request({
    url: '/ai/mcp/market/applications',
    method: 'post',
    data
  })
}

export function listGrantScopeOptions() {
  return request({
    url: '/ai/mcp/market/applications/grant-scopes',
    method: 'get'
  })
}

export function listToolApplications(params) {
  return request({
    url: '/ai/mcp/market/applications',
    method: 'get',
    params
  })
}

export function approveToolApplication(data) {
  return request({
    url: '/ai/mcp/market/applications/approve',
    method: 'put',
    data
  })
}

export function rejectToolApplication(data) {
  return request({
    url: '/ai/mcp/market/applications/reject',
    method: 'put',
    data
  })
}

export function listToolAuditLogs(params) {
  return request({
    url: '/ai/mcp/market/audit-logs',
    method: 'get',
    params
  })
}

export function getToolMarketStats() {
  return request({
    url: '/ai/mcp/market/stats',
    method: 'get'
  })
}

export function listToolBindings(params) {
  return request({
    url: '/ai/mcp/market/bindings',
    method: 'get',
    params
  })
}

export function saveToolBinding(data) {
  return request({
    url: '/ai/mcp/market/bindings',
    method: 'post',
    data
  })
}

export function deleteToolBinding(bindingId) {
  return request({
    url: `/ai/mcp/market/bindings/${bindingId}`,
    method: 'delete'
  })
}

export function refreshMcpClients() {
  return request({
    url: '/ai/mcp/market/clients/refresh',
    method: 'put'
  })
}
