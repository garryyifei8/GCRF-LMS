import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createTestingPinia } from '@pinia/testing'
import { setActivePinia } from 'pinia'

// Mock circulation API module (hoisted)
vi.mock('@/api/circulation', () => ({
  getCirculationRecords: vi.fn(),
  getCirculationRecordById: vi.fn(),
  borrowBook: vi.fn(),
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

import { useCirculationStore } from '@/stores/circulation'

describe('useCirculationStore', () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia({ stubActions: false }))
    vi.clearAllMocks()
    if (typeof localStorage !== 'undefined' && typeof localStorage.clear === 'function') {
      localStorage.clear()
    }
    if (typeof sessionStorage !== 'undefined' && typeof sessionStorage.clear === 'function') {
      sessionStorage.clear()
    }
  })

  it('borrowCart_addItem_shouldIncrementCount', () => {
    const store = useCirculationStore()
    expect(store.borrowCartCount).toBe(0)

    store.addToBorrowCart({ id: 1, title: 'Book A', barcode: 'B001' })
    expect(store.borrowCartCount).toBe(1)
    expect(store.borrowCart[0].id).toBe(1)
    expect(store.borrowCart[0].title).toBe('Book A')

    store.addToBorrowCart({ id: 2, title: 'Book B', barcode: 'B002' })
    expect(store.borrowCartCount).toBe(2)
  })

  it('borrowCart_removeItem_shouldDecrement', () => {
    const store = useCirculationStore()
    store.addToBorrowCart({ id: 1, title: 'Book A' })
    store.addToBorrowCart({ id: 2, title: 'Book B' })
    expect(store.borrowCartCount).toBe(2)

    store.removeFromBorrowCart(1)
    expect(store.borrowCartCount).toBe(1)
    expect(store.borrowCart[0].id).toBe(2)
  })

  it('borrowCart_clear_shouldEmpty', () => {
    const store = useCirculationStore()
    store.addToBorrowCart({ id: 1, title: 'Book A' })
    store.addToBorrowCart({ id: 2, title: 'Book B' })
    expect(store.borrowCartCount).toBe(2)

    store.clearBorrowCart()
    expect(store.borrowCartCount).toBe(0)
    expect(store.borrowCart).toEqual([])
  })

  it('returnCart_addItem_shouldIncrementCount', () => {
    const store = useCirculationStore()
    expect(store.returnCartCount).toBe(0)

    store.addToReturnCart({ id: 10, bookId: 1, bookTitle: 'Book A', barcode: 'R001' })
    expect(store.returnCartCount).toBe(1)
    expect(store.returnCart[0].id).toBe(10)
    expect(store.returnCart[0].bookTitle).toBe('Book A')
  })

  it('returnCart_shouldHandleDuplicates', () => {
    const store = useCirculationStore()
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})

    store.addToReturnCart({ id: 10, bookId: 1, bookTitle: 'Book A' })
    store.addToReturnCart({ id: 10, bookId: 1, bookTitle: 'Book A' })

    expect(store.returnCartCount).toBe(1)
    expect(warnSpy).toHaveBeenCalled()

    // Same applies to borrow cart
    store.addToBorrowCart({ id: 5, title: 'Book X' })
    store.addToBorrowCart({ id: 5, title: 'Book X' })
    expect(store.borrowCartCount).toBe(1)

    warnSpy.mockRestore()
  })

  it('isBorrowing_whenOperationActive_shouldBeTrue', () => {
    const store = useCirculationStore()
    expect(store.isBorrowing).toBe(false)
    expect(store.isReturning).toBe(false)

    store.startBorrow({ id: 1, name: 'Reader 1' })
    expect(store.isBorrowing).toBe(true)
    expect(store.borrowOperation.reader.name).toBe('Reader 1')

    store.endBorrow(true)
    expect(store.isBorrowing).toBe(false)
    expect(store.borrowOperation.reader).toBeNull()
  })

  it('fineRules_loadFromApi_shouldCache', async () => {
    const store = useCirculationStore()
    expect(store.isFineRulesValid).toBe(false)

    // First load - sets timestamp (cache becomes valid)
    await store.loadFineRules()
    expect(store.fineRulesLoadedAt).not.toBeNull()
    expect(store.isFineRulesValid).toBe(true)

    // Cache duration should be 30 minutes (within window)
    const elapsed = Date.now() - store.fineRulesLoadedAt
    expect(elapsed).toBeLessThan(30 * 60 * 1000)

    // Second load (within 30 min) - should use cache, timestamp unchanged
    const firstTimestamp = store.fineRulesLoadedAt
    await new Promise((r) => setTimeout(r, 10))
    await store.loadFineRules()
    expect(store.fineRulesLoadedAt).toBe(firstTimestamp)
  })

  it('fineRules_force_shouldBypassCache', async () => {
    const store = useCirculationStore()

    await store.loadFineRules()
    const firstTimestamp = store.fineRulesLoadedAt
    expect(firstTimestamp).not.toBeNull()

    await new Promise((r) => setTimeout(r, 10))

    // Force reload - timestamp should update
    await store.loadFineRules(true)
    expect(store.fineRulesLoadedAt).toBeGreaterThan(firstTimestamp)
  })

  it('borrowRules_shouldCache', async () => {
    const store = useCirculationStore()
    expect(store.isBorrowRulesValid).toBe(false)

    await store.loadBorrowRules()
    expect(store.borrowRulesLoadedAt).not.toBeNull()
    expect(store.isBorrowRulesValid).toBe(true)

    const firstTimestamp = store.borrowRulesLoadedAt
    await new Promise((r) => setTimeout(r, 10))
    await store.loadBorrowRules()
    // Cache hit - timestamp unchanged
    expect(store.borrowRulesLoadedAt).toBe(firstTimestamp)

    // Force reload - timestamp updates
    await store.loadBorrowRules(true)
    expect(store.borrowRulesLoadedAt).toBeGreaterThan(firstTimestamp)
  })

  it('reset_shouldClearAllState', async () => {
    const store = useCirculationStore()

    // Populate state
    store.addToBorrowCart({ id: 1, title: 'Book A' })
    store.addToReturnCart({ id: 10, bookId: 1, bookTitle: 'Book A' })
    store.startBorrow({ id: 1, name: 'Reader 1' })
    store.startReturn({ id: 2, name: 'Reader 2' })
    await store.loadFineRules()
    await store.loadBorrowRules()

    expect(store.borrowCartCount).toBe(1)
    expect(store.returnCartCount).toBe(1)
    expect(store.isBorrowing).toBe(true)
    expect(store.isReturning).toBe(true)
    expect(store.fineRulesLoadedAt).not.toBeNull()
    expect(store.borrowRulesLoadedAt).not.toBeNull()

    store.reset()

    expect(store.borrowCartCount).toBe(0)
    expect(store.returnCartCount).toBe(0)
    expect(store.isBorrowing).toBe(false)
    expect(store.isReturning).toBe(false)
    expect(store.borrowOperation.reader).toBeNull()
    expect(store.returnOperation.reader).toBeNull()
    expect(store.fineRulesLoadedAt).toBeNull()
    expect(store.borrowRulesLoadedAt).toBeNull()
  })
})
