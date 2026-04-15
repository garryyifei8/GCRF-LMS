import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import AICard from '@/components/ui/Card/AICard.vue'

describe('AICard', () => {
  const factory = (props = {}) =>
    mount(AICard, {
      props: { title: 'AI Feature', ...props },
      global: { plugins: [ElementPlus] }
    })

  it('renders_requiredProps_shouldShowTitle', () => {
    const wrapper = factory({
      title: 'Test Book Title'
    })

    const titleElement = wrapper.find('.ai-card-title')
    expect(titleElement.exists()).toBe(true)
    expect(titleElement.text()).toBe('Test Book Title')
  })

  it('renders_withTags_shouldRenderAllTags', () => {
    const tags = ['Fiction', 'AI Recommended', 'Bestseller']
    const wrapper = factory({
      tags
    })

    const tagElements = wrapper.findAll('.ai-card-tag')
    expect(tagElements).toHaveLength(3)
    expect(tagElements[0].text()).toBe('Fiction')
    expect(tagElements[1].text()).toBe('AI Recommended')
    expect(tagElements[2].text()).toBe('Bestseller')
  })

  it('renders_withScore_shouldShowScore', () => {
    const wrapper = factory({
      image: 'https://example.com/book.jpg',
      score: 85
    })

    const scoreCircle = wrapper.find('.ai-score-circle')
    expect(scoreCircle.exists()).toBe(true)

    const scoreValue = wrapper.find('.ai-score-value')
    expect(scoreValue.exists()).toBe(true)
    expect(scoreValue.text()).toBe('85')
  })

  it('click_shouldEmitClickEvent', async () => {
    const wrapper = factory()

    const cardElement = wrapper.find('.ai-card')
    await cardElement.trigger('click')

    expect(wrapper.emitted('click')).toBeTruthy()
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('renders_withoutBadge_shouldHideAIBadge', () => {
    const wrapper = factory({
      showAIBadge: false
    })

    const badgeContainer = wrapper.find('.ai-badge-container')
    expect(badgeContainer.exists()).toBe(false)
  })
})
