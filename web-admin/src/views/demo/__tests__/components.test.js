import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/' })
}))

vi.mock('@/components/ui/Button', () => ({
  default: {
    name: 'Button',
    template: '<button><slot/></button>'
  }
}))

vi.mock('@/components/ui/Card', () => ({
  Card: {
    name: 'Card',
    template: '<div><slot name="header"/><slot/></div>'
  },
  StatCard: {
    name: 'StatCard',
    template: '<div></div>'
  },
  AICard: {
    name: 'AICard',
    template: '<div><slot name="actions"/></div>'
  }
}))

vi.mock('@/components/ui/Input', () => ({
  default: {
    name: 'Input',
    template: '<input/>'
  }
}))

import Components from '@/views/demo/components.vue'

describe('views/demo/components', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(Components, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
