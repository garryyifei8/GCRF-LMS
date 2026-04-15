import { describe, it, expect, beforeEach, vi } from 'vitest'

// Mock element-plus (hoisted)
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn(() => Promise.resolve())
  }
}))

// Mock user store
const mockLogout = vi.fn()
vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    logout: mockLogout
  })
}))

// Mock router (default export)
vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
    currentRoute: { value: { fullPath: '/dashboard' } }
  }
}))

import {
  errorHandler,
  ErrorType,
  globalErrorHandler,
  unhandledRejectionHandler
} from '@/utils/errorHandler'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

describe('errorHandler', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Default: confirm resolves
    ElMessageBox.confirm.mockImplementation(() => Promise.resolve())
  })

  it('ErrorType_shouldExportAllEnumValues', () => {
    expect(ErrorType.BUSINESS).toBe('business')
    expect(ErrorType.NETWORK).toBe('network')
    expect(ErrorType.AUTH).toBe('auth')
    expect(ErrorType.PERMISSION).toBe('permission')
    expect(ErrorType.VALIDATION).toBe('validation')
    expect(ErrorType.SYSTEM).toBe('system')
    expect(ErrorType.TIMEOUT).toBe('timeout')
    expect(ErrorType.UNKNOWN).toBe('unknown')
    expect(Object.keys(ErrorType)).toHaveLength(8)
  })

  it('handleAuthError_shouldShowMessageBoxAndLogout', async () => {
    errorHandler.handleAuthError('登录已过期')
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '登录已过期',
      '登录过期',
      expect.objectContaining({ confirmButtonText: '重新登录' })
    )
    // Wait microtask for .then()
    await Promise.resolve()
    await Promise.resolve()
    expect(mockLogout).toHaveBeenCalled()
    expect(router.push).toHaveBeenCalledWith(expect.objectContaining({ name: 'Login' }))
  })

  it('handlePermissionError_shouldShowErrorAndRedirect', () => {
    vi.useFakeTimers()
    errorHandler.handlePermissionError('权限不足')
    expect(ElMessage.error).toHaveBeenCalledWith(expect.objectContaining({ message: '权限不足' }))
    vi.advanceTimersByTime(2000)
    expect(router.push).toHaveBeenCalledWith({ name: 'Home' })
    vi.useRealTimers()
  })

  it('handleValidationError_withErrors_shouldShowList', () => {
    const data = {
      errors: [{ message: '用户名不能为空' }, { message: '密码长度不足' }]
    }
    errorHandler.handleValidationError('验证失败', data)
    expect(ElMessage.error).toHaveBeenCalledWith(
      expect.objectContaining({
        message: '用户名不能为空\n密码长度不足'
      })
    )
  })

  it('handleValidationError_withoutErrors_shouldShowGeneric', () => {
    errorHandler.handleValidationError('参数错误', {})
    expect(ElMessage.error).toHaveBeenCalledWith(expect.objectContaining({ message: '参数错误' }))
  })

  it('handleBusinessError_shouldShowMessage', () => {
    errorHandler.handleBusinessError('业务出错')
    expect(ElMessage.error).toHaveBeenCalledWith(expect.objectContaining({ message: '业务出错' }))
  })

  it('handleSystemError_shouldSuggestRetry', () => {
    errorHandler.handleSystemError('服务器内部错误')
    expect(ElMessage.error).toHaveBeenCalledWith(
      expect.objectContaining({
        message: expect.stringContaining('请稍后重试或联系管理员')
      })
    )
  })

  it('handleTimeoutError_shouldShowWarning', async () => {
    const promise = errorHandler.handleTimeoutError('请求超时')
    expect(ElMessage.warning).toHaveBeenCalledWith(expect.objectContaining({ message: '请求超时' }))
    await expect(promise).rejects.toMatchObject({
      type: ErrorType.TIMEOUT,
      message: '请求超时'
    })
  })

  it('handleNetworkError_shouldSuggestCheck', async () => {
    const promise = errorHandler.handleNetworkError('网络异常')
    expect(ElMessage.error).toHaveBeenCalledWith(
      expect.objectContaining({
        message: expect.stringContaining('请检查网络连接后重试')
      })
    )
    await expect(promise).rejects.toMatchObject({
      type: ErrorType.NETWORK
    })
  })

  it('handleResponseError_status401_shouldDelegateToAuthHandler', async () => {
    const spy = vi.spyOn(errorHandler, 'handleAuthError')
    const error = {
      response: {
        status: 401,
        data: { message: '未授权' }
      }
    }
    const promise = errorHandler.handleResponseError(error)
    expect(spy).toHaveBeenCalledWith('未授权')
    await expect(promise).rejects.toMatchObject({
      type: ErrorType.AUTH,
      status: 401
    })
    spy.mockRestore()
  })

  it('handleBusinessCode_code1001_shouldMapToValidationMessage', async () => {
    const response = { code: 1001, message: 'ignored' }
    const promise = errorHandler.handleBusinessCode(response)
    expect(ElMessage.error).toHaveBeenCalledWith(
      expect.objectContaining({ message: '参数验证失败' })
    )
    await expect(promise).rejects.toMatchObject({
      type: ErrorType.BUSINESS,
      code: 1001,
      message: '参数验证失败'
    })
  })

  it('unhandledRejectionHandler_chunkLoadError_handledViaGlobalErrorHandler', () => {
    // ChunkLoadError is detected in globalErrorHandler (Vue errorHandler)
    const reloadSpy = vi.fn()
    Object.defineProperty(window, 'location', {
      value: { reload: reloadSpy },
      writable: true
    })
    const error = new Error('Loading chunk 1 failed')
    error.name = 'ChunkLoadError'
    globalErrorHandler(error, {}, 'render')
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      expect.stringContaining('页面资源加载失败'),
      '提示',
      expect.objectContaining({ confirmButtonText: '刷新' })
    )
  })

  it('unhandledRejectionHandler_handledTypedError_shouldNotShowMessage', () => {
    const event = {
      reason: { type: ErrorType.NETWORK, message: '网络错误' },
      preventDefault: vi.fn()
    }
    unhandledRejectionHandler(event)
    expect(ElMessage.error).not.toHaveBeenCalled()
    expect(event.preventDefault).not.toHaveBeenCalled()
  })
})
