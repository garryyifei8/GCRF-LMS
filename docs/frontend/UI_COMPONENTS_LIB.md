# 国创睿峰智能图书馆 - UI 组件库实现

> 基于 Vue 3 + Tailwind CSS + TypeScript
> 更新日期: 2025-01-14

---

## 目录结构

```
src/components/
├── ui/                      # 基础 UI 组件
│   ├── Button/
│   │   ├── Button.vue
│   │   ├── Button.types.ts
│   │   └── index.ts
│   ├── Card/
│   │   ├── Card.vue
│   │   ├── StatCard.vue
│   │   ├── AICard.vue
│   │   └── index.ts
│   ├── Table/
│   │   ├── Table.vue
│   │   ├── TableColumn.vue
│   │   └── index.ts
│   ├── Form/
│   │   ├── Input.vue
│   │   ├── Select.vue
│   │   ├── Switch.vue
│   │   └── index.ts
│   ├── Modal/
│   │   ├── Modal.vue
│   │   ├── Drawer.vue
│   │   └── index.ts
│   ├── Badge/
│   │   ├── Badge.vue
│   │   ├── Tag.vue
│   │   └── index.ts
│   ├── Alert/
│   │   ├── Alert.vue
│   │   └── index.ts
│   ├── Loading/
│   │   ├── Spinner.vue
│   │   ├── Skeleton.vue
│   │   └── index.ts
│   └── Navigation/
│       ├── Sidebar.vue
│       ├── TopBar.vue
│       ├── Pagination.vue
│       └── index.ts
├── ai/                      # AI 特色组件
│   ├── AIBadge.vue
│   ├── AIRecommendCard.vue
│   ├── AIChatBox.vue
│   └── AIAnalysis.vue
└── layouts/                 # 布局组件
    ├── MainLayout.vue
    ├── AuthLayout.vue
    └── EmptyLayout.vue
```

---

## 1. 按钮组件 (Button)

### Button.types.ts
```typescript
export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'text' | 'ai'
export type ButtonSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl'

export interface ButtonProps {
  variant?: ButtonVariant
  size?: ButtonSize
  loading?: boolean
  disabled?: boolean
  fullWidth?: boolean
  icon?: string
  iconPosition?: 'left' | 'right'
}
```

### Button.vue
```vue
<template>
  <button
    :class="buttonClasses"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <!-- Loading Spinner -->
    <svg
      v-if="loading"
      class="animate-spin -ml-1 mr-2 h-4 w-4"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle
        class="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        stroke-width="4"
      />
      <path
        class="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>

    <!-- Left Icon -->
    <component
      v-if="icon && iconPosition === 'left' && !loading"
      :is="icon"
      class="w-5 h-5 mr-2"
    />

    <!-- Slot Content -->
    <slot />

    <!-- Right Icon -->
    <component
      v-if="icon && iconPosition === 'right' && !loading"
      :is="icon"
      class="w-5 h-5 ml-2"
    />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ButtonProps } from './Button.types'

const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
  loading: false,
  disabled: false,
  fullWidth: false,
  iconPosition: 'left'
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const buttonClasses = computed(() => {
  const baseClasses = [
    'inline-flex items-center justify-center',
    'font-medium',
    'rounded-lg',
    'transition-all duration-200',
    'focus:outline-none focus:ring-2 focus:ring-offset-2',
    props.fullWidth ? 'w-full' : '',
    props.disabled || props.loading ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
  ]

  // Size classes
  const sizeClasses = {
    xs: 'px-2.5 py-1.5 text-xs',
    sm: 'px-3 py-2 text-sm',
    md: 'px-4 py-2 text-base',
    lg: 'px-6 py-3 text-lg',
    xl: 'px-8 py-4 text-xl'
  }

  // Variant classes
  const variantClasses = {
    primary: `
      bg-indigo-600 hover:bg-indigo-700
      text-white
      focus:ring-indigo-500
      shadow-md hover:shadow-lg
    `,
    secondary: `
      bg-white hover:bg-gray-50
      text-gray-700
      border border-gray-300
      focus:ring-indigo-500
      dark:bg-gray-800 dark:hover:bg-gray-700
      dark:text-gray-300 dark:border-gray-600
    `,
    danger: `
      bg-red-600 hover:bg-red-700
      text-white
      focus:ring-red-500
      shadow-md hover:shadow-lg
    `,
    text: `
      text-indigo-600 hover:text-indigo-700
      hover:bg-indigo-50
      focus:ring-indigo-500
      dark:text-indigo-400 dark:hover:bg-indigo-900/20
    `,
    ai: `
      bg-gradient-to-r from-indigo-600 to-purple-600
      hover:from-indigo-700 hover:to-purple-700
      text-white font-semibold
      focus:ring-indigo-500
      shadow-lg hover:shadow-xl
      relative overflow-hidden
      before:absolute before:inset-0
      before:bg-gradient-to-r before:from-transparent before:via-white/10 before:to-transparent
      before:-translate-x-full hover:before:translate-x-full
      before:transition-transform before:duration-700
    `
  }

  return [
    ...baseClasses,
    sizeClasses[props.size],
    variantClasses[props.variant]
  ].join(' ')
})

const handleClick = (event: MouseEvent) => {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}
</script>
```

### 使用示例
```vue
<template>
  <div class="space-y-4">
    <!-- Primary Button -->
    <Button variant="primary" @click="handleSave">
      保存
    </Button>

    <!-- Secondary Button -->
    <Button variant="secondary" @click="handleCancel">
      取消
    </Button>

    <!-- Danger Button -->
    <Button variant="danger" @click="handleDelete">
      删除
    </Button>

    <!-- Text Button -->
    <Button variant="text">
      查看更多
    </Button>

    <!-- AI Button -->
    <Button variant="ai" icon="SparklesIcon">
      AI 智能推荐
    </Button>

    <!-- Loading State -->
    <Button :loading="true">
      加载中...
    </Button>

    <!-- Disabled State -->
    <Button :disabled="true">
      禁用状态
    </Button>

    <!-- Full Width -->
    <Button :full-width="true">
      全宽按钮
    </Button>
  </div>
</template>

<script setup lang="ts">
import Button from '@/components/ui/Button'
import { SparklesIcon } from '@heroicons/vue/24/solid'

const handleSave = () => console.log('Save clicked')
const handleCancel = () => console.log('Cancel clicked')
const handleDelete = () => console.log('Delete clicked')
</script>
```

---

## 2. 卡片组件 (Card)

### Card.vue - 基础卡片
```vue
<template>
  <div
    :class="cardClasses"
    @click="handleClick"
  >
    <!-- Header Slot -->
    <div v-if="$slots.header" class="border-b border-gray-200 dark:border-gray-700 px-6 py-4">
      <slot name="header" />
    </div>

    <!-- Body -->
    <div :class="bodyClasses">
      <slot />
    </div>

    <!-- Footer Slot -->
    <div v-if="$slots.footer" class="border-t border-gray-200 dark:border-gray-700 px-6 py-4">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface CardProps {
  hoverable?: boolean
  bordered?: boolean
  noPadding?: boolean
  shadow?: 'none' | 'sm' | 'md' | 'lg' | 'xl'
  clickable?: boolean
}

const props = withDefaults(defineProps<CardProps>(), {
  hoverable: false,
  bordered: false,
  noPadding: false,
  shadow: 'md',
  clickable: false
})

const emit = defineEmits<{
  click: []
}>()

const cardClasses = computed(() => [
  'bg-white dark:bg-gray-800',
  'rounded-lg',
  'transition-all duration-300',
  props.bordered ? 'border border-gray-200 dark:border-gray-700' : '',
  props.clickable ? 'cursor-pointer' : '',
  props.hoverable ? 'hover:shadow-lg hover:-translate-y-1' : '',
  {
    'shadow-none': props.shadow === 'none',
    'shadow-sm': props.shadow === 'sm',
    'shadow-md': props.shadow === 'md',
    'shadow-lg': props.shadow === 'lg',
    'shadow-xl': props.shadow === 'xl'
  }
])

const bodyClasses = computed(() => [
  props.noPadding ? '' : 'p-6'
])

const handleClick = () => {
  if (props.clickable) {
    emit('click')
  }
}
</script>
```

### StatCard.vue - 统计卡片
```vue
<template>
  <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border-l-4"
       :class="borderColorClass">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <!-- Icon -->
      <div class="p-3 rounded-full" :class="iconBgClass">
        <component :is="icon" class="w-8 h-8" :class="iconColorClass" />
      </div>

      <!-- Trend -->
      <span v-if="trend" class="text-sm font-medium flex items-center gap-1"
            :class="trendColorClass">
        <component :is="trendIcon" class="w-4 h-4" />
        {{ Math.abs(trend) }}%
      </span>
    </div>

    <!-- Content -->
    <div>
      <p class="text-sm text-gray-500 dark:text-gray-400 mb-1">
        {{ label }}
      </p>
      <p class="text-3xl font-bold text-gray-900 dark:text-white">
        {{ formatValue(value) }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowUpIcon, ArrowDownIcon } from '@heroicons/vue/24/solid'

interface StatCardProps {
  label: string
  value: number | string
  icon: any
  color?: 'indigo' | 'green' | 'amber' | 'red'
  trend?: number
  formatter?: (value: number | string) => string
}

const props = withDefaults(defineProps<StatCardProps>(), {
  color: 'indigo'
})

const colorClasses = {
  indigo: {
    border: 'border-indigo-600',
    iconBg: 'bg-indigo-100 dark:bg-indigo-900/30',
    iconColor: 'text-indigo-600 dark:text-indigo-400'
  },
  green: {
    border: 'border-green-600',
    iconBg: 'bg-green-100 dark:bg-green-900/30',
    iconColor: 'text-green-600 dark:text-green-400'
  },
  amber: {
    border: 'border-amber-600',
    iconBg: 'bg-amber-100 dark:bg-amber-900/30',
    iconColor: 'text-amber-600 dark:text-amber-400'
  },
  red: {
    border: 'border-red-600',
    iconBg: 'bg-red-100 dark:bg-red-900/30',
    iconColor: 'text-red-600 dark:text-red-400'
  }
}

const borderColorClass = computed(() => colorClasses[props.color].border)
const iconBgClass = computed(() => colorClasses[props.color].iconBg)
const iconColorClass = computed(() => colorClasses[props.color].iconColor)

const trendIcon = computed(() => {
  if (!props.trend) return null
  return props.trend > 0 ? ArrowUpIcon : ArrowDownIcon
})

const trendColorClass = computed(() => {
  if (!props.trend) return ''
  return props.trend > 0
    ? 'text-green-600 dark:text-green-400'
    : 'text-red-600 dark:text-red-400'
})

const formatValue = (value: number | string) => {
  if (props.formatter) {
    return props.formatter(value)
  }
  return value.toLocaleString()
}
</script>
```

### AICard.vue - AI 推荐卡片
```vue
<template>
  <div class="relative overflow-hidden rounded-xl bg-gradient-to-br from-indigo-50 to-purple-50 dark:from-indigo-900/20 dark:to-purple-900/20 border border-indigo-200 dark:border-indigo-800 p-6">
    <!-- Background Decoration -->
    <div class="absolute top-0 right-0 w-64 h-64 bg-gradient-radial from-indigo-400/10 to-transparent rounded-full blur-3xl" />

    <!-- AI Badge -->
    <div class="absolute top-4 right-4">
      <span class="px-3 py-1 bg-gradient-to-r from-indigo-600 to-purple-600 text-white text-xs font-semibold rounded-full flex items-center gap-1">
        <SparklesIcon class="w-3 h-3" />
        {{ badgeText }}
      </span>
    </div>

    <!-- Content -->
    <div class="relative z-10">
      <!-- Title -->
      <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-2">
        {{ title }}
      </h3>

      <!-- Description -->
      <p class="text-gray-600 dark:text-gray-300 text-sm mb-4">
        {{ description }}
      </p>

      <!-- Slot for content -->
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { SparklesIcon } from '@heroicons/vue/24/solid'

interface AICardProps {
  title: string
  description: string
  badgeText?: string
}

withDefaults(defineProps<AICardProps>(), {
  badgeText: 'AI驱动'
})
</script>
```

---

## 3. 表格组件 (Table)

### Table.vue
```vue
<template>
  <div class="overflow-x-auto">
    <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
      <!-- Header -->
      <thead class="bg-gray-50 dark:bg-gray-800">
        <tr>
          <th
            v-for="column in columns"
            :key="column.key"
            :class="getHeaderClass(column)"
            @click="handleSort(column)"
          >
            <div class="flex items-center gap-2">
              <span>{{ column.label }}</span>

              <!-- Sort Icons -->
              <template v-if="column.sortable">
                <ArrowUpIcon
                  v-if="sortColumn === column.key && sortOrder === 'asc'"
                  class="w-4 h-4"
                />
                <ArrowDownIcon
                  v-else-if="sortColumn === column.key && sortOrder === 'desc'"
                  class="w-4 h-4"
                />
                <ChevronUpDownIcon v-else class="w-4 h-4 opacity-50" />
              </template>
            </div>
          </th>
        </tr>
      </thead>

      <!-- Body -->
      <tbody class="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
        <tr
          v-for="(row, index) in paginatedData"
          :key="index"
          class="hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
          :class="{ 'cursor-pointer': rowClickable }"
          @click="handleRowClick(row)"
        >
          <td
            v-for="column in columns"
            :key="column.key"
            class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-gray-300"
          >
            <!-- Custom Cell Renderer -->
            <slot
              v-if="$slots[`cell-${column.key}`]"
              :name="`cell-${column.key}`"
              :row="row"
              :value="row[column.key]"
            />

            <!-- Default Cell -->
            <template v-else>
              {{ row[column.key] }}
            </template>
          </td>
        </tr>

        <!-- Empty State -->
        <tr v-if="paginatedData.length === 0">
          <td :colspan="columns.length" class="px-6 py-12 text-center">
            <div class="flex flex-col items-center justify-center text-gray-400">
              <svg class="w-16 h-16 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
              </svg>
              <p class="text-lg font-medium">暂无数据</p>
              <p class="text-sm mt-1">{{ emptyText }}</p>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Pagination -->
    <div v-if="pagination" class="border-t border-gray-200 dark:border-gray-700 px-4 py-3">
      <Pagination
        v-model:current="currentPage"
        :total="filteredData.length"
        :page-size="pageSize"
        @change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ArrowUpIcon, ArrowDownIcon, ChevronUpDownIcon } from '@heroicons/vue/24/outline'
import Pagination from './Pagination.vue'

interface Column {
  key: string
  label: string
  sortable?: boolean
  align?: 'left' | 'center' | 'right'
  width?: string
}

interface TableProps {
  data: any[]
  columns: Column[]
  pagination?: boolean
  pageSize?: number
  rowClickable?: boolean
  emptyText?: string
}

const props = withDefaults(defineProps<TableProps>(), {
  pagination: true,
  pageSize: 10,
  rowClickable: false,
  emptyText: '暂时没有数据'
})

const emit = defineEmits<{
  rowClick: [row: any]
  sort: [column: string, order: 'asc' | 'desc']
}>()

const sortColumn = ref<string | null>(null)
const sortOrder = ref<'asc' | 'desc'>('asc')
const currentPage = ref(1)

const filteredData = computed(() => {
  let data = [...props.data]

  // Sorting
  if (sortColumn.value) {
    data.sort((a, b) => {
      const aVal = a[sortColumn.value!]
      const bVal = b[sortColumn.value!]
      const comparison = aVal > bVal ? 1 : aVal < bVal ? -1 : 0
      return sortOrder.value === 'asc' ? comparison : -comparison
    })
  }

  return data
})

const paginatedData = computed(() => {
  if (!props.pagination) return filteredData.value

  const start = (currentPage.value - 1) * props.pageSize
  const end = start + props.pageSize
  return filteredData.value.slice(start, end)
})

const getHeaderClass = (column: Column) => [
  'px-6 py-3',
  'text-xs font-medium uppercase tracking-wider',
  'text-gray-500 dark:text-gray-400',
  column.sortable ? 'cursor-pointer select-none hover:text-gray-700 dark:hover:text-gray-200' : '',
  {
    'text-left': column.align === 'left' || !column.align,
    'text-center': column.align === 'center',
    'text-right': column.align === 'right'
  }
]

const handleSort = (column: Column) => {
  if (!column.sortable) return

  if (sortColumn.value === column.key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortColumn.value = column.key
    sortOrder.value = 'asc'
  }

  emit('sort', column.key, sortOrder.value)
}

const handleRowClick = (row: any) => {
  if (props.rowClickable) {
    emit('rowClick', row)
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
}
</script>
```

---

## 4. 表单组件 (Form)

### Input.vue
```vue
<template>
  <div class="mb-4">
    <!-- Label -->
    <label
      v-if="label"
      :for="inputId"
      class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
    >
      {{ label }}
      <span v-if="required" class="text-red-600">*</span>
    </label>

    <!-- Input Container -->
    <div class="relative">
      <!-- Prefix Icon -->
      <div v-if="prefixIcon" class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
        <component :is="prefixIcon" class="h-5 w-5 text-gray-400" />
      </div>

      <!-- Input -->
      <input
        :id="inputId"
        v-model="inputValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :class="inputClasses"
        @blur="handleBlur"
        @focus="handleFocus"
        @input="handleInput"
      />

      <!-- Suffix Icon -->
      <div v-if="suffixIcon" class="absolute inset-y-0 right-0 pr-3 flex items-center">
        <component :is="suffixIcon" class="h-5 w-5 text-gray-400 cursor-pointer" @click="handleSuffixClick" />
      </div>
    </div>

    <!-- Helper Text -->
    <p v-if="helperText && !error" class="mt-1 text-sm text-gray-500 dark:text-gray-400">
      {{ helperText }}
    </p>

    <!-- Error Message -->
    <p v-if="error" class="mt-1 text-sm text-red-600 dark:text-red-400">
      {{ error }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface InputProps {
  modelValue: string | number
  label?: string
  type?: string
  placeholder?: string
  disabled?: boolean
  readonly?: boolean
  required?: boolean
  error?: string
  helperText?: string
  prefixIcon?: any
  suffixIcon?: any
}

const props = withDefaults(defineProps<InputProps>(), {
  type: 'text',
  disabled: false,
  readonly: false,
  required: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  blur: []
  focus: []
  suffixClick: []
}>()

const inputId = ref(`input-${Math.random().toString(36).substr(2, 9)}`)
const isFocused = ref(false)

const inputValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const inputClasses = computed(() => [
  'w-full',
  'px-4 py-2',
  props.prefixIcon ? 'pl-10' : '',
  props.suffixIcon ? 'pr-10' : '',
  'border',
  'rounded-lg',
  'bg-white dark:bg-gray-800',
  'text-gray-900 dark:text-white',
  'placeholder-gray-400 dark:placeholder-gray-500',
  'transition-colors',
  'focus:outline-none focus:ring-2',
  props.error
    ? 'border-red-300 dark:border-red-600 focus:ring-red-500 focus:border-transparent'
    : 'border-gray-300 dark:border-gray-600 focus:ring-indigo-500 focus:border-transparent',
  props.disabled ? 'opacity-50 cursor-not-allowed bg-gray-100 dark:bg-gray-900' : '',
  props.readonly ? 'cursor-default' : ''
])

const handleBlur = () => {
  isFocused.value = false
  emit('blur')
}

const handleFocus = () => {
  isFocused.value = true
  emit('focus')
}

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
}

const handleSuffixClick = () => {
  emit('suffixClick')
}
</script>
```

---

## 5. Modal 组件

### Modal.vue
```vue
<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition ease-out duration-300"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition ease-in duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 overflow-y-auto"
        @click="handleOverlayClick"
      >
        <!-- Overlay -->
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75 dark:bg-gray-900 dark:bg-opacity-75 transition-opacity" />

        <!-- Modal Container -->
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <Transition
            enter-active-class="transition ease-out duration-300"
            enter-from-class="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
            enter-to-class="opacity-100 translate-y-0 sm:scale-100"
            leave-active-class="transition ease-in duration-200"
            leave-from-class="opacity-100 translate-y-0 sm:scale-100"
            leave-to-class="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
          >
            <div
              v-if="modelValue"
              class="relative transform overflow-hidden rounded-lg bg-white dark:bg-gray-800 px-4 pb-4 pt-5 text-left shadow-xl transition-all sm:my-8 sm:w-full sm:p-6"
              :class="sizeClass"
              @click.stop
            >
              <!-- Close Button -->
              <button
                v-if="showClose"
                class="absolute top-4 right-4 text-gray-400 hover:text-gray-500 dark:hover:text-gray-300"
                @click="handleClose"
              >
                <XMarkIcon class="w-6 h-6" />
              </button>

              <!-- Title -->
              <div v-if="title || $slots.title" class="mb-4">
                <slot name="title">
                  <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                    {{ title }}
                  </h3>
                </slot>
              </div>

              <!-- Content -->
              <div class="mb-6">
                <slot />
              </div>

              <!-- Footer -->
              <div v-if="$slots.footer" class="flex justify-end gap-3">
                <slot name="footer" />
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { XMarkIcon } from '@heroicons/vue/24/outline'

interface ModalProps {
  modelValue: boolean
  title?: string
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
  showClose?: boolean
  closeOnOverlay?: boolean
}

const props = withDefaults(defineProps<ModalProps>(), {
  size: 'md',
  showClose: true,
  closeOnOverlay: true
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
}>()

const sizeClass = computed(() => ({
  'sm:max-w-sm': props.size === 'sm',
  'sm:max-w-lg': props.size === 'md',
  'sm:max-w-2xl': props.size === 'lg',
  'sm:max-w-4xl': props.size === 'xl',
  'sm:max-w-full sm:mx-4': props.size === 'full'
}))

const handleClose = () => {
  emit('update:modelValue', false)
  emit('close')
}

const handleOverlayClick = () => {
  if (props.closeOnOverlay) {
    handleClose()
  }
}
</script>
```

---

## 6. AI 特色组件

### AIBadge.vue
```vue
<template>
  <span
    class="inline-flex items-center gap-1 px-3 py-1 text-xs font-semibold rounded-full"
    :class="badgeClasses"
  >
    <SparklesIcon class="w-3 h-3" />
    <slot>AI</slot>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { SparklesIcon } from '@heroicons/vue/24/solid'

interface AIBadgeProps {
  variant?: 'gradient' | 'solid' | 'outline'
}

const props = withDefaults(defineProps<AIBadgeProps>(), {
  variant: 'gradient'
})

const badgeClasses = computed(() => {
  const variants = {
    gradient: 'bg-gradient-to-r from-indigo-600 to-purple-600 text-white',
    solid: 'bg-indigo-600 text-white',
    outline: 'border-2 border-indigo-600 text-indigo-600 dark:text-indigo-400'
  }
  return variants[props.variant]
})
</script>
```

### AIChatBox.vue
```vue
<template>
  <div class="flex flex-col h-full bg-white dark:bg-gray-800 rounded-lg shadow-lg overflow-hidden">
    <!-- Header -->
    <div class="bg-gradient-to-r from-indigo-600 to-purple-600 text-white p-4 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="relative">
          <div class="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
            <SparklesIcon class="w-6 h-6" />
          </div>
          <div class="absolute bottom-0 right-0 w-3 h-3 bg-green-400 rounded-full border-2 border-white" />
        </div>
        <div>
          <h3 class="font-semibold">AI 图书馆助手</h3>
          <p class="text-xs opacity-90">在线</p>
        </div>
      </div>
      <button @click="handleClose" class="hover:bg-white/10 rounded-lg p-1">
        <XMarkIcon class="w-6 h-6" />
      </button>
    </div>

    <!-- Messages -->
    <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 space-y-4">
      <div
        v-for="(message, index) in messages"
        :key="index"
        class="flex gap-3"
        :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
      >
        <!-- Avatar -->
        <div
          v-if="message.role === 'assistant'"
          class="w-8 h-8 bg-gradient-to-br from-indigo-600 to-purple-600 rounded-full flex items-center justify-center flex-shrink-0"
        >
          <SparklesIcon class="w-5 h-5 text-white" />
        </div>

        <!-- Message Bubble -->
        <div
          class="max-w-[70%] rounded-lg p-3"
          :class="message.role === 'user'
            ? 'bg-indigo-600 text-white'
            : 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white'"
        >
          <p class="text-sm whitespace-pre-wrap">{{ message.content }}</p>
          <p class="text-xs opacity-70 mt-1">
            {{ formatTime(message.timestamp) }}
          </p>
        </div>

        <!-- User Avatar -->
        <div
          v-if="message.role === 'user'"
          class="w-8 h-8 bg-gray-300 dark:bg-gray-600 rounded-full flex items-center justify-center flex-shrink-0"
        >
          <UserIcon class="w-5 h-5 text-gray-600 dark:text-gray-300" />
        </div>
      </div>

      <!-- Typing Indicator -->
      <div v-if="isTyping" class="flex gap-3">
        <div class="w-8 h-8 bg-gradient-to-br from-indigo-600 to-purple-600 rounded-full flex items-center justify-center">
          <SparklesIcon class="w-5 h-5 text-white" />
        </div>
        <div class="bg-gray-100 dark:bg-gray-700 rounded-lg p-3">
          <div class="flex gap-1">
            <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0ms" />
            <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 150ms" />
            <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 300ms" />
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="px-4 py-2 border-t border-gray-200 dark:border-gray-700">
      <div class="flex gap-2 flex-wrap">
        <button
          v-for="(action, index) in quickActions"
          :key="index"
          class="px-3 py-1 text-sm bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-full transition-colors"
          @click="handleQuickAction(action)"
        >
          {{ action }}
        </button>
      </div>
    </div>

    <!-- Input -->
    <div class="p-4 border-t border-gray-200 dark:border-gray-700">
      <div class="flex gap-2">
        <input
          v-model="inputMessage"
          type="text"
          placeholder="输入消息..."
          class="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
          @keyup.enter="handleSend"
        />
        <button
          class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!inputMessage.trim()"
          @click="handleSend"
        >
          <PaperAirplaneIcon class="w-5 h-5" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { SparklesIcon, XMarkIcon, UserIcon, PaperAirplaneIcon } from '@heroicons/vue/24/solid'

interface Message {
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

const emit = defineEmits<{
  close: []
}>()

const messages = ref<Message[]>([
  {
    role: 'assistant',
    content: '您好！我是AI图书馆助手，有什么可以帮您的吗？',
    timestamp: new Date()
  }
])

const quickActions = [
  '如何借书？',
  '查询借阅记录',
  '推荐新书',
  '催还提醒'
]

const inputMessage = ref('')
const isTyping = ref(false)
const messagesContainer = ref<HTMLElement>()

const handleSend = async () => {
  if (!inputMessage.value.trim()) return

  // Add user message
  messages.value.push({
    role: 'user',
    content: inputMessage.value,
    timestamp: new Date()
  })

  const userInput = inputMessage.value
  inputMessage.value = ''

  // Scroll to bottom
  await nextTick()
  scrollToBottom()

  // Simulate AI response
  isTyping.value = true
  setTimeout(() => {
    messages.value.push({
      role: 'assistant',
      content: `您好！关于"${userInput}"，让我为您解答...（这里是模拟的AI回复）`,
      timestamp: new Date()
    })
    isTyping.value = false
    nextTick(() => scrollToBottom())
  }, 1500)
}

const handleQuickAction = (action: string) => {
  inputMessage.value = action
  handleSend()
}

const handleClose = () => {
  emit('close')
}

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}
</script>
```

---

## 使用指南

### 1. 安装依赖

```bash
npm install @heroicons/vue
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

### 2. 配置 Tailwind

```javascript
// tailwind.config.js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        'ai-primary': '#667eea',
        'ai-secondary': '#764ba2',
      }
    },
  },
  plugins: [],
}
```

### 3. 全局注册组件

```typescript
// src/components/index.ts
import type { App } from 'vue'
import Button from './ui/Button/Button.vue'
import Card from './ui/Card/Card.vue'
import StatCard from './ui/Card/StatCard.vue'
import AICard from './ui/Card/AICard.vue'
import Table from './ui/Table/Table.vue'
import Input from './ui/Form/Input.vue'
import Modal from './ui/Modal/Modal.vue'

export function registerComponents(app: App) {
  app.component('Button', Button)
  app.component('Card', Card)
  app.component('StatCard', StatCard)
  app.component('AICard', AICard)
  app.component('Table', Table)
  app.component('Input', Input)
  app.component('Modal', Modal)
}
```

```typescript
// src/main.ts
import { createApp } from 'vue'
import App from './App.vue'
import { registerComponents } from './components'
import './assets/tailwind.css'

const app = createApp(App)
registerComponents(app)
app.mount('#app')
```

---

## 完整页面示例

见 `FRONTEND_DESIGN_SPEC_V2.md` 中的完整工作台页面示例。

---

**版本**: v1.0
**更新日期**: 2025-01-14
**技术栈**: Vue 3 + TypeScript + Tailwind CSS
**维护团队**: 国创睿峰前端团队
