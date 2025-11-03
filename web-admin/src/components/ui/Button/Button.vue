<template>
  <button
    :type="type"
    :class="buttonClasses"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <!-- 加载图标 -->
    <el-icon v-if="loading" class="animate-spin">
      <Loading />
    </el-icon>

    <!-- 左侧图标 -->
    <el-icon v-if="icon && iconPosition === 'left' && !loading">
      <component :is="icon" />
    </el-icon>

    <!-- 默认插槽内容 -->
    <span v-if="$slots.default" :class="{ 'mx-2': icon || loading }">
      <slot></slot>
    </span>

    <!-- 右侧图标 -->
    <el-icon v-if="icon && iconPosition === 'right' && !loading">
      <component :is="icon" />
    </el-icon>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import type { ButtonProps, ButtonEmits } from './types'

// Props
const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
  loading: false,
  disabled: false,
  fullWidth: false,
  iconPosition: 'left',
  type: 'button'
})

// Emits
const emit = defineEmits<ButtonEmits>()

// 计算按钮样式类
const buttonClasses = computed(() => {
  const classes = [
    'inline-flex items-center justify-center',
    'font-medium rounded-lg',
    'transition-all duration-200',
    'focus:outline-none focus:ring-2 focus:ring-offset-2',
    'disabled:opacity-50 disabled:cursor-not-allowed'
  ]

  // 全宽样式
  if (props.fullWidth) {
    classes.push('w-full')
  }

  // 尺寸样式
  const sizeClasses = {
    xs: 'px-2.5 py-1.5 text-xs gap-1',
    sm: 'px-3 py-2 text-sm gap-1.5',
    md: 'px-4 py-2 text-base gap-2',
    lg: 'px-6 py-3 text-lg gap-2.5',
    xl: 'px-8 py-4 text-xl gap-3'
  }
  classes.push(sizeClasses[props.size])

  // 变体样式
  const variantClasses = {
    primary: [
      'bg-indigo-600 text-white',
      'hover:bg-indigo-700',
      'focus:ring-indigo-500',
      'dark:bg-indigo-500 dark:hover:bg-indigo-600'
    ],
    secondary: [
      'bg-gray-200 text-gray-900',
      'hover:bg-gray-300',
      'focus:ring-gray-400',
      'dark:bg-gray-700 dark:text-gray-100 dark:hover:bg-gray-600'
    ],
    danger: [
      'bg-red-600 text-white',
      'hover:bg-red-700',
      'focus:ring-red-500',
      'dark:bg-red-500 dark:hover:bg-red-600'
    ],
    text: [
      'bg-transparent text-gray-700',
      'hover:bg-gray-100',
      'focus:ring-gray-300',
      'dark:text-gray-300 dark:hover:bg-gray-800'
    ],
    ai: [
      'bg-ai-gradient text-white',
      'hover:shadow-lg hover:scale-105',
      'focus:ring-ai-primary',
      'ai-glow'
    ]
  }
  classes.push(...variantClasses[props.variant])

  return classes.join(' ')
})

// 处理点击事件
const handleClick = (event: MouseEvent) => {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}
</script>

<style scoped>
/* 旋转动画 */
.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
