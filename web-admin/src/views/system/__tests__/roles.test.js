import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/system/roles' })
}))

vi.mock('@/api/system', () => ({
  getRoles: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: { records: [], total: 0 }
    })
  ),
  getAllRoles: vi.fn(),
  getRoleById: vi.fn(),
  createRole: vi.fn(),
  updateRole: vi.fn(),
  deleteRole: vi.fn(),
  assignPermissions: vi.fn(),
  getRolePermissions: vi.fn(),
  getPermissionTree: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: []
    })
  ),
  getAllPermissions: vi.fn(),
  getUsers: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
  deleteUser: vi.fn(),
  resetPassword: vi.fn(),
  updateUserStatus: vi.fn(),
  batchDeleteUsers: vi.fn(),
  getUserById: vi.fn(),
  getDepartments: vi.fn(),
  getDepartmentTree: vi.fn(),
  getDepartmentById: vi.fn(),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn()
}))

import Roles from '@/views/system/roles.vue'

describe('views/system/roles', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(Roles, {
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
