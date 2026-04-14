/**
 * 系统配置 Store
 * 用于缓存系统配置、权限等数据
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAllRoles, getPermissionTree } from '@/api/system'

export const useSystemStore = defineStore(
  'system',
  () => {
    // ========================================
    // State
    // ========================================

    // 系统配置
    const config = ref({
      // 系统基本信息
      systemName: '国创睿峰智能图书馆管理系统',
      systemShortName: 'GCRF图书馆',
      version: '1.0.0',
      copyright: '© 2024 国创睿峰科技',

      // 系统设置
      pageSize: 20, // 默认分页大小
      pageSizes: [10, 20, 50, 100], // 分页大小选项
      uploadMaxSize: 10, // 上传文件最大大小（MB）
      dateFormat: 'YYYY-MM-DD', // 日期格式
      datetimeFormat: 'YYYY-MM-DD HH:mm:ss', // 日期时间格式
      timeFormat: 'HH:mm:ss', // 时间格式

      // 业务配置
      defaultPassword: '123456', // 默认密码
      passwordMinLength: 6, // 密码最小长度
      sessionTimeout: 30, // 会话超时时间（分钟）
      autoLogout: true, // 是否自动登出

      // UI配置
      theme: 'light', // 主题 (light/dark)
      sidebarCollapsed: false, // 侧边栏是否收起
      showBreadcrumb: true, // 是否显示面包屑
      showTags: true, // 是否显示标签页
      fixedHeader: true, // 是否固定头部
      showLogo: true // 是否显示Logo
    })

    // 角色列表缓存
    const roles = ref([])
    // 角色加载时间戳
    const rolesLoadedAt = ref(null)
    // 角色是否正在加载
    const rolesLoading = ref(false)

    // 权限树缓存
    const permissionTree = ref([])
    // 权限加载时间戳
    const permissionTreeLoadedAt = ref(null)
    // 权限是否正在加载
    const permissionTreeLoading = ref(false)

    // 借阅规则配置
    const borrowRules = ref({
      // 按读者类型配置
      student: {
        maxBorrowCount: 5,
        borrowDays: 30,
        maxRenewCount: 2,
        renewDays: 15
      },
      teacher: {
        maxBorrowCount: 10,
        borrowDays: 60,
        maxRenewCount: 3,
        renewDays: 30
      },
      staff: {
        maxBorrowCount: 8,
        borrowDays: 45,
        maxRenewCount: 2,
        renewDays: 20
      },
      public: {
        maxBorrowCount: 3,
        borrowDays: 15,
        maxRenewCount: 1,
        renewDays: 7
      }
    })

    // 罚款规则配置
    const fineRules = ref({
      overduePerDay: 0.5, // 每天逾期罚款
      damagedBase: 10.0, // 损坏基础罚款
      lostMultiplier: 2.0, // 遗失赔偿倍数
      maxFine: 100.0 // 最大罚款金额
    })

    // 缓存有效期（毫秒）
    const CACHE_DURATION = 30 * 60 * 1000 // 30分钟

    // ========================================
    // Getters
    // ========================================

    /**
     * 角色缓存是否有效
     */
    const isRolesValid = computed(() => {
      if (!rolesLoadedAt.value) return false
      return Date.now() - rolesLoadedAt.value < CACHE_DURATION
    })

    /**
     * 权限缓存是否有效
     */
    const isPermissionTreeValid = computed(() => {
      if (!permissionTreeLoadedAt.value) return false
      return Date.now() - permissionTreeLoadedAt.value < CACHE_DURATION
    })

    /**
     * 角色选项（用于下拉选择）
     */
    const roleOptions = computed(() => {
      return roles.value.map((role) => ({
        value: role.code,
        label: role.name
      }))
    })

    /**
     * 根据读者类型获取借阅规则
     */
    const getBorrowRuleByType = computed(() => {
      return (readerType) => {
        return borrowRules.value[readerType] || borrowRules.value.public
      }
    })

    /**
     * 根据角色编码获取角色名称
     */
    const getRoleName = computed(() => {
      return (code) => {
        const role = roles.value.find((r) => r.code === code)
        return role ? role.name : code
      }
    })

    /**
     * 主题配置
     */
    const isDark = computed(() => {
      return config.value.theme === 'dark'
    })

    // ========================================
    // Actions - 系统配置
    // ========================================

    /**
     * 更新系统配置
     * @param {Object} updates - 要更新的配置项
     */
    function updateConfig(updates) {
      config.value = {
        ...config.value,
        ...updates
      }
    }

    /**
     * 切换主题
     */
    function toggleTheme() {
      config.value.theme = config.value.theme === 'light' ? 'dark' : 'light'
    }

    /**
     * 切换侧边栏
     */
    function toggleSidebar() {
      config.value.sidebarCollapsed = !config.value.sidebarCollapsed
    }

    /**
     * 重置配置为默认值
     */
    function resetConfig() {
      config.value = {
        systemName: '国创睿峰智能图书馆管理系统',
        systemShortName: 'GCRF图书馆',
        version: '1.0.0',
        copyright: '© 2024 国创睿峰科技',
        pageSize: 20,
        pageSizes: [10, 20, 50, 100],
        uploadMaxSize: 10,
        dateFormat: 'YYYY-MM-DD',
        datetimeFormat: 'YYYY-MM-DD HH:mm:ss',
        timeFormat: 'HH:mm:ss',
        defaultPassword: '123456',
        passwordMinLength: 6,
        sessionTimeout: 30,
        autoLogout: true,
        theme: 'light',
        sidebarCollapsed: false,
        showBreadcrumb: true,
        showTags: true,
        fixedHeader: true,
        showLogo: true
      }
    }

    // ========================================
    // Actions - 角色管理
    // ========================================

    /**
     * 加载角色列表（带缓存）
     * @param {boolean} force - 是否强制刷新
     */
    async function loadRoles(force = false) {
      // 如果缓存有效且不是强制刷新，直接返回
      if (!force && isRolesValid.value && roles.value.length > 0) {
        return roles.value
      }

      try {
        rolesLoading.value = true
        const response = await getAllRoles()

        if (response.data) {
          roles.value = Array.isArray(response.data) ? response.data : []
          rolesLoadedAt.value = Date.now()
        }

        return roles.value
      } catch (error) {
        console.error('Failed to load roles:', error)
        throw error
      } finally {
        rolesLoading.value = false
      }
    }

    /**
     * 清除角色缓存
     */
    function clearRolesCache() {
      roles.value = []
      rolesLoadedAt.value = null
    }

    // ========================================
    // Actions - 权限管理
    // ========================================

    /**
     * 加载权限树（带缓存）
     * @param {boolean} force - 是否强制刷新
     */
    async function loadPermissionTree(force = false) {
      // 如果缓存有效且不是强制刷新，直接返回
      if (!force && isPermissionTreeValid.value && permissionTree.value.length > 0) {
        return permissionTree.value
      }

      try {
        permissionTreeLoading.value = true
        const response = await getPermissionTree()

        if (response.data) {
          permissionTree.value = Array.isArray(response.data) ? response.data : []
          permissionTreeLoadedAt.value = Date.now()
        }

        return permissionTree.value
      } catch (error) {
        console.error('Failed to load permission tree:', error)
        throw error
      } finally {
        permissionTreeLoading.value = false
      }
    }

    /**
     * 清除权限缓存
     */
    function clearPermissionCache() {
      permissionTree.value = []
      permissionTreeLoadedAt.value = null
    }

    // ========================================
    // Actions - 业务规则
    // ========================================

    /**
     * 更新借阅规则
     * @param {string} readerType - 读者类型
     * @param {Object} rules - 借阅规则
     */
    function updateBorrowRules(readerType, rules) {
      if (!borrowRules.value[readerType]) {
        console.warn('Invalid reader type:', readerType)
        return
      }

      borrowRules.value[readerType] = {
        ...borrowRules.value[readerType],
        ...rules
      }
    }

    /**
     * 更新罚款规则
     * @param {Object} rules - 罚款规则
     */
    function updateFineRules(rules) {
      fineRules.value = {
        ...fineRules.value,
        ...rules
      }
    }

    /**
     * 重置业务规则
     */
    function resetBusinessRules() {
      borrowRules.value = {
        student: { maxBorrowCount: 5, borrowDays: 30, maxRenewCount: 2, renewDays: 15 },
        teacher: { maxBorrowCount: 10, borrowDays: 60, maxRenewCount: 3, renewDays: 30 },
        staff: { maxBorrowCount: 8, borrowDays: 45, maxRenewCount: 2, renewDays: 20 },
        public: { maxBorrowCount: 3, borrowDays: 15, maxRenewCount: 1, renewDays: 7 }
      }

      fineRules.value = {
        overduePerDay: 0.5,
        damagedBase: 10.0,
        lostMultiplier: 2.0,
        maxFine: 100.0
      }
    }

    // ========================================
    // Actions - 清理
    // ========================================

    /**
     * 清除所有缓存
     */
    function clearAllCache() {
      clearRolesCache()
      clearPermissionCache()
    }

    /**
     * 重置Store（清除所有数据）
     */
    function reset() {
      resetConfig()
      clearAllCache()
      resetBusinessRules()
    }

    // ========================================
    // Return
    // ========================================

    return {
      // State
      config,
      roles,
      rolesLoadedAt,
      rolesLoading,
      permissionTree,
      permissionTreeLoadedAt,
      permissionTreeLoading,
      borrowRules,
      fineRules,

      // Getters
      isRolesValid,
      isPermissionTreeValid,
      roleOptions,
      getBorrowRuleByType,
      getRoleName,
      isDark,

      // Actions - 系统配置
      updateConfig,
      toggleTheme,
      toggleSidebar,
      resetConfig,

      // Actions - 角色管理
      loadRoles,
      clearRolesCache,

      // Actions - 权限管理
      loadPermissionTree,
      clearPermissionCache,

      // Actions - 业务规则
      updateBorrowRules,
      updateFineRules,
      resetBusinessRules,

      // Actions - 清理
      clearAllCache,
      reset
    }
  },
  {
    // 持久化配置（只持久化系统配置）
    persist: {
      enabled: true,
      strategies: [
        {
          key: 'system',
          storage: localStorage,
          // 只持久化系统配置，缓存数据不持久化
          paths: ['config']
        }
      ]
    }
  }
)
