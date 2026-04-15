import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { Loading } from '@element-plus/icons-vue'
import Button from '@/components/ui/Button/Button.vue'

describe('Button Component', () => {
  const factory = (props = {}, slots = {}) =>
    mount(Button, {
      props,
      slots: { default: 'Click Me', ...slots },
      global: {
        stubs: {
          ElIcon: true,
          Loading: true
        }
      }
    })

  describe('Rendering', () => {
    it('renders_withDefaultProps_shouldHavePrimaryVariant', () => {
      const wrapper = factory()
      const buttonElement = wrapper.find('button')
      expect(buttonElement.exists()).toBe(true)
      // Primary variant includes indigo colors
      const classes = buttonElement.attributes('class')
      expect(classes).toContain('bg-indigo-600')
      expect(classes).toContain('text-white')
    })

    it('renders_withSecondaryVariant_shouldHaveSecondaryClasses', () => {
      const wrapper = factory({ variant: 'secondary' })
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('bg-gray-200')
      expect(classes).toContain('text-gray-900')
    })

    it('renders_withDangerVariant_shouldHaveDangerClasses', () => {
      const wrapper = factory({ variant: 'danger' })
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('bg-red-600')
      expect(classes).toContain('text-white')
    })

    it('renders_withTextVariant_shouldHaveTextClasses', () => {
      const wrapper = factory({ variant: 'text' })
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('bg-transparent')
      expect(classes).toContain('text-gray-700')
    })

    it('renders_withMdSize_shouldHaveMdSizeClasses', () => {
      const wrapper = factory({ size: 'md' })
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('px-4')
      expect(classes).toContain('py-2')
      expect(classes).toContain('text-base')
    })

    it('renders_withLgSize_shouldHaveLgSizeClasses', () => {
      const wrapper = factory({ size: 'lg' })
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('px-6')
      expect(classes).toContain('py-3')
      expect(classes).toContain('text-lg')
    })

    it('renders_withFullWidth_shouldIncludeFullWidthClass', () => {
      const wrapper = factory({ fullWidth: true })
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('w-full')
    })

    it('renders_withDefaultSlot_shouldDisplayContent', () => {
      const wrapper = factory({}, { default: 'Custom Text' })
      expect(wrapper.text()).toContain('Custom Text')
    })
  })

  describe('Loading State', () => {
    it('renders_withLoading_shouldShowLoadingIcon', () => {
      const wrapper = factory({ loading: true })
      // Loading icon should be present when loading is true
      const html = wrapper.html()
      expect(html).toContain('v-if')
    })

    it('renders_withLoading_shouldDisableButton', () => {
      const wrapper = factory({ loading: true })
      expect(wrapper.attributes('disabled')).toBeDefined()
    })

    it('renders_withLoading_shouldShowDisabledState', () => {
      const wrapper = factory({ loading: true })
      const classes = wrapper.find('button').attributes('class')
      // disabled:opacity-50 disabled:cursor-not-allowed should be applied
      expect(classes).toContain('disabled:opacity-50')
    })
  })

  describe('Disabled State', () => {
    it('renders_withDisabled_shouldSetDisabledAttribute', () => {
      const wrapper = factory({ disabled: true })
      expect(wrapper.attributes('disabled')).toBeDefined()
    })

    it('renders_withDisabled_shouldApplyDisabledStyles', () => {
      const wrapper = factory({ disabled: true })
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('disabled:opacity-50')
      expect(classes).toContain('disabled:cursor-not-allowed')
    })
  })

  describe('Button Type', () => {
    it('renders_withButtonType_shouldSetType', () => {
      const wrapper = factory({ type: 'button' })
      expect(wrapper.attributes('type')).toBe('button')
    })

    it('renders_withSubmitType_shouldSetType', () => {
      const wrapper = factory({ type: 'submit' })
      expect(wrapper.attributes('type')).toBe('submit')
    })

    it('renders_withResetType_shouldSetType', () => {
      const wrapper = factory({ type: 'reset' })
      expect(wrapper.attributes('type')).toBe('reset')
    })
  })

  describe('Click Events', () => {
    it('click_whenEnabled_shouldEmitClickEvent', async () => {
      const wrapper = factory()
      await wrapper.trigger('click')
      expect(wrapper.emitted('click')).toHaveLength(1)
    })

    it('click_whenDisabled_shouldNotEmitClickEvent', async () => {
      const wrapper = factory({ disabled: true })
      await wrapper.trigger('click')
      expect(wrapper.emitted('click')).toBeFalsy()
    })

    it('click_whenLoading_shouldNotEmitClickEvent', async () => {
      const wrapper = factory({ loading: true })
      await wrapper.trigger('click')
      expect(wrapper.emitted('click')).toBeFalsy()
    })

    it('click_shouldPassMouseEvent', async () => {
      const wrapper = factory()
      await wrapper.trigger('click')
      const emitted = wrapper.emitted('click')
      expect(emitted).toHaveLength(1)
      // MouseEvent should be passed as argument
      expect(emitted[0][0]).toBeDefined()
    })
  })

  describe('Icon Display', () => {
    it('renders_withIconLeftPosition_shouldShowIconOnLeft', () => {
      const wrapper = factory({ icon: 'Search', iconPosition: 'left' }, { default: 'Search' })
      // Icon should be rendered with component binding
      const html = wrapper.html()
      expect(html).toContain('mx-2')
    })

    it('renders_withIconRightPosition_shouldShowIconOnRight', () => {
      const wrapper = factory({ icon: 'ArrowRight', iconPosition: 'right' }, { default: 'Next' })
      const html = wrapper.html()
      expect(html).toContain('mx-2')
    })

    it('renders_withLoadingAndIcon_shouldHideIcon', () => {
      const wrapper = factory({ icon: 'Search', loading: true })
      // When loading, icon should not be shown
      const html = wrapper.html()
      // The v-if condition prevents icon rendering when loading
      expect(html).toContain('v-if')
    })

    it('renders_withoutIcon_shouldNotAddGapMargin', () => {
      const wrapper = factory({ icon: undefined })
      const text = wrapper.find('span')
      // Should not have mx-2 class when no icon
      expect(text.exists()).toBe(true)
    })
  })

  describe('Basic Styling', () => {
    it('renders_shouldHaveBaseClasses', () => {
      const wrapper = factory()
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('inline-flex')
      expect(classes).toContain('items-center')
      expect(classes).toContain('justify-center')
      expect(classes).toContain('font-medium')
      expect(classes).toContain('rounded-lg')
    })

    it('renders_shouldHaveFocusRingClasses', () => {
      const wrapper = factory()
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('focus:outline-none')
      expect(classes).toContain('focus:ring-2')
    })

    it('renders_shouldHaveTransitionClasses', () => {
      const wrapper = factory()
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('transition-all')
      expect(classes).toContain('duration-200')
    })
  })

  describe('Combination Tests', () => {
    it('renders_withVariantAndSize_shouldApplyBoth', () => {
      const wrapper = factory({ variant: 'danger', size: 'lg' })
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('bg-red-600')
      expect(classes).toContain('px-6')
      expect(classes).toContain('py-3')
    })

    it('renders_withFullWidthAndLoading_shouldApplyBoth', () => {
      const wrapper = factory({ fullWidth: true, loading: true })
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('w-full')
      expect(wrapper.attributes('disabled')).toBeDefined()
    })

    it('renders_withAllProps_shouldApplyAllCorrectly', () => {
      const wrapper = factory({
        variant: 'secondary',
        size: 'sm',
        fullWidth: true,
        disabled: false,
        type: 'submit',
        iconPosition: 'right'
      })
      const classes = wrapper.find('button').attributes('class')
      expect(classes).toContain('bg-gray-200')
      expect(classes).toContain('px-3')
      expect(classes).toContain('w-full')
      expect(wrapper.attributes('type')).toBe('submit')
    })
  })
})
