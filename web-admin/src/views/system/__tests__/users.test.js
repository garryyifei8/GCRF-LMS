import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/system/users' })
}))

vi.mock('@/api/system', () => ({
  getUsers: vi.fn(() =>
    Promise.resolve({
      code: 200,
      data: { records: [], total: 0 }
    })
  ),
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
  getAllPermissions: vi.fn(),
  getDepartments: vi.fn(),
  getDepartmentTree: vi.fn(),
  getDepartmentById: vi.fn(),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn()
}))

import Users from '@/views/system/users.vue'

describe('views/system/users', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(Users, {
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
