import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createTestingPinia } from '@pinia/testing'
import { setActivePinia } from 'pinia'

// Mock API module (hoisted)
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  getUserInfo: vi.fn()
}))

// Mock router module (used by logout via router.push)
vi.mock('@/router', () => ({
  default: { push: vi.fn() }
}))

import { useUserStore } from '@/stores/user'
import * as authApi from '@/api/auth'
import router from '@/router'

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia({ stubActions: false }))
    vi.clearAllMocks()
    if (typeof localStorage !== 'undefined' && typeof localStorage.clear === 'function') {
      localStorage.clear()
    }
  })

  it('login_success_shouldSetTokenUserInfoPermissions', async () => {
    authApi.login.mockResolvedValue({
      data: {
        accessToken: 'abc123',
        userId: 1,
        username: 'admin',
        realName: '张三',
        userType: 'ADMIN',
        permissions: ['user:read', 'user:write']
      }
    })

    const store = useUserStore()
    await store.login({ username: 'admin', password: '123' })

    expect(store.token).toBe('abc123')
    expect(store.userInfo.username).toBe('admin')
    expect(store.userInfo.name).toBe('张三')
    expect(store.userInfo.role).toBe('ADMIN')
    expect(store.permissions).toContain('user:read')
    expect(store.permissions).toContain('user:write')
  })

  it('login_apiError_shouldThrowAndNotSetToken', async () => {
    authApi.login.mockRejectedValue(new Error('Invalid credentials'))

    const store = useUserStore()
    await expect(store.login({ username: 'admin', password: 'wrong' })).rejects.toThrow(
      'Invalid credentials'
    )

    expect(store.token).toBe('')
    expect(store.userInfo).toEqual({})
    expect(store.permissions).toEqual([])
  })

  it('logout_shouldClearAllStateAndCallApi', async () => {
    authApi.logout.mockResolvedValue({ data: null })

    const store = useUserStore()
    store.setToken('xyz')
    store.setUserInfo({ username: 'admin' })
    store.setPermissions(['user:read'])

    await store.logout()

    expect(authApi.logout).toHaveBeenCalledOnce()
    expect(store.token).toBe('')
    expect(store.userInfo).toEqual({})
    expect(store.permissions).toEqual([])
    expect(router.push).toHaveBeenCalledWith({ name: 'Login' })
  })

  it('logout_apiError_shouldStillClearState', async () => {
    authApi.logout.mockRejectedValue(new Error('Network error'))

    const store = useUserStore()
    store.setToken('xyz')
    store.setUserInfo({ username: 'admin' })
    store.setPermissions(['user:read'])

    await store.logout()

    expect(store.token).toBe('')
    expect(store.userInfo).toEqual({})
    expect(store.permissions).toEqual([])
    expect(router.push).toHaveBeenCalledWith({ name: 'Login' })
  })

  it('hasPermission_withWildcard_shouldReturnTrue', () => {
    const store = useUserStore()
    store.setPermissions(['*'])

    expect(store.hasPermission('any:permission')).toBe(true)
    expect(store.hasPermission('user:delete')).toBe(true)
  })

  it('hasPermission_matchingPermission_shouldReturnTrue', () => {
    const store = useUserStore()
    store.setPermissions(['user:read', 'book:write'])

    expect(store.hasPermission('user:read')).toBe(true)
    expect(store.hasPermission('book:write')).toBe(true)
  })

  it('hasPermission_nonMatchingPermission_shouldReturnFalse', () => {
    const store = useUserStore()
    store.setPermissions(['user:read'])

    expect(store.hasPermission('user:delete')).toBe(false)
    expect(store.hasPermission('book:write')).toBe(false)
  })

  it('setters_shouldUpdateState', () => {
    const store = useUserStore()

    store.setToken('token-001')
    expect(store.token).toBe('token-001')

    store.setUserInfo({ username: 'tester', name: 'Test User' })
    expect(store.userInfo.username).toBe('tester')
    expect(store.userInfo.name).toBe('Test User')

    store.setPermissions(['perm:a', 'perm:b'])
    expect(store.permissions).toEqual(['perm:a', 'perm:b'])
  })
})
