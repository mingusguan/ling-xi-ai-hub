import request from '@/utils/request'

// 查询子系统列表
export function listSubsystem(query) {
  return request({
    url: '/system/subsystem/list',
    method: 'get',
    params: query
  })
}

// 查询子系统下拉框列表
export function optionselectSubsystem() {
  return request({
    url: '/system/subsystem/optionselect',
    method: 'get'
  })
}

// 查询子系统详细
export function getSubsystem(subsystemId) {
  return request({
    url: '/system/subsystem/' + subsystemId,
    method: 'get'
  })
}

// 新增子系统
export function addSubsystem(data) {
  return request({
    url: '/system/subsystem',
    method: 'post',
    data: data
  })
}

// 修改子系统
export function updateSubsystem(data) {
  return request({
    url: '/system/subsystem',
    method: 'put',
    data: data
  })
}

// 删除子系统
export function delSubsystem(subsystemId) {
  return request({
    url: '/system/subsystem/' + subsystemId,
    method: 'delete'
  })
}
