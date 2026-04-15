import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/circulation/borrow' })
}))

vi.mock('@/api/circulation', () => ({
  getCirculationRecords: vi.fn(),
  getCirculationRecordById: vi.fn(),
  borrowBook: vi.fn().mockResolvedValue({ code: 200, data: { borrowId: 1 }, message: 'ok' }),
  returnBook: vi.fn(),
  renewBook: vi.fn(),
  getReservations: vi.fn(),
  reserveBook: vi.fn(),
  cancelReservation: vi.fn(),
  getCirculationStats: vi.fn(),
  batchReturnBooks: vi.fn(),
  getReaderBorrowHistory: vi.fn(),
  getBookCirculationHistory: vi.fn(),
  getBorrowRecordByBarcode: vi.fn(),
  pickupReservation: vi.fn(),
  getOverdueBorrows: vi.fn(),
  processReservation: vi.fn(),
  notifyReservation: vi.fn()
}))

vi.mock('@/api/readers', () => ({
  getReaders: vi.fn(),
  getReaderById: vi.fn(),
  createReader: vi.fn(),
  updateReader: vi.fn(),
  deleteReader: vi.fn(),
  batchDeleteReaders: vi.fn(),
  getReaderTypes: vi.fn(),
  issueCard: vi.fn(),
  updateReaderStatus: vi.fn(),
  getReaderByCardNumber: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      readerId: 42,
      realName: '张三',
      readerType: 'student',
      grade: '高三一班',
      department: null,
      cardNumber: 'LIB-2024-001',
      currentBorrowCount: 1,
      maxBorrowCount: 5,
      borrowDays: 30,
      status: 'active',
      currentBorrows: []
    }
  })
}))

vi.mock('@/api/books', () => ({
  getBooks: vi.fn(),
  getBookById: vi.fn(),
  createBook: vi.fn(),
  updateBook: vi.fn(),
  deleteBook: vi.fn(),
  batchDeleteBooks: vi.fn(),
  batchImportBooks: vi.fn(),
  downloadImportTemplate: vi.fn(),
  getBookCategories: vi.fn(),
  getBookByBarcode: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      bookId: 99,
      title: '三体',
      author: '刘慈欣',
      isbn: '978-7-5366-9293-0',
      status: 'available',
      availableCopies: 3
    }
  }),
  lookupByIsbn: vi.fn(),
  generateBarcodes: vi.fn(),
  searchBooks: vi.fn(),
  getBookInventory: vi.fn(),
  updateBookInventory: vi.fn(),
  getBookStats: vi.fn(),
  healthCheck: vi.fn(),
  searchBookByISBN: vi.fn(),
  getCategoryTree: vi.fn(),
  importBooks: vi.fn()
}))

import BorrowView from '@/views/circulation/borrow.vue'

describe('views/circulation/borrow', () => {
  const factory = () =>
    mount(BorrowView, {
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

  it('searchReader_shouldCallGetReaderByCardNumber', async () => {
    const { getReaderByCardNumber } = await import('@/api/readers')
    const wrapper = factory()

    // Set card number and trigger search
    wrapper.vm.readerCardNo = 'LIB-2024-001'
    await wrapper.vm.searchReader()
    await flushPromises()

    expect(getReaderByCardNumber).toHaveBeenCalledWith('LIB-2024-001')
    // Reader info should be populated from mock response
    expect(wrapper.vm.readerInfo).not.toBeNull()
    expect(wrapper.vm.readerInfo.name).toBe('张三')
  })

  it('addBook_shouldCallGetBookByBarcodeAndAddToList', async () => {
    const { getBookByBarcode } = await import('@/api/books')
    const wrapper = factory()

    // First set up reader info so addBook is enabled
    wrapper.vm.readerInfo = {
      id: 42,
      name: '张三',
      type: 'student',
      cardNo: 'LIB-2024-001',
      borrowedCount: 1,
      maxBorrow: 5,
      borrowDays: 30,
      status: 'normal',
      currentBorrows: []
    }
    wrapper.vm.bookBarcode = 'GCRF-0001'
    await wrapper.vm.addBook()
    await flushPromises()

    expect(getBookByBarcode).toHaveBeenCalledWith('GCRF-0001')
    expect(wrapper.vm.borrowList.length).toBe(1)
    expect(wrapper.vm.borrowList[0].title).toBe('三体')
  })

  it('confirmBorrow_shouldCallBorrowBookForEachItemInList', async () => {
    const { borrowBook } = await import('@/api/circulation')
    const wrapper = factory()

    // Prepare reader and borrow list
    wrapper.vm.readerInfo = {
      id: 42,
      name: '张三',
      type: 'student',
      cardNo: 'LIB-2024-001',
      borrowedCount: 1,
      maxBorrow: 5,
      borrowDays: 30,
      status: 'normal',
      currentBorrows: []
    }
    wrapper.vm.borrowList = [
      {
        id: 99,
        title: '三体',
        author: '刘慈欣',
        isbn: '978-xxx',
        barcode: 'GCRF-0001',
        status: 'available'
      }
    ]

    await wrapper.vm.confirmBorrow()
    await flushPromises()

    expect(borrowBook).toHaveBeenCalledWith({
      readerId: 42,
      bookId: 99,
      remark: ''
    })
    // After success, borrow list should be cleared
    expect(wrapper.vm.borrowList.length).toBe(0)
  })

  it('confirmBorrow_whenBorrowBookFails_shouldShowWarningAndKeepFailedBooks', async () => {
    const { borrowBook } = await import('@/api/circulation')
    borrowBook.mockResolvedValue({ code: 500, message: '库存不足' })

    const wrapper = factory()

    wrapper.vm.readerInfo = {
      id: 42,
      name: '张三',
      type: 'student',
      cardNo: 'LIB-2024-001',
      borrowedCount: 1,
      maxBorrow: 5,
      borrowDays: 30,
      status: 'normal',
      currentBorrows: []
    }
    wrapper.vm.borrowList = [
      {
        id: 99,
        title: '三体',
        author: '刘慈欣',
        isbn: '978-xxx',
        barcode: 'GCRF-0001',
        status: 'available'
      }
    ]

    await wrapper.vm.confirmBorrow()
    await flushPromises()

    // borrowBook was called but returned failure
    expect(borrowBook).toHaveBeenCalled()
    // Failed book remains in the list
    expect(wrapper.vm.borrowList.length).toBe(1)
    // submitting flag should be reset
    expect(wrapper.vm.submitting).toBe(false)
  })
})
