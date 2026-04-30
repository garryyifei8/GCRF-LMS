import request from '@/utils/request'

// ========================================
// 用户管理 API
// ========================================

/**
 * 获取用户列表
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.keyword - 搜索关键词(用户名/姓名)
 * @param {string} params.role - 角色筛选
 * @param {string} params.status - 状态筛选 (active/inactive)
 * @returns {Promise}
 */
export function getUsers(params) {
  return request({
    url: '/api/v1/system/users',
    method: 'get',
    params
  })
}

/**
 * 获取用户详情
 * @param {number} id - 用户ID
 * @returns {Promise}
 */
export function getUserById(id) {
  return request({
    url: `/api/v1/system/users/${id}`,
    method: 'get'
  })
}

/**
 * 创建用户
 * @param {Object} data - 用户数据
 * @param {string} data.username - 用户名(必填)
 * @param {string} data.password - 密码(必填,新增时)
 * @param {string} data.realName - 姓名(必填)
 * @param {string} data.role - 角色(必填)
 * @param {string} data.department - 部门
 * @param {string} data.phone - 联系电话(必填)
 * @param {string} data.email - 邮箱(必填)
 * @param {string} data.status - 状态 (active/inactive)
 * @returns {Promise}
 */
export function createUser(data) {
  return request({
    url: '/api/v1/system/users',
    method: 'post',
    data
  })
}

/**
 * 更新用户信息
 * @param {number} id - 用户ID
 * @param {Object} data - 用户数据
 * @returns {Promise}
 */
export function updateUser(id, data) {
  return request({
    url: `/api/v1/system/users/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除用户
 * @param {number} id - 用户ID
 * @returns {Promise}
 */
export function deleteUser(id) {
  return request({
    url: `/api/v1/system/users/${id}`,
    method: 'delete'
  })
}

/**
 * 重置用户密码
 * @param {number} id - 用户ID
 * @param {string} newPassword - 新密码(可选,不传则重置为默认密码123456)
 * @returns {Promise}
 */
export function resetPassword(id, newPassword = '123456') {
  return request({
    url: `/api/v1/system/users/${id}/password/reset`,
    method: 'put',
    data: { newPassword }
  })
}

/**
 * 更新用户状态
 * @param {number} id - 用户ID
 * @param {string} status - 状态 (active/inactive)
 * @returns {Promise}
 */
export function updateUserStatus(id, status) {
  return request({
    url: `/api/v1/system/users/${id}/status`,
    method: 'put',
    data: { status }
  })
}

/**
 * 批量删除用户
 * @param {Array<number>} ids - 用户ID数组
 * @returns {Promise}
 */
export function batchDeleteUsers(ids) {
  return request({
    url: '/api/v1/system/users/batch',
    method: 'delete',
    params: {
      ids: ids.join(',')
    }
  })
}

// ========================================
// 消息中心 API
// ========================================

/**
 * 获取用户消息列表
 * @param {Object} params - 查询参数
 * @param {number} params.userId - 用户ID
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @returns {Promise}
 */
export function getMessages(params) {
  return request({
    url: '/api/v1/system/messages',
    method: 'get',
    params
  })
}

/**
 * 获取未读消息数量
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export function getUnreadCount(userId) {
  return request({
    url: '/api/v1/system/messages/unread-count',
    method: 'get',
    params: { userId }
  })
}

/**
 * 标记消息为已读
 * @param {number} id - 消息ID
 * @returns {Promise}
 */
export function markMessageRead(id) {
  return request({
    url: `/api/v1/system/messages/${id}/read`,
    method: 'put'
  })
}

// ========================================
// 问题反馈 API
// ========================================

/**
 * 提交反馈
 * @param {Object} data - 反馈数据
 * @param {number} data.userId - 用户ID
 * @param {string} data.userName - 用户名称
 * @param {string} data.title - 反馈标题
 * @param {string} data.content - 反馈内容
 * @param {string} data.feedbackType - 反馈类型 (BUG/FEATURE/OTHER)
 * @returns {Promise}
 */
export function submitFeedback(data) {
  return request({
    url: '/api/v1/system/feedback',
    method: 'post',
    data
  })
}

/**
 * 获取用户反馈列表
 * @param {Object} params - 查询参数
 * @param {number} params.userId - 用户ID
 * @returns {Promise}
 */
export function getFeedback(params) {
  return request({
    url: '/api/v1/system/feedback',
    method: 'get',
    params
  })
}

// ========================================
// 角色管理 API
// ========================================

/**
 * 获取角色列表
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.keyword - 搜索关键词(角色名称/编码)
 * @returns {Promise}
 */
export function getRoles(params) {
  return request({
    url: '/api/v1/system/roles',
    method: 'get',
    params
  })
}

/**
 * 获取所有角色(不分页)
 * @returns {Promise}
 */
export function getAllRoles() {
  return request({
    url: '/api/v1/system/roles/all',
    method: 'get'
  })
}

/**
 * 获取角色详情
 * @param {number} id - 角色ID
 * @returns {Promise}
 */
export function getRoleById(id) {
  return request({
    url: `/api/v1/system/roles/${id}`,
    method: 'get'
  })
}

/**
 * 创建角色
 * @param {Object} data - 角色数据
 * @param {string} data.code - 角色编码(必填)
 * @param {string} data.name - 角色名称(必填)
 * @param {number} data.sort - 排序
 * @param {string} data.remark - 备注
 * @returns {Promise}
 */
export function createRole(data) {
  return request({
    url: '/api/v1/system/roles',
    method: 'post',
    data
  })
}

/**
 * 更新角色信息
 * @param {number} id - 角色ID
 * @param {Object} data - 角色数据
 * @returns {Promise}
 */
export function updateRole(id, data) {
  return request({
    url: `/api/v1/system/roles/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除角色
 * @param {number} id - 角色ID
 * @returns {Promise}
 */
export function deleteRole(id) {
  return request({
    url: `/api/v1/system/roles/${id}`,
    method: 'delete'
  })
}

/**
 * 分配权限给角色
 * @param {number} roleId - 角色ID
 * @param {Array<string>} permissions - 权限ID数组
 * @returns {Promise}
 */
export function assignPermissions(roleId, permissions) {
  return request({
    url: `/api/v1/system/roles/${roleId}/permissions`,
    method: 'put',
    data: { permissions }
  })
}

/**
 * 获取角色的权限列表
 * @param {number} roleId - 角色ID
 * @returns {Promise}
 */
export function getRolePermissions(roleId) {
  return request({
    url: `/api/v1/system/roles/${roleId}/permissions`,
    method: 'get'
  })
}

// ========================================
// 权限管理 API
// ========================================

/**
 * 获取权限树(用于权限配置)
 * @returns {Promise}
 */
export function getPermissionTree() {
  return request({
    url: '/api/v1/system/permissions/tree',
    method: 'get'
  })
}

/**
 * 获取所有权限列表
 * @returns {Promise}
 */
export function getAllPermissions() {
  return request({
    url: '/api/v1/system/permissions',
    method: 'get'
  })
}

// ========================================
// 部门管理 API (与 department.js 保持一致)
// ========================================

/**
 * 获取部门列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getDepartments(params) {
  return request({
    url: '/api/v1/system/departments',
    method: 'get',
    params
  })
}

/**
 * 获取部门树形结构
 * @returns {Promise}
 */
export function getDepartmentTree() {
  return request({
    url: '/api/v1/system/departments/tree',
    method: 'get'
  })
}

/**
 * 获取部门详情
 * @param {number} id - 部门ID
 * @returns {Promise}
 */
export function getDepartmentById(id) {
  return request({
    url: `/api/v1/system/departments/${id}`,
    method: 'get'
  })
}

/**
 * 创建部门
 * @param {Object} data - 部门数据
 * @param {string} data.deptCode - 部门编码(必填)
 * @param {string} data.deptName - 部门名称(必填)
 * @param {number} data.parentId - 上级部门ID
 * @param {string} data.phone - 联系电话
 * @param {string} data.email - 邮箱
 * @param {string} data.description - 描述
 * @returns {Promise}
 */
export function createDepartment(data) {
  return request({
    url: '/api/v1/system/departments',
    method: 'post',
    data
  })
}

/**
 * 更新部门信息
 * @param {number} id - 部门ID
 * @param {Object} data - 部门数据
 * @returns {Promise}
 */
export function updateDepartment(id, data) {
  return request({
    url: `/api/v1/system/departments/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除部门
 * @param {number} id - 部门ID
 * @returns {Promise}
 */
export function deleteDepartment(id) {
  return request({
    url: `/api/v1/system/departments/${id}`,
    method: 'delete'
  })
}

// ========================================
// 系统配置 API
// ========================================

/**
 * 检查系统是否已完成初始化
 * @returns {Promise<{code: number, data: boolean}>}
 */
export function checkInitialized() {
  return request({
    url: '/api/v1/system/config/initialized',
    method: 'get'
  })
}

/**
 * 提交系统初始化配置（首次部署向导）
 * @param {Record<string,string>} data - 初始化配置 kv 对象
 * @returns {Promise<{code: number, data: null}>}
 */
export function initializeSystem(data) {
  return request({
    url: '/api/v1/system/config/initialize',
    method: 'post',
    data
  })
}

/**
 * 获取所有系统配置项
 * @returns {Promise<{code: number, data: Record<string,string>}>}
 */
export function getSystemConfig() {
  return request({
    url: '/api/v1/system/config',
    method: 'get'
  })
}

/**
 * 批量保存系统配置项
 * @param {Record<string,string>} data - kv 对象
 * @returns {Promise}
 */
export function saveSystemConfig(data) {
  return request({
    url: '/api/v1/system/config',
    method: 'put',
    data
  })
}

// ========================================
// 数据备份 API
// ========================================

/**
 * 触发一次手动备份
 * @returns {Promise<{code: number, data: object}>}
 */
export function createBackup() {
  return request({
    url: '/api/v1/system/backup',
    method: 'post'
  })
}

/**
 * 获取最近 10 条备份记录
 * @returns {Promise<{code: number, data: Array}>}
 */
export function listBackups() {
  return request({
    url: '/api/v1/system/backup',
    method: 'get'
  })
}

/**
 * 下载备份文件
 * @param {number|string} id - 备份记录 ID
 * @returns {Promise<Blob>}
 */
export function downloadBackup(id) {
  return request({
    url: `/api/v1/system/backup/${id}/download`,
    method: 'get',
    responseType: 'blob'
  })
}
