import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

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

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()

    // 添加 token
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }

    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 兼容两种响应格式：
    // 1. 标准格式: { code, message, data, success, timestamp }
    // 2. 直接格式: { records, total, size/pageSize, current/pageNum, pages }

    // 如果响应中有 code 字段，使用标准格式处理
    if ('code' in res) {
      // 根据后端返回的code判断
      if (res.code !== 200) {
        ElMessage.error(res.message || '请求失败')

        // Token 失效
        if (res.code === 401) {
          ElMessageBox.confirm('登录已过期,请重新登录', '系统提示', {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            const userStore = useUserStore()
            userStore.logout()
            router.push({ name: 'Login' })
          })
        }

        return Promise.reject(new Error(res.message || 'Error'))
      }

      // 返回完整的响应对象,保留code和message
      return res
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
  (error) => {
    console.error('Response error:', error)

    let message = '请求失败'

    if (error.response) {
      switch (error.response.status) {
        case 400:
          message = '请求参数错误'
          break
        case 401:
          message = '未授权,请重新登录'
          const userStore = useUserStore()
          userStore.logout()
          router.push({ name: 'Login' })
          break
        case 403:
          message = '权限不足'
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 500:
          message = '服务器错误'
          break
        case 503:
          message = '服务暂时不可用'
          break
        default:
          message = error.response.data?.message || '请求失败'
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时'
    } else if (error.message === 'Network Error') {
      message = '网络连接失败'
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
