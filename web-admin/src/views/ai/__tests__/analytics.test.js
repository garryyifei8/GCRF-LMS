import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/' })
}))

vi.mock('echarts', () => ({
  init: vi.fn(() => ({
    setOption: vi.fn(),
    resize: vi.fn(),
    dispose: vi.fn()
  })),
  graphic: { LinearGradient: vi.fn() }
}))

vi.mock('@/api/analytics', () => ({
  getOverview: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
  getBorrowTrends: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  getCategoryStats: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  getCategoryDistribution: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  getPopularBooks: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  getBookRankings: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  getActiveReaders: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  getReaderRankings: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  getReaderHeatmap: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
  getCollectionAnalysis: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
  getRecentActivities: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  exportBorrowStatistics: vi.fn(() => Promise.resolve(new Blob())),
  exportPopularBooks: vi.fn(() => Promise.resolve(new Blob())),
  exportActiveReaders: vi.fn(() => Promise.resolve(new Blob())),
  exportCategoryStats: vi.fn(() => Promise.resolve(new Blob())),
  exportComprehensiveReport: vi.fn(() => Promise.resolve(new Blob()))
}))

import Analytics from '@/views/ai/analytics.vue'

describe('views/ai/analytics', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(Analytics, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
