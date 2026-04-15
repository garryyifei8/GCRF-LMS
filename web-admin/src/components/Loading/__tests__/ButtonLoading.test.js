import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ButtonLoading from '@/components/Loading/ButtonLoading.vue'

describe('ButtonLoading', () => {
  const factory = (props = {}) =>
    mount(ButtonLoading, { props, global: { plugins: [ElementPlus] } })

  it('renders_withDefaultSize_shouldApplyDefaultClass', () => {
    const wrapper = factory()
    const root = wrapper.find('.button-loading')

    expect(root.exists()).toBe(true)
    expect(root.classes()).not.toContain('small')
    expect(root.classes()).not.toContain('large')
  })

  it('renders_withLargeSize_shouldApplyLargeClass', () => {
    const wrapper = factory({ size: 'large' })
    const root = wrapper.find('.button-loading')

    expect(root.exists()).toBe(true)
    expect(root.classes()).toContain('large')
    expect(root.classes()).not.toContain('small')
  })

  it('renders_withText_shouldShowText', () => {
    const testText = 'Loading...'
    const wrapper = factory({ text: testText })
    const textElement = wrapper.find('.button-loading-text')

    expect(textElement.exists()).toBe(true)
    expect(textElement.text()).toBe(testText)
  })
})
