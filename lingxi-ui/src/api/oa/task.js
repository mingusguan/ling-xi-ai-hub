import request from '@/utils/request'

export function listTask(params) {
  return request({ url: '/oa/task/list', method: 'get', params })
}

export function saveTask(data) {
  return request({ url: '/oa/task', method: 'post', data })
}

export function updateTaskStatus(taskId, taskStatus) {
  return request({
    url: `/oa/task/${taskId}/status`,
    method: 'put',
    data: { taskStatus }
  })
}
