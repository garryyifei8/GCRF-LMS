<template>
  <div class="ai-card" @click="handleClick">
    <!-- AI 徽章 -->
    <div v-if="showAIBadge" class="ai-badge-container">
      <span class="ai-badge">
        <el-icon :size="12"><MagicStick /></el-icon>
        <span>AI 推荐</span>
      </span>
    </div>

    <!-- 图片区域 -->
    <div v-if="image" class="ai-card-image">
      <img :src="image" :alt="title" />
      <!-- 推荐分数 -->
      <div v-if="score !== undefined" class="ai-score">
        <div class="ai-score-circle">
          <span class="ai-score-value">{{ score }}</span>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="ai-card-content">
      <h3 class="ai-card-title">{{ title }}</h3>
      <p v-if="description" class="ai-card-description">
        {{ description }}
      </p>

      <!-- 标签列表 -->
      <div v-if="tags && tags.length > 0" class="ai-card-tags">
        <span
          v-for="tag in tags"
          :key="tag"
          class="ai-card-tag"
        >
          {{ tag }}
        </span>
      </div>

      <!-- 操作按钮插槽 -->
      <div v-if="$slots.actions" class="ai-card-actions">
        <slot name="actions"></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { MagicStick } from '@element-plus/icons-vue'
import type { AICardProps, AICardEmits } from './types'

// Props
const props = withDefaults(defineProps<AICardProps>(), {
  showAIBadge: true
})

// Emits
const emit = defineEmits<AICardEmits>()

// 处理点击事件
const handleClick = (event: MouseEvent) => {
  emit('click', event)
}
</script>

<style scoped>
.ai-card {
  @apply relative bg-white dark:bg-gray-800;
  @apply rounded-xl shadow-sm border border-gray-200 dark:border-gray-700;
  @apply overflow-hidden cursor-pointer;
  @apply transition-all duration-300;
}

.ai-card:hover {
  @apply shadow-lg -translate-y-1;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.2);
}

/* AI 徽章 */
.ai-badge-container {
  @apply absolute top-4 right-4 z-10;
}

.ai-badge {
  @apply inline-flex items-center gap-1;
  @apply px-2 py-1 rounded-full text-xs font-medium;
  @apply bg-ai-gradient text-white;
  @apply shadow-lg;
}

/* 图片区域 */
.ai-card-image {
  @apply relative w-full h-48 overflow-hidden bg-gray-100 dark:bg-gray-900;
}

.ai-card-image img {
  @apply w-full h-full object-cover;
  @apply transition-transform duration-300;
}

.ai-card:hover .ai-card-image img {
  @apply scale-110;
}

/* AI 分数 */
.ai-score {
  @apply absolute bottom-4 left-4;
}

.ai-score-circle {
  @apply flex items-center justify-center;
  @apply w-14 h-14 rounded-full;
  @apply bg-white dark:bg-gray-800 shadow-lg;
  @apply border-4 border-ai-primary;
}

.ai-score-value {
  @apply text-xl font-bold text-ai-primary;
}

/* 内容区域 */
.ai-card-content {
  @apply p-6;
}

.ai-card-title {
  @apply text-lg font-semibold text-gray-900 dark:text-gray-100;
  @apply mb-2 line-clamp-2;
}

.ai-card-description {
  @apply text-sm text-gray-600 dark:text-gray-400;
  @apply mb-4 line-clamp-3;
}

/* 标签 */
.ai-card-tags {
  @apply flex flex-wrap gap-2 mb-4;
}

.ai-card-tag {
  @apply inline-flex items-center;
  @apply px-2 py-1 rounded-md text-xs font-medium;
  @apply bg-gray-100 dark:bg-gray-700;
  @apply text-gray-700 dark:text-gray-300;
}

/* 操作按钮区域 */
.ai-card-actions {
  @apply flex gap-2 pt-4 border-t border-gray-200 dark:border-gray-700;
}

/* 文本截断 */
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
