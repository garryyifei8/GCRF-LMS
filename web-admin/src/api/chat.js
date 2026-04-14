import request from '@/utils/request'

/**
 * 发送消息获取AI回复
 * @param {Object} data - 请求参数
 * @param {string} data.sessionId - 会话ID（可选，新会话时不传）
 * @param {string} data.content - 消息内容
 * @param {number} data.readerId - 读者ID（可选）
 */
export function sendMessage(data) {
  return request({
    url: '/api/v1/chat/message',
    method: 'post',
    data
  })
}

/**
 * 获取对话历史
 * @param {string} sessionId - 会话ID
 */
export function getChatHistory(sessionId) {
  return request({
    url: `/api/v1/chat/history/${sessionId}`,
    method: 'get'
  })
}

/**
 * 提交反馈
 * @param {Object} data - 反馈参数
 * @param {string} data.sessionId - 会话ID
 * @param {number} data.messageId - 消息ID（可选）
 * @param {number} data.faqId - FAQ知识ID（可选）
 * @param {string} data.feedbackType - 反馈类型: helpful, unhelpful, report
 * @param {string} data.comment - 反馈评论（可选）
 * @param {number} data.readerId - 读者ID（可选）
 */
export function submitFeedback(data) {
  return request({
    url: '/api/v1/chat/feedback',
    method: 'post',
    data
  })
}

/**
 * 获取热门问题
 * @param {number} limit - 返回数量
 */
export function getHotQuestions(limit = 10) {
  return request({
    url: '/api/v1/chat/hot-questions',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取对话统计
 */
export function getChatStats() {
  return request({
    url: '/api/v1/chat/stats',
    method: 'get'
  })
}

/**
 * 刷新缓存
 */
export function refreshCache() {
  return request({
    url: '/api/v1/chat/cache/refresh',
    method: 'post'
  })
}
