import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

// Use vi.hoisted so these mocks are available when vi.mock factories run
const {
  mockGetReaders,
  mockCreateReader,
  mockUpdateReader,
  mockDeleteReader,
  mockBatchDeleteReaders,
  mockUpdateReaderStatus,
  mockGetReaderById,
  mockGetReaderTypes,
  mockIssueCard,
  mockGetReaderByCardNumber
} = vi.hoisted(() => ({
  mockGetReaders: vi.fn(),
  mockCreateReader: vi.fn(),
  mockUpdateReader: vi.fn(),
  mockDeleteReader: vi.fn(),
  mockBatchDeleteReaders: vi.fn(),
  mockUpdateReaderStatus: vi.fn(),
  mockGetReaderById: vi.fn(),
  mockGetReaderTypes: vi.fn(),
  mockIssueCard: vi.fn(),
  mockGetReaderByCardNumber: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/readers/students' }),
  createRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    currentRoute: { value: { path: '/readers/students', query: {}, params: {} } },
    beforeEach: vi.fn(),
    afterEach: vi.fn(),
    install: vi.fn()
  }),
  createWebHistory: vi.fn(() => ({})),
  RouterLink: { template: '<a><slot /></a>' },
  RouterView: { template: '<div />' }
}))

vi.mock('@/api/readers', () => ({
  getReaders: mockGetReaders,
  getReaderById: mockGetReaderById,
  createReader: mockCreateReader,
  updateReader: mockUpdateReader,
  deleteReader: mockDeleteReader,
  batchDeleteReaders: mockBatchDeleteReaders,
  updateReaderStatus: mockUpdateReaderStatus,
  getReaderTypes: mockGetReaderTypes,
  issueCard: mockIssueCard,
  getReaderByCardNumber: mockGetReaderByCardNumber
}))

vi.mock('@/utils/excel', () => ({
  exportExcel: vi.fn(),
  readExcel: vi.fn().mockResolvedValue([]),
  downloadTemplate: vi.fn()
}))

vi.mock('@/components/AvatarUpload.vue', () => ({
  default: { template: '<div class="avatar-upload-stub" />' }
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue('confirm'),
      alert: vi.fn().mockResolvedValue('confirm')
    }
  }
})

import StudentsView from '@/views/readers/students.vue'

const mockStudentRecord = {
  readerId: 1,
  studentId: 'S001',
  realName: '张三',
  gender: 'male',
  grade: '高一',
  department: '1班',
  phone: '13800138000',
  idCard: '110101200001011234',
  cardNumber: 'C0001',
  currentBorrowCount: 2,
  maxBorrowCount: 5,
  status: 'active',
  address: '北京市',
  createdTime: '2024-01-01T00:00:00',
  avatar: ''
}

const mockListResponse = {
  code: 200,
  data: {
    records: [mockStudentRecord],
    total: 1,
    pageNum: 1,
    pageSize: 20
  }
}

describe('views/readers/students', () => {
  const factory = () =>
    mount(StudentsView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true, 'router-view': true }
      }
    })

  beforeEach(() => {
    vi.clearAllMocks()
    mockGetReaders.mockResolvedValue(mockListResponse)
    mockDeleteReader.mockResolvedValue({ code: 200 })
    mockCreateReader.mockResolvedValue({ code: 200, data: { readerId: 2 } })
    mockUpdateReader.mockResolvedValue({ code: 200 })
    mockUpdateReaderStatus.mockResolvedValue({ code: 200 })
    mockBatchDeleteReaders.mockResolvedValue({ code: 200 })
  })

  it('mount_shouldRenderWithoutError', async () => {
    const wrapper = factory()
    await flushPromises()
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('.page-container').exists()).toBe(true)
  })

  it('onMount_shouldCallGetReadersAPI', async () => {
    factory()
    await flushPromises()
    expect(mockGetReaders).toHaveBeenCalledTimes(1)
    expect(mockGetReaders).toHaveBeenCalledWith(
      expect.objectContaining({ readerType: 'student', pageNum: 1, pageSize: 20 })
    )
  })

  it('handleSearch_shouldResetPageAndReloadList', async () => {
    const wrapper = factory()
    await flushPromises()

    mockGetReaders.mockClear()

    wrapper.vm.queryForm.keyword = 'test keyword'
    wrapper.vm.handleSearch()
    await flushPromises()

    expect(mockGetReaders).toHaveBeenCalledTimes(1)
    expect(mockGetReaders).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: 'test keyword', pageNum: 1 })
    )
    expect(wrapper.vm.pagination.page).toBe(1)
  })

  it('handlePageChange_shouldUpdatePageAndReloadList', async () => {
    const wrapper = factory()
    await flushPromises()

    mockGetReaders.mockClear()

    wrapper.vm.handlePageChange(3)
    await flushPromises()

    expect(wrapper.vm.pagination.page).toBe(3)
    expect(mockGetReaders).toHaveBeenCalledTimes(1)
    expect(mockGetReaders).toHaveBeenCalledWith(expect.objectContaining({ pageNum: 3 }))
  })

  it('handleDelete_shouldConfirmAndCallDeleteAPI', async () => {
    const { ElMessageBox } = await import('element-plus')
    const wrapper = factory()
    await flushPromises()

    mockGetReaders.mockClear()

    const mockRow = { id: 1, name: '张三' }
    await wrapper.vm.handleDelete(mockRow)
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      expect.stringContaining('张三'),
      expect.any(String),
      expect.objectContaining({ type: 'warning' })
    )
    expect(mockDeleteReader).toHaveBeenCalledWith(1)
    expect(mockGetReaders).toHaveBeenCalled()
  })
})
