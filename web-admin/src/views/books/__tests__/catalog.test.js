import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'
import CatalogView from '@/views/books/catalog.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn()
  }),
  useRoute: () => ({
    params: {},
    query: {},
    path: '/books/catalog'
  })
}))

vi.mock('@/api/books', () => ({
  searchBookByISBN: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: {
        isbn: '9787501234567',
        title: 'Test Book',
        author: 'Test Author',
        publisher: 'Test Publisher',
        publishDate: '2024-01-01',
        category: 'literature',
        price: 29.99,
        summary: 'Test Summary',
        coverUrl: '',
        pages: 100,
        language: 'zh',
        binding: 'paperback'
      }
    })
  ),
  createBook: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: { id: 1 }
    })
  ),
  getCategoryTree: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: [
        {
          name: '文学',
          code: 'literature',
          children: []
        },
        {
          name: '计算机',
          code: 'computer',
          children: []
        }
      ]
    })
  )
}))

describe('views/books/catalog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(CatalogView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: {
          'router-link': true,
          'avatar-upload': true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('.page-header-title').text()).toContain('图书编目')
  })
})
