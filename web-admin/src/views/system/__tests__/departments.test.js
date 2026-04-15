import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/system/departments' })
}))

vi.mock('@/api/system', () => ({
  getDepartments: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: { records: [], total: 0 }
    })
  ),
  getDepartmentTree: vi.fn(),
  getDepartmentById: vi.fn(),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn(),
  getUsers: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
  deleteUser: vi.fn(),
  resetPassword: vi.fn(),
  updateUserStatus: vi.fn(),
  batchDeleteUsers: vi.fn(),
  getUserById: vi.fn(),
  getRoles: vi.fn(),
  getAllRoles: vi.fn(),
  getRoleById: vi.fn(),
  createRole: vi.fn(),
  updateRole: vi.fn(),
  deleteRole: vi.fn(),
  assignPermissions: vi.fn(),
  getRolePermissions: vi.fn(),
  getPermissionTree: vi.fn(),
  getAllPermissions: vi.fn()
}))

vi.mock('@/api/department', () => ({
  getDepartmentList: vi.fn(),
  getDepartmentById: vi.fn(),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn()
}))

import Departments from '@/views/system/departments.vue'

describe('views/system/departments', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(Departments, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: {
          'router-link': true,
          'el-icon': true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
