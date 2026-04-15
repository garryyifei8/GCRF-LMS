import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), go: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/' })
}))

import NotFound from '@/views/error/404.vue'

describe('views/error/404', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(NotFound, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
