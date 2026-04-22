import request from '@/utils/request'

// 文档列表（按部门/分类）- 支持分页
export function listDocument(deptId, categoryId, pageNum, pageSize) {
  return request({
    url: '/knowledge/doc/list',
    method: 'get',
    params: { deptId, categoryId, pageNum, pageSize }
  })
}

// 文档详情
export function getDocument(docId) {
  return request({
    url: '/knowledge/doc/' + docId,
    method: 'get'
  })
}

// 文件上传（获取文件URL）
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/file/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: formData
  })
}

// 上传文档（传入fileUrl）
export function uploadDocument(data) {
  return request({
    url: '/knowledge/doc/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data
  })
}

// 删除文档
export function delDocument(docId) {
  return request({
    url: '/knowledge/doc/' + docId,
    method: 'delete'
  })
}

// 重新向量化
export function reEmbedDocument(docId) {
  return request({
    url: '/knowledge/doc/' + docId + '/re-embed',
    method: 'post'
  })
}
