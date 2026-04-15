import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import PageLoading from '@/components/Loading/PageLoading.vue'

describe('PageLoading', () => {
  const factory = (props = {}) => mount(PageLoading, { props, global: { plugins: [ElementPlus] } })

  it('renders_fullscreen_shouldApplyFullscreenClass', () => {
    const wrapper = factory({ fullscreen: true })
    expect(wrapper.find('.page-loading').classes()).toContain('fullscreen')
  })

  it('renders_withMask_shouldShowMask', () => {
    const wrapper = factory({ mask: true })
    expect(wrapper.find('.page-loading-mask').exists()).toBe(true)
  })

  it('renders_withCustomText_shouldDisplayText', () => {
    const customText = 'Loading data...'
    const wrapper = factory({ text: customText })
    expect(wrapper.find('.page-loading-text').text()).toBe(customText)
  })

  it('renders_withCustomIcon_shouldRenderIcon', () => {
    // Define a simple test icon component
    const TestIcon = {
      name: 'TestIcon',
      template: '<span class="test-icon">Icon</span>'
    }
    const wrapper = factory({ icon: TestIcon })
    expect(wrapper.find('.page-loading-icon').exists()).toBe(true)
    expect(wrapper.find('.test-icon').exists()).toBe(true)
  })
})
