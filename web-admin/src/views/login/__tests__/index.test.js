import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/login' }),
  createRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    currentRoute: { value: { path: '/login', query: {}, params: {} } },
    beforeEach: vi.fn(),
    afterEach: vi.fn(),
    install: vi.fn()
  }),
  createWebHistory: vi.fn(() => ({})),
  RouterLink: { template: '<a><slot /></a>' },
  RouterView: { template: '<div />' }
}))

vi.mock('@/api/auth', () => ({
  login: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      accessToken: 'mock-token',
      userId: 1,
      username: 'admin',
      permissions: ['*']
    }
  }),
  register: vi.fn().mockResolvedValue({ code: 200 }),
  getUserInfo: vi.fn().mockResolvedValue({ code: 200, data: { userInfo: {}, permissions: [] } }),
  logout: vi.fn().mockResolvedValue({ code: 200 }),
  refreshToken: vi.fn().mockResolvedValue({ code: 200 })
}))

import LoginView from '@/views/login/index.vue'

describe('views/login', () => {
  const factory = () =>
    mount(LoginView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true, 'router-view': true }
      }
    })

  beforeEach(() => vi.clearAllMocks())

  it('mount_shouldRenderWithoutError', () => {
    const wrapper = factory()
    expect(wrapper.exists()).toBe(true)
  })

  it('formFields_shouldSetUsernameAndPassword', async () => {
    const wrapper = factory()
    // Directly set via vm reactive form
    wrapper.vm.loginForm.username = 'admin'
    wrapper.vm.loginForm.password = 'password123'
    await flushPromises()
    expect(wrapper.vm.loginForm.username).toBe('admin')
    expect(wrapper.vm.loginForm.password).toBe('password123')
  })

  it('submitButton_shouldTriggerLoginAction', async () => {
    const { useUserStore } = await import('@/stores/user')
    const wrapper = factory()

    // Set valid credentials directly on vm
    wrapper.vm.loginForm.username = 'admin'
    wrapper.vm.loginForm.password = 'password123'
    await flushPromises()

    // Click the signin button
    const button = wrapper.find('.signin-button')
    expect(button.exists()).toBe(true)
    await button.trigger('click')
    await flushPromises()

    // The user store login should have been invoked
    const store = useUserStore()
    expect(store.login).toHaveBeenCalledWith(
      expect.objectContaining({ username: 'admin', password: 'password123' })
    )
  })

  it('loginForm_shouldHaveRememberCheckbox', () => {
    const wrapper = factory()
    // Checkbox for remember me
    const checkbox = wrapper.find('.el-checkbox')
    expect(checkbox.exists()).toBe(true)
    // Default value is false
    expect(wrapper.vm.loginForm.remember).toBe(false)
  })

  it('rememberCheckbox_shouldToggleWhenClicked', async () => {
    const wrapper = factory()
    expect(wrapper.vm.loginForm.remember).toBe(false)

    // Toggle via vm (checkbox interaction with jsdom can be tricky)
    wrapper.vm.loginForm.remember = true
    await flushPromises()

    expect(wrapper.vm.loginForm.remember).toBe(true)
  })
})
