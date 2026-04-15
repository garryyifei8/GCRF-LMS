import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'
import CollectionView from '@/views/books/collection.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn()
  }),
  useRoute: () => ({
    params: {},
    query: {},
    path: '/books/collection'
  })
}))

describe('views/books/collection', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(CollectionView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: {
          'router-link': true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('.page-header-title').text()).toContain('馆藏管理')
  })
})
