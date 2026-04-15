import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createTestingPinia } from '@pinia/testing'
import { setActivePinia } from 'pinia'

// Mock API module (hoisted)
vi.mock('@/api/system', () => ({
  getAllRoles: vi.fn(),
  getPermissionTree: vi.fn()
}))

import { useSystemStore } from '@/stores/system'
import * as systemApi from '@/api/system'

describe('useSystemStore', () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia({ stubActions: false }))
    vi.clearAllMocks()
    if (typeof localStorage !== 'undefined' && typeof localStorage.clear === 'function') {
      localStorage.clear()
    }
  })

  it('loadRoles_shouldFetchFromApiAndCache', async () => {
    systemApi.getAllRoles.mockResolvedValue({
      data: [
        { code: 'ADMIN', name: '管理员' },
        { code: 'USER', name: '普通用户' }
      ]
    })

    const store = useSystemStore()
    const result = await store.loadRoles()

    expect(systemApi.getAllRoles).toHaveBeenCalledOnce()
    expect(result).toHaveLength(2)
    expect(store.roles[0].code).toBe('ADMIN')
    expect(store.rolesLoadedAt).toBeTruthy()
    expect(store.isRolesValid).toBe(true)
  })

  it('loadRoles_shouldUseCacheOnSecondCall', async () => {
    systemApi.getAllRoles.mockResolvedValue({
      data: [{ code: 'ADMIN', name: '管理员' }]
    })

    const store = useSystemStore()
    await store.loadRoles()
    await store.loadRoles()

    expect(systemApi.getAllRoles).toHaveBeenCalledOnce()
  })

  it('loadRoles_withForceTrue_shouldBypassCache', async () => {
    systemApi.getAllRoles.mockResolvedValue({
      data: [{ code: 'ADMIN', name: '管理员' }]
    })

    const store = useSystemStore()
    await store.loadRoles()
    await store.loadRoles(true)

    expect(systemApi.getAllRoles).toHaveBeenCalledTimes(2)
  })

  it('loadPermissionTree_shouldFetchFromApiAndCache', async () => {
    systemApi.getPermissionTree.mockResolvedValue({
      data: [
        { id: 1, name: '系统管理', children: [] },
        { id: 2, name: '图书管理', children: [] }
      ]
    })

    const store = useSystemStore()
    const result = await store.loadPermissionTree()

    expect(systemApi.getPermissionTree).toHaveBeenCalledOnce()
    expect(result).toHaveLength(2)
    expect(store.permissionTree[0].name).toBe('系统管理')
    expect(store.isPermissionTreeValid).toBe(true)
  })

  it('loadPermissionTree_apiError_shouldThrowAndKeepLoadingFalse', async () => {
    systemApi.getPermissionTree.mockRejectedValue(new Error('Network error'))

    const store = useSystemStore()
    await expect(store.loadPermissionTree()).rejects.toThrow('Network error')

    expect(store.permissionTree).toEqual([])
    expect(store.permissionTreeLoading).toBe(false)
  })

  it('roleOptions_shouldMapRolesToValueLabel', async () => {
    systemApi.getAllRoles.mockResolvedValue({
      data: [
        { code: 'ADMIN', name: '管理员' },
        { code: 'USER', name: '普通用户' }
      ]
    })

    const store = useSystemStore()
    await store.loadRoles()

    expect(store.roleOptions).toEqual([
      { value: 'ADMIN', label: '管理员' },
      { value: 'USER', label: '普通用户' }
    ])
    expect(store.getRoleName('ADMIN')).toBe('管理员')
    expect(store.getRoleName('UNKNOWN')).toBe('UNKNOWN')
  })

  it('getBorrowRuleByType_shouldReturnRulesForType', () => {
    const store = useSystemStore()

    expect(store.getBorrowRuleByType('student').maxBorrowCount).toBe(5)
    expect(store.getBorrowRuleByType('teacher').borrowDays).toBe(60)
    // Falls back to public for unknown type
    expect(store.getBorrowRuleByType('unknown').maxBorrowCount).toBe(3)
  })

  it('toggleTheme_shouldFlipBetweenLightAndDark', () => {
    const store = useSystemStore()

    expect(store.config.theme).toBe('light')
    expect(store.isDark).toBe(false)

    store.toggleTheme()
    expect(store.config.theme).toBe('dark')
    expect(store.isDark).toBe(true)

    store.toggleTheme()
    expect(store.config.theme).toBe('light')
  })

  it('updateConfig_shouldMergeUpdatesIntoConfig', () => {
    const store = useSystemStore()

    store.updateConfig({ pageSize: 50, theme: 'dark' })

    expect(store.config.pageSize).toBe(50)
    expect(store.config.theme).toBe('dark')
    // Other fields preserved
    expect(store.config.systemShortName).toBe('GCRF图书馆')
  })

  it('reset_shouldClearAllStateAndCaches', async () => {
    systemApi.getAllRoles.mockResolvedValue({
      data: [{ code: 'ADMIN', name: '管理员' }]
    })
    systemApi.getPermissionTree.mockResolvedValue({
      data: [{ id: 1, name: '系统管理' }]
    })

    const store = useSystemStore()
    await store.loadRoles()
    await store.loadPermissionTree()
    store.updateConfig({ theme: 'dark', pageSize: 100 })
    store.updateBorrowRules('student', { maxBorrowCount: 99 })

    store.reset()

    expect(store.roles).toEqual([])
    expect(store.rolesLoadedAt).toBeNull()
    expect(store.permissionTree).toEqual([])
    expect(store.permissionTreeLoadedAt).toBeNull()
    expect(store.config.theme).toBe('light')
    expect(store.config.pageSize).toBe(20)
    expect(store.borrowRules.student.maxBorrowCount).toBe(5)
  })
})
