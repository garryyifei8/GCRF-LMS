import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createTestingPinia } from '@pinia/testing'
import { setActivePinia } from 'pinia'

// Mock API module (hoisted)
vi.mock('@/api/readers', () => ({
  getReaderTypes: vi.fn()
}))

import { useReaderStore } from '@/stores/reader'
import * as readersApi from '@/api/readers'

const mockReaderTypes = [
  { code: 'STUDENT', name: '学生', maxBorrowCount: 5, borrowDays: 30 },
  { code: 'TEACHER', name: '教师', maxBorrowCount: 10, borrowDays: 60 }
]

describe('useReaderStore', () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia({ stubActions: false }))
    vi.clearAllMocks()
    if (typeof localStorage !== 'undefined' && typeof localStorage.clear === 'function') {
      localStorage.clear()
    }
  })

  it('loadReaderTypes_shouldFetchFromApi', async () => {
    readersApi.getReaderTypes.mockResolvedValue({ data: mockReaderTypes })

    const store = useReaderStore()
    const result = await store.loadReaderTypes()

    expect(readersApi.getReaderTypes).toHaveBeenCalledOnce()
    expect(result).toEqual(mockReaderTypes)
    expect(store.readerTypes).toEqual(mockReaderTypes)
    expect(store.readerTypesLoadedAt).not.toBeNull()
  })

  it('loadReaderTypes_withinCache_shouldReturnCached', async () => {
    readersApi.getReaderTypes.mockResolvedValue({ data: mockReaderTypes })

    const store = useReaderStore()
    await store.loadReaderTypes()
    await store.loadReaderTypes() // second call should hit cache

    expect(readersApi.getReaderTypes).toHaveBeenCalledOnce()
  })

  it('loadReaderTypes_force_shouldBypassCache', async () => {
    readersApi.getReaderTypes.mockResolvedValue({ data: mockReaderTypes })

    const store = useReaderStore()
    await store.loadReaderTypes()
    await store.loadReaderTypes(true) // force refresh

    expect(readersApi.getReaderTypes).toHaveBeenCalledTimes(2)
  })

  it('loadReaderTypes_apiError_shouldThrow', async () => {
    readersApi.getReaderTypes.mockRejectedValue(new Error('Network error'))

    const store = useReaderStore()
    await expect(store.loadReaderTypes()).rejects.toThrow('Network error')
    expect(store.readerTypesLoading).toBe(false)
  })

  it('setCurrentReader_shouldSetAndAddRecent', () => {
    const store = useReaderStore()
    const reader = {
      id: 1,
      cardNumber: 'C001',
      realName: '张三',
      phone: '13800138000',
      readerType: 'STUDENT',
      status: 'active'
    }

    store.setCurrentReader(reader)

    expect(store.currentReader).toEqual(reader)
    expect(store.hasCurrentReader).toBe(true)
    expect(store.recentReaders).toHaveLength(1)
    expect(store.recentReaders[0].id).toBe(1)
  })

  it('clearCurrentReader_shouldResetCurrentReader', () => {
    const store = useReaderStore()
    store.setCurrentReader({ id: 1, cardNumber: 'C001', realName: '张三' })

    store.clearCurrentReader()

    expect(store.currentReader).toBeNull()
    expect(store.hasCurrentReader).toBe(false)
  })

  it('updateCurrentReader_shouldMergeUpdates', () => {
    const store = useReaderStore()
    store.setCurrentReader({ id: 1, realName: '张三', status: 'active' })

    store.updateCurrentReader({ status: 'frozen', phone: '13900000000' })

    expect(store.currentReader.status).toBe('frozen')
    expect(store.currentReader.phone).toBe('13900000000')
    expect(store.currentReader.realName).toBe('张三')
  })

  it('readerTypeOptions_shouldMapToOptions', async () => {
    readersApi.getReaderTypes.mockResolvedValue({ data: mockReaderTypes })

    const store = useReaderStore()
    await store.loadReaderTypes()

    expect(store.readerTypeOptions).toEqual([
      { value: 'STUDENT', label: '学生', maxBorrowCount: 5, borrowDays: 30 },
      { value: 'TEACHER', label: '教师', maxBorrowCount: 10, borrowDays: 60 }
    ])
    expect(store.getReaderTypeName('STUDENT')).toBe('学生')
    expect(store.getReaderTypeByCode('TEACHER').maxBorrowCount).toBe(10)
  })

  it('currentReaderBorrowCapacity_shouldComputeRemaining', () => {
    const store = useReaderStore()
    store.setCurrentReader({
      id: 1,
      maxBorrowCount: 5,
      currentBorrowCount: 2,
      status: 'active'
    })

    expect(store.currentReaderBorrowCapacity).toEqual({
      canBorrow: true,
      maxBorrowCount: 5,
      currentBorrowCount: 2,
      remainingCount: 3
    })
  })

  it('addSearchHistory_shouldDedupeAndCap', () => {
    const store = useReaderStore()

    store.addSearchHistory('张三')
    store.addSearchHistory('李四')
    store.addSearchHistory('张三') // duplicate, should move to top
    store.addSearchHistory('  ') // empty, ignored

    expect(store.searchHistory).toEqual(['张三', '李四'])
  })

  it('reset_shouldClearAllState', async () => {
    readersApi.getReaderTypes.mockResolvedValue({ data: mockReaderTypes })

    const store = useReaderStore()
    await store.loadReaderTypes()
    store.setCurrentReader({ id: 1, cardNumber: 'C001', realName: '张三' })
    store.addSearchHistory('keyword')

    store.reset()

    expect(store.readerTypes).toEqual([])
    expect(store.readerTypesLoadedAt).toBeNull()
    expect(store.currentReader).toBeNull()
    expect(store.recentReaders).toEqual([])
    expect(store.searchHistory).toEqual([])
  })
})
