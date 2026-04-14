/**
 * 流通管理 Store
 * 用于管理借阅、归还等流通操作状态
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCirculationStore = defineStore(
  'circulation',
  () => {
    // ========================================
    // State
    // ========================================

    // 当前借阅操作状态
    const borrowOperation = ref({
      reader: null, // 读者信息
      books: [], // 待借图书列表
      inProgress: false // 是否正在操作中
    })

    // 当前归还操作状态
    const returnOperation = ref({
      reader: null, // 读者信息
      records: [], // 待还借阅记录列表
      inProgress: false // 是否正在操作中
    })

    // 借阅清单（购物车模式）
    const borrowCart = ref([])

    // 归还清单
    const returnCart = ref([])

    // 罚款规则配置缓存
    const fineRules = ref({
      overduePerDay: 0.5, // 每天逾期罚款金额
      damagedBase: 10.0, // 图书损坏基础罚款
      lostMultiplier: 2.0, // 图书遗失赔偿倍数（图书价格 * 倍数）
      maxFine: 100.0 // 最大罚款金额
    })

    // 罚款规则加载时间戳
    const fineRulesLoadedAt = ref(null)

    // 借阅规则配置缓存
    const borrowRules = ref({
      maxRenewCount: 2, // 最大续借次数
      maxBorrowDays: 30, // 最大借阅天数
      reserveValidDays: 7 // 预约有效天数
    })

    // 借阅规则加载时间戳
    const borrowRulesLoadedAt = ref(null)

    // 缓存有效期（毫秒）
    const CACHE_DURATION = 30 * 60 * 1000 // 30分钟

    // ========================================
    // Getters
    // ========================================

    /**
     * 借阅购物车图书数量
     */
    const borrowCartCount = computed(() => {
      return borrowCart.value.length
    })

    /**
     * 归还购物车记录数量
     */
    const returnCartCount = computed(() => {
      return returnCart.value.length
    })

    /**
     * 是否正在进行借阅操作
     */
    const isBorrowing = computed(() => {
      return borrowOperation.value.inProgress
    })

    /**
     * 是否正在进行归还操作
     */
    const isReturning = computed(() => {
      return returnOperation.value.inProgress
    })

    /**
     * 罚款规则缓存是否有效
     */
    const isFineRulesValid = computed(() => {
      if (!fineRulesLoadedAt.value) return false
      return Date.now() - fineRulesLoadedAt.value < CACHE_DURATION
    })

    /**
     * 借阅规则缓存是否有效
     */
    const isBorrowRulesValid = computed(() => {
      if (!borrowRulesLoadedAt.value) return false
      return Date.now() - borrowRulesLoadedAt.value < CACHE_DURATION
    })

    /**
     * 计算逾期罚款
     */
    const calculateOverdueFine = computed(() => {
      return (overdueDays) => {
        if (overdueDays <= 0) return 0
        const fine = overdueDays * fineRules.value.overduePerDay
        return Math.min(fine, fineRules.value.maxFine)
      }
    })

    /**
     * 计算损坏赔偿
     */
    const calculateDamageFine = computed(() => {
      return (bookPrice) => {
        return fineRules.value.damagedBase + (bookPrice || 0) * 0.5
      }
    })

    /**
     * 计算遗失赔偿
     */
    const calculateLostFine = computed(() => {
      return (bookPrice) => {
        return (bookPrice || 0) * fineRules.value.lostMultiplier
      }
    })

    // ========================================
    // Actions - 借阅操作
    // ========================================

    /**
     * 开始借阅流程
     * @param {Object} reader - 读者信息
     */
    function startBorrow(reader) {
      borrowOperation.value = {
        reader,
        books: [...borrowCart.value],
        inProgress: true
      }
    }

    /**
     * 结束借阅流程
     * @param {boolean} success - 是否成功
     */
    function endBorrow(success = true) {
      if (success) {
        // 清空借阅购物车
        borrowCart.value = []
      }

      borrowOperation.value = {
        reader: null,
        books: [],
        inProgress: false
      }
    }

    /**
     * 添加图书到借阅购物车
     * @param {Object} book - 图书信息
     */
    function addToBorrowCart(book) {
      if (!book || !book.id) return

      // 检查是否已存在
      const exists = borrowCart.value.some((b) => b.id === book.id)
      if (exists) {
        console.warn('Book already in borrow cart:', book.id)
        return
      }

      borrowCart.value.push({
        id: book.id,
        title: book.title,
        author: book.author,
        isbn: book.isbn,
        barcode: book.barcode,
        coverUrl: book.coverUrl,
        addedAt: new Date().toISOString()
      })
    }

    /**
     * 从借阅购物车移除图书
     * @param {number} bookId - 图书ID
     */
    function removeFromBorrowCart(bookId) {
      borrowCart.value = borrowCart.value.filter((b) => b.id !== bookId)
    }

    /**
     * 清空借阅购物车
     */
    function clearBorrowCart() {
      borrowCart.value = []
    }

    // ========================================
    // Actions - 归还操作
    // ========================================

    /**
     * 开始归还流程
     * @param {Object} reader - 读者信息
     */
    function startReturn(reader) {
      returnOperation.value = {
        reader,
        records: [...returnCart.value],
        inProgress: true
      }
    }

    /**
     * 结束归还流程
     * @param {boolean} success - 是否成功
     */
    function endReturn(success = true) {
      if (success) {
        // 清空归还购物车
        returnCart.value = []
      }

      returnOperation.value = {
        reader: null,
        records: [],
        inProgress: false
      }
    }

    /**
     * 添加借阅记录到归还购物车
     * @param {Object} record - 借阅记录
     */
    function addToReturnCart(record) {
      if (!record || !record.id) return

      // 检查是否已存在
      const exists = returnCart.value.some((r) => r.id === record.id)
      if (exists) {
        console.warn('Record already in return cart:', record.id)
        return
      }

      returnCart.value.push({
        id: record.id,
        bookId: record.bookId,
        bookTitle: record.bookTitle,
        barcode: record.barcode,
        borrowDate: record.borrowDate,
        dueDate: record.dueDate,
        isOverdue: record.isOverdue,
        overdueDays: record.overdueDays,
        addedAt: new Date().toISOString()
      })
    }

    /**
     * 从归还购物车移除记录
     * @param {number} recordId - 借阅记录ID
     */
    function removeFromReturnCart(recordId) {
      returnCart.value = returnCart.value.filter((r) => r.id !== recordId)
    }

    /**
     * 清空归还购物车
     */
    function clearReturnCart() {
      returnCart.value = []
    }

    // ========================================
    // Actions - 规则配置
    // ========================================

    /**
     * 加载罚款规则（带缓存）
     * @param {boolean} force - 是否强制刷新
     */
    async function loadFineRules(force = false) {
      // 如果缓存有效且不是强制刷新，直接返回
      if (!force && isFineRulesValid.value) {
        return fineRules.value
      }

      try {
        // TODO: 调用API获取罚款规则
        // const response = await getFineRulesAPI()
        // fineRules.value = response.data

        // 暂时使用默认值
        fineRulesLoadedAt.value = Date.now()
        return fineRules.value
      } catch (error) {
        console.error('Failed to load fine rules:', error)
        throw error
      }
    }

    /**
     * 加载借阅规则（带缓存）
     * @param {boolean} force - 是否强制刷新
     */
    async function loadBorrowRules(force = false) {
      // 如果缓存有效且不是强制刷新，直接返回
      if (!force && isBorrowRulesValid.value) {
        return borrowRules.value
      }

      try {
        // TODO: 调用API获取借阅规则
        // const response = await getBorrowRulesAPI()
        // borrowRules.value = response.data

        // 暂时使用默认值
        borrowRulesLoadedAt.value = Date.now()
        return borrowRules.value
      } catch (error) {
        console.error('Failed to load borrow rules:', error)
        throw error
      }
    }

    /**
     * 更新罚款规则
     * @param {Object} rules - 新的罚款规则
     */
    function updateFineRules(rules) {
      fineRules.value = {
        ...fineRules.value,
        ...rules
      }
      fineRulesLoadedAt.value = Date.now()
    }

    /**
     * 更新借阅规则
     * @param {Object} rules - 新的借阅规则
     */
    function updateBorrowRules(rules) {
      borrowRules.value = {
        ...borrowRules.value,
        ...rules
      }
      borrowRulesLoadedAt.value = Date.now()
    }

    /**
     * 清除规则缓存
     */
    function clearRulesCache() {
      fineRulesLoadedAt.value = null
      borrowRulesLoadedAt.value = null
    }

    /**
     * 重置Store（清除所有数据）
     */
    function reset() {
      borrowOperation.value = {
        reader: null,
        books: [],
        inProgress: false
      }
      returnOperation.value = {
        reader: null,
        records: [],
        inProgress: false
      }
      borrowCart.value = []
      returnCart.value = []
      clearRulesCache()
    }

    // ========================================
    // Return
    // ========================================

    return {
      // State
      borrowOperation,
      returnOperation,
      borrowCart,
      returnCart,
      fineRules,
      fineRulesLoadedAt,
      borrowRules,
      borrowRulesLoadedAt,

      // Getters
      borrowCartCount,
      returnCartCount,
      isBorrowing,
      isReturning,
      isFineRulesValid,
      isBorrowRulesValid,
      calculateOverdueFine,
      calculateDamageFine,
      calculateLostFine,

      // Actions - 借阅操作
      startBorrow,
      endBorrow,
      addToBorrowCart,
      removeFromBorrowCart,
      clearBorrowCart,

      // Actions - 归还操作
      startReturn,
      endReturn,
      addToReturnCart,
      removeFromReturnCart,
      clearReturnCart,

      // Actions - 规则配置
      loadFineRules,
      loadBorrowRules,
      updateFineRules,
      updateBorrowRules,
      clearRulesCache,
      reset
    }
  },
  {
    // 持久化配置（只持久化购物车数据）
    persist: {
      enabled: true,
      strategies: [
        {
          key: 'circulation',
          storage: sessionStorage, // 使用 sessionStorage，关闭标签页即清除
          // 只持久化购物车数据
          paths: ['borrowCart', 'returnCart']
        }
      ]
    }
  }
)
