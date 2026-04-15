import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} }))
}))

import request from '@/utils/request'
import { login, register, logout, getUserInfo, refreshToken } from '@/api/auth'

describe('api/auth', () => {
  beforeEach(() => vi.clearAllMocks())

  it('login_shouldPostToAuthLogin', () => {
    const data = { username: 'admin', password: '123456' }
    login(data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/auth/login',
      method: 'post',
      data
    })
  })

  it('register_shouldPostToAuthRegister', () => {
    const data = { username: 'newuser', password: '123456', email: 'user@example.com' }
    register(data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/auth/register',
      method: 'post',
      data
    })
  })

  it('logout_shouldPostToAuthLogout', () => {
    logout()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/auth/logout',
      method: 'post'
    })
  })

  it('getUserInfo_shouldGetAuthInfo', () => {
    getUserInfo()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/auth/info',
      method: 'get'
    })
  })

  it('refreshToken_shouldPostToAuthRefresh', () => {
    refreshToken()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/auth/refresh',
      method: 'post'
    })
  })
})
