import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} }))
}))

import request from '@/utils/request'
import * as api from '@/api/system'

describe('api/system', () => {
  beforeEach(() => vi.clearAllMocks())

  // ========================================
  // 用户管理 API tests
  // ========================================

  it('getUsers should call request with correct shape', () => {
    const params = { pageNum: 1, pageSize: 10, keyword: 'test', role: 'admin', status: 'active' }
    api.getUsers(params)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/users',
      method: 'get',
      params
    })
  })

  it('getUserById should call request with interpolated URL', () => {
    api.getUserById(123)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/users/123',
      method: 'get'
    })
  })

  it('createUser should call request with data', () => {
    const data = {
      username: 'newuser',
      password: 'pwd',
      realName: 'New User',
      role: 'user',
      phone: '123456',
      email: 'user@example.com'
    }
    api.createUser(data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/users',
      method: 'post',
      data
    })
  })

  it('updateUser should call request with id and data', () => {
    const data = { realName: 'Updated Name', phone: '654321' }
    api.updateUser(123, data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/users/123',
      method: 'put',
      data
    })
  })

  it('deleteUser should call request with interpolated URL', () => {
    api.deleteUser(123)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/users/123',
      method: 'delete'
    })
  })

  it('resetPassword should call request with newPassword data', () => {
    api.resetPassword(123, 'newpwd123')
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/users/123/password/reset',
      method: 'put',
      data: { newPassword: 'newpwd123' }
    })
  })

  it('resetPassword should use default password when not provided', () => {
    api.resetPassword(123)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/users/123/password/reset',
      method: 'put',
      data: { newPassword: '123456' }
    })
  })

  it('updateUserStatus should call request with status data', () => {
    api.updateUserStatus(123, 'inactive')
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/users/123/status',
      method: 'put',
      data: { status: 'inactive' }
    })
  })

  it('batchDeleteUsers should call request with comma-separated ids', () => {
    api.batchDeleteUsers([1, 2, 3])
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/users/batch',
      method: 'delete',
      params: {
        ids: '1,2,3'
      }
    })
  })

  // ========================================
  // 角色管理 API tests
  // ========================================

  it('getRoles should call request with params', () => {
    const params = { pageNum: 1, pageSize: 10, keyword: 'admin' }
    api.getRoles(params)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/roles',
      method: 'get',
      params
    })
  })

  it('getAllRoles should call request without params', () => {
    api.getAllRoles()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/roles/all',
      method: 'get'
    })
  })

  it('getRoleById should call request with interpolated URL', () => {
    api.getRoleById(456)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/roles/456',
      method: 'get'
    })
  })

  it('createRole should call request with data', () => {
    const data = { code: 'ADMIN', name: 'Administrator', sort: 1, remark: 'System admin' }
    api.createRole(data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/roles',
      method: 'post',
      data
    })
  })

  it('updateRole should call request with id and data', () => {
    const data = { name: 'Super Admin', sort: 0 }
    api.updateRole(456, data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/roles/456',
      method: 'put',
      data
    })
  })

  it('deleteRole should call request with interpolated URL', () => {
    api.deleteRole(456)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/roles/456',
      method: 'delete'
    })
  })

  it('assignPermissions should call request with permissions data', () => {
    const permissions = ['perm1', 'perm2', 'perm3']
    api.assignPermissions(456, permissions)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/roles/456/permissions',
      method: 'put',
      data: { permissions }
    })
  })

  it('getRolePermissions should call request with interpolated URL', () => {
    api.getRolePermissions(456)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/roles/456/permissions',
      method: 'get'
    })
  })

  // ========================================
  // 权限管理 API tests
  // ========================================

  it('getPermissionTree should call request without params', () => {
    api.getPermissionTree()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/permissions/tree',
      method: 'get'
    })
  })

  it('getAllPermissions should call request without params', () => {
    api.getAllPermissions()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/permissions',
      method: 'get'
    })
  })

  // ========================================
  // 部门管理 API tests
  // ========================================

  it('getDepartments should call request with params', () => {
    const params = { pageNum: 1, pageSize: 20 }
    api.getDepartments(params)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments',
      method: 'get',
      params
    })
  })

  it('getDepartmentTree should call request without params', () => {
    api.getDepartmentTree()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments/tree',
      method: 'get'
    })
  })

  it('getDepartmentById should call request with interpolated URL', () => {
    api.getDepartmentById(789)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments/789',
      method: 'get'
    })
  })

  it('createDepartment should call request with data', () => {
    const data = {
      deptCode: 'IT',
      deptName: 'IT Department',
      parentId: 1,
      phone: '123456',
      email: 'it@example.com',
      description: 'IT Dept'
    }
    api.createDepartment(data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments',
      method: 'post',
      data
    })
  })

  it('updateDepartment should call request with id and data', () => {
    const data = { deptName: 'HR Department', phone: '654321' }
    api.updateDepartment(789, data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments/789',
      method: 'put',
      data
    })
  })

  it('deleteDepartment should call request with interpolated URL', () => {
    api.deleteDepartment(789)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments/789',
      method: 'delete'
    })
  })
})
