import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Card from '../Card.vue'

describe('Card', () => {
  const factory = (props = {}, slots = {}) =>
    mount(Card, {
      props,
      slots: { default: 'Content', ...slots }
    })

  it('renders_withDefaultVariant_shouldApplyDefaultClass', () => {
    const wrapper = factory()
    expect(wrapper.classes()).toContain('rounded-xl')
    expect(wrapper.classes()).toContain('transition-all')
    expect(wrapper.html()).toContain('bg-white')
    expect(wrapper.html()).toContain('border')
  })

  it('renders_withHeaderSlot_shouldShowHeader', () => {
    const wrapper = factory({}, { header: '<h1>Header</h1>' })
    expect(wrapper.html()).toContain('Header')
    expect(wrapper.find('.card-header').exists()).toBe(true)
  })

  it('renders_withFooterSlot_shouldShowFooter', () => {
    const wrapper = factory({}, { footer: '<div>Footer</div>' })
    expect(wrapper.html()).toContain('Footer')
    expect(wrapper.find('.card-footer').exists()).toBe(true)
  })

  it('click_whenClickable_shouldEmitClick', async () => {
    const wrapper = factory({ clickable: true })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('click_whenNotClickable_shouldNotEmit', async () => {
    const wrapper = factory()
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })

  // Additional tests for variant styling
  it('renders_withBorderedVariant_shouldApplyBorderedClass', () => {
    const wrapper = factory({ variant: 'bordered' })
    expect(wrapper.html()).toContain('border-2')
  })

  it('renders_withElevatedVariant_shouldApplyElevatedClass', () => {
    const wrapper = factory({ variant: 'elevated' })
    expect(wrapper.html()).toContain('shadow-sm')
  })

  it('renders_withFlatVariant_shouldNotHaveShadow', () => {
    const wrapper = factory({ variant: 'flat' })
    const html = wrapper.html()
    expect(html).not.toContain('shadow-sm')
    expect(html).toContain('bg-gray-50')
  })

  // Test padding variants
  it('renders_withSmallPadding_shouldApplySmPaddingClass', () => {
    const wrapper = factory({ padding: 'sm' })
    expect(wrapper.html()).toContain('p-4')
  })

  it('renders_withMediumPadding_shouldApplyMdPaddingClass', () => {
    const wrapper = factory({ padding: 'md' })
    expect(wrapper.html()).toContain('p-6')
  })

  it('renders_withLargePadding_shouldApplyLgPaddingClass', () => {
    const wrapper = factory({ padding: 'lg' })
    expect(wrapper.html()).toContain('p-8')
  })

  it('renders_withNoPadding_shouldApplyNoPaddingClass', () => {
    const wrapper = factory({ padding: 'none' })
    expect(wrapper.html()).toContain('p-0')
  })

  // Test hover effect
  it('renders_withHoverable_shouldApplyHoverClasses', () => {
    const wrapper = factory({ hoverable: true })
    expect(wrapper.html()).toContain('hover:shadow-lg')
    expect(wrapper.html()).toContain('hover:-translate-y-1')
  })

  // Test shadow prop
  it('renders_withoutShadow_shouldNotApplyShadow', () => {
    const wrapper = factory({ shadow: false, variant: 'default' })
    expect(wrapper.html()).not.toContain('shadow-sm')
  })

  // Test clickable styling
  it('renders_withClickable_shouldApplyCursorPointer', () => {
    const wrapper = factory({ clickable: true })
    expect(wrapper.html()).toContain('cursor-pointer')
  })

  // Test default content slot
  it('renders_withDefaultSlot_shouldDisplayContent', () => {
    const wrapper = factory({}, { default: 'Test Content' })
    expect(wrapper.html()).toContain('Test Content')
  })
})
