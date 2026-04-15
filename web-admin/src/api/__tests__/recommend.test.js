import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} }))
}))

import request from '@/utils/request'
import * as api from '@/api/recommend'

describe('api/recommend', () => {
  beforeEach(() => vi.clearAllMocks())

  describe('getRecommendationsForReader', () => {
    it('calls request with correct shape for default params', async () => {
      const readerId = 123
      await api.getRecommendationsForReader(readerId)

      expect(request).toHaveBeenCalledWith({
        url: `/api/v1/recommend/books/${readerId}`,
        method: 'get',
        params: {
          limit: 20,
          algorithm: 'HYBRID',
          scene: 'HOMEPAGE'
        }
      })
    })

    it('calls request with custom params', async () => {
      const readerId = 456
      const customParams = {
        limit: 10,
        algorithm: 'USER_CF',
        scene: 'DETAIL'
      }
      await api.getRecommendationsForReader(readerId, customParams)

      expect(request).toHaveBeenCalledWith({
        url: `/api/v1/recommend/books/${readerId}`,
        method: 'get',
        params: {
          limit: 10,
          algorithm: 'USER_CF',
          scene: 'DETAIL'
        }
      })
    })

    it('merges partial params with defaults', async () => {
      const readerId = 789
      await api.getRecommendationsForReader(readerId, { limit: 5 })

      expect(request).toHaveBeenCalledWith({
        url: `/api/v1/recommend/books/${readerId}`,
        method: 'get',
        params: {
          limit: 5,
          algorithm: 'HYBRID',
          scene: 'HOMEPAGE'
        }
      })
    })
  })

  describe('getPopularBooks', () => {
    it('calls request with default limit', async () => {
      await api.getPopularBooks()

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/recommend/popular',
        method: 'get',
        params: { limit: 20 }
      })
    })

    it('calls request with custom limit', async () => {
      await api.getPopularBooks(15)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/recommend/popular',
        method: 'get',
        params: { limit: 15 }
      })
    })
  })

  describe('getSimilarBooks', () => {
    it('calls request with default limit', async () => {
      const bookId = 100
      await api.getSimilarBooks(bookId)

      expect(request).toHaveBeenCalledWith({
        url: `/api/v1/recommend/similar/${bookId}`,
        method: 'get',
        params: { limit: 10 }
      })
    })

    it('calls request with custom limit', async () => {
      const bookId = 200
      await api.getSimilarBooks(bookId, 5)

      expect(request).toHaveBeenCalledWith({
        url: `/api/v1/recommend/similar/${bookId}`,
        method: 'get',
        params: { limit: 5 }
      })
    })
  })

  describe('batchRecommend', () => {
    it('calls request with post method and data', async () => {
      const data = {
        readerType: 'STUDENT',
        algorithm: 'HYBRID',
        countPerReader: 5,
        scene: 'HOMEPAGE',
        pageNum: 1,
        pageSize: 100
      }
      await api.batchRecommend(data)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/recommend/batch',
        method: 'post',
        data
      })
    })
  })

  describe('getRecommendStats', () => {
    it('calls request with default days', async () => {
      await api.getRecommendStats()

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/recommend/stats',
        method: 'get',
        params: { days: 30 }
      })
    })

    it('calls request with custom days', async () => {
      await api.getRecommendStats(7)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/recommend/stats',
        method: 'get',
        params: { days: 7 }
      })
    })
  })

  describe('recordClick', () => {
    it('calls request with post method and params', async () => {
      const readerId = 111
      const bookId = 222
      await api.recordClick(readerId, bookId)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/recommend/click',
        method: 'post',
        params: { readerId, bookId }
      })
    })
  })

  describe('recordBorrow', () => {
    it('calls request with post method and params', async () => {
      const readerId = 333
      const bookId = 444
      await api.recordBorrow(readerId, bookId)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/recommend/borrow',
        method: 'post',
        params: { readerId, bookId }
      })
    })
  })

  describe('recomputeSimilarityMatrix', () => {
    it('calls request with post method and no params', async () => {
      await api.recomputeSimilarityMatrix()

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/recommend/recompute',
        method: 'post'
      })
    })
  })
})
