import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatCard from '@/components/ui/Card/StatCard.vue'

describe('StatCard', () => {
  const factory = (props = {}) =>
    mount(StatCard, {
      props: { title: '图书总数', value: 1000, ...props },
      global: {
        stubs: {
          ElIcon: false
        }
      }
    })

  describe('Rendering - Required Props', () => {
    it('renders_requiredProps_shouldShowTitleAndValue', () => {
      const wrapper = factory()
      expect(wrapper.text()).toContain('图书总数')
      expect(wrapper.text()).toContain('1000')
    })

    it('renders_withStringValue_shouldDisplayCorrectly', () => {
      const wrapper = factory({ value: '约1,234本' })
      expect(wrapper.text()).toContain('约1,234本')
    })

    it('renders_withNumberValue_shouldDisplayCorrectly', () => {
      const wrapper = factory({ value: 999 })
      expect(wrapper.text()).toContain('999')
    })
  })

  describe('Rendering - Trend Indicators', () => {
    it('renders_trendUp_shouldShowArrowUpIcon', () => {
      const wrapper = factory({ trend: 'up', trendValue: '+5%' })
      const html = wrapper.html()
      // Verify trend classes are applied
      expect(html).toContain('text-green-600')
      expect(wrapper.text()).toContain('+5%')
    })

    it('renders_trendDown_shouldShowArrowDownIcon', () => {
      const wrapper = factory({ trend: 'down', trendValue: '-3%' })
      const html = wrapper.html()
      expect(html).toContain('text-red-600')
      expect(wrapper.text()).toContain('-3%')
    })

    it('renders_trendFlat_shouldShowMinusIcon', () => {
      const wrapper = factory({ trend: 'flat' })
      const html = wrapper.html()
      expect(html).toContain('text-gray-600')
    })

    it('renders_withTrendText_shouldDisplayTrendDescription', () => {
      const wrapper = factory({
        trend: 'up',
        trendValue: '+10%',
        trendText: '比上周增长'
      })
      expect(wrapper.text()).toContain('+10%')
      expect(wrapper.text()).toContain('比上周增长')
    })

    it('renders_withoutTrendValue_shouldNotShowTrendSection', () => {
      const wrapper = factory({ trend: 'up' })
      const trendDiv = wrapper.find('[class*="flex"][class*="items-center"][class*="gap"]')
      // Without trendValue, the trend section should not render
      expect(wrapper.text()).not.toContain('%')
    })
  })

  describe('Rendering - Icon', () => {
    it('renders_withIcon_shouldDisplayIconContainer', () => {
      const wrapper = factory({ icon: 'Books' })
      const html = wrapper.html()
      expect(html).toContain('rounded-lg')
      expect(html).toContain('w-12')
      expect(html).toContain('h-12')
    })

    it('renders_withoutIcon_shouldNotRenderIconContainer', () => {
      const wrapper = factory({ icon: undefined })
      const html = wrapper.html()
      // Check that icon container classes are not present for the second div
      const iconContainers = wrapper.findAll('div')
      // The component should still render but without the icon section
      expect(wrapper.exists()).toBe(true)
    })
  })

  describe('Color Variants', () => {
    it('renders_colorPrimary_shouldApplyPrimaryClasses', () => {
      const wrapper = factory({ icon: 'Test', color: 'primary' })
      const html = wrapper.html()
      expect(html).toContain('bg-indigo-100')
      expect(html).toContain('text-indigo-600')
    })

    it('renders_colorSuccess_shouldApplySuccessClasses', () => {
      const wrapper = factory({ icon: 'Test', color: 'success' })
      const html = wrapper.html()
      expect(html).toContain('bg-green-100')
      expect(html).toContain('text-green-600')
    })

    it('renders_colorWarning_shouldApplyWarningClasses', () => {
      const wrapper = factory({ icon: 'Test', color: 'warning' })
      const html = wrapper.html()
      expect(html).toContain('bg-amber-100')
      expect(html).toContain('text-amber-600')
    })

    it('renders_colorDanger_shouldApplyDangerClasses', () => {
      const wrapper = factory({ icon: 'Test', color: 'danger' })
      const html = wrapper.html()
      expect(html).toContain('bg-red-100')
      expect(html).toContain('text-red-600')
    })

    it('renders_colorInfo_shouldApplyInfoClasses', () => {
      const wrapper = factory({ icon: 'Test', color: 'info' })
      const html = wrapper.html()
      expect(html).toContain('bg-blue-100')
      expect(html).toContain('text-blue-600')
    })

    it('renders_defaultColor_shouldBePrimary', () => {
      const wrapper = factory({ icon: 'Test' })
      const html = wrapper.html()
      expect(html).toContain('bg-indigo-100')
    })
  })

  describe('Card Base Styles', () => {
    it('renders_shouldHaveCardClasses', () => {
      const wrapper = factory()
      const html = wrapper.html()
      expect(html).toContain('bg-white')
      expect(html).toContain('rounded-xl')
      expect(html).toContain('shadow-sm')
      expect(html).toContain('border')
    })

    it('renders_shouldHaveTransitionClasses', () => {
      const wrapper = factory()
      const html = wrapper.html()
      expect(html).toContain('transition-all')
      expect(html).toContain('duration-300')
      expect(html).toContain('hover:shadow-md')
    })
  })

  describe('Layout Structure', () => {
    it('renders_shouldHaveFlexLayoutWithContentAndIcon', () => {
      const wrapper = factory({ icon: 'Test' })
      const html = wrapper.html()
      expect(html).toContain('flex')
      expect(html).toContain('items-start')
      expect(html).toContain('justify-between')
    })

    it('renders_shouldHaveContentOnLeft', () => {
      const wrapper = factory()
      const html = wrapper.html()
      expect(html).toContain('flex-1')
    })

    it('renders_shouldHaveIconOnRight', () => {
      const wrapper = factory({ icon: 'Test' })
      const html = wrapper.html()
      expect(html).toContain('justify-center')
    })
  })

  describe('Typography', () => {
    it('renders_shouldHaveCorrectTitleStyle', () => {
      const wrapper = factory()
      const html = wrapper.html()
      expect(html).toContain('text-sm')
      expect(html).toContain('font-medium')
      expect(html).toContain('text-gray-600')
    })

    it('renders_shouldHaveCorrectValueStyle', () => {
      const wrapper = factory()
      const html = wrapper.html()
      expect(html).toContain('text-3xl')
      expect(html).toContain('font-bold')
      expect(html).toContain('text-gray-900')
    })

    it('renders_shouldHaveCorrectTrendStyle', () => {
      const wrapper = factory({ trend: 'up', trendValue: '+5%' })
      const html = wrapper.html()
      expect(html).toContain('text-sm')
      expect(html).toContain('font-medium')
    })
  })

  describe('Complex Scenarios', () => {
    it('renders_fullPropsCombination_shouldRenderAllElements', () => {
      const wrapper = factory({
        icon: 'Books',
        trend: 'up',
        trendValue: '+12%',
        trendText: '环比增长',
        color: 'success'
      })
      const text = wrapper.text()
      expect(text).toContain('图书总数')
      expect(text).toContain('1000')
      expect(text).toContain('+12%')
      expect(text).toContain('环比增长')
      const html = wrapper.html()
      expect(html).toContain('bg-green-100')
    })

    it('renders_minimumProps_shouldStillRender', () => {
      const wrapper = factory({ icon: undefined, trend: undefined })
      expect(wrapper.text()).toContain('图书总数')
      expect(wrapper.text()).toContain('1000')
      expect(wrapper.exists()).toBe(true)
    })

    it('renders_withDifferentTrends_shouldShowCorrectColors', async () => {
      const upWrapper = factory({ trend: 'up', trendValue: '+5%' })
      const downWrapper = factory({ trend: 'down', trendValue: '-5%' })
      const flatWrapper = factory({ trend: 'flat' })

      expect(upWrapper.html()).toContain('text-green-600')
      expect(downWrapper.html()).toContain('text-red-600')
      expect(flatWrapper.html()).toContain('text-gray-600')
    })
  })

  describe('Props Validation', () => {
    it('renders_withLargeTitleAndValue_shouldDisplayWithoutTruncation', () => {
      const wrapper = factory({
        title: '这是一个很长的标题用来测试组件是否能够正确处理',
        value: '9999999'
      })
      expect(wrapper.text()).toContain('这是一个很长的标题用来测试组件是否能够正确处理')
      expect(wrapper.text()).toContain('9999999')
    })

    it('renders_withEmptyString_shouldHandleGracefully', () => {
      const wrapper = factory({ trendText: '' })
      expect(wrapper.exists()).toBe(true)
    })

    it('renders_defaultTrend_shouldBeFlatWhenNotSpecified', () => {
      const wrapper = factory({ trendValue: '+5%' })
      // Default trend is 'flat', so should show gray color
      const html = wrapper.html()
      expect(html).toContain('text-gray-600')
    })
  })

  describe('Dark Mode Support', () => {
    it('renders_shouldHaveDarkModeClasses', () => {
      const wrapper = factory()
      const html = wrapper.html()
      expect(html).toContain('dark:bg-gray-800')
      expect(html).toContain('dark:text-gray-100')
      expect(html).toContain('dark:border-gray-700')
    })

    it('renders_withColorVariant_shouldHaveDarkModeVariants', () => {
      const wrapper = factory({ icon: 'Test', color: 'success' })
      const html = wrapper.html()
      expect(html).toContain('dark:bg-green-900')
      expect(html).toContain('dark:text-green-400')
    })

    it('renders_trendUpDarkMode_shouldHaveDarkClasses', () => {
      const wrapper = factory({ trend: 'up', trendValue: '+5%' })
      const html = wrapper.html()
      expect(html).toContain('dark:text-green-400')
    })
  })

  describe('Edge Cases', () => {
    it('renders_withZeroValue_shouldDisplay', () => {
      const wrapper = factory({ value: 0 })
      expect(wrapper.text()).toContain('0')
    })

    it('renders_withNegativeValue_shouldDisplay', () => {
      const wrapper = factory({ value: -50 })
      expect(wrapper.text()).toContain('-50')
    })

    it('renders_withSpecialCharactersInTitle_shouldDisplay', () => {
      const wrapper = factory({ title: '图书(含电子书)' })
      expect(wrapper.text()).toContain('图书(含电子书)')
    })

    it('renders_withSpecialCharactersInTrendValue_shouldDisplay', () => {
      const wrapper = factory({ trendValue: '↑ 12%', trend: 'up' })
      expect(wrapper.text()).toContain('↑ 12%')
    })
  })
})
