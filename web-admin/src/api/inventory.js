import request from '@/utils/request'

// ========== 库存管理 APIs ==========

/**
 * 获取库存列表
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.location - 位置筛选
 * @param {string} params.keyword - 搜索关键词
 */
export function getInventoryList(params) {
  return request({
    url: '/api/v1/inventory',
    method: 'get',
    params
  })
}

/**
 * 获取库存详情
 * @param {number} id - 库存记录ID
 */
export function getInventoryById(id) {
  return request({
    url: `/api/v1/inventory/${id}`,
    method: 'get'
  })
}

/**
 * 库存调整
 * @param {Object} data - 调整数据
 * @param {number} data.bookId - 图书ID
 * @param {number} data.adjustQuantity - 调整数量（正数增加，负数减少）
 * @param {string} data.reason - 调整原因
 */
export function adjustInventory(data) {
  return request({
    url: '/api/v1/inventory/adjust',
    method: 'post',
    data
  })
}

/**
 * 获取库存预警列表
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 */
export function getInventoryAlerts(params) {
  return request({
    url: '/api/v1/inventory/alerts',
    method: 'get',
    params
  })
}

/**
 * 获取库存统计
 */
export function getInventoryStats() {
  return request({
    url: '/api/v1/inventory/stats',
    method: 'get'
  })
}

// ========== 盘点任务 APIs ==========

/**
 * 获取盘点任务列表
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.status - 任务状态 (PENDING/IN_PROGRESS/COMPLETED/CANCELLED)
 * @param {string} params.taskType - 任务类型 (FULL/PARTIAL/SPOT)
 */
export function getInventoryTasks(params) {
  return request({
    url: '/api/v1/inventory/tasks',
    method: 'get',
    params
  })
}

/**
 * 获取盘点任务详情
 * @param {number} id - 任务ID
 */
export function getInventoryTaskById(id) {
  return request({
    url: `/api/v1/inventory/tasks/${id}`,
    method: 'get'
  })
}

/**
 * 创建盘点任务
 * @param {Object} data - 任务数据
 * @param {string} data.taskName - 任务名称
 * @param {string} data.taskType - 任务类型 (FULL/PARTIAL/SPOT)
 * @param {number} data.totalBooks - 预计盘点图书数
 * @param {number} data.operatorId - 操作员ID
 * @param {string} data.operatorName - 操作员姓名
 * @param {string} data.notes - 备注
 */
export function createInventoryTask(data) {
  return request({
    url: '/api/v1/inventory/tasks',
    method: 'post',
    data
  })
}

/**
 * 更新盘点任务
 * @param {number} id - 任务ID
 * @param {Object} data - 任务数据
 */
export function updateInventoryTask(id, data) {
  return request({
    url: `/api/v1/inventory/tasks/${id}`,
    method: 'put',
    data
  })
}

/**
 * 开始盘点任务
 * @param {number} id - 任务ID
 */
export function startInventoryTask(id) {
  return request({
    url: `/api/v1/inventory/tasks/${id}/start`,
    method: 'post'
  })
}

/**
 * 完成盘点任务
 * @param {number} id - 任务ID
 */
export function completeInventoryTask(id) {
  return request({
    url: `/api/v1/inventory/tasks/${id}/complete`,
    method: 'post'
  })
}

/**
 * 取消盘点任务
 * @param {number} id - 任务ID
 */
export function cancelInventoryTask(id) {
  return request({
    url: `/api/v1/inventory/tasks/${id}/cancel`,
    method: 'post'
  })
}

/**
 * 获取盘点明细列表
 * @param {number} taskId - 任务ID
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.status - 状态筛选 (PENDING/CHECKED)
 */
export function getInventoryTaskItems(taskId, params) {
  return request({
    url: `/api/v1/inventory/tasks/${taskId}/items`,
    method: 'get',
    params
  })
}

/**
 * 录入盘点结果
 * @param {number} taskId - 任务ID
 * @param {Object} data - 盘点数据
 * @param {number} data.bookId - 图书ID
 * @param {number} data.actualQuantity - 实际数量
 */
export function recordInventoryItem(taskId, data) {
  return request({
    url: `/api/v1/inventory/tasks/${taskId}/items`,
    method: 'post',
    data
  })
}

/**
 * 导出盘点报告
 * @param {number} taskId - 任务ID
 */
export function exportInventoryReport(taskId) {
  return request({
    url: `/api/v1/inventory/tasks/${taskId}/export`,
    method: 'get',
    responseType: 'blob'
  })
}

// 保留兼容性的别名
export const getInventoryDiffs = getInventoryTaskItems
export const recordInventoryResult = recordInventoryItem
export const deleteInventoryTask = cancelInventoryTask
export const pauseInventoryTask = cancelInventoryTask
export const resumeInventoryTask = startInventoryTask
