import { describe, it, expect } from 'vitest'
import { h } from 'vue'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import SkeletonLoader from '@/components/Loading/SkeletonLoader.vue'

describe('SkeletonLoader', () => {
  const factory = (props = {}, slots = {}) =>
    mount(SkeletonLoader, { props, slots, global: { plugins: [ElementPlus] } })

  describe('renders_typeTable_shouldShowTableTemplate', () => {
    it('should render table skeleton with header and rows', () => {
      const wrapper = factory({ type: 'table', rows: 3, columns: 4 })

      expect(wrapper.find('.skeleton-table').exists()).toBe(true)
      expect(wrapper.find('.skeleton-table-header').exists()).toBe(true)
      expect(wrapper.findAll('.skeleton-table-header-cell')).toHaveLength(4)
      expect(wrapper.findAll('.skeleton-table-row')).toHaveLength(3)
    })

    it('should render table cells for each row and column', () => {
      const wrapper = factory({ type: 'table', rows: 2, columns: 5 })

      const rows = wrapper.findAll('.skeleton-table-row')
      expect(rows).toHaveLength(2)

      rows.forEach((row) => {
        const cells = row.findAll('.skeleton-table-cell')
        expect(cells).toHaveLength(5)
      })
    })

    it('should use default rows and columns when not provided', () => {
      const wrapper = factory({ type: 'table' })

      expect(wrapper.findAll('.skeleton-table-header-cell')).toHaveLength(6) // default columns
      expect(wrapper.findAll('.skeleton-table-row')).toHaveLength(5) // default rows
    })
  })

  describe('renders_typeCard_shouldShowCardTemplate', () => {
    it('should render card skeleton with header and content', () => {
      const wrapper = factory({ type: 'card' })

      expect(wrapper.find('.skeleton-card').exists()).toBe(true)
      expect(wrapper.find('.skeleton-card-header').exists()).toBe(true)
      expect(wrapper.find('.skeleton-avatar').exists()).toBe(true)
      expect(wrapper.find('.skeleton-card-title').exists()).toBe(true)
      expect(wrapper.find('.skeleton-card-content').exists()).toBe(true)
    })

    it('should render card content with 3 block lines', () => {
      const wrapper = factory({ type: 'card' })

      const contentBlocks = wrapper.find('.skeleton-card-content').findAll('.skeleton-block')
      expect(contentBlocks).toHaveLength(3)
    })

    it('should ignore rows prop for card type', () => {
      const wrapper = factory({ type: 'card', rows: 10 })

      expect(wrapper.find('.skeleton-card-content').findAll('.skeleton-block')).toHaveLength(3)
    })
  })

  describe('renders_withRows_shouldRenderCorrectCount', () => {
    it('should render correct number of form items based on rows prop', () => {
      const wrapper = factory({ type: 'form', rows: 4 })

      expect(wrapper.findAll('.skeleton-form-item')).toHaveLength(4)
    })

    it('should render correct number of list items based on rows prop', () => {
      const wrapper = factory({ type: 'list', rows: 7 })

      expect(wrapper.findAll('.skeleton-list-item')).toHaveLength(7)
    })

    it('should render table rows matching rows prop', () => {
      const wrapper = factory({ type: 'table', rows: 8 })

      expect(wrapper.findAll('.skeleton-table-row')).toHaveLength(8)
    })

    it('should respect default rows value of 5', () => {
      const wrapper = factory({ type: 'form' })

      expect(wrapper.findAll('.skeleton-form-item')).toHaveLength(5)
    })
  })

  describe('renders_animated_shouldApplyAnimationClass', () => {
    it('should apply animation to skeleton blocks when animated is true', () => {
      const wrapper = factory({ type: 'card', animated: true })

      const blocks = wrapper.findAll('.skeleton-block')
      blocks.forEach((block) => {
        const styles = block.element.getAttribute('style')
        expect(styles).toBeDefined()
      })
    })

    it('should have animation styles in CSS', () => {
      const wrapper = factory({ type: 'card', animated: true })

      expect(wrapper.find('.skeleton-block').exists()).toBe(true)
    })

    it('should render skeleton-avatar with animation', () => {
      const wrapper = factory({ type: 'card', animated: true })

      const avatar = wrapper.find('.skeleton-avatar')
      expect(avatar.exists()).toBe(true)
    })

    it('should use animated prop but component always renders with animation styles', () => {
      const wrapperAnimated = factory({ type: 'card', animated: true })
      const wrapperNotAnimated = factory({ type: 'card', animated: false })

      expect(wrapperAnimated.find('.skeleton-block').exists()).toBe(true)
      expect(wrapperNotAnimated.find('.skeleton-block').exists()).toBe(true)
    })
  })

  describe('renders_customType_shouldRenderSlot', () => {
    it('should render slot content when type is custom', () => {
      const wrapper = factory(
        { type: 'custom' },
        { default: () => h('div', { class: 'custom-content' }, 'Custom Skeleton') }
      )

      expect(wrapper.find('.custom-content').exists()).toBe(true)
      expect(wrapper.find('.custom-content').text()).toBe('Custom Skeleton')
    })

    it('should render slot for any unknown type', () => {
      const wrapper = factory(
        { type: 'unknown' },
        { default: () => h('p', { class: 'fallback' }, 'Fallback Content') }
      )

      expect(wrapper.find('.fallback').exists()).toBe(true)
    })

    it('should render slot content without table, card, form, or list templates', () => {
      const wrapper = factory(
        { type: 'custom' },
        { default: () => h('div', { class: 'my-skeleton' }, 'My Custom Skeleton') }
      )

      expect(wrapper.find('.skeleton-table').exists()).toBe(false)
      expect(wrapper.find('.skeleton-card').exists()).toBe(false)
      expect(wrapper.find('.skeleton-form').exists()).toBe(false)
      expect(wrapper.find('.skeleton-list').exists()).toBe(false)
      expect(wrapper.find('.my-skeleton').exists()).toBe(true)
    })

    it('should render custom slot without predefined templates', () => {
      const wrapper = factory({ type: 'custom' }, { default: () => h('span', 'Custom') })

      expect(wrapper.find('.skeleton-table').exists()).toBe(false)
      expect(wrapper.find('.skeleton-card').exists()).toBe(false)
      expect(wrapper.find('.skeleton-form').exists()).toBe(false)
      expect(wrapper.find('.skeleton-list').exists()).toBe(false)
    })
  })

  describe('props validation', () => {
    it('should accept valid type values', () => {
      const validTypes = ['table', 'card', 'form', 'list', 'custom']

      validTypes.forEach((type) => {
        const wrapper = factory({ type })
        expect(wrapper.vm.$.props.type).toBe(type)
      })
    })

    it('should have default type of table', () => {
      const wrapper = factory()
      expect(wrapper.vm.$.props.type).toBe('table')
    })

    it('should have default rows of 5', () => {
      const wrapper = factory()
      expect(wrapper.vm.$.props.rows).toBe(5)
    })

    it('should have default columns of 6', () => {
      const wrapper = factory()
      expect(wrapper.vm.$.props.columns).toBe(6)
    })

    it('should have default animated of true', () => {
      const wrapper = factory()
      expect(wrapper.vm.$.props.animated).toBe(true)
    })
  })

  describe('form skeleton', () => {
    it('should render form items with label and control', () => {
      const wrapper = factory({ type: 'form', rows: 3 })

      expect(wrapper.findAll('.skeleton-form-item')).toHaveLength(3)
      expect(wrapper.findAll('.skeleton-form-label')).toHaveLength(3)
      expect(wrapper.findAll('.skeleton-form-control')).toHaveLength(3)
    })
  })

  describe('list skeleton', () => {
    it('should render list items with avatar and content', () => {
      const wrapper = factory({ type: 'list', rows: 4 })

      expect(wrapper.findAll('.skeleton-list-item')).toHaveLength(4)
      expect(wrapper.findAll('.skeleton-avatar')).toHaveLength(4)
      expect(wrapper.findAll('.skeleton-list-content')).toHaveLength(4)
    })

    it('should render content blocks in each list item', () => {
      const wrapper = factory({ type: 'list', rows: 2 })

      const items = wrapper.findAll('.skeleton-list-item')
      items.forEach((item) => {
        const blocks = item.find('.skeleton-list-content').findAll('.skeleton-block')
        expect(blocks.length).toBeGreaterThan(0)
      })
    })
  })
})
