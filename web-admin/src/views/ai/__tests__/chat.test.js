import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/' })
}))

vi.mock('@/api/chat', () => ({
  sendMessage: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
  getChatStats: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
  getHotQuestions: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  submitFeedback: vi.fn(() => Promise.resolve({ code: 200 })),
  getChatHistory: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
  refreshCache: vi.fn(() => Promise.resolve({ code: 200 }))
}))

import Chat from '@/views/ai/chat.vue'

describe('views/ai/chat', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(Chat, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
