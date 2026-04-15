import { describe, it, expect, beforeEach, vi } from 'vitest'

// Hoisted shared state — these are evaluated BEFORE vi.mock factories run,
// so the mock factories can safely close over them.
const captured = vi.hoisted(() => ({
  requestOnFulfilled: null,
  requestOnRejected: null,
  responseOnFulfilled: null,
  responseOnRejected: null
}))

const mockUserStore = vi.hoisted(() => ({
  token: 'test-token',
  logout: () => {}
}))

const mockErrorHandler = vi.hoisted(() => ({
  handleBusinessCode: null, // assigned in beforeEach via vi.fn()
  handleResponseError: null
}))

// Mock axios BEFORE importing request.js so the interceptors are captured.
vi.mock('axios', () => {
  const isCancel = vi.fn(() => false)
  const create = vi.fn(() => {
    const instance = function (config) {
      // When request.js calls service(config) for retry, return a resolved promise
      return Promise.resolve({ retried: true, config })
    }
    instance.interceptors = {
      request: {
        use: vi.fn((onFulfilled, onRejected) => {
          captured.requestOnFulfilled = onFulfilled
          captured.requestOnRejected = onRejected
        })
      },
      response: {
        use: vi.fn((onFulfilled, onRejected) => {
          captured.responseOnFulfilled = onFulfilled
          captured.responseOnRejected = onRejected
        })
      }
    }
    return instance
  })
  const CancelToken = function (executor) {
    if (typeof executor === 'function') {
      executor(() => {})
    }
  }
  CancelToken.source = () => ({ token: 'cancel-token', cancel: () => {} })

  const axiosDefault = { create, isCancel, CancelToken }
  return { default: axiosDefault, isCancel, CancelToken }
})

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockUserStore
}))

vi.mock('@/router', () => ({
  default: { push: vi.fn() }
}))

// request.js imports errorHandler as named { errorHandler }
vi.mock('@/utils/errorHandler', () => ({
  errorHandler: mockErrorHandler,
  default: mockErrorHandler
}))

// Avoid pulling actual UI library in tests
vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), warning: vi.fn(), success: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) }
}))

// Importing request.js triggers axios.create + interceptor registration
import '@/utils/request'
import axios from 'axios'

describe('request.js interceptors', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUserStore.token = 'test-token'
    mockErrorHandler.handleBusinessCode = vi.fn()
    mockErrorHandler.handleResponseError = vi.fn()
    axios.isCancel.mockImplementation(() => false)
  })

  // --- Request interceptor ---

  it('requestInterceptor_withToken_shouldAddAuthHeader', async () => {
    const config = { headers: {} }
    const result = await captured.requestOnFulfilled(config)
    expect(result.headers['Authorization']).toBe('Bearer test-token')
  })

  it('requestInterceptor_withoutToken_shouldNotAddAuthHeader', async () => {
    mockUserStore.token = ''
    const config = { headers: {} }
    const result = await captured.requestOnFulfilled(config)
    expect(result.headers['Authorization']).toBeUndefined()
  })

  // --- Response interceptor (success path) ---

  it('responseInterceptor_success_shouldDelegateToBusinessHandler', async () => {
    mockErrorHandler.handleBusinessCode.mockImplementation((res) => Promise.resolve(res))
    const response = {
      data: { code: 200, message: 'ok', data: { id: 1 } },
      config: {}
    }
    const result = await captured.responseOnFulfilled(response)
    expect(mockErrorHandler.handleBusinessCode).toHaveBeenCalledWith(response.data)
    expect(result).toEqual({ code: 200, message: 'ok', data: { id: 1 } })
  })

  it('responseInterceptor_businessError_shouldCallHandler', async () => {
    mockErrorHandler.handleBusinessCode.mockImplementation((res) =>
      Promise.reject({ type: 'business', code: res.code, message: res.message })
    )
    const response = {
      data: { code: 5002, message: '图书不存在' },
      config: {}
    }
    await expect(captured.responseOnFulfilled(response)).rejects.toMatchObject({
      type: 'business',
      code: 5002
    })
    expect(mockErrorHandler.handleBusinessCode).toHaveBeenCalledWith(response.data)
  })

  it('responseInterceptor_businessCode401_shouldTriggerRefreshAndRetry', async () => {
    const config = { headers: {}, _retry: false }
    const response = {
      data: { code: 401, message: 'unauthorized' },
      config
    }
    const result = await captured.responseOnFulfilled(response)
    expect(config._retry).toBe(true)
    expect(config.headers['Authorization']).toBe('Bearer test-token')
    expect(result).toEqual({ retried: true, config })
    expect(mockErrorHandler.handleBusinessCode).not.toHaveBeenCalled()
  })

  it('responseInterceptor_readerServicePage_shouldWrapToStandardFormat', async () => {
    const response = {
      data: {
        records: [{ id: 1 }, { id: 2 }],
        total: 25,
        size: 10,
        current: 2,
        pages: 3
      },
      config: {}
    }
    const result = await captured.responseOnFulfilled(response)
    expect(result).toMatchObject({
      code: 200,
      message: '操作成功',
      success: true,
      data: {
        records: [{ id: 1 }, { id: 2 }],
        total: 25,
        pageNum: 2,
        pageSize: 10,
        pages: 3
      }
    })
  })

  it('responseInterceptor_unknownFormat_shouldReturnRawData', async () => {
    const response = { data: { foo: 'bar' }, config: {} }
    const result = await captured.responseOnFulfilled(response)
    expect(result).toEqual({ foo: 'bar' })
  })

  // --- Response interceptor (error path) ---

  it('responseInterceptor_httpError_shouldCallHandler', async () => {
    mockErrorHandler.handleResponseError.mockImplementation((err) =>
      Promise.reject({ type: 'system', message: err.message || 'failed' })
    )
    const error = {
      response: { status: 500, data: { message: 'server error' } },
      config: { headers: {} },
      message: 'Request failed'
    }
    await expect(captured.responseOnRejected(error)).rejects.toMatchObject({ type: 'system' })
    expect(mockErrorHandler.handleResponseError).toHaveBeenCalledWith(error)
  })

  it('responseInterceptor_cancelled_shouldRejectSilently', async () => {
    axios.isCancel.mockImplementation(() => true)
    const cancelError = { message: 'canceled' }
    await expect(captured.responseOnRejected(cancelError)).rejects.toBe(cancelError)
    expect(mockErrorHandler.handleResponseError).not.toHaveBeenCalled()
  })

  it('responseInterceptor_timeout_shouldDelegateToHandler', async () => {
    mockErrorHandler.handleResponseError.mockImplementation(() =>
      Promise.reject({ type: 'timeout', message: 'timeout' })
    )
    const error = {
      request: {},
      code: 'ECONNABORTED',
      message: 'timeout of 15000ms exceeded',
      config: { headers: {} }
    }
    await expect(captured.responseOnRejected(error)).rejects.toMatchObject({ type: 'timeout' })
    expect(mockErrorHandler.handleResponseError).toHaveBeenCalledWith(error)
  })

  it('responseInterceptor_networkError_shouldDelegateToHandler', async () => {
    mockErrorHandler.handleResponseError.mockImplementation(() =>
      Promise.reject({ type: 'network', message: 'Network Error' })
    )
    const error = {
      request: {},
      message: 'Network Error',
      config: { headers: {} }
    }
    await expect(captured.responseOnRejected(error)).rejects.toMatchObject({ type: 'network' })
    expect(mockErrorHandler.handleResponseError).toHaveBeenCalledWith(error)
  })

  it('responseInterceptor_http401_shouldTriggerRefreshAndRetry', async () => {
    const config = { headers: {}, _retry: false }
    const error = {
      response: { status: 401, data: {} },
      config,
      message: 'Unauthorized'
    }
    const result = await captured.responseOnRejected(error)
    expect(config._retry).toBe(true)
    expect(config.headers['Authorization']).toBe('Bearer test-token')
    expect(result).toEqual({ retried: true, config })
    expect(mockErrorHandler.handleResponseError).not.toHaveBeenCalled()
  })
})
