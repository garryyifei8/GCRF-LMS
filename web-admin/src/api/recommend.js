import request from '@/utils/request'

/**
 * 获取读者个性化推荐
 * @param {number} readerId - 读者ID
 * @param {Object} params - 查询参数
 * @param {number} params.limit - 推荐数量
 * @param {string} params.algorithm - 算法类型 (USER_CF/ITEM_CF/POPULAR/HYBRID)
 * @param {string} params.scene - 推荐场景 (HOMEPAGE/DETAIL/SEARCH/TOPIC)
 */
export function getRecommendationsForReader(readerId, params = {}) {
  return request({
    url: `/api/v1/recommend/books/${readerId}`,
    method: 'get',
    params: {
      limit: params.limit || 20,
      algorithm: params.algorithm || 'HYBRID',
      scene: params.scene || 'HOMEPAGE'
    }
  })
}

/**
 * 获取热门图书
 * @param {number} limit - 返回数量
 */
export function getPopularBooks(limit = 20) {
  return request({
    url: '/api/v1/recommend/popular',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取相似图书
 * @param {number} bookId - 图书ID
 * @param {number} limit - 返回数量
 */
export function getSimilarBooks(bookId, limit = 10) {
  return request({
    url: `/api/v1/recommend/similar/${bookId}`,
    method: 'get',
    params: { limit }
  })
}

/**
 * 批量生成推荐（管理后台）
 * @param {Object} data - 请求参数
 * @param {string} data.readerType - 读者类型筛选
 * @param {string} data.algorithm - 算法类型
 * @param {number} data.countPerReader - 每位读者推荐数量
 * @param {string} data.scene - 推荐场景
 * @param {number} data.pageNum - 页码
 * @param {number} data.pageSize - 每页数量
 */
export function batchRecommend(data) {
  return request({
    url: '/api/v1/recommend/batch',
    method: 'post',
    data
  })
}

/**
 * 获取推荐效果统计
 * @param {number} days - 统计天数
 */
export function getRecommendStats(days = 30) {
  return request({
    url: '/api/v1/recommend/stats',
    method: 'get',
    params: { days }
  })
}

/**
 * 记录推荐点击
 * @param {number} readerId - 读者ID
 * @param {number} bookId - 图书ID
 */
export function recordClick(readerId, bookId) {
  return request({
    url: '/api/v1/recommend/click',
    method: 'post',
    params: { readerId, bookId }
  })
}

/**
 * 记录推荐借阅
 * @param {number} readerId - 读者ID
 * @param {number} bookId - 图书ID
 */
export function recordBorrow(readerId, bookId) {
  return request({
    url: '/api/v1/recommend/borrow',
    method: 'post',
    params: { readerId, bookId }
  })
}

/**
 * 触发相似度矩阵重新计算
 */
export function recomputeSimilarityMatrix() {
  return request({
    url: '/api/v1/recommend/recompute',
    method: 'post'
  })
}
