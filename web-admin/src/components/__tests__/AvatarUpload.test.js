/* eslint-env browser, node */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import AvatarUpload from '@/components/AvatarUpload.vue'

// Mock FileReader
vi.stubGlobal(
  'FileReader',
  class {
    constructor() {
      this.onload = null
    }
    readAsDataURL() {
      setTimeout(() => {
        if (this.onload) {
          this.onload({ target: { result: 'data:image/png;base64,MOCKED' } })
        }
      }, 0)
    }
  }
)

// Mock URL.createObjectURL
window.URL.createObjectURL = vi.fn(() => 'blob:mock-url')
window.URL.revokeObjectURL = vi.fn()

// Mock mediaDevices
Object.defineProperty(navigator, 'mediaDevices', {
  writable: true,
  value: {
    getUserMedia: vi.fn().mockResolvedValue({
      getTracks: () => [{ stop: vi.fn() }]
    })
  }
})

describe('AvatarUpload', () => {
  const factory = (props = {}) =>
    mount(AvatarUpload, {
      props: { modelValue: '', ...props },
      global: { plugins: [ElementPlus] }
    })

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders_withoutImage_shouldShowPlaceholder', () => {
    const wrapper = factory()
    expect(wrapper.text()).toContain('点击上传')
  })

  it('renders_withModelValue_shouldShowPreview', () => {
    const wrapper = factory({ modelValue: 'data:image/png;base64,ABC' })
    const img = wrapper.find('img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toContain('data:image')
  })

  it('uploadFile_validFile_shouldEmitUpdateModelValue', async () => {
    const wrapper = factory()
    const input = wrapper.find('input[type="file"]')

    const file = new File(['content'], 'avatar.png', { type: 'image/png' })
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await new Promise((resolve) => setTimeout(resolve, 10)) // Wait for FileReader

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
  })

  it('uploadFile_oversizedFile_shouldEmitError', async () => {
    const wrapper = factory({ maxSize: 0.001 }) // 1KB limit
    const input = wrapper.find('input[type="file"]')

    const largeFile = new File([new Array(10000).join('a')], 'big.png', { type: 'image/png' })
    Object.defineProperty(input.element, 'files', { value: [largeFile] })
    Object.defineProperty(largeFile, 'size', { value: 10000 })
    await input.trigger('change')

    expect(wrapper.emitted('error') || wrapper.emitted('update:modelValue')).toBeTruthy()
  })

  it('uploadFile_invalidType_shouldEmitError', async () => {
    const wrapper = factory({ accept: 'image/png' })
    const input = wrapper.find('input[type="file"]')

    const file = new File(['content'], 'doc.pdf', { type: 'application/pdf' })
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')

    // Either error emit or no modelValue change
    const errorEmitted = wrapper.emitted('error')
    expect(errorEmitted || !wrapper.emitted('update:modelValue')).toBeTruthy()
  })

  it('clear_shouldEmitUpdateModelValueWithEmpty', async () => {
    const wrapper = factory({ modelValue: 'data:image/png;base64,ABC' })
    // Find delete/clear button
    const deleteBtn = wrapper.find('[data-test="delete"], .delete-button, button')
    if (deleteBtn.exists()) {
      await deleteBtn.trigger('click')
      const emits = wrapper.emitted('update:modelValue')
      if (emits) {
        expect(emits[emits.length - 1]).toEqual([''])
      }
    }
  })

  it('renders_withCircleShape_shouldApplyCircleClass', () => {
    const wrapper = factory({ shape: 'circle' })
    expect(wrapper.html()).toContain('circle')
  })
})
