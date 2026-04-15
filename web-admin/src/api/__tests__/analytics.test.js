import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} }))
}))

import request from '@/utils/request'
import * as api from '@/api/analytics'

describe('api/analytics', () => {
  beforeEach(() => vi.clearAllMocks())

  describe('Query endpoints (no params)', () => {
    it('getOverview should call request with correct shape', async () => {
      await api.getOverview()
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/overview',
        method: 'get'
      })
    })

    it('getCategoryStats should call request with correct shape', async () => {
      await api.getCategoryStats()
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/category-stats',
        method: 'get'
      })
    })

    it('getCategoryDistribution should call request with correct shape', async () => {
      await api.getCategoryDistribution()
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/category-distribution',
        method: 'get'
      })
    })

    it('getReaderHeatmap should call request with correct shape', async () => {
      await api.getReaderHeatmap()
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/reader-heatmap',
        method: 'get'
      })
    })

    it('getCollectionAnalysis should call request with correct shape', async () => {
      await api.getCollectionAnalysis()
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/collection-analysis',
        method: 'get'
      })
    })
  })

  describe('Query endpoints (with params)', () => {
    it('getBorrowTrends should call request with params', async () => {
      const params = { timeRange: 'LAST_30_DAYS', granularity: 'DAILY' }
      await api.getBorrowTrends(params)
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/borrow-trends',
        method: 'get',
        params
      })
    })

    it('getPopularBooks should call request with params', async () => {
      const params = { rankBy: 'BORROW_COUNT', timeRange: 'THIS_MONTH', limit: 10 }
      await api.getPopularBooks(params)
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/popular-books',
        method: 'get',
        params
      })
    })

    it('getBookRankings should call request with params', async () => {
      const params = { rankBy: 'RATING', timeRange: 'THIS_YEAR', limit: 20 }
      await api.getBookRankings(params)
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/book-rankings',
        method: 'get',
        params
      })
    })

    it('getActiveReaders should call request with params', async () => {
      const params = { rankBy: 'BORROW_COUNT', timeRange: 'THIS_MONTH', limit: 15 }
      await api.getActiveReaders(params)
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/active-readers',
        method: 'get',
        params
      })
    })

    it('getRecentActivities should call request with params', async () => {
      const params = { limit: 50 }
      await api.getRecentActivities(params)
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/recent-activities',
        method: 'get',
        params
      })
    })
  })

  describe('Alias endpoints', () => {
    it('getReaderRankings should call getActiveReaders with same params', async () => {
      const params = { rankBy: 'BORROW_COUNT', timeRange: 'THIS_MONTH', limit: 15 }
      await api.getReaderRankings(params)
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/active-readers',
        method: 'get',
        params
      })
    })
  })

  describe('Export endpoints (no params)', () => {
    it('exportCategoryStats should call request with responseType blob', async () => {
      await api.exportCategoryStats()
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/export/category-stats',
        method: 'get',
        responseType: 'blob'
      })
    })

    it('exportComprehensiveReport should call request with responseType blob', async () => {
      await api.exportComprehensiveReport()
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/export/comprehensive-report',
        method: 'get',
        responseType: 'blob'
      })
    })
  })

  describe('Export endpoints (with params)', () => {
    it('exportBorrowStatistics should call request with params and responseType blob', async () => {
      const params = { timeRange: 'LAST_30_DAYS', granularity: 'DAILY' }
      await api.exportBorrowStatistics(params)
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/export/borrow-statistics',
        method: 'get',
        params,
        responseType: 'blob'
      })
    })

    it('exportPopularBooks should call request with params and responseType blob', async () => {
      const params = { rankBy: 'BORROW_COUNT', limit: 10 }
      await api.exportPopularBooks(params)
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/export/popular-books',
        method: 'get',
        params,
        responseType: 'blob'
      })
    })

    it('exportActiveReaders should call request with params and responseType blob', async () => {
      const params = { timeRange: 'THIS_MONTH', limit: 20 }
      await api.exportActiveReaders(params)
      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/export/active-readers',
        method: 'get',
        params,
        responseType: 'blob'
      })
    })
  })
})
