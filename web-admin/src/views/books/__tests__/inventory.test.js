import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'
import InventoryView from '@/views/books/inventory.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn()
  }),
  useRoute: () => ({
    params: {},
    query: {},
    path: '/books/inventory'
  })
}))

vi.mock('@/api/inventory', () => ({
  getInventoryTasks: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: {
        records: [],
        list: [],
        total: 0
      }
    })
  ),
  getInventoryTaskById: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: {
        id: 1,
        taskNo: 'TASK001',
        taskName: 'Test Task',
        scope: 'ALL',
        status: 'PENDING',
        totalCount: 100,
        checkedCount: 0,
        diffCount: 0,
        progress: 0,
        creator: 'Admin',
        createdAt: '2024-01-01'
      }
    })
  ),
  createInventoryTask: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: { id: 1 }
    })
  ),
  startInventoryTask: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: {}
    })
  ),
  pauseInventoryTask: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: {}
    })
  ),
  resumeInventoryTask: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: {}
    })
  ),
  exportInventoryReport: vi.fn(() =>
    Promise.resolve(new Blob(['test'], { type: 'application/vnd.ms-excel' }))
  ),
  getInventoryDiffs: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: {
        records: [],
        list: [],
        total: 0
      }
    })
  )
}))

vi.mock('@/api/books', () => ({
  getBookCategories: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: [
        { name: '文学', code: 'literature' },
        { name: '计算机', code: 'computer' }
      ]
    })
  )
}))

describe('views/books/inventory', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(InventoryView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: {
          'router-link': true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('.page-header-title').text()).toContain('库存盘点')
  })
})
