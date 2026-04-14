<template>
  <span class="button-loading" :class="{ small: size === 'small', large: size === 'large' }">
    <span class="button-loading-spinner">
      <i class="spinner-icon" />
    </span>
    <span v-if="text" class="button-loading-text">{{ text }}</span>
  </span>
</template>

<script setup>
const props = defineProps({
  // 加载文本
  text: {
    type: String,
    default: ''
  },
  // 大小
  size: {
    type: String,
    default: 'default',
    validator: (value) => ['small', 'default', 'large'].includes(value)
  }
})
</script>

<style lang="scss" scoped>
.button-loading {
  display: inline-flex;
  align-items: center;
  gap: 8px;

  &.small {
    gap: 6px;

    .button-loading-spinner {
      width: 12px;
      height: 12px;
    }

    .button-loading-text {
      font-size: 12px;
    }
  }

  &.large {
    gap: 10px;

    .button-loading-spinner {
      width: 18px;
      height: 18px;
    }

    .button-loading-text {
      font-size: 16px;
    }
  }

  &-spinner {
    display: inline-block;
    width: 14px;
    height: 14px;
    position: relative;

    .spinner-icon {
      display: block;
      width: 100%;
      height: 100%;
      border: 2px solid currentColor;
      border-top-color: transparent;
      border-radius: 50%;
      animation: button-spinner-rotate 0.8s linear infinite;
    }
  }

  &-text {
    font-size: 14px;
    line-height: 1;
  }
}

@keyframes button-spinner-rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
