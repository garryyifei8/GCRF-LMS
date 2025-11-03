<template>
  <div :class="cardClasses">
    <div class="flex items-start justify-between">
      <!-- 左侧内容 -->
      <div class="flex-1">
        <p class="text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">
          {{ title }}
        </p>
        <p class="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">
          {{ value }}
        </p>

        <!-- 趋势指示器 -->
        <div v-if="trend && trendValue" :class="trendClasses">
          <el-icon :class="trendIconClasses">
            <component :is="trendIcon" />
          </el-icon>
          <span class="text-sm font-medium">{{ trendValue }}</span>
          <span v-if="trendText" class="text-xs text-gray-500 dark:text-gray-400 ml-1">
            {{ trendText }}
          </span>
        </div>
      </div>

      <!-- 右侧图标 -->
      <div v-if="icon" :class="iconContainerClasses">
        <el-icon :size="24">
          <component :is="icon" />
        </el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowUp, ArrowDown, Minus } from '@element-plus/icons-vue'
import type { StatCardProps } from './types'

// Props
const props = withDefaults(defineProps<StatCardProps>(), {
  color: 'primary',
  trend: 'flat'
})

// 卡片样式类
const cardClasses = computed(() => {
  return [
    'bg-white dark:bg-gray-800',
    'rounded-xl shadow-sm',
    'border border-gray-200 dark:border-gray-700',
    'p-6',
    'transition-all duration-300',
    'hover:shadow-md'
  ].join(' ')
})

// 图标容器样式
const iconContainerClasses = computed(() => {
  const colorClasses = {
    primary: 'bg-indigo-100 text-indigo-600 dark:bg-indigo-900/30 dark:text-indigo-400',
    success: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
    warning: 'bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400',
    danger: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400',
    info: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400'
  }

  return [
    'flex items-center justify-center',
    'w-12 h-12 rounded-lg',
    colorClasses[props.color]
  ].join(' ')
})

// 趋势图标
const trendIcon = computed(() => {
  switch (props.trend) {
    case 'up':
      return ArrowUp
    case 'down':
      return ArrowDown
    default:
      return Minus
  }
})

// 趋势样式类
const trendClasses = computed(() => {
  const baseClasses = 'flex items-center gap-1'

  const colorClasses = {
    up: 'text-green-600 dark:text-green-400',
    down: 'text-red-600 dark:text-red-400',
    flat: 'text-gray-600 dark:text-gray-400'
  }

  return [baseClasses, colorClasses[props.trend!]].join(' ')
})

// 趋势图标样式
const trendIconClasses = computed(() => {
  return 'text-sm'
})
</script>
