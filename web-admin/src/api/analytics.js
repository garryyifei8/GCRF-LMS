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
 * 获取分类分布数据
 */
export function getCategoryDistribution() {
  return request({
    url: '/api/v1/analytics/category-distribution',
    method: 'get'
  })
}

/**
 * 获取图书排行榜(热门图书)
 * @param {Object} params - 查询参数
 * @param {string} params.rankBy - 排序依据 (BORROW_COUNT, RATING, etc.)
 * @param {string} params.timeRange - 时间范围 (THIS_MONTH, THIS_YEAR, etc.)
 * @param {number} params.limit - 返回数量限制
 */
export function getPopularBooks(params) {
  return request({
    url: '/api/v1/analytics/popular-books',
    method: 'get',
    params
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
export function getActiveReaders(params) {
  return request({
    url: '/api/v1/analytics/active-readers',
    method: 'get',
    params
  })
}

/**
 * 获取活跃读者排行榜(别名)
 */
export function getReaderRankings(params) {
  return getActiveReaders(params)
}

/**
 * 获取读者活跃度热力图数据
 */
export function getReaderHeatmap() {
  return request({
    url: '/api/v1/analytics/reader-heatmap',
    method: 'get'
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

// ==================== 导出功能 ====================

/**
 * 导出借阅统计Excel
 * @param {Object} params - 查询参数
 * @param {string} params.timeRange - 时间范围
 * @param {string} params.granularity - 粒度
 */
export function exportBorrowStatistics(params) {
  return request({
    url: '/api/v1/analytics/export/borrow-statistics',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * 导出热门图书Excel
 * @param {Object} params - 查询参数
 */
export function exportPopularBooks(params) {
  return request({
    url: '/api/v1/analytics/export/popular-books',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * 导出活跃读者Excel
 * @param {Object} params - 查询参数
 */
export function exportActiveReaders(params) {
  return request({
    url: '/api/v1/analytics/export/active-readers',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * 导出分类统计Excel
 */
export function exportCategoryStats() {
  return request({
    url: '/api/v1/analytics/export/category-stats',
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 导出综合报告PDF
 */
export function exportComprehensiveReport() {
  return request({
    url: '/api/v1/analytics/export/comprehensive-report',
    method: 'get',
    responseType: 'blob'
  })
}
