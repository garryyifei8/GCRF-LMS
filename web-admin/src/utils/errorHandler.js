import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/**
 * 错误类型枚举
 */
export const ErrorType = {
  BUSINESS: 'business', // 业务错误
  NETWORK: 'network', // 网络错误
  AUTH: 'auth', // 认证错误
  PERMISSION: 'permission', // 权限错误
  VALIDATION: 'validation', // 验证错误
  SYSTEM: 'system', // 系统错误
  TIMEOUT: 'timeout', // 超时错误
  UNKNOWN: 'unknown' // 未知错误
}

/**
 * HTTP 状态码映射
 */
const HTTP_STATUS_MAP = {
  400: { type: ErrorType.VALIDATION, message: '请求参数错误' },
  401: { type: ErrorType.AUTH, message: '登录已过期，请重新登录' },
  403: { type: ErrorType.PERMISSION, message: '权限不足，无法访问' },
  404: { type: ErrorType.BUSINESS, message: '请求的资源不存在' },
  408: { type: ErrorType.TIMEOUT, message: '请求超时，请稍后重试' },
  409: { type: ErrorType.BUSINESS, message: '数据冲突，请刷新后重试' },
  422: { type: ErrorType.VALIDATION, message: '数据验证失败' },
  429: { type: ErrorType.BUSINESS, message: '请求过于频繁，请稍后重试' },
  500: { type: ErrorType.SYSTEM, message: '服务器内部错误' },
  502: { type: ErrorType.NETWORK, message: '网关错误' },
  503: { type: ErrorType.SYSTEM, message: '服务暂时不可用，请稍后重试' },
  504: { type: ErrorType.TIMEOUT, message: '网关超时' }
}

/**
 * 业务错误码映射
 */
const BUSINESS_CODE_MAP = {
  // 通用错误码 (1000-1999)
  1001: '参数验证失败',
  1002: '数据不存在',
  1003: '数据已存在',
  1004: '操作失败',
  1005: '数据状态不正确',

  // 认证错误码 (2000-2999)
  2001: '登录失败，用户名或密码错误',
  2002: '账号已被禁用',
  2003: 'Token已过期',
  2004: 'Token无效',
  2005: '登录已过期，请重新登录',

  // 权限错误码 (3000-3999)
  3001: '无权限访问',
  3002: '角色不存在',
  3003: '权限配置错误',

  // 读者错误码 (4000-4999)
  4001: '读者证号已存在',
  4002: '读者信息不存在',
  4003: '读者账号已过期',
  4004: '读者违章，暂停借阅',

  // 图书错误码 (5000-5999)
  5001: 'ISBN已存在',
  5002: '图书不存在',
  5003: '图书库存不足',
  5004: '条码号已存在',

  // 流通错误码 (6000-6999)
  6001: '借阅数量超限',
  6002: '图书已借出',
  6003: '该读者未借阅此书',
  6004: '续借次数超限',
  6005: '罚金未结清'
}

/**
 * 错误处理类
 */
class ErrorHandler {
  /**
   * 处理响应错误
   * @param {Object} error - axios错误对象
   * @returns {Promise} rejected promise
   */
  handleResponseError(error) {
    // 1. 响应错误（有response对象）
    if (error.response) {
      const { status, data } = error.response
      const errorInfo = HTTP_STATUS_MAP[status] || {
        type: ErrorType.UNKNOWN,
        message: '请求失败'
      }

      // 优先使用服务器返回的错误信息
      const message = data?.message || errorInfo.message

      // 根据错误类型处理
      switch (errorInfo.type) {
        case ErrorType.AUTH:
          this.handleAuthError(message)
          break
        case ErrorType.PERMISSION:
          this.handlePermissionError(message)
          break
        case ErrorType.VALIDATION:
          this.handleValidationError(message, data)
          break
        case ErrorType.BUSINESS:
          this.handleBusinessError(message)
          break
        case ErrorType.SYSTEM:
          this.handleSystemError(message)
          break
        case ErrorType.TIMEOUT:
          this.handleTimeoutError(message)
          break
        default:
          this.handleUnknownError(message)
      }

      return Promise.reject({
        type: errorInfo.type,
        message,
        status,
        data
      })
    }

    // 2. 请求错误（无response对象）
    if (error.request) {
      // 网络错误
      if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
        return this.handleTimeoutError('请求超时，请检查网络连接')
      }
      if (error.message === 'Network Error') {
        return this.handleNetworkError('网络连接失败，请检查网络设置')
      }
      return this.handleNetworkError('网络请求失败')
    }

    // 3. 其他错误
    return this.handleUnknownError(error.message || '未知错误')
  }

  /**
   * 处理业务错误（code !== 200）
   * @param {Object} response - 响应数据
   * @returns {Promise} rejected promise
   */
  handleBusinessCode(response) {
    const { code, message } = response

    // code为200表示成功
    if (code === 200) {
      return Promise.resolve(response)
    }

    // 处理特殊业务错误码
    if (code === 401 || code === 2003 || code === 2004 || code === 2005) {
      return this.handleAuthError(message || '登录已过期')
    }

    if (code === 403 || code === 3001) {
      return this.handlePermissionError(message || '权限不足')
    }

    // 使用业务错误码映射或使用返回的message
    const errorMessage = BUSINESS_CODE_MAP[code] || message || '操作失败'
    this.handleBusinessError(errorMessage)

    return Promise.reject({
      type: ErrorType.BUSINESS,
      code,
      message: errorMessage
    })
  }

  /**
   * 处理认证错误
   */
  handleAuthError(message) {
    ElMessageBox.confirm(message, '登录过期', {
      confirmButtonText: '重新登录',
      cancelButtonText: '取消',
      type: 'warning',
      showClose: false,
      closeOnClickModal: false,
      closeOnPressEscape: false
    })
      .then(() => {
        const userStore = useUserStore()
        userStore.logout()
        router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
      })
      .catch(() => {
        // 用户取消，也要跳转登录
        const userStore = useUserStore()
        userStore.logout()
        router.push({ name: 'Login' })
      })
  }

  /**
   * 处理权限错误
   */
  handlePermissionError(message) {
    ElMessage.error({
      message,
      duration: 3000,
      showClose: true
    })

    // 权限不足，返回首页
    setTimeout(() => {
      router.push({ name: 'Home' })
    }, 1500)
  }

  /**
   * 处理验证错误
   */
  handleValidationError(message, data) {
    // 如果有详细的验证错误信息
    if (data?.errors && Array.isArray(data.errors)) {
      const errorMessages = data.errors.map((err) => err.message || err).join('\n')
      ElMessage.error({
        message: errorMessages,
        duration: 4000,
        showClose: true
      })
    } else {
      ElMessage.error({
        message,
        duration: 3000,
        showClose: true
      })
    }
  }

  /**
   * 处理业务错误
   */
  handleBusinessError(message) {
    ElMessage.error({
      message,
      duration: 3000,
      showClose: true
    })
  }

  /**
   * 处理系统错误
   */
  handleSystemError(message) {
    ElMessage.error({
      message: `${message}，请稍后重试或联系管理员`,
      duration: 4000,
      showClose: true
    })
  }

  /**
   * 处理超时错误
   */
  handleTimeoutError(message) {
    ElMessage.warning({
      message,
      duration: 3000,
      showClose: true
    })

    return Promise.reject({
      type: ErrorType.TIMEOUT,
      message
    })
  }

  /**
   * 处理网络错误
   */
  handleNetworkError(message) {
    ElMessage.error({
      message: `${message}，请检查网络连接后重试`,
      duration: 4000,
      showClose: true
    })

    return Promise.reject({
      type: ErrorType.NETWORK,
      message
    })
  }

  /**
   * 处理未知错误
   */
  handleUnknownError(message) {
    ElMessage.error({
      message: message || '发生未知错误',
      duration: 3000,
      showClose: true
    })

    return Promise.reject({
      type: ErrorType.UNKNOWN,
      message
    })
  }
}

// 导出单例
export const errorHandler = new ErrorHandler()

/**
 * 全局错误处理函数（用于Vue的errorHandler）
 */
export function globalErrorHandler(error, vm, info) {
  console.error('Global Error:', error)
  console.error('Component:', vm)
  console.error('Error Info:', info)

  // 根据错误类型显示不同提示
  if (error.name === 'ChunkLoadError') {
    ElMessageBox.confirm('页面资源加载失败，可能是版本更新导致，是否刷新页面？', '提示', {
      confirmButtonText: '刷新',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      window.location.reload()
    })
  } else if (error.message?.includes('ResizeObserver')) {
    // ResizeObserver错误可以忽略
    return
  } else {
    ElMessage.error({
      message: `页面发生错误: ${error.message}`,
      duration: 3000,
      showClose: true
    })
  }
}

/**
 * Promise错误处理函数（用于window.onunhandledrejection）
 */
export function unhandledRejectionHandler(event) {
  console.error('Unhandled Promise Rejection:', event.reason)

  // 如果是已处理的错误对象，不重复提示
  if (event.reason?.type && Object.values(ErrorType).includes(event.reason.type)) {
    return
  }

  ElMessage.error({
    message: `操作失败: ${event.reason?.message || '未知错误'}`,
    duration: 3000,
    showClose: true
  })

  // 阻止默认的控制台错误输出
  event.preventDefault()
}

export default errorHandler
