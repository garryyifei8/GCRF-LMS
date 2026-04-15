import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'
import ReservationsView from '@/views/circulation/reservations.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/circulation/reservations' })
}))

vi.mock('@/api/circulation', () => ({
  getReservations: vi.fn(),
  cancelReservation: vi.fn(),
  processReservation: vi.fn(),
  notifyReservation: vi.fn()
}))

describe('views/circulation/reservations', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(ReservationsView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { 'router-link': true }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
