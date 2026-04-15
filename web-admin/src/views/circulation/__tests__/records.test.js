import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'
import RecordsView from '@/views/circulation/records.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/circulation/records' })
}))

vi.mock('@/api/circulation', () => ({
  getCirculationRecords: vi.fn()
}))

describe('views/circulation/records', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(RecordsView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
