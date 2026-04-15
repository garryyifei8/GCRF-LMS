import { http, HttpResponse } from 'msw'
import {
  generateOverview,
  generateBorrowTrends,
  generateCategoryStats,
  generateBookRankings,
  generateReaderRankings,
  generateCollectionAnalysis,
  generateRecentActivities,
  generateGradeBorrowPreferences,
  generateYearlyCirculation,
  generateCategoryDistribution,
  generateReaderHeatmap
} from '../data/analytics'

export const analyticsHandlers = [
  // 获取借阅概览统计数据
  http.get('/api/v1/analytics/overview', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateOverview()
    })
  }),

  // 获取借阅趋势数据
  http.get('/api/v1/analytics/borrow-trends', ({ request }) => {
    const url = new URL(request.url)
    const timeRange = url.searchParams.get('timeRange') || 'LAST_30_DAYS'
    const granularity = url.searchParams.get('granularity') || 'DAILY'

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateBorrowTrends(timeRange, granularity)
    })
  }),

  // 获取分类统计数据
  http.get('/api/v1/analytics/category-stats', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateCategoryStats()
    })
  }),

  // 获取分类分布数据
  http.get('/api/v1/analytics/category-distribution', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateCategoryDistribution()
    })
  }),

  // 获取热门图书排行
  http.get('/api/v1/analytics/popular-books', ({ request }) => {
    const url = new URL(request.url)
    const params = {
      rankBy: url.searchParams.get('rankBy') || 'BORROW_COUNT',
      timeRange: url.searchParams.get('timeRange') || 'THIS_MONTH',
      limit: parseInt(url.searchParams.get('limit') || '20')
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateBookRankings(params)
    })
  }),

  // 获取图书排行榜
  http.get('/api/v1/analytics/book-rankings', ({ request }) => {
    const url = new URL(request.url)
    const params = {
      rankBy: url.searchParams.get('rankBy') || 'BORROW_COUNT',
      timeRange: url.searchParams.get('timeRange') || 'THIS_MONTH',
      limit: parseInt(url.searchParams.get('limit') || '20')
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateBookRankings(params)
    })
  }),

  // 获取活跃读者排行
  http.get('/api/v1/analytics/active-readers', ({ request }) => {
    const url = new URL(request.url)
    const params = {
      rankBy: url.searchParams.get('rankBy') || 'BORROW_COUNT',
      timeRange: url.searchParams.get('timeRange') || 'THIS_MONTH',
      limit: parseInt(url.searchParams.get('limit') || '20')
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateReaderRankings(params)
    })
  }),

  // 获取读者排行榜
  http.get('/api/v1/analytics/reader-rankings', ({ request }) => {
    const url = new URL(request.url)
    const params = {
      rankBy: url.searchParams.get('rankBy') || 'BORROW_COUNT',
      timeRange: url.searchParams.get('timeRange') || 'THIS_MONTH',
      limit: parseInt(url.searchParams.get('limit') || '20')
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateReaderRankings(params)
    })
  }),

  // 获取读者活跃度热力图
  http.get('/api/v1/analytics/reader-heatmap', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateReaderHeatmap()
    })
  }),

  // 获取馆藏资源分析数据
  http.get('/api/v1/analytics/collection-analysis', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateCollectionAnalysis()
    })
  }),

  // 获取最近活动记录
  http.get('/api/v1/analytics/recent-activities', ({ request }) => {
    const url = new URL(request.url)
    const limit = parseInt(url.searchParams.get('limit') || '50')

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateRecentActivities(limit)
    })
  }),

  // 获取年级借阅偏好数据
  http.get('/api/v1/analytics/grade-preferences', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateGradeBorrowPreferences()
    })
  }),

  // 获取年度流通统计
  http.get('/api/v1/analytics/yearly-circulation', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateYearlyCirculation()
    })
  }),

  // 导出借阅统计
  http.get('/api/v1/analytics/export/borrow-statistics', () => {
    return new HttpResponse('Mock CSV content for borrow statistics\nDate,Count\n2026-04-01,42', {
      status: 200,
      headers: { 'Content-Type': 'application/octet-stream' }
    })
  }),

  // 导出热门图书
  http.get('/api/v1/analytics/export/popular-books', () => {
    return new HttpResponse('Mock CSV content for popular books\nTitle,BorrowCount\n图书A,120', {
      status: 200,
      headers: { 'Content-Type': 'application/octet-stream' }
    })
  }),

  // 导出活跃读者
  http.get('/api/v1/analytics/export/active-readers', () => {
    return new HttpResponse('Mock CSV content for active readers\nName,BorrowCount\n张三,30', {
      status: 200,
      headers: { 'Content-Type': 'application/octet-stream' }
    })
  }),

  // 导出分类统计
  http.get('/api/v1/analytics/export/category-stats', () => {
    return new HttpResponse('Mock CSV content for category stats\nCategory,Count\n文学,200', {
      status: 200,
      headers: { 'Content-Type': 'application/octet-stream' }
    })
  }),

  // 导出综合报告
  http.get('/api/v1/analytics/export/comprehensive-report', () => {
    return new HttpResponse(
      'Mock CSV content for comprehensive report\nSection,Data\n借阅总量,1500',
      {
        status: 200,
        headers: { 'Content-Type': 'application/octet-stream' }
      }
    )
  })
]
