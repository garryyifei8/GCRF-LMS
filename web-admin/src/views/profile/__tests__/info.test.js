import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/' })
}))

import Info from '@/views/profile/info.vue'

describe('views/profile/info', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(Info, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
