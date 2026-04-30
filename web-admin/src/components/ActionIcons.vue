<template>
  <div class="action-icons">
    <el-tooltip
      v-for="action in visibleActions"
      :key="action.key"
      :content="action.label"
      placement="top"
      :show-after="200"
    >
      <button
        class="action-icons__btn"
        :class="[`is-${action.variant || 'default'}`, { 'is-disabled': action.disabled }]"
        :disabled="action.disabled"
        :aria-label="action.label"
        @click.stop="handle(action)"
      >
        <el-icon :size="16">
          <component :is="action.icon" />
        </el-icon>
      </button>
    </el-tooltip>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  actions: {
    type: Array,
    required: true
    // [{ key, label, icon, variant?: 'default'|'primary'|'success'|'warning'|'danger', disabled?, hidden? }]
  }
})
const emit = defineEmits(['action'])

const visibleActions = computed(() => props.actions.filter((a) => !a.hidden))

function handle(action) {
  if (action.disabled) return
  emit('action', action.key)
}
</script>

<style lang="scss" scoped>
.action-icons {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  &__btn {
    width: 30px;
    height: 30px;
    border-radius: 8px;
    border: none;
    background: rgba(93, 135, 255, 0.08);
    color: #5d87ff;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition:
      background 0.18s ease,
      color 0.18s ease,
      transform 0.12s ease;

    &:hover {
      background: rgba(93, 135, 255, 0.18);
      transform: translateY(-1px);
    }
    &:active {
      transform: translateY(0);
    }

    &.is-primary {
      color: #5d87ff;
      background: rgba(93, 135, 255, 0.1);
      &:hover {
        background: rgba(93, 135, 255, 0.22);
      }
    }
    &.is-success {
      color: #13c296;
      background: rgba(19, 194, 150, 0.1);
      &:hover {
        background: rgba(19, 194, 150, 0.22);
      }
    }
    &.is-warning {
      color: #ffae1f;
      background: rgba(255, 174, 31, 0.1);
      &:hover {
        background: rgba(255, 174, 31, 0.22);
      }
    }
    &.is-danger {
      color: #fa896b;
      background: rgba(250, 137, 107, 0.1);
      &:hover {
        background: rgba(250, 137, 107, 0.22);
      }
    }

    &.is-disabled,
    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
      &:hover {
        transform: none;
        background: rgba(93, 135, 255, 0.08);
      }
    }
  }
}
</style>
