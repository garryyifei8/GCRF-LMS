import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} }))
}))

import request from '@/utils/request'
import * as api from '@/api/chat'

describe('api/chat', () => {
  beforeEach(() => vi.clearAllMocks())

  it('sendMessage calls request with correct shape', async () => {
    const data = { content: 'Hello', sessionId: 'abc123', readerId: 1 }
    await api.sendMessage(data)

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/chat/message',
      method: 'post',
      data
    })
  })

  it('getChatHistory calls request with correct shape', async () => {
    const sessionId = 'session-123'
    await api.getChatHistory(sessionId)

    expect(request).toHaveBeenCalledWith({
      url: `/api/v1/chat/history/${sessionId}`,
      method: 'get'
    })
  })

  it('submitFeedback calls request with correct shape', async () => {
    const data = {
      sessionId: 'session-456',
      feedbackType: 'helpful',
      comment: 'Good response'
    }
    await api.submitFeedback(data)

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/chat/feedback',
      method: 'post',
      data
    })
  })

  it('getHotQuestions calls request with correct shape (default limit)', async () => {
    await api.getHotQuestions()

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/chat/hot-questions',
      method: 'get',
      params: { limit: 10 }
    })
  })

  it('getHotQuestions calls request with correct shape (custom limit)', async () => {
    await api.getHotQuestions(5)

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/chat/hot-questions',
      method: 'get',
      params: { limit: 5 }
    })
  })

  it('getChatStats calls request with correct shape', async () => {
    await api.getChatStats()

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/chat/stats',
      method: 'get'
    })
  })

  it('refreshCache calls request with correct shape', async () => {
    await api.refreshCache()

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/chat/cache/refresh',
      method: 'post'
    })
  })
})
