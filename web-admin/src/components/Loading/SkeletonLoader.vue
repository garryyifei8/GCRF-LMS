<template>
  <div class="skeleton-loader">
    <!-- 表格骨架屏 -->
    <template v-if="type === 'table'">
      <div class="skeleton-table">
        <!-- 表头 -->
        <div class="skeleton-table-header">
          <div v-for="i in columns" :key="`header-${i}`" class="skeleton-table-header-cell">
            <div class="skeleton-block" :style="{ width: getRandomWidth(60, 90) }" />
          </div>
        </div>
        <!-- 表格行 -->
        <div v-for="i in rows" :key="`row-${i}`" class="skeleton-table-row">
          <div v-for="j in columns" :key="`cell-${i}-${j}`" class="skeleton-table-cell">
            <div class="skeleton-block" :style="{ width: getRandomWidth(40, 85) }" />
          </div>
        </div>
      </div>
    </template>

    <!-- 卡片骨架屏 -->
    <template v-else-if="type === 'card'">
      <div class="skeleton-card">
        <div class="skeleton-card-header">
          <div class="skeleton-avatar" />
          <div class="skeleton-card-title">
            <div class="skeleton-block" style="width: 60%" />
            <div class="skeleton-block" style="width: 40%; margin-top: 8px" />
          </div>
        </div>
        <div class="skeleton-card-content">
          <div
            v-for="i in 3"
            :key="`card-line-${i}`"
            class="skeleton-block"
            style="margin-bottom: 12px"
          />
        </div>
      </div>
    </template>

    <!-- 表单骨架屏 -->
    <template v-else-if="type === 'form'">
      <div class="skeleton-form">
        <div v-for="i in rows" :key="`form-${i}`" class="skeleton-form-item">
          <div class="skeleton-form-label">
            <div class="skeleton-block" style="width: 80px" />
          </div>
          <div class="skeleton-form-control">
            <div class="skeleton-block" />
          </div>
        </div>
      </div>
    </template>

    <!-- 列表骨架屏 -->
    <template v-else-if="type === 'list'">
      <div class="skeleton-list">
        <div v-for="i in rows" :key="`list-${i}`" class="skeleton-list-item">
          <div class="skeleton-avatar" />
          <div class="skeleton-list-content">
            <div class="skeleton-block" style="width: 70%; margin-bottom: 8px" />
            <div class="skeleton-block" style="width: 50%" />
          </div>
        </div>
      </div>
    </template>

    <!-- 自定义骨架屏 -->
    <template v-else>
      <slot />
    </template>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  // 骨架屏类型: table, card, form, list, custom
  type: {
    type: String,
    default: 'table',
    validator: (value) => ['table', 'card', 'form', 'list', 'custom'].includes(value)
  },
  // 行数
  rows: {
    type: Number,
    default: 5
  },
  // 列数（仅table类型有效）
  columns: {
    type: Number,
    default: 6
  },
  // 是否显示动画
  animated: {
    type: Boolean,
    default: true
  }
})

// 生成随机宽度（百分比）
const getRandomWidth = (min, max) => {
  return `${Math.floor(Math.random() * (max - min + 1) + min)}%`
}
</script>

<style lang="scss" scoped>
.skeleton-loader {
  width: 100%;
}

// 通用骨架块样式
.skeleton-block {
  height: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  border-radius: 4px;
  animation: skeleton-loading 1.5s ease-in-out infinite;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

// 表格骨架屏
.skeleton-table {
  width: 100%;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  overflow: hidden;

  &-header {
    display: flex;
    background: #fafafa;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;

    &-cell {
      flex: 1;
      padding: 0 8px;
    }
  }

  &-row {
    display: flex;
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }
  }

  &-cell {
    flex: 1;
    padding: 0 8px;
  }
}

// 卡片骨架屏
.skeleton-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;

  &-header {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
  }

  &-title {
    flex: 1;
    margin-left: 12px;
  }

  &-content {
    .skeleton-block {
      height: 14px;
    }
  }
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s ease-in-out infinite;
}

// 表单骨架屏
.skeleton-form {
  &-item {
    display: flex;
    align-items: center;
    margin-bottom: 24px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  &-label {
    width: 120px;
    padding-right: 12px;
  }

  &-control {
    flex: 1;

    .skeleton-block {
      height: 32px;
    }
  }
}

// 列表骨架屏
.skeleton-list {
  &-item {
    display: flex;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }
  }

  &-content {
    flex: 1;
    margin-left: 12px;

    .skeleton-block {
      height: 14px;
    }
  }
}
</style>
