import request from '@/utils/request'

/**
 * 获取组织节点列表（一级子节点）
 * @param {number|null} parentId - 父节点ID，为空则获取根节点
 * @returns {Promise}
 */
export function listOrgNodes(parentId) {
  return request({ url: '/api/v1/org/nodes', method: 'get', params: { parentId } })
}

/**
 * 获取单个组织节点详情
 * @param {number} id - 节点ID
 * @returns {Promise}
 */
export function getOrgNode(id) {
  return request({ url: `/api/v1/org/nodes/${id}`, method: 'get' })
}

/**
 * 获取节点子树（递归）
 * @param {number} id - 节点ID
 * @returns {Promise}
 */
export function getOrgSubtree(id) {
  return request({ url: `/api/v1/org/nodes/${id}/subtree`, method: 'get' })
}

/**
 * 创建组织节点
 * @param {Object} data - 节点数据
 * @param {number} data.parentId - 父节点ID（根节点时为空）
 * @param {string} data.type - 节点类型 REGION/DISTRICT/SCHOOL/SUB_SCHOOL/BRANCH/STAGE/GRADE/CLASS
 * @param {string} data.name - 节点名称
 * @param {string} data.code - 节点编码
 * @returns {Promise}
 */
export function createOrgNode(data) {
  return request({ url: '/api/v1/org/nodes', method: 'post', data })
}

/**
 * 更新组织节点
 * @param {number} id - 节点ID
 * @param {Object} data - 更新数据
 * @param {string} data.name - 节点名称
 * @param {string} data.status - 状态 ACTIVE/INACTIVE
 * @returns {Promise}
 */
export function updateOrgNode(id, data) {
  return request({ url: `/api/v1/org/nodes/${id}`, method: 'put', data })
}

/**
 * 删除组织节点
 * @param {number} id - 节点ID
 * @returns {Promise}
 */
export function deleteOrgNode(id) {
  return request({ url: `/api/v1/org/nodes/${id}`, method: 'delete' })
}

/**
 * 移动组织节点到新父节点
 * @param {number} id - 节点ID
 * @param {number} newParentId - 新父节点ID
 * @returns {Promise}
 */
export function moveOrgNode(id, newParentId) {
  return request({ url: `/api/v1/org/nodes/${id}/move`, method: 'post', data: { newParentId } })
}

/**
 * 建学校（自动创建租户 schema）
 * @param {Object} data - 学校数据
 * @param {number} data.parentId - 父节点ID（教育局或区县）
 * @param {string} data.name - 学校名称
 * @param {string} data.code - 学校编码
 * @returns {Promise}
 */
export function createSchool(data) {
  return request({ url: '/api/v1/org/schools', method: 'post', data })
}

/**
 * 批量导入组织节点（Excel）
 * @param {File} file - .xlsx 文件
 * @returns {Promise}
 */
export function importOrgExcel(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request({
    url: '/api/v1/org/nodes/import',
    method: 'post',
    data: fd,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
