<template>
  <div class="messages-page">
    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Bell /></el-icon>
            <span>消息中心</span>
            <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="header-badge" />
          </div>
          <el-button v-if="unreadCount > 0" size="small" :loading="markingAll" @click="markAllRead">
            全部标为已读
          </el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-radio-group v-model="filter" @change="onFilterChange">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="unread">未读</el-radio-button>
          <el-radio-button value="read">已读</el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading">
        <div v-if="messages.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无消息" :image-size="100" />
        </div>

        <div v-else>
          <div v-for="m in messages" :key="m.id" class="message-row" :class="{ unread: !m.isRead }">
            <div class="message-row-left">
              <span class="unread-indicator" :class="{ active: !m.isRead }" />
            </div>
            <div class="message-row-body">
              <div class="message-row-header">
                <span class="message-title">{{ m.title }}</span>
                <span class="message-time">{{ formatTime(m.createdAt) }}</span>
              </div>
              <div class="message-content">{{ m.content }}</div>
            </div>
            <div class="message-row-actions">
              <el-button v-if="!m.isRead" link size="small" type="primary" @click="markRead(m.id)">
                标记已读
              </el-button>
              <el-tag v-else type="info" size="small" effect="plain">已读</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="fetchMessages"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMessages, getUnreadCount, markMessageRead } from '@/api/system'

const userStore = useUserStore()

const messages = ref([])
const loading = ref(false)
const markingAll = ref(false)
const unreadCount = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filter = ref('all')

async function fetchMessages() {
  const userId = userStore.userInfo?.id
  if (!userId) return
  loading.value = true
  try {
    const params = { userId, pageNum: pageNum.value, pageSize: pageSize.value }
    if (filter.value === 'unread') params.isRead = false
    if (filter.value === 'read') params.isRead = true

    const res = await getMessages(params)
    if (res.code === 200) {
      messages.value = res.data?.records ?? res.data?.list ?? []
      total.value = res.data?.total ?? messages.value.length
    }
  } catch {
    ElMessage.error('获取消息失败')
  } finally {
    loading.value = false
  }
}

async function fetchUnreadCount() {
  const userId = userStore.userInfo?.id
  if (!userId) return
  try {
    const res = await getUnreadCount(userId)
    if (res.code === 200) unreadCount.value = res.data ?? 0
  } catch {
    // ignore
  }
}

async function markRead(id) {
  try {
    const res = await markMessageRead(id)
    if (res.code === 200) {
      const msg = messages.value.find((m) => m.id === id)
      if (msg) msg.isRead = true
      if (unreadCount.value > 0) unreadCount.value--
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

async function markAllRead() {
  const unreadIds = messages.value.filter((m) => !m.isRead).map((m) => m.id)
  if (unreadIds.length === 0) return
  markingAll.value = true
  try {
    await Promise.all(unreadIds.map((id) => markMessageRead(id)))
    messages.value.forEach((m) => {
      m.isRead = true
    })
    unreadCount.value = 0
    ElMessage.success('已全部标为已读')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    markingAll.value = false
  }
}

function onFilterChange() {
  pageNum.value = 1
  fetchMessages()
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(async () => {
  await Promise.all([fetchMessages(), fetchUnreadCount()])
})
</script>

<style lang="scss" scoped>
.messages-page {
  .page-card {
    border-radius: 12px;

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .header-left {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 600;
        color: #2a3547;

        .el-icon {
          font-size: 20px;
          color: #5d87ff;
        }

        .header-badge {
          margin-left: 4px;
        }
      }
    }
  }

  .filter-bar {
    margin-bottom: 20px;
  }

  .empty-state {
    padding: 40px 0;
    text-align: center;
  }

  .message-row {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 16px 0;
    border-bottom: 1px solid #f5f5f5;
    transition: background 0.2s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #fafafa;
      margin: 0 -12px;
      padding: 16px 12px;
      border-radius: 8px;
    }

    &.unread {
      .message-title {
        font-weight: 700;
      }
    }

    .message-row-left {
      padding-top: 4px;
      flex-shrink: 0;

      .unread-indicator {
        display: block;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #e0e0e0;

        &.active {
          background: #5d87ff;
        }
      }
    }

    .message-row-body {
      flex: 1;
      min-width: 0;

      .message-row-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 6px;

        .message-title {
          font-size: 14px;
          color: #2a3547;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          flex: 1;
          margin-right: 12px;
        }

        .message-time {
          font-size: 12px;
          color: #a1a5b7;
          flex-shrink: 0;
        }
      }

      .message-content {
        font-size: 13px;
        color: #5a6a85;
        line-height: 1.6;
      }
    }

    .message-row-actions {
      flex-shrink: 0;
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    padding-top: 24px;
  }
}
</style>
