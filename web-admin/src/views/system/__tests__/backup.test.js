import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import ElementPlus from 'element-plus'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: '/system/backup' })
}))

import Backup from '@/views/system/backup.vue'

describe('views/system/backup', () => {
  it('mount_shouldRenderWithoutError', () => {
    const wrapper = mount(Backup, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: {
          'router-link': true,
          'el-icon': true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
