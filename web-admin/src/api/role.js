import request from '@/utils/request'

/**
 * 获取角色列表 (IAM 服务)
 * @returns {Promise}
 */
export function getRoles() {
  return request({ url: '/api/v1/roles', method: 'get' })
}

/**
 * 获取角色详情（含权限列表）
 * @param {number|string} id - 角色ID
 * @returns {Promise}
 */
export function getRoleDetail(id) {
  return request({ url: `/api/v1/roles/${id}`, method: 'get' })
}

/**
 * 获取权限列表 (IAM 服务)
 * @returns {Promise}
 */
export function getPermissions() {
  return request({ url: '/api/v1/permissions', method: 'get' })
}

/**
 * 获取用户已分配的角色列表
 * @param {number|string} userId - 用户ID
 * @returns {Promise}
 */
export function getUserRoles(userId) {
  return request({ url: `/api/v1/users/${userId}/roles`, method: 'get' })
}

/**
 * 为用户分配角色
 * @param {number|string} userId - 用户ID
 * @param {Object} payload - 请求体
 * @param {string} payload.roleCode - 角色编码
 * @param {string|null} payload.expiresAt - 过期时间（ISO字符串，null=永久）
 * @returns {Promise}
 */
export function assignRole(userId, payload) {
  return request({ url: `/api/v1/users/${userId}/roles`, method: 'post', data: payload })
}

/**
 * 撤销用户的某个角色
 * @param {number|string} userId - 用户ID
 * @param {number|string} roleId - 角色ID
 * @param {number|string|null} schoolId - 学校ID（可选，用于数据范围过滤）
 * @returns {Promise}
 */
export function revokeRole(userId, roleId, schoolId) {
  return request({
    url: `/api/v1/users/${userId}/roles/${roleId}`,
    method: 'delete',
    params: schoolId ? { schoolId } : {}
  })
}
