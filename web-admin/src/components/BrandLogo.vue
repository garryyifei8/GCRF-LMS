<template>
  <div class="brand-logo" :class="{ 'is-mono': mono, 'is-stacked': stacked }">
    <!-- 自定义 Logo 优先 -->
    <img
      v-if="brand.logoUrl"
      :src="brand.logoUrl"
      :alt="brand.name"
      class="brand-logo__img"
      :style="{ width: size + 'px', height: size + 'px' }"
    />

    <!-- 内置 SVG：开卷+山峰，国风方印底 -->
    <svg
      v-else
      class="brand-logo__svg"
      :width="size"
      :height="size"
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      :aria-label="brand.name"
    >
      <!-- 印章红方底 + 渐变 -->
      <defs>
        <linearGradient
          :id="`${gid}-bg`"
          x1="0"
          y1="0"
          x2="64"
          y2="64"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0%" stop-color="#5D87FF" />
          <stop offset="100%" stop-color="#3759C7" />
        </linearGradient>
        <linearGradient
          :id="`${gid}-peak`"
          x1="32"
          y1="14"
          x2="32"
          y2="46"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.95" />
          <stop offset="100%" stop-color="#E0E8FF" stop-opacity="0.85" />
        </linearGradient>
      </defs>

      <!-- 圆角方印底 -->
      <rect x="0" y="0" width="64" height="64" rx="14" :fill="`url(#${gid}-bg)`" />

      <!-- 远山（虚） -->
      <path d="M6 44 L18 30 L24 36 L34 22 L44 32 L52 26 L58 44 Z" fill="#FFFFFF" opacity="0.18" />

      <!-- 主峰（睿峰）+ 中央书脊 -->
      <path d="M14 46 L24 30 L32 38 L40 28 L50 46 Z" :fill="`url(#${gid}-peak)`" />

      <!-- 开卷书页 - 左 -->
      <path d="M16 46 Q22 40 32 42 L32 54 Q22 50 16 54 Z" fill="#FFFFFF" opacity="0.95" />
      <!-- 开卷书页 - 右 -->
      <path d="M48 46 Q42 40 32 42 L32 54 Q42 50 48 54 Z" fill="#FFFFFF" opacity="0.85" />
      <!-- 中央书脊 -->
      <line
        x1="32"
        y1="42"
        x2="32"
        y2="54"
        stroke="#3759C7"
        stroke-width="1.2"
        stroke-linecap="round"
      />

      <!-- 印章红角点 -->
      <circle cx="51" cy="13" r="3.5" fill="#E04B3F" />
    </svg>

    <div v-if="!iconOnly" class="brand-logo__text">
      <div class="brand-logo__title">{{ brand.name }}</div>
      <div v-if="brand.subtitle" class="brand-logo__subtitle">{{ brand.subtitle }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useBrandStore } from '@/stores/brand'

const props = defineProps({
  size: { type: Number, default: 36 },
  iconOnly: { type: Boolean, default: false },
  stacked: { type: Boolean, default: false },
  mono: { type: Boolean, default: false }
})

const brandStore = useBrandStore()
const brand = computed(() => brandStore.brand)
const gid = computed(() => `gcrf-${Math.random().toString(36).slice(2, 8)}`)
</script>

<style lang="scss" scoped>
.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 12px;

  &.is-stacked {
    flex-direction: column;
    gap: 8px;
    align-items: center;
  }

  &__img,
  &__svg {
    flex-shrink: 0;
    border-radius: 12px;
    box-shadow: 0 6px 16px rgba(93, 135, 255, 0.25);
  }

  &__text {
    display: flex;
    flex-direction: column;
    line-height: 1.2;
  }

  &__title {
    font-size: 18px;
    font-weight: 700;
    color: #1f2937;
    letter-spacing: 1px;
    font-family: 'PingFang SC', 'Source Han Sans', 'Noto Sans CJK SC', sans-serif;
  }

  &__subtitle {
    font-size: 11px;
    color: #6b7280;
    margin-top: 2px;
    letter-spacing: 0.5px;
  }

  &.is-mono {
    .brand-logo__title {
      color: #fff;
    }
    .brand-logo__subtitle {
      color: rgba(255, 255, 255, 0.75);
    }
  }
}
</style>
