import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'
import ReturnView from '@/views/circulation/return.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/circulation/return' })
}))

vi.mock('@/api/circulation', () => ({
  getBorrowRecordByBarcode: vi.fn(),
  returnBook: vi.fn()
}))

describe('views/circulation/return', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(ReturnView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
