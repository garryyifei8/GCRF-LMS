import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import Input from '@/components/ui/Input/Input.vue'

describe('Input', () => {
  const factory = (props = {}) =>
    mount(Input, {
      props: { modelValue: '', ...props },
      global: { plugins: [ElementPlus] }
    })

  it('vModel_typing_shouldEmitUpdateModelValue', async () => {
    const wrapper = factory()
    const input = wrapper.find('input')
    await input.setValue('hello')
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['hello'])
  })

  it('renders_withErrorStatus_shouldApplyErrorStyles', () => {
    const wrapper = factory({ status: 'error' })
    expect(wrapper.html()).toContain('border-red-500')
  })

  it('renders_withErrorText_shouldShowErrorMessage', () => {
    const wrapper = factory({ errorText: 'Required field' })
    expect(wrapper.text()).toContain('Required field')
  })

  it('clear_whenClearableAndValue_shouldEmitClear', async () => {
    const wrapper = factory({ clearable: true, modelValue: 'abc' })
    const clearBtn = wrapper.find('.clear-button')
    expect(clearBtn.exists()).toBe(true)
    await clearBtn.trigger('click')
    expect(wrapper.emitted('clear')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([''])
  })

  it('renders_withShowCount_shouldShowCharacterCount', () => {
    const wrapper = factory({ showCount: true, maxlength: 100, modelValue: 'test' })
    expect(wrapper.text()).toMatch(/4.*100/)
  })

  it('renders_withMaxlength_shouldSetAttribute', () => {
    const wrapper = factory({ maxlength: 50 })
    expect(wrapper.find('input').attributes('maxlength')).toBe('50')
  })

  it('focus_shouldEmitFocus', async () => {
    const wrapper = factory()
    await wrapper.find('input').trigger('focus')
    expect(wrapper.emitted('focus')).toBeTruthy()
  })

  it('renders_whenDisabled_shouldDisableInput', () => {
    const wrapper = factory({ disabled: true })
    expect(wrapper.find('input').attributes('disabled')).toBeDefined()
  })
})
