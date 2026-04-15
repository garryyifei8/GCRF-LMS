import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/readers/card' })
}))

import CardView from '@/views/readers/card.vue'

describe('views/readers/card', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(CardView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: {
          'router-link': true,
          AvatarUpload: true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
