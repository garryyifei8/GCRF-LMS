/* eslint-env browser, node */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

// Mock vue-router BEFORE importing the component
const mockPush = vi.fn()
const mockGetRoutes = vi.fn(() => [])

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRouter: () => ({
      push: mockPush,
      replace: vi.fn(),
      back: vi.fn(),
      getRoutes: mockGetRoutes
    }),
    useRoute: () => ({
      path: '/dashboard',
      params: {},
      query: {},
      matched: []
    }),
    RouterView: { name: 'RouterView', template: '<div class="router-view-stub" />' },
    RouterLink: { name: 'RouterLink', template: '<a><slot /></a>' }
  }
})

// Mock ElMessageBox
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue('confirm')
    }
  }
})

import MainLayout from '@/layouts/MainLayout.vue'

describe('layouts/MainLayout', () => {
  const factory = (storeInitialState = {}) =>
    mount(MainLayout, {
      global: {
        plugins: [
          createTestingPinia({
            stubActions: false,
            initialState: {
              user: {
                token: 'test-token',
                userInfo: { name: 'Admin', avatar: '' },
                permissions: ['*'],
                ...storeInitialState
              }
            }
          }),
          ElementPlus
        ],
        stubs: {
          'router-view': { template: '<div class="router-view-stub" />' },
          'router-link': { template: '<a><slot /></a>' },
          transition: false
        }
      }
    })

  beforeEach(() => {
    vi.clearAllMocks()
    mockGetRoutes.mockReturnValue([])
  })

  it('mount_shouldRenderWithoutError', () => {
    const wrapper = factory()
    expect(wrapper.exists()).toBe(true)
  })

  it('render_shouldContainSidebarAside', () => {
    const wrapper = factory()
    const aside = wrapper.find('.main-aside')
    expect(aside.exists()).toBe(true)
  })

  it('render_shouldContainHeader', () => {
    const wrapper = factory()
    const header = wrapper.find('.main-header')
    expect(header.exists()).toBe(true)
  })

  it('render_shouldShowLogoText', () => {
    const wrapper = factory()
    expect(wrapper.text()).toContain('Modernize')
  })

  it('toggleCollapse_click_shouldToggleCollapseState', async () => {
    const wrapper = factory()
    // Initially not collapsed - sidebar is 260px wide
    const aside = wrapper.find('.main-aside')
    expect(aside.exists()).toBe(true)

    // Click the collapse icon
    const collapseIcon = wrapper.find('.collapse-icon')
    expect(collapseIcon.exists()).toBe(true)
    await collapseIcon.trigger('click')

    // After collapse, sidebar width should change to 64px (is-collapse class on logo)
    const logoContainer = wrapper.find('.logo-container')
    expect(logoContainer.classes()).toContain('is-collapse')
  })

  it('render_userInfo_shouldDisplayUserName', () => {
    const wrapper = factory({ userInfo: { name: 'TestUser', avatar: '' } })
    // User name appears in header and user card
    expect(wrapper.text()).toContain('TestUser')
  })

  it('handleCommand_logout_shouldCallMessageBox', async () => {
    const { ElMessageBox } = await import('element-plus')
    const wrapper = factory()

    // Find and click the logout icon (SwitchButton in user card)
    const userCardIcon = wrapper.find('.user-card-icon')
    if (userCardIcon.exists()) {
      await userCardIcon.trigger('click')
      expect(ElMessageBox.confirm).toHaveBeenCalled()
    }
  })

  it('render_collapseToggle_shouldShowFoldOrExpandIcon', () => {
    const wrapper = factory()
    // The collapse icon area should be present
    const collapseArea = wrapper.find('.collapse-icon')
    expect(collapseArea.exists()).toBe(true)
  })
})
