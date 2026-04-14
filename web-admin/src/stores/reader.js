/**
 * 读者管理 Store
 * 用于缓存读者相关数据，减少API请求
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getReaderTypes } from '@/api/readers'

export const useReaderStore = defineStore(
  'reader',
  () => {
    // ========================================
    // State
    // ========================================

    // 读者类型配置缓存
    const readerTypes = ref([])
    // 读者类型加载时间戳
    const readerTypesLoadedAt = ref(null)
    // 读者类型是否正在加载
    const readerTypesLoading = ref(false)

    // 当前操作的读者信息（用于借还书等操作）
    const currentReader = ref(null)

    // 读者搜索历史（最多保存10条）
    const searchHistory = ref([])

    // 最近操作的读者（最多保存20条）
    const recentReaders = ref([])

    // 缓存有效期（毫秒）
    const CACHE_DURATION = 10 * 60 * 1000 // 10分钟

    // ========================================
    // Getters
    // ========================================

    /**
     * 读者类型缓存是否有效
     */
    const isReaderTypesValid = computed(() => {
      if (!readerTypesLoadedAt.value) return false
      return Date.now() - readerTypesLoadedAt.value < CACHE_DURATION
    })

    /**
     * 读者类型选项（用于下拉选择）
     */
    const readerTypeOptions = computed(() => {
      return readerTypes.value.map((type) => ({
        value: type.code,
        label: type.name,
        maxBorrowCount: type.maxBorrowCount,
        borrowDays: type.borrowDays
      }))
    })

    /**
     * 根据类型编码获取类型信息
     */
    const getReaderTypeByCode = computed(() => {
      return (code) => {
        return readerTypes.value.find((type) => type.code === code) || null
      }
    })

    /**
     * 根据类型编码获取类型名称
     */
    const getReaderTypeName = computed(() => {
      return (code) => {
        const type = readerTypes.value.find((t) => t.code === code)
        return type ? type.name : code
      }
    })

    /**
     * 当前读者是否已选择
     */
    const hasCurrentReader = computed(() => {
      return currentReader.value !== null
    })

    /**
     * 当前读者的借阅能力
     */
    const currentReaderBorrowCapacity = computed(() => {
      if (!currentReader.value) {
        return {
          canBorrow: false,
          maxBorrowCount: 0,
          currentBorrowCount: 0,
          remainingCount: 0
        }
      }

      const maxCount = currentReader.value.maxBorrowCount || 0
      const currentCount = currentReader.value.currentBorrowCount || 0
      const remaining = maxCount - currentCount

      return {
        canBorrow: remaining > 0 && currentReader.value.status === 'active',
        maxBorrowCount: maxCount,
        currentBorrowCount: currentCount,
        remainingCount: remaining
      }
    })

    // ========================================
    // Actions
    // ========================================

    /**
     * 加载读者类型配置（带缓存）
     * @param {boolean} force - 是否强制刷新
     */
    async function loadReaderTypes(force = false) {
      // 如果缓存有效且不是强制刷新，直接返回
      if (!force && isReaderTypesValid.value && readerTypes.value.length > 0) {
        return readerTypes.value
      }

      try {
        readerTypesLoading.value = true
        const response = await getReaderTypes()

        if (response.data) {
          readerTypes.value = Array.isArray(response.data) ? response.data : []
          readerTypesLoadedAt.value = Date.now()
        }

        return readerTypes.value
      } catch (error) {
        console.error('Failed to load reader types:', error)
        throw error
      } finally {
        readerTypesLoading.value = false
      }
    }

    /**
     * 设置当前操作的读者
     * @param {Object|null} reader - 读者信息
     */
    function setCurrentReader(reader) {
      currentReader.value = reader

      // 如果是新选择的读者，添加到最近操作列表
      if (reader && reader.id) {
        addRecentReader(reader)
      }
    }

    /**
     * 清除当前读者
     */
    function clearCurrentReader() {
      currentReader.value = null
    }

    /**
     * 更新当前读者信息（部分更新）
     * @param {Object} updates - 要更新的字段
     */
    function updateCurrentReader(updates) {
      if (!currentReader.value) return

      currentReader.value = {
        ...currentReader.value,
        ...updates
      }
    }

    /**
     * 添加最近操作的读者
     * @param {Object} reader - 读者信息
     */
    function addRecentReader(reader) {
      if (!reader || !reader.id) return

      // 移除重复项
      const filtered = recentReaders.value.filter((r) => r.id !== reader.id)

      // 添加到开头
      recentReaders.value = [
        {
          id: reader.id,
          cardNumber: reader.cardNumber,
          realName: reader.realName,
          phone: reader.phone,
          readerType: reader.readerType,
          status: reader.status,
          operatedAt: new Date().toISOString()
        },
        ...filtered
      ].slice(0, 20) // 只保留最近20条
    }

    /**
     * 清除最近操作的读者
     */
    function clearRecentReaders() {
      recentReaders.value = []
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
     * 清除读者类型缓存
     */
    function clearReaderTypesCache() {
      readerTypes.value = []
      readerTypesLoadedAt.value = null
    }

    /**
     * 重置Store（清除所有数据）
     */
    function reset() {
      clearReaderTypesCache()
      clearCurrentReader()
      clearRecentReaders()
      clearSearchHistory()
    }

    // ========================================
    // Return
    // ========================================

    return {
      // State
      readerTypes,
      readerTypesLoadedAt,
      readerTypesLoading,
      currentReader,
      searchHistory,
      recentReaders,

      // Getters
      isReaderTypesValid,
      readerTypeOptions,
      getReaderTypeByCode,
      getReaderTypeName,
      hasCurrentReader,
      currentReaderBorrowCapacity,

      // Actions
      loadReaderTypes,
      setCurrentReader,
      clearCurrentReader,
      updateCurrentReader,
      addRecentReader,
      clearRecentReaders,
      addSearchHistory,
      clearSearchHistory,
      clearReaderTypesCache,
      reset
    }
  },
  {
    // 持久化配置（只持久化必要的数据）
    persist: {
      enabled: true,
      strategies: [
        {
          key: 'reader',
          storage: localStorage,
          // 只持久化部分数据，缓存的数据不持久化
          paths: ['searchHistory', 'recentReaders']
        }
      ]
    }
  }
)
