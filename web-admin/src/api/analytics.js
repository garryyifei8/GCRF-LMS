import request from '@/utils/request'

/**
 * 获取借阅概览统计数据
 */
export function getOverview() {
  return request({
    url: '/api/v1/analytics/overview',
    method: 'get'
  })
}

/**
 * 获取借阅趋势数据
 * @param {Object} params - 查询参数
 * @param {string} params.timeRange - 时间范围 (LAST_30_DAYS, LAST_7_DAYS, THIS_MONTH, etc.)
 * @param {string} params.granularity - 粒度 (DAILY, WEEKLY, MONTHLY)
 */
export function getBorrowTrends(params) {
  return request({
    url: '/api/v1/analytics/borrow-trends',
    method: 'get',
    params
  })
}

/**
 * 获取分类统计数据
 */
export function getCategoryStats() {
  return request({
    url: '/api/v1/analytics/category-stats',
    method: 'get'
  })
}

/**
 * 获取图书排行榜
 * @param {Object} params - 查询参数
 * @param {string} params.rankBy - 排序依据 (BORROW_COUNT, RATING, etc.)
 * @param {string} params.timeRange - 时间范围 (THIS_MONTH, THIS_YEAR, etc.)
 * @param {number} params.limit - 返回数量限制
 */
export function getBookRankings(params) {
  return request({
    url: '/api/v1/analytics/book-rankings',
    method: 'get',
    params
  })
}

/**
 * 获取活跃读者排行榜
 * @param {Object} params - 查询参数
 * @param {string} params.rankBy - 排序依据 (BORROW_COUNT, etc.)
 * @param {string} params.timeRange - 时间范围 (THIS_MONTH, THIS_YEAR, etc.)
 * @param {number} params.limit - 返回数量限制
 */
export function getReaderRankings(params) {
  return request({
    url: '/api/v1/analytics/reader-rankings',
    method: 'get',
    params
  })
}

/**
 * 获取馆藏资源分析数据
 */
export function getCollectionAnalysis() {
  return request({
    url: '/api/v1/analytics/collection-analysis',
    method: 'get'
  })
}

/**
 * 获取最近活动记录
 * @param {Object} params - 查询参数
 * @param {number} params.limit - 返回数量限制
 */
export function getRecentActivities(params) {
  return request({
    url: '/api/v1/analytics/recent-activities',
    method: 'get',
    params
  })
}
