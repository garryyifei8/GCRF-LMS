<template>
  <div class="page-loading" :class="{ fullscreen }">
    <div class="page-loading-content">
      <!-- 自定义图标或默认spinner -->
      <div v-if="icon" class="page-loading-icon">
        <component :is="icon" class="custom-icon" />
      </div>
      <div v-else class="page-loading-spinner">
        <div class="spinner-ring" />
        <div class="spinner-ring" />
        <div class="spinner-ring" />
        <div class="spinner-ring" />
      </div>

      <!-- 加载文本 -->
      <div v-if="text" class="page-loading-text">{{ text }}</div>

      <!-- 加载提示 -->
      <div v-if="tip" class="page-loading-tip">{{ tip }}</div>
    </div>

    <!-- 遮罩层 -->
    <div v-if="mask" class="page-loading-mask" />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 是否全屏显示
  fullscreen: {
    type: Boolean,
    default: false
  },
  // 是否显示遮罩
  mask: {
    type: Boolean,
    default: true
  },
  // 加载文本
  text: {
    type: String,
    default: '加载中...'
  },
  // 提示信息
  tip: {
    type: String,
    default: ''
  },
  // 自定义图标组件
  icon: {
    type: [Object, String],
    default: null
  },
  // 背景色
  background: {
    type: String,
    default: 'rgba(255, 255, 255, 0.9)'
  }
})
</script>

<style lang="scss" scoped>
.page-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;

  &.fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
  }

  &-content {
    position: relative;
    z-index: 1001;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }

  &-spinner {
    position: relative;
    width: 64px;
    height: 64px;

    .spinner-ring {
      position: absolute;
      width: 100%;
      height: 100%;
      border: 4px solid transparent;
      border-top-color: #1890ff;
      border-radius: 50%;
      animation: spinner-rotate 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;

      &:nth-child(1) {
        animation-delay: -0.45s;
      }

      &:nth-child(2) {
        animation-delay: -0.3s;
      }

      &:nth-child(3) {
        animation-delay: -0.15s;
      }
    }
  }

  &-icon {
    font-size: 48px;
    color: #1890ff;

    .custom-icon {
      animation: icon-pulse 1.5s ease-in-out infinite;
    }
  }

  &-text {
    margin-top: 16px;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.65);
    font-weight: 500;
  }

  &-tip {
    margin-top: 8px;
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
  }

  &-mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: v-bind(background);
    z-index: 1000;
  }
}

@keyframes spinner-rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@keyframes icon-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(0.9);
  }
}
</style>
