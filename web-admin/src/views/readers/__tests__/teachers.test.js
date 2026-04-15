import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/readers/teachers' })
}))

vi.mock('@/api/readers', () => ({
  getReaders: vi
    .fn()
    .mockResolvedValue({ code: 200, data: { records: [], total: 0, pageNum: 1, pageSize: 10 } }),
  getReaderById: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  createReader: vi.fn().mockResolvedValue({ code: 200 }),
  updateReader: vi.fn().mockResolvedValue({ code: 200 }),
  deleteReader: vi.fn().mockResolvedValue({ code: 200 }),
  batchDeleteReaders: vi.fn().mockResolvedValue({ code: 200 }),
  getReaderTypes: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  issueCard: vi.fn().mockResolvedValue({ code: 200 }),
  updateReaderStatus: vi.fn().mockResolvedValue({ code: 200 }),
  getReaderByCardNumber: vi.fn().mockResolvedValue({ code: 200, data: {} })
}))

vi.mock('@/utils/excel', () => ({
  exportExcel: vi.fn(),
  readExcel: vi.fn().mockResolvedValue([]),
  downloadTemplate: vi.fn()
}))

import TeachersView from '@/views/readers/teachers.vue'

describe('views/readers/teachers', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(TeachersView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: {
          'router-link': true,
          AvatarUpload: true,
          'upload-filled': true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
