import request from '@/utils/request'

// 公文写作 / 改写润色
export function writeDocument(data) {
  return request({
    url: '/ai/tool/document/write',
    method: 'post',
    data,
    timeout: 120000
  })
}

// 报表解读
export function interpretReport(data) {
  return request({
    url: '/ai/tool/report/interpret',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    data,
    timeout: 120000
  })
}
