import request from '@/utils/request'

/**
 * 获取读者列表(分页+搜索)
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.keyword - 搜索关键词(姓名/借阅证号/手机号/邮箱)
 * @param {string} params.readerType - 读者类型 (student/teacher/staff/public)
 * @param {string} params.status - 读者状态 (active/suspended/expired/blacklisted)
 */
export function getReaders(params) {
  return request({
    url: '/api/v1/readers',
    method: 'get',
    params
  })
}

/**
 * 获取读者详情
 * @param {number} id - 读者ID
 */
export function getReaderById(id) {
  return request({
    url: `/api/v1/readers/${id}`,
    method: 'get'
  })
}

/**
 * 新增读者
 * @param {Object} data - 读者数据
 * @param {string} data.realName - 姓名(必填)
 * @param {string} data.phone - 手机号(必填)
 * @param {string} data.readerType - 读者类型(必填)
 * @param {string} data.gender - 性别
 * @param {string} data.idCard - 身份证号
 * @param {string} data.email - 邮箱
 * @param {string} data.address - 地址
 * @param {number} data.maxBorrowCount - 最大借阅数量
 * @param {number} data.depositAmount - 押金金额
 * @param {string} data.cardExpireDate - 借阅证到期日期
 * @param {string} data.status - 状态
 */
export function createReader(data) {
  return request({
    url: '/api/v1/readers',
    method: 'post',
    data
  })
}

/**
 * 更新读者信息
 * @param {number} id - 读者ID
 * @param {Object} data - 读者数据
 */
export function updateReader(id, data) {
  return request({
    url: `/api/v1/readers/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除读者
 * @param {number} id - 读者ID
 */
export function deleteReader(id) {
  return request({
    url: `/api/v1/readers/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除读者
 * @param {Array<number>} ids - 读者ID数组
 */
export function batchDeleteReaders(ids) {
  return request({
    url: '/api/v1/readers/batch',
    method: 'delete',
    params: {
      ids: ids.join(',')
    }
  })
}

/**
 * 获取读者类型列表
 */
export function getReaderTypes() {
  return request({
    url: '/api/v1/readers/types',
    method: 'get'
  })
}

/**
 * 办理/更新借阅证
 * @param {number} id - 读者ID
 * @param {Object} data - 借阅证数据
 * @param {string} data.cardExpireDate - 到期日期
 * @param {number} data.depositAmount - 押金金额
 */
export function issueCard(id, data) {
  return request({
    url: `/api/v1/readers/${id}/card`,
    method: 'post',
    data
  })
}

/**
 * 更新读者状态(冻结/解冻)
 * @param {number} id - 读者ID
 * @param {string} status - 状态 (suspended=冻结, active=解冻)
 */
export function updateReaderStatus(id, status) {
  return request({
    url: `/api/v1/readers/${id}/status`,
    method: 'put',
    data: { status }
  })
}

/**
 * 根据借阅证号查询读者信息
 * @param {string} cardNumber - 借阅证号
 */
export function getReaderByCardNumber(cardNumber) {
  return request({
    url: `/api/v1/readers/card/${cardNumber}`,
    method: 'get'
  })
}
