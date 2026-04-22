import request from '@/utils/request'

export function listLeave(params) {
  return request({ url: '/oa/workflow/leave/list', method: 'get', params })
}

export function listMyApprovedLeaves(params) {
  return request({ url: '/oa/workflow/leave/my-approved', method: 'get', params })
}

export function saveLeave(data) {
  return request({ url: '/oa/workflow/leave', method: 'post', data })
}

export function approveLeave(data) {
  return request({ url: '/oa/workflow/leave/approve', method: 'post', data })
}

export function getLeaveDetail(leaveId) {
  return request({ url: `/oa/workflow/leave/${leaveId}`, method: 'get' })
}

export function listExpense(params) {
  return request({ url: '/oa/workflow/expense/list', method: 'get', params })
}

export function listMyApprovedExpenses(params) {
  return request({ url: '/oa/workflow/expense/my-approved', method: 'get', params })
}

export function saveExpense(data) {
  return request({ url: '/oa/workflow/expense', method: 'post', data })
}

export function approveExpense(data) {
  return request({ url: '/oa/workflow/expense/approve', method: 'post', data })
}

export function getExpenseDetail(expenseId) {
  return request({ url: `/oa/workflow/expense/${expenseId}`, method: 'get' })
}

export function listTemplate(params) {
  return request({ url: '/oa/workflow/template/list', method: 'get', params })
}

export function saveTemplate(data) {
  return request({ url: '/oa/workflow/template', method: 'post', data })
}

export function getTemplateDetail(templateId) {
  return request({ url: `/oa/workflow/template/${templateId}`, method: 'get' })
}

export function getActiveTemplate(businessType) {
  return request({ url: '/oa/workflow/template/active', method: 'get', params: { businessType } })
}

export function deployTemplate(templateId) {
  return request({ url: `/oa/workflow/template/${templateId}/deploy`, method: 'post' })
}

export function listTodoTasks(params) {
  return request({ url: '/oa/workflow/task/todo', method: 'get', params })
}

export function getWorkflowHistory(processInstanceId) {
  return request({ url: '/oa/workflow/history', method: 'get', params: { processInstanceId } })
}

export function listApprovalRecords(params) {
  return request({ url: '/oa/workflow/record/list', method: 'get', params })
}

export function cleanupProcessDefinitions(processDefinitionKey) {
  return request({ 
    url: '/oa/workflow/process/cleanup', 
    method: 'delete',
    params: { processDefinitionKey }
  })
}

export function cancelLeave(params) {
  return request({ url: '/oa/workflow/leave/cancel', method: 'post', params })
}

export function cancelExpense(params) {
  return request({ url: '/oa/workflow/expense/cancel', method: 'post', params })
}

export function getTimeoutWarningList() {
  return request({ 
    url: '/oa/workflow/timeout/warning/current', 
    method: 'get'
  })
}

export function calculateLeaveDuration(startTime, endTime) {
  return request({ 
    url: '/oa/workflow/leave/calculate-duration', 
    method: 'get',
    params: { startTime, endTime }
  })
}
