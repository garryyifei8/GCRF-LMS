import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import { errorHandler } from './errorHandler'

// 创建 axios 实例
const service = axios.create({
  // 注意: API 路径中已包含 /api 前缀，所以 baseURL 应为空
  // 如果 VITE_API_BASE_URL 未定义，则默认为空字符串（不是 /api）
  baseURL: import.meta.env.VITE_API_BASE_URL !== undefined ? import.meta.env.VITE_API_BASE_URL : '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// Token刷新相关
let isRefreshing = false // 是否正在刷新Token
let requestsQueue = [] // 等待队列

/**
 * 刷新Token
 */
const refreshToken = async () => {
  const userStore = useUserStore()
  try {
    // TODO: 调用刷新Token的API
    // const res = await axios.post('/api/auth/refresh', { refreshToken: userStore.refreshToken })
    // userStore.setToken(res.data.token)
    // return res.data.token

    // 暂时返回当前token（实际应该调用刷新接口）
    return userStore.token
  } catch (error) {
    // 刷新失败，清除登录信息
    userStore.logout()
    router.push({ name: 'Login' })
    throw error
  }
}

/**
 * 将请求加入等待队列
 */
const addRequestToQueue = (config) => {
  return new Promise((resolve, reject) => {
    requestsQueue.push({ config, resolve, reject })
  })
}

/**
 * 重试队列中的请求
 */
const retryRequestsQueue = (token) => {
  requestsQueue.forEach(({ config, resolve, reject }) => {
    config.headers['Authorization'] = `Bearer ${token}`
    service(config)
      .then((response) => resolve(response))
      .catch((error) => reject(error))
  })
  requestsQueue = []
}

// 请求拦截器
service.interceptors.request.use(
  async (config) => {
    const userStore = useUserStore()

    // 如果正在刷新Token，将请求加入队列
    if (isRefreshing) {
      return addRequestToQueue(config)
    }

    // 添加 token
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }

    // 添加请求唯一标识（用于取消重复请求）
    config.cancelToken = new axios.CancelToken((cancel) => {
      config.cancel = cancel
    })

    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  async (response) => {
    const res = response.data

    // 兼容两种响应格式：
    // 1. 标准格式: { code, message, data, success, timestamp }
    // 2. 直接格式: { records, total, size/pageSize, current/pageNum, pages }

    // 如果响应中有 code 字段，使用标准格式处理
    if ('code' in res) {
      // Token过期，尝试刷新
      if (res.code === 401 && !response.config._retry) {
        if (!isRefreshing) {
          isRefreshing = true
          response.config._retry = true

          try {
            const newToken = await refreshToken()
            isRefreshing = false

            // 重试当前请求
            response.config.headers['Authorization'] = `Bearer ${newToken}`
            const retryResponse = await service(response.config)

            // 重试队列中的请求
            retryRequestsQueue(newToken)

            return retryResponse
          } catch (error) {
            isRefreshing = false
            requestsQueue = []
            return Promise.reject(error)
          }
        } else {
          // 正在刷新，加入队列
          return addRequestToQueue(response.config)
        }
      }

      // 使用统一错误处理器处理业务错误
      return errorHandler.handleBusinessCode(res)
    }

    // 如果响应中有 records 字段，说明是直接格式（如Reader Service）
    // 将其包装成标准格式以保持一致性
    if ('records' in res) {
      return {
        code: 200,
        message: '操作成功',
        data: {
          records: res.records,
          total: res.total || 0,
          pageNum: res.current || res.pageNum || 1,
          pageSize: res.size || res.pageSize || 10,
          pages: res.pages || Math.ceil((res.total || 0) / (res.size || res.pageSize || 10))
        },
        success: true,
        timestamp: Date.now()
      }
    }

    // 其他情况，直接返回原始响应
    return res
  },
  async (error) => {
    // 如果是取消的请求，不处理
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }

    // Token过期（HTTP 401），尝试刷新
    if (error.response?.status === 401 && !error.config._retry) {
      if (!isRefreshing) {
        isRefreshing = true
        error.config._retry = true

        try {
          const newToken = await refreshToken()
          isRefreshing = false

          // 重试当前请求
          error.config.headers['Authorization'] = `Bearer ${newToken}`
          const retryResponse = await service(error.config)

          // 重试队列中的请求
          retryRequestsQueue(newToken)

          return retryResponse
        } catch (refreshError) {
          isRefreshing = false
          requestsQueue = []
          return errorHandler.handleResponseError(refreshError)
        }
      } else {
        // 正在刷新，加入队列
        return addRequestToQueue(error.config)
      }
    }

    // 使用统一错误处理器
    return errorHandler.handleResponseError(error)
  }
)

export default service
