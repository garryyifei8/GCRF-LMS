<template>
  <el-popover
    placement="bottom"
    :width="320"
    trigger="click"
    popper-class="message-center-popover"
    @show="fetchMessages"
  >
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
        <el-button :icon="Bell" circle class="message-btn" />
      </el-badge>
    </template>

    <div class="message-panel">
      <div class="message-panel-header">
        <span class="panel-title">消息通知</span>
        <span v-if="unreadCount > 0" class="unread-label">{{ unreadCount }} 条未读</span>
      </div>

      <el-scrollbar max-height="320px">
        <div v-if="loading" class="message-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <div v-else-if="messages.length === 0" class="message-empty">
          <el-icon><Bell /></el-icon>
          <span>暂无消息</span>
        </div>

        <div v-for="m in messages" :key="m.id" class="message-item" :class="{ unread: !m.isRead }">
          <div class="message-item-header">
            <span class="message-title">{{ m.title }}</span>
            <span v-if="!m.isRead" class="unread-dot" />
          </div>
          <div class="message-content">{{ m.content }}</div>
          <div class="message-footer">
            <span class="message-time">{{ formatTime(m.createdAt) }}</span>
            <el-button
              v-if="!m.isRead"
              link
              size="small"
              type="primary"
              @click.stop="markRead(m.id)"
            >
              标记已读
            </el-button>
          </div>
        </div>
      </el-scrollbar>

      <div class="message-panel-footer">
        <el-button link type="primary" @click="goToMessages">查看全部消息</el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMessages, getUnreadCount, markMessageRead } from '@/api/system'

const router = useRouter()
const userStore = useUserStore()

const messages = ref([])
const unreadCount = ref(0)
const loading = ref(false)

let pollTimer = null

async function fetchUnreadCount() {
  const userId = userStore.userInfo?.id
  if (!userId) return
  try {
    const res = await getUnreadCount(userId)
    if (res.code === 200) {
      unreadCount.value = res.data ?? 0
    }
  } catch {
    // silently fail for polling
  }
}

async function fetchMessages() {
  const userId = userStore.userInfo?.id
  if (!userId) return
  loading.value = true
  try {
    const res = await getMessages({ userId, pageNum: 1, pageSize: 10 })
    if (res.code === 200) {
      messages.value = res.data?.records ?? res.data?.list ?? []
    }
  } catch (err) {
    ElMessage.error('获取消息失败')
  } finally {
    loading.value = false
  }
}

async function markRead(id) {
  try {
    const res = await markMessageRead(id)
    if (res.code === 200) {
      const msg = messages.value.find((m) => m.id === id)
      if (msg) msg.isRead = true
      if (unreadCount.value > 0) unreadCount.value--
      ElMessage.success('已标记为已读')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diffMs = now - d
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour} 小时前`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay} 天前`
  return d.toLocaleDateString('zh-CN')
}

function goToMessages() {
  router.push({ name: 'ProfileMessages' })
}

onMounted(() => {
  fetchUnreadCount()
  pollTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style lang="scss" scoped>
.message-btn {
  font-size: 18px;
}

.message-panel {
  .message-panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 4px 0 12px;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 4px;

    .panel-title {
      font-size: 15px;
      font-weight: 600;
      color: #2a3547;
    }

    .unread-label {
      font-size: 12px;
      color: #5d87ff;
      background: rgba(93, 135, 255, 0.1);
      padding: 2px 8px;
      border-radius: 10px;
    }
  }

  .message-loading,
  .message-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 32px 0;
    color: #a1a5b7;
    font-size: 13px;

    .el-icon {
      font-size: 28px;
    }
  }

  .message-item {
    padding: 10px 4px;
    border-bottom: 1px solid #f5f5f5;
    cursor: default;
    transition: background 0.2s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #fafafa;
    }

    &.unread {
      background: rgba(93, 135, 255, 0.04);
    }

    .message-item-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 4px;

      .message-title {
        font-size: 13px;
        font-weight: 600;
        color: #2a3547;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .unread-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #5d87ff;
        flex-shrink: 0;
        margin-left: 6px;
      }
    }

    .message-content {
      font-size: 12px;
      color: #5a6a85;
      line-height: 1.5;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      margin-bottom: 6px;
    }

    .message-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .message-time {
        font-size: 11px;
        color: #a1a5b7;
      }
    }
  }

  .message-panel-footer {
    padding: 10px 0 2px;
    border-top: 1px solid #f0f0f0;
    text-align: center;
  }
}
</style>
