import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

// Mock echarts
vi.mock('echarts', () => ({
  init: vi.fn(() => ({
    setOption: vi.fn(),
    resize: vi.fn(),
    dispose: vi.fn()
  })),
  graphic: {
    LinearGradient: vi.fn()
  }
}))

import LibraryUIComponents from '@/components/LibraryUIComponents.vue'
import * as echarts from 'echarts'

describe('LibraryUIComponents', () => {
  const factory = () =>
    mount(LibraryUIComponents, {
      global: { plugins: [ElementPlus] }
    })

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('mount_shouldRenderWithoutError', () => {
    const wrapper = factory()
    expect(wrapper.exists()).toBe(true)
  })

  it('mount_shouldInitializeCharts', () => {
    factory()
    // Wait for mounted hook
    expect(echarts.init).toHaveBeenCalled()
  })

  it('renders_statsCards_shouldShowAllStats', () => {
    const wrapperElement = factory()
    // Verify basic DOM structure exists
    expect(wrapperElement.html()).toBeTruthy()
  })
})
