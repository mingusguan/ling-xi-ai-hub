import request from '@/utils/request'

// 查询当前用户假期余额
export function getMyLeaveBalance(year) {
  return request({
    url: '/oa/leave/quota/balance',
    method: 'get',
    params: { year }
  })
}

// 查询当前用户待审批请假统计
export function getPendingLeaveStats() {
  return request({
    url: '/oa/leave/quota/pending-stats',
    method: 'get'
  })
}

// 查询指定用户假期余额(管理员)
export function getUserLeaveBalance(userId) {
  return request({
    url: `/oa/leave/quota/balance/${userId}`,
    method: 'get'
  })
}

// 查询假期额度列表
export function listLeaveQuota(query) {
  return request({
    url: '/oa/leave/quota/list',
    method: 'get',
    params: query
  })
}

// 手动调整假期额度
export function adjustLeaveQuota(data) {
  return request({
    url: '/oa/leave/quota/adjust',
    method: 'post',
    data: data
  })
}

// 手动触发生成年度额度(管理员)
export function generateAnnualQuota(year) {
  return request({
    url: `/oa/leave/quota/generate/${year}`,
    method: 'post'
  })
}

// 修改额度状态
export function updateLeaveQuotaStatus(data) {
  return request({
    url: '/oa/leave/quota/changeStatus',
    method: 'put',
    data: data
  })
}

// 查询假期规则列表
export function listLeaveRule(query) {
  return request({
    url: '/oa/leave/rule/list',
    method: 'get',
    params: query
  })
}

// 获取启用的假期类型列表（不分页）
export function getEnabledLeaveTypes() {
  return request({
    url: '/oa/leave/rule/enabled',
    method: 'get'
  })
}

// 查询假期规则详情
export function getLeaveRule(ruleId) {
  return request({
    url: `/oa/leave/rule/${ruleId}`,
    method: 'get'
  })
}

// 新增假期规则
export function addLeaveRule(data) {
  return request({
    url: '/oa/leave/rule',
    method: 'post',
    data: data
  })
}

// 修改假期规则
export function updateLeaveRule(data) {
  return request({
    url: '/oa/leave/rule',
    method: 'put',
    data: data
  })
}

// 删除假期规则
export function delLeaveRule(ruleId) {
  return request({
    url: `/oa/leave/rule/${ruleId}`,
    method: 'delete'
  })
}

// 查询额度调整记录
export function listLeaveAdjustment(query) {
  return request({
    url: '/oa/leave/adjustment/list',
    method: 'get',
    params: query
  })
}
