<template>
  <div class="quick-actions">
    <el-tooltip content="图书借出" placement="left">
      <el-button
        type="primary"
        :icon="Reading"
        circle
        size="large"
        @click="goTo('/circulation/borrow')"
      />
    </el-tooltip>
    <el-tooltip content="图书归还" placement="left">
      <el-button
        type="success"
        :icon="Refresh"
        circle
        size="large"
        @click="goTo('/circulation/return')"
      />
    </el-tooltip>
    <el-tooltip content="读者办证" placement="left">
      <el-button
        type="warning"
        :icon="UserFilled"
        circle
        size="large"
        @click="goTo('/readers/card')"
      />
    </el-tooltip>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Reading, Refresh, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const goTo = (path) => router.push(path)
</script>

<style lang="scss" scoped>
.quick-actions {
  position: fixed;
  bottom: 32px;
  right: 32px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 12px;

  :deep(.el-button.is-circle) {
    width: 56px;
    height: 56px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    transition: transform 0.2s;
  }
  :deep(.el-button.is-circle:hover) {
    transform: scale(1.05);
  }
}
</style>
