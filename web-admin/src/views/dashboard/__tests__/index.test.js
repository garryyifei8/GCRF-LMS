import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/dashboard' })
}))

// Mock echarts - MUST be mocked or chart init throws in jsdom
// LinearGradient is used with `new`, so we provide a real class inside the factory
vi.mock('echarts', () => {
  const mockChartInstance = {
    setOption: vi.fn(),
    resize: vi.fn(),
    dispose: vi.fn(),
    on: vi.fn()
  }
  class LinearGradient {}
  return {
    default: {
      init: vi.fn(() => mockChartInstance),
      graphic: { LinearGradient }
    },
    init: vi.fn(() => mockChartInstance),
    graphic: { LinearGradient }
  }
})

// Mock all @/api/analytics exports used by dashboard
vi.mock('@/api/analytics', () => ({
  getOverview: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      totalBooks: 1000,
      totalReaders: 500,
      todayBorrows: 30,
      overdueCount: 5
    }
  }),
  getBorrowTrends: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      labels: ['12-01', '12-02', '12-03'],
      borrowCounts: [10, 20, 15],
      returnCounts: [8, 18, 12]
    }
  }),
  getCategoryStats: vi.fn().mockResolvedValue({
    code: 200,
    data: [
      { categoryName: '计算机科学', borrowCount: 100 },
      { categoryName: '文学', borrowCount: 80 }
    ]
  }),
  getBookRankings: vi.fn().mockResolvedValue({
    code: 200,
    data: [{ title: '深入理解计算机系统', author: 'test', category: '计算机', borrowCount: 28 }]
  }),
  getReaderRankings: vi.fn().mockResolvedValue({
    code: 200,
    data: [{ name: '张晓明', type: 'STUDENT', department: '计算机学院', borrowCount: 15 }]
  }),
  getCollectionAnalysis: vi.fn().mockResolvedValue({
    code: 200,
    data: [
      {
        category: '计算机科学',
        total: 120,
        available: 95,
        circulationRate: 79,
        monthBorrow: 156,
        status: '正常'
      }
    ]
  }),
  getRecentActivities: vi.fn().mockResolvedValue({
    code: 200,
    data: [
      {
        id: 1,
        type: 'borrow',
        title: '张晓明 借阅图书',
        detail: '深入理解计算机系统',
        time: '10分钟前'
      }
    ]
  }),
  // Other exports not used by dashboard but exported from the module
  getCategoryDistribution: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getPopularBooks: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getActiveReaders: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getReaderHeatmap: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  exportBorrowStatistics: vi.fn().mockResolvedValue({ code: 200 }),
  exportPopularBooks: vi.fn().mockResolvedValue({ code: 200 }),
  exportActiveReaders: vi.fn().mockResolvedValue({ code: 200 }),
  exportCategoryStats: vi.fn().mockResolvedValue({ code: 200 }),
  exportComprehensiveReport: vi.fn().mockResolvedValue({ code: 200 })
}))

import DashboardView from '@/views/dashboard/index.vue'

describe('views/dashboard', () => {
  const factory = () =>
    mount(DashboardView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true }
      }
    })

  beforeEach(() => vi.clearAllMocks())

  it('mount_shouldRenderWithoutError', () => {
    const wrapper = factory()
    expect(wrapper.exists()).toBe(true)
  })

  it('onMounted_shouldCallOverviewApi', async () => {
    const { getOverview } = await import('@/api/analytics')
    factory()
    await flushPromises()
    expect(getOverview).toHaveBeenCalled()
  })

  it('statCards_shouldBeRenderedInTemplate', async () => {
    const wrapper = factory()
    await flushPromises()
    // The dashboard renders stat cards with class .stat-card
    const statCards = wrapper.findAll('.stat-card')
    expect(statCards.length).toBeGreaterThan(0)
  })

  it('onMounted_shouldCallAllDashboardApis', async () => {
    const {
      getOverview,
      getBorrowTrends,
      getCategoryStats,
      getBookRankings,
      getReaderRankings,
      getCollectionAnalysis,
      getRecentActivities
    } = await import('@/api/analytics')

    factory()
    await flushPromises()

    expect(getOverview).toHaveBeenCalled()
    expect(getBorrowTrends).toHaveBeenCalled()
    expect(getCategoryStats).toHaveBeenCalled()
    expect(getBookRankings).toHaveBeenCalled()
    expect(getReaderRankings).toHaveBeenCalled()
    expect(getCollectionAnalysis).toHaveBeenCalled()
    expect(getRecentActivities).toHaveBeenCalled()
  })
})
