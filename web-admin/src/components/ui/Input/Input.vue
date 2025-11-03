<template>
  <div class="input-wrapper">
    <!-- 标签 -->
    <label v-if="showLabel && label" :class="labelClasses">
      {{ label }}
      <span v-if="required" class="text-red-500 ml-1">*</span>
    </label>

    <!-- 输入框容器 -->
    <div :class="containerClasses">
      <!-- 前缀图标 -->
      <div v-if="prefixIcon" class="input-prefix">
        <el-icon>
          <component :is="prefixIcon" />
        </el-icon>
      </div>

      <!-- 输入框 -->
      <input
        ref="inputRef"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :required="required"
        :maxlength="maxlength"
        :class="inputClasses"
        @input="handleInput"
        @change="handleChange"
        @blur="handleBlur"
        @focus="handleFocus"
      />

      <!-- 后缀图标或清空按钮 -->
      <div v-if="suffixIcon || (clearable && modelValue)" class="input-suffix">
        <!-- 清空按钮 -->
        <button
          v-if="clearable && modelValue && !disabled && !readonly"
          type="button"
          class="clear-button"
          @click="handleClear"
        >
          <el-icon><CircleClose /></el-icon>
        </button>

        <!-- 后缀图标 -->
        <el-icon v-else-if="suffixIcon">
          <component :is="suffixIcon" />
        </el-icon>
      </div>

      <!-- 字数统计 -->
      <div v-if="showCount && maxlength" class="input-count">
        {{ String(modelValue || '').length }} / {{ maxlength }}
      </div>
    </div>

    <!-- 帮助文字或错误信息 -->
    <div v-if="helpText || errorText" :class="helpTextClasses">
      {{ errorText || helpText }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { CircleClose } from '@element-plus/icons-vue'
import type { InputProps, InputEmits } from './types'

// Props
const props = withDefaults(defineProps<InputProps>(), {
  type: 'text',
  size: 'md',
  status: 'default',
  showLabel: true,
  clearable: false,
  showCount: false
})

// Emits
const emit = defineEmits<InputEmits>()

// Refs
const inputRef = ref<HTMLInputElement>()

// 标签样式
const labelClasses = computed(() => {
  return [
    'block mb-2',
    'text-sm font-medium',
    'text-gray-700 dark:text-gray-300'
  ].join(' ')
})

// 容器样式
const containerClasses = computed(() => {
  const classes = [
    'relative flex items-center',
    'rounded-lg border',
    'transition-all duration-200'
  ]

  // 状态样式
  const statusClasses = {
    default: 'border-gray-300 dark:border-gray-600 focus-within:border-indigo-500 focus-within:ring-2 focus-within:ring-indigo-200 dark:focus-within:ring-indigo-800',
    success: 'border-green-500 focus-within:ring-2 focus-within:ring-green-200 dark:focus-within:ring-green-800',
    error: 'border-red-500 focus-within:ring-2 focus-within:ring-red-200 dark:focus-within:ring-red-800',
    warning: 'border-amber-500 focus-within:ring-2 focus-within:ring-amber-200 dark:focus-within:ring-amber-800'
  }
  classes.push(statusClasses[props.status])

  // 禁用样式
  if (props.disabled) {
    classes.push('bg-gray-100 dark:bg-gray-800 opacity-60 cursor-not-allowed')
  } else {
    classes.push('bg-white dark:bg-gray-900')
  }

  return classes.join(' ')
})

// 输入框样式
const inputClasses = computed(() => {
  const classes = [
    'flex-1 outline-none bg-transparent',
    'text-gray-900 dark:text-gray-100',
    'placeholder:text-gray-400 dark:placeholder:text-gray-500'
  ]

  // 尺寸样式
  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2 text-base',
    lg: 'px-5 py-3 text-lg'
  }
  classes.push(sizeClasses[props.size])

  // 前缀图标padding
  if (props.prefixIcon) {
    classes.push('pl-10')
  }

  // 后缀图标或清空按钮padding
  if (props.suffixIcon || props.clearable) {
    classes.push('pr-10')
  }

  // 字数统计padding
  if (props.showCount && props.maxlength) {
    classes.push('pr-20')
  }

  if (props.disabled) {
    classes.push('cursor-not-allowed')
  }

  return classes.join(' ')
})

// 帮助文字样式
const helpTextClasses = computed(() => {
  const classes = ['mt-1.5 text-sm']

  const statusColors = {
    default: 'text-gray-500 dark:text-gray-400',
    success: 'text-green-600 dark:text-green-400',
    error: 'text-red-600 dark:text-red-400',
    warning: 'text-amber-600 dark:text-amber-400'
  }
  classes.push(statusColors[props.status])

  return classes.join(' ')
})

// 处理输入事件
const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = props.type === 'number' ? Number(target.value) : target.value
  emit('update:modelValue', value)
  emit('input', value)
}

// 处理变化事件
const handleChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = props.type === 'number' ? Number(target.value) : target.value
  emit('change', value)
}

// 处理失焦事件
const handleBlur = (event: FocusEvent) => {
  emit('blur', event)
}

// 处理聚焦事件
const handleFocus = (event: FocusEvent) => {
  emit('focus', event)
}

// 处理清空
const handleClear = () => {
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}
</script>

<style scoped>
.input-wrapper {
  @apply w-full;
}

.input-prefix,
.input-suffix {
  @apply absolute flex items-center;
  @apply text-gray-400 dark:text-gray-500;
}

.input-prefix {
  @apply left-3;
}

.input-suffix {
  @apply right-3;
}

.clear-button {
  @apply flex items-center justify-center;
  @apply w-4 h-4 rounded-full;
  @apply text-gray-400 hover:text-gray-600;
  @apply dark:text-gray-500 dark:hover:text-gray-300;
  @apply transition-colors duration-200;
  @apply cursor-pointer;
}

.input-count {
  @apply absolute right-3;
  @apply text-xs text-gray-400 dark:text-gray-500;
  @apply pointer-events-none;
}
</style>
