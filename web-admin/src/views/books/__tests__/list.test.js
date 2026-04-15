import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/books/list' }),
  createRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    currentRoute: { value: { path: '/books/list', query: {}, params: {} } },
    beforeEach: vi.fn(),
    afterEach: vi.fn(),
    install: vi.fn()
  }),
  createWebHistory: vi.fn(() => ({})),
  RouterLink: { template: '<a><slot /></a>' },
  RouterView: { template: '<div />' }
}))

vi.mock('@/api/books', () => ({
  getBooks: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      records: [
        {
          bookId: 1,
          isbn: '9787111111111',
          callNumber: 'BC001',
          title: '人工智能基础',
          author: '周志华',
          publisher: '机械工业出版社',
          category: 'computer',
          price: 99.0,
          totalCopies: 5,
          availableCopies: 3,
          borrowCount: 12,
          status: 'available',
          description: '经典AI教材',
          coverUrl: ''
        }
      ],
      total: 1
    }
  }),
  deleteBook: vi.fn().mockResolvedValue({ code: 200 }),
  batchDeleteBooks: vi.fn().mockResolvedValue({ code: 200 }),
  getBookCategories: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getBookById: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  createBook: vi.fn().mockResolvedValue({ code: 200 }),
  updateBook: vi.fn().mockResolvedValue({ code: 200 }),
  batchImportBooks: vi.fn().mockResolvedValue({ code: 200 }),
  downloadImportTemplate: vi.fn().mockResolvedValue({ code: 200 }),
  getBookByBarcode: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  lookupByIsbn: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  generateBarcodes: vi.fn().mockResolvedValue({ code: 200 }),
  searchBooks: vi.fn().mockResolvedValue({ code: 200, data: { records: [], total: 0 } }),
  getBookInventory: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  updateBookInventory: vi.fn().mockResolvedValue({ code: 200 }),
  getBookStats: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  healthCheck: vi.fn().mockResolvedValue({ code: 200 }),
  searchBookByISBN: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  getCategoryTree: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  importBooks: vi.fn().mockResolvedValue({ code: 200 })
}))

import BookListView from '@/views/books/list.vue'

describe('views/books/list', () => {
  const factory = () =>
    mount(BookListView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true, 'router-view': true }
      }
    })

  beforeEach(() => vi.clearAllMocks())

  it('mount_shouldRenderWithoutError', async () => {
    const wrapper = factory()
    await flushPromises()
    expect(wrapper.exists()).toBe(true)
    // Page header should be present
    expect(wrapper.find('.page-container').exists()).toBe(true)
  })

  it('searchInput_shouldTriggerListRefresh', async () => {
    const { getBooks } = await import('@/api/books')
    const wrapper = factory()
    await flushPromises()

    const callCountAfterMount = getBooks.mock.calls.length

    // Set keyword and trigger search
    wrapper.vm.queryForm.keyword = '人工智能'
    wrapper.vm.handleSearch()
    await flushPromises()

    expect(getBooks.mock.calls.length).toBeGreaterThan(callCountAfterMount)
    // The last call should include the keyword
    const lastCall = getBooks.mock.calls[getBooks.mock.calls.length - 1][0]
    expect(lastCall.keyword).toBe('人工智能')
  })

  it('pagination_shouldTriggerListRefresh', async () => {
    const { getBooks } = await import('@/api/books')
    const wrapper = factory()
    await flushPromises()

    const callCountAfterMount = getBooks.mock.calls.length

    // Simulate page change
    wrapper.vm.handlePageChange(2)
    await flushPromises()

    expect(getBooks.mock.calls.length).toBeGreaterThan(callCountAfterMount)
    // Pagination page should be updated
    expect(wrapper.vm.pagination.page).toBe(2)
    const lastCall = getBooks.mock.calls[getBooks.mock.calls.length - 1][0]
    expect(lastCall.pageNum).toBe(2)
  })

  it('categoryFilter_shouldTriggerListRefresh', async () => {
    const { getBooks } = await import('@/api/books')
    const wrapper = factory()
    await flushPromises()

    const callCountAfterMount = getBooks.mock.calls.length

    // Set category filter and trigger search
    wrapper.vm.queryForm.category = 'computer'
    wrapper.vm.handleSearch()
    await flushPromises()

    expect(getBooks.mock.calls.length).toBeGreaterThan(callCountAfterMount)
    const lastCall = getBooks.mock.calls[getBooks.mock.calls.length - 1][0]
    expect(lastCall.category).toBe('computer')
  })

  it('statusFilter_shouldTriggerListRefresh', async () => {
    const { getBooks } = await import('@/api/books')
    const wrapper = factory()
    await flushPromises()

    const callCountAfterMount = getBooks.mock.calls.length

    // Set status filter and trigger search
    wrapper.vm.queryForm.status = 'available'
    wrapper.vm.handleSearch()
    await flushPromises()

    expect(getBooks.mock.calls.length).toBeGreaterThan(callCountAfterMount)
    const lastCall = getBooks.mock.calls[getBooks.mock.calls.length - 1][0]
    expect(lastCall.status).toBe('available')
  })
})
