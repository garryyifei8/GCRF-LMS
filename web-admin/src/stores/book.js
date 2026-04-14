/**
 * 图书管理 Store
 * 用于缓存图书相关数据，减少API请求
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getBookCategories, getBooks } from '@/api/books'

export const useBookStore = defineStore(
  'book',
  () => {
    // ========================================
    // State
    // ========================================

    // 图书分类缓存（树形结构）
    const categories = ref([])
    // 分类加载时间戳（用于控制缓存有效期）
    const categoriesLoadedAt = ref(null)
    // 分类是否正在加载
    const categoriesLoading = ref(false)

    // 热门图书缓存
    const hotBooks = ref([])
    // 热门图书加载时间戳
    const hotBooksLoadedAt = ref(null)

    // 最近访问的图书（最多保存20条）
    const recentBooks = ref([])

    // 搜索历史（最多保存10条）
    const searchHistory = ref([])

    // 缓存有效期（毫秒）
    const CACHE_DURATION = 5 * 60 * 1000 // 5分钟

    // ========================================
    // Getters
    // ========================================

    /**
     * 分类缓存是否有效
     */
    const isCategoriesValid = computed(() => {
      if (!categoriesLoadedAt.value) return false
      return Date.now() - categoriesLoadedAt.value < CACHE_DURATION
    })

    /**
     * 热门图书缓存是否有效
     */
    const isHotBooksValid = computed(() => {
      if (!hotBooksLoadedAt.value) return false
      return Date.now() - hotBooksLoadedAt.value < CACHE_DURATION
    })

    /**
     * 根据分类编码获取分类名称
     */
    const getCategoryName = computed(() => {
      return (categoryCode) => {
        const findCategory = (cats) => {
          for (const cat of cats) {
            if (cat.code === categoryCode) {
              return cat.name
            }
            if (cat.children && cat.children.length > 0) {
              const found = findCategory(cat.children)
              if (found) return found
            }
          }
          return null
        }
        return findCategory(categories.value) || categoryCode
      }
    })

    /**
     * 将分类树扁平化为下拉选项
     */
    const categoryOptions = computed(() => {
      const flatten = (cats, level = 0) => {
        const result = []
        cats.forEach((cat) => {
          result.push({
            value: cat.code,
            label: '\u00A0'.repeat(level * 2) + cat.name,
            level
          })
          if (cat.children && cat.children.length > 0) {
            result.push(...flatten(cat.children, level + 1))
          }
        })
        return result
      }
      return flatten(categories.value)
    })

    // ========================================
    // Actions
    // ========================================

    /**
     * 加载图书分类（带缓存）
     * @param {boolean} force - 是否强制刷新
     */
    async function loadCategories(force = false) {
      // 如果缓存有效且不是强制刷新，直接返回
      if (!force && isCategoriesValid.value && categories.value.length > 0) {
        return categories.value
      }

      try {
        categoriesLoading.value = true
        const response = await getBookCategories()

        if (response.data) {
          categories.value = response.data
          categoriesLoadedAt.value = Date.now()
        }

        return categories.value
      } catch (error) {
        console.error('Failed to load book categories:', error)
        throw error
      } finally {
        categoriesLoading.value = false
      }
    }

    /**
     * 加载热门图书（带缓存）
     * @param {number} limit - 数量限制
     * @param {boolean} force - 是否强制刷新
     */
    async function loadHotBooks(limit = 10, force = false) {
      // 如果缓存有效且不是强制刷新，直接返回
      if (!force && isHotBooksValid.value && hotBooks.value.length > 0) {
        return hotBooks.value
      }

      try {
        const response = await getBooks({
          pageNum: 1,
          pageSize: limit,
          sortBy: 'borrowCount',
          sortOrder: 'desc'
        })

        if (response.data && response.data.records) {
          hotBooks.value = response.data.records
          hotBooksLoadedAt.value = Date.now()
        }

        return hotBooks.value
      } catch (error) {
        console.error('Failed to load hot books:', error)
        throw error
      }
    }

    /**
     * 添加最近访问的图书
     * @param {Object} book - 图书信息
     */
    function addRecentBook(book) {
      if (!book || !book.id) return

      // 移除重复项
      const filtered = recentBooks.value.filter((b) => b.id !== book.id)

      // 添加到开头
      recentBooks.value = [
        {
          id: book.id,
          title: book.title,
          author: book.author,
          isbn: book.isbn,
          coverUrl: book.coverUrl,
          visitedAt: new Date().toISOString()
        },
        ...filtered
      ].slice(0, 20) // 只保留最近20条
    }

    /**
     * 清除最近访问的图书
     */
    function clearRecentBooks() {
      recentBooks.value = []
    }

    /**
     * 添加搜索历史
     * @param {string} keyword - 搜索关键词
     */
    function addSearchHistory(keyword) {
      if (!keyword || !keyword.trim()) return

      const trimmed = keyword.trim()

      // 移除重复项
      const filtered = searchHistory.value.filter((k) => k !== trimmed)

      // 添加到开头
      searchHistory.value = [trimmed, ...filtered].slice(0, 10) // 只保留最近10条
    }

    /**
     * 清除搜索历史
     */
    function clearSearchHistory() {
      searchHistory.value = []
    }

    /**
     * 清除所有缓存
     */
    function clearCache() {
      categories.value = []
      categoriesLoadedAt.value = null
      hotBooks.value = []
      hotBooksLoadedAt.value = null
    }

    /**
     * 重置Store（清除所有数据）
     */
    function reset() {
      clearCache()
      clearRecentBooks()
      clearSearchHistory()
    }

    // ========================================
    // Return
    // ========================================

    return {
      // State
      categories,
      categoriesLoadedAt,
      categoriesLoading,
      hotBooks,
      hotBooksLoadedAt,
      recentBooks,
      searchHistory,

      // Getters
      isCategoriesValid,
      isHotBooksValid,
      getCategoryName,
      categoryOptions,

      // Actions
      loadCategories,
      loadHotBooks,
      addRecentBook,
      clearRecentBooks,
      addSearchHistory,
      clearSearchHistory,
      clearCache,
      reset
    }
  },
  {
    // 持久化配置（只持久化必要的数据）
    persist: {
      enabled: true,
      strategies: [
        {
          key: 'book',
          storage: localStorage,
          // 只持久化部分数据，缓存的数据不持久化
          paths: ['recentBooks', 'searchHistory']
        }
      ]
    }
  }
)
