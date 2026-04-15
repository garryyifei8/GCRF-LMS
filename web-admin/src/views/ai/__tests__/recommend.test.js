import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/' })
}))

vi.mock('@/api/recommend', () => ({
  getRecommendationsForReader: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  getPopularBooks: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  getSimilarBooks: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  batchRecommend: vi.fn(() => Promise.resolve({ code: 200, data: { records: [], total: 0 } })),
  getRecommendStats: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
  recordClick: vi.fn(() => Promise.resolve({ code: 200 })),
  recordBorrow: vi.fn(() => Promise.resolve({ code: 200 })),
  recomputeSimilarityMatrix: vi.fn(() => Promise.resolve({ code: 200 }))
}))

import Recommend from '@/views/ai/recommend.vue'

describe('views/ai/recommend', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(Recommend, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
