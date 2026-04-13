/**
 * 流通管理相关API
 */
import request from '@/utils/request'

/**
 * 获取流通记录列表(分页+搜索)
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.keyword - 搜索关键词(图书名/读者名/借阅证号/图书条码)
 * @param {string} params.status - 记录状态 (BORROWED/RETURNED/OVERDUE/LOST)
 * @param {string} params.startDate - 开始日期
 * @param {string} params.endDate - 结束日期
 */
export function getCirculationRecords(params) {
  return request({
    url: '/api/v1/borrows',
    method: 'get',
    params
  })
}

/**
 * 获取流通记录详情
 * @param {number} id - 记录ID
 */
export function getCirculationRecordById(id) {
  return request({
    url: `/api/v1/borrows/${id}`,
    method: 'get'
  })
}

/**
 * 借书
 * @param {Object} data - 借书数据
 * @param {number} data.readerId - 读者ID
 * @param {number} data.bookId - 图书ID
 * @param {string} data.remark - 备注
 */
export function borrowBook(data) {
  return request({
    url: '/api/v1/borrows/borrow',
    method: 'post',
    data
  })
}

/**
 * 还书
 * @param {Object} data - 还书数据
 * @param {number} data.borrowId - 借阅记录ID
 * @param {boolean} data.payFine - 是否支付罚金
 * @param {string} data.remark - 备注
 */
export function returnBook(data) {
  return request({
    url: '/api/v1/borrows/return',
    method: 'post',
    data
  })
}

/**
 * 续借
 * @param {Object} data - 续借数据
 * @param {number} data.borrowId - 借阅记录ID
 * @param {number} data.renewDays - 续借天数
 * @param {string} data.remark - 备注
 */
export function renewBook(data) {
  return request({
    url: '/api/v1/borrows/renew',
    method: 'post',
    data
  })
}

/**
 * 获取预约列表(分页+搜索)
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.keyword - 搜索关键词
 * @param {string} params.status - 预约状态 (RESERVED/PICKED_UP/CANCELLED/EXPIRED)
 */
export function getReservations(params) {
  return request({
    url: '/api/v1/reserves',
    method: 'get',
    params
  })
}

/**
 * 预约图书
 * @param {Object} data - 预约数据
 * @param {number} data.readerId - 读者ID
 * @param {number} data.bookId - 图书ID
 * @param {string} data.remark - 备注
 */
export function reserveBook(data) {
  return request({
    url: '/api/v1/reserves/reserve',
    method: 'post',
    data
  })
}

/**
 * 取消预约
 * @param {number} reservationId - 预约记录ID
 */
export function cancelReservation(reservationId) {
  return request({
    url: `/api/v1/reserves/${reservationId}/cancel`,
    method: 'post'
  })
}

/**
 * 获取流通统计数据
 * @param {Object} params - 查询参数
 * @param {string} params.startDate - 开始日期
 * @param {string} params.endDate - 结束日期
 */
export function getCirculationStats(params) {
  return request({
    url: '/api/v1/analytics/circulation',
    method: 'get',
    params
  })
}

/**
 * 批量还书
 * @param {Array<number>} borrowIds - 借阅记录ID数组
 */
export function batchReturnBooks(borrowIds) {
  return request({
    url: '/api/v1/borrows/batch-return',
    method: 'post',
    data: { borrowIds }
  })
}

/**
 * 获取读者的借阅历史
 * @param {number} readerId - 读者ID
 * @param {Object} params - 查询参数
 */
export function getReaderBorrowHistory(readerId, params) {
  return request({
    url: `/api/v1/borrows`,
    method: 'get',
    params: { ...params, readerId }
  })
}

/**
 * 获取图书的流通历史
 * @param {number} bookId - 图书ID
 * @param {Object} params - 查询参数
 */
export function getBookCirculationHistory(bookId, params) {
  return request({
    url: `/api/v1/borrows`,
    method: 'get',
    params: { ...params, bookId }
  })
}

/**
 * 根据图书条码查询借阅记录
 * @param {string} barcode - 图书条码
 */
export function getBorrowRecordByBarcode(barcode) {
  return request({
    url: `/api/v1/borrows`,
    method: 'get',
    params: { barcode }
  })
}

/**
 * 取书（完成预约）
 * @param {number} reservationId - 预约ID
 */
export function pickupReservation(reservationId) {
  return request({
    url: `/api/v1/reserves/${reservationId}/pickup`,
    method: 'post'
  })
}

/**
 * 获取逾期借阅记录
 */
export function getOverdueBorrows() {
  return request({
    url: '/api/v1/borrows/overdue',
    method: 'get'
  })
}

/**
 * 处理预约（确认取书）
 * @param {number} reservationId - 预约ID
 */
export function processReservation(reservationId) {
  return request({
    url: `/api/v1/reserves/${reservationId}/pickup`,
    method: 'post'
  })
}

/**
 * 发送预约通知
 * @param {number} reservationId - 预约ID
 */
export function notifyReservation(reservationId) {
  return request({
    url: `/api/v1/reserves/${reservationId}/notify`,
    method: 'post'
  })
}
