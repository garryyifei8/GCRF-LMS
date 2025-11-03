<template>
  <div :class="cardClasses" @click="handleClick">
    <!-- 头部插槽 -->
    <div v-if="$slots.header" class="card-header">
      <slot name="header"></slot>
    </div>

    <!-- 主内容 -->
    <div :class="contentClasses">
      <slot></slot>
    </div>

    <!-- 底部插槽 -->
    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CardProps, CardEmits } from './types'

// Props
const props = withDefaults(defineProps<CardProps>(), {
  variant: 'default',
  padding: 'md',
  shadow: true,
  hoverable: false,
  clickable: false
})

// Emits
const emit = defineEmits<CardEmits>()

// 计算卡片样式类
const cardClasses = computed(() => {
  const classes = [
    'rounded-xl',
    'transition-all duration-300',
    'overflow-hidden'
  ]

  // 变体样式
  const variantClasses = {
    default: [
      'bg-white dark:bg-gray-800',
      'border border-gray-200 dark:border-gray-700'
    ],
    bordered: [
      'bg-white dark:bg-gray-800',
      'border-2 border-gray-300 dark:border-gray-600'
    ],
    elevated: [
      'bg-white dark:bg-gray-800',
      'border-0'
    ],
    flat: [
      'bg-gray-50 dark:bg-gray-900',
      'border-0'
    ]
  }
  classes.push(...variantClasses[props.variant])

  // 阴影
  if (props.shadow && props.variant !== 'flat') {
    classes.push('shadow-sm')
  }

  // 悬停效果
  if (props.hoverable) {
    classes.push('hover:shadow-lg hover:-translate-y-1')
  }

  // 可点击
  if (props.clickable) {
    classes.push('cursor-pointer')
  }

  return classes.join(' ')
})

// 计算内容区域样式
const contentClasses = computed(() => {
  const paddingClasses = {
    none: 'p-0',
    sm: 'p-4',
    md: 'p-6',
    lg: 'p-8'
  }
  return paddingClasses[props.padding]
})

// 处理点击事件
const handleClick = (event: MouseEvent) => {
  if (props.clickable) {
    emit('click', event)
  }
}
</script>

<style scoped>
.card-header {
  @apply px-6 py-4 border-b border-gray-200 dark:border-gray-700;
  @apply bg-gray-50 dark:bg-gray-800;
}

.card-footer {
  @apply px-6 py-4 border-t border-gray-200 dark:border-gray-700;
  @apply bg-gray-50 dark:bg-gray-800;
}
</style>
