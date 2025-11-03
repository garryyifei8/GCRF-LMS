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
 * @param {string} params.status - 记录状态 (borrowed/returned/renewed/overdue)
 * @param {string} params.startDate - 开始日期
 * @param {string} params.endDate - 结束日期
 */
export function getCirculationRecords(params) {
  return request({
    url: '/api/v1/circulation/records',
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
    url: `/api/v1/circulation/records/${id}`,
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
    url: '/api/v1/circulation/borrow',
    method: 'post',
    data
  })
}

/**
 * 还书
 * @param {Object} data - 还书数据
 * @param {number} data.recordId - 借阅记录ID
 * @param {string} data.remark - 备注
 */
export function returnBook(data) {
  return request({
    url: '/api/v1/circulation/return',
    method: 'post',
    data
  })
}

/**
 * 续借
 * @param {Object} data - 续借数据
 * @param {number} data.recordId - 借阅记录ID
 * @param {string} data.remark - 备注
 */
export function renewBook(data) {
  return request({
    url: '/api/v1/circulation/renew',
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
 * @param {string} params.status - 预约状态 (pending/ready/picked/cancelled/expired)
 */
export function getReservations(params) {
  return request({
    url: '/api/v1/circulation/reservations',
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
    url: '/api/v1/circulation/reserve',
    method: 'post',
    data
  })
}

/**
 * 取消预约
 * @param {Object} data - 取消预约数据
 * @param {number} data.reservationId - 预约记录ID
 * @param {string} data.remark - 备注
 */
export function cancelReservation(data) {
  return request({
    url: '/api/v1/circulation/cancel-reservation',
    method: 'post',
    data
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
    url: '/api/v1/circulation/stats',
    method: 'get',
    params
  })
}

/**
 * 批量还书
 * @param {Array<number>} recordIds - 借阅记录ID数组
 */
export function batchReturnBooks(recordIds) {
  return request({
    url: '/api/v1/circulation/batch-return',
    method: 'post',
    data: { recordIds }
  })
}

/**
 * 获取读者的借阅历史
 * @param {number} readerId - 读者ID
 * @param {Object} params - 查询参数
 */
export function getReaderBorrowHistory(readerId, params) {
  return request({
    url: `/api/v1/circulation/readers/${readerId}/history`,
    method: 'get',
    params
  })
}

/**
 * 获取图书的流通历史
 * @param {number} bookId - 图书ID
 * @param {Object} params - 查询参数
 */
export function getBookCirculationHistory(bookId, params) {
  return request({
    url: `/api/v1/circulation/books/${bookId}/history`,
    method: 'get',
    params
  })
}
