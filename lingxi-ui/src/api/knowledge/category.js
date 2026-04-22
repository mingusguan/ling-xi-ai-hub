import request from '@/utils/request'

// 查询分类平铺列表
export function listCategory(deptId) {
  return request({
    url: '/knowledge/category/list',
    method: 'get',
    params: { deptId }
  })
}

// 查询分类树
export function treeCategory(deptId) {
  return request({
    url: '/knowledge/category/tree',
    method: 'get',
    params: { deptId }
  })
}

// 新增分类
export function addCategory(data) {
  return request({
    url: '/knowledge/category',
    method: 'post',
    data
  })
}

// 修改分类
export function updateCategory(data) {
  return request({
    url: '/knowledge/category',
    method: 'put',
    data
  })
}

// 删除分类
export function delCategory(categoryId) {
  return request({
    url: '/knowledge/category/' + categoryId,
    method: 'delete'
  })
}
