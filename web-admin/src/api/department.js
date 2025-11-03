import request from '@/utils/request'

/**
 * 查询部门列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getDepartmentList(params) {
  return request({
    url: '/api/departments',
    method: 'get',
    params
  })
}

/**
 * 获取部门详情
 * @param {Number} id - 部门ID
 * @returns {Promise}
 */
export function getDepartmentById(id) {
  return request({
    url: `/api/departments/${id}`,
    method: 'get'
  })
}

/**
 * 创建部门
 * @param {Object} data - 部门数据
 * @returns {Promise}
 */
export function createDepartment(data) {
  return request({
    url: '/api/departments',
    method: 'post',
    data
  })
}

/**
 * 更新部门
 * @param {Object} data - 部门数据
 * @returns {Promise}
 */
export function updateDepartment(data) {
  return request({
    url: '/api/departments',
    method: 'put',
    data
  })
}

/**
 * 删除部门
 * @param {Number} id - 部门ID
 * @returns {Promise}
 */
export function deleteDepartment(id) {
  return request({
    url: `/api/departments/${id}`,
    method: 'delete'
  })
}
