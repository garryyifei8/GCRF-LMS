import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createTestingPinia } from '@pinia/testing'
import { setActivePinia } from 'pinia'

// Mock API module (hoisted)
vi.mock('@/api/books', () => ({
  getBookCategories: vi.fn(),
  getBooks: vi.fn()
}))

import { useBookStore } from '@/stores/book'
import * as booksApi from '@/api/books'

const mockCategoriesTree = [
  {
    code: 'A',
    name: '马克思主义',
    children: [
      { code: 'A1', name: '马克思著作', children: [] },
      { code: 'A2', name: '恩格斯著作', children: [] }
    ]
  },
  {
    code: 'T',
    name: '工业技术',
    children: [
      {
        code: 'TP',
        name: '自动化技术',
        children: [{ code: 'TP3', name: '计算技术', children: [] }]
      }
    ]
  }
]

describe('useBookStore', () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia({ stubActions: false }))
    vi.clearAllMocks()
    if (typeof localStorage !== 'undefined' && typeof localStorage.clear === 'function') {
      localStorage.clear()
    }
  })

  it('loadCategories_firstCall_shouldFetchFromApi', async () => {
    booksApi.getBookCategories.mockResolvedValue({ data: mockCategoriesTree })

    const store = useBookStore()
    const result = await store.loadCategories()

    expect(booksApi.getBookCategories).toHaveBeenCalledOnce()
    expect(result).toEqual(mockCategoriesTree)
    expect(store.categories).toEqual(mockCategoriesTree)
    expect(store.categoriesLoadedAt).toBeTypeOf('number')
  })

  it('loadCategories_withinCache_shouldNotRefetch', async () => {
    booksApi.getBookCategories.mockResolvedValue({ data: mockCategoriesTree })

    const store = useBookStore()
    await store.loadCategories()
    await store.loadCategories()

    expect(booksApi.getBookCategories).toHaveBeenCalledOnce()
  })

  it('loadCategories_force_shouldBypassCache', async () => {
    booksApi.getBookCategories.mockResolvedValue({ data: mockCategoriesTree })

    const store = useBookStore()
    await store.loadCategories()
    await store.loadCategories(true)

    expect(booksApi.getBookCategories).toHaveBeenCalledTimes(2)
  })

  it('loadCategories_apiError_shouldThrowAndResetLoading', async () => {
    booksApi.getBookCategories.mockRejectedValue(new Error('Network error'))

    const store = useBookStore()
    await expect(store.loadCategories()).rejects.toThrow('Network error')

    expect(store.categoriesLoading).toBe(false)
    expect(store.categories).toEqual([])
  })

  it('loadHotBooks_shouldCacheFor5Minutes', async () => {
    booksApi.getBooks.mockResolvedValue({
      data: { records: [{ id: 1, title: 'Hot Book' }], total: 1 }
    })

    const store = useBookStore()
    await store.loadHotBooks(10)
    await store.loadHotBooks(10)

    expect(booksApi.getBooks).toHaveBeenCalledOnce()
    expect(store.hotBooks).toEqual([{ id: 1, title: 'Hot Book' }])
    expect(store.isHotBooksValid).toBe(true)

    // Force should bypass
    await store.loadHotBooks(10, true)
    expect(booksApi.getBooks).toHaveBeenCalledTimes(2)
  })

  it('addRecentBook_shouldDedupAndCapAt20', () => {
    const store = useBookStore()

    // Add 25 books
    for (let i = 1; i <= 25; i++) {
      store.addRecentBook({
        id: i,
        title: `Book ${i}`,
        author: 'Author',
        isbn: `ISBN-${i}`,
        coverUrl: ''
      })
    }

    // Add a duplicate (id=10) — should move to top, no extra entry
    store.addRecentBook({
      id: 10,
      title: 'Book 10',
      author: 'Author',
      isbn: 'ISBN-10',
      coverUrl: ''
    })

    expect(store.recentBooks.length).toBe(20)
    expect(store.recentBooks[0].id).toBe(10)
    // Verify uniqueness
    const ids = store.recentBooks.map((b) => b.id)
    expect(new Set(ids).size).toBe(20)
  })

  it('getCategoryName_shouldReturnNameFromTree', async () => {
    booksApi.getBookCategories.mockResolvedValue({ data: mockCategoriesTree })

    const store = useBookStore()
    await store.loadCategories()

    expect(store.getCategoryName('A')).toBe('马克思主义')
    expect(store.getCategoryName('TP3')).toBe('计算技术')
    // Unknown code falls back to the code itself
    expect(store.getCategoryName('ZZ')).toBe('ZZ')
  })

  it('categoryOptions_shouldFlattenTree', async () => {
    booksApi.getBookCategories.mockResolvedValue({ data: mockCategoriesTree })

    const store = useBookStore()
    await store.loadCategories()

    const options = store.categoryOptions
    // 2 root + 2 + 1 + 1 = 6
    expect(options.length).toBe(6)
    expect(options[0]).toMatchObject({ value: 'A', level: 0 })
    expect(options[1].level).toBe(1)
    expect(options[1].value).toBe('A1')
    // Indented label
    expect(options[1].label.startsWith('\u00A0\u00A0')).toBe(true)

    const tp3 = options.find((o) => o.value === 'TP3')
    expect(tp3.level).toBe(2)
  })

  it('clearCache_shouldResetTimestamps', async () => {
    booksApi.getBookCategories.mockResolvedValue({ data: mockCategoriesTree })
    booksApi.getBooks.mockResolvedValue({
      data: { records: [{ id: 1, title: 'Hot Book' }], total: 1 }
    })

    const store = useBookStore()
    await store.loadCategories()
    await store.loadHotBooks()

    store.clearCache()

    expect(store.categories).toEqual([])
    expect(store.categoriesLoadedAt).toBe(null)
    expect(store.hotBooks).toEqual([])
    expect(store.hotBooksLoadedAt).toBe(null)
    expect(store.isCategoriesValid).toBe(false)
    expect(store.isHotBooksValid).toBe(false)
  })

  it('reset_shouldClearAllState', async () => {
    booksApi.getBookCategories.mockResolvedValue({ data: mockCategoriesTree })

    const store = useBookStore()
    await store.loadCategories()
    store.addRecentBook({ id: 1, title: 'Book 1' })
    store.addSearchHistory('vue')
    store.addSearchHistory('pinia')

    expect(store.recentBooks.length).toBe(1)
    expect(store.searchHistory.length).toBe(2)

    store.reset()

    expect(store.categories).toEqual([])
    expect(store.categoriesLoadedAt).toBe(null)
    expect(store.hotBooks).toEqual([])
    expect(store.hotBooksLoadedAt).toBe(null)
    expect(store.recentBooks).toEqual([])
    expect(store.searchHistory).toEqual([])
  })
})
