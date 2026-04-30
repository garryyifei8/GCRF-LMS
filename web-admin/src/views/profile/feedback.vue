<template>
  <div class="feedback-page">
    <el-row :gutter="24">
      <!-- 提交反馈表单 -->
      <el-col :span="14">
        <el-card class="page-card" shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon><ChatDotSquare /></el-icon>
              <span>提交反馈</span>
            </div>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="90px"
            label-position="left"
          >
            <el-form-item label="反馈类型" prop="feedbackType">
              <el-radio-group v-model="form.feedbackType">
                <el-radio-button value="BUG">
                  <el-icon><Warning /></el-icon> Bug 报告
                </el-radio-button>
                <el-radio-button value="FEATURE">
                  <el-icon><StarFilled /></el-icon> 功能建议
                </el-radio-button>
                <el-radio-button value="OTHER">
                  <el-icon><More /></el-icon> 其他
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="标题" prop="title">
              <el-input
                v-model="form.title"
                placeholder="请简要描述问题或建议（5-100 字）"
                maxlength="100"
                show-word-limit
                clearable
              />
            </el-form-item>

            <el-form-item label="详细描述" prop="content">
              <el-input
                v-model="form.content"
                type="textarea"
                placeholder="请详细描述您遇到的问题或功能建议，包括操作步骤、期望结果等..."
                :rows="6"
                maxlength="1000"
                show-word-limit
                resize="none"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">
                提交反馈
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 我的反馈列表 -->
      <el-col :span="10">
        <el-card class="page-card" shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon><List /></el-icon>
              <span>我的反馈记录</span>
              <el-button link type="primary" class="refresh-btn" @click="fetchFeedbacks">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>

          <div v-if="loadingList" class="list-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>

          <div v-else-if="feedbacks.length === 0" class="list-empty">
            <el-empty description="暂无反馈记录" :image-size="80" />
          </div>

          <el-scrollbar v-else max-height="480px">
            <div v-for="item in feedbacks" :key="item.id" class="feedback-item">
              <div class="feedback-item-header">
                <el-tag :type="typeTagMap[item.feedbackType]?.type" size="small" effect="light">
                  {{ typeTagMap[item.feedbackType]?.label || item.feedbackType }}
                </el-tag>
                <el-tag
                  :type="statusTagMap[item.status]?.type"
                  size="small"
                  effect="plain"
                  class="status-tag"
                >
                  {{ statusTagMap[item.status]?.label || '待处理' }}
                </el-tag>
              </div>
              <div class="feedback-title">{{ item.title }}</div>
              <div class="feedback-content">{{ item.content }}</div>
              <div class="feedback-time">{{ formatDate(item.createdAt) }}</div>
            </div>
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  ChatDotSquare,
  Warning,
  StarFilled,
  More,
  List,
  Refresh,
  Loading
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { submitFeedback, getFeedback } from '@/api/system'

const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)
const loadingList = ref(false)
const feedbacks = ref([])

const form = reactive({
  feedbackType: 'BUG',
  title: '',
  content: ''
})

const rules = {
  feedbackType: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度为 5-100 字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入详细描述', trigger: 'blur' },
    { min: 10, max: 1000, message: '描述长度为 10-1000 字', trigger: 'blur' }
  ]
}

const typeTagMap = {
  BUG: { type: 'danger', label: 'Bug 报告' },
  FEATURE: { type: 'success', label: '功能建议' },
  OTHER: { type: 'info', label: '其他' }
}

const statusTagMap = {
  PENDING: { type: 'warning', label: '待处理' },
  PROCESSING: { type: 'primary', label: '处理中' },
  RESOLVED: { type: 'success', label: '已解决' },
  CLOSED: { type: 'info', label: '已关闭' }
}

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await submitFeedback({
        userId: userStore.userInfo?.id,
        userName: userStore.userInfo?.name || userStore.userInfo?.username,
        title: form.title,
        content: form.content,
        feedbackType: form.feedbackType
      })
      if (res.code === 200) {
        ElMessage.success('反馈提交成功，感谢您的宝贵意见！')
        resetForm()
        fetchFeedbacks()
      } else {
        ElMessage.error(res.message || '提交失败，请稍后重试')
      }
    } catch {
      ElMessage.error('网络异常，请稍后重试')
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  formRef.value?.resetFields()
  form.feedbackType = 'BUG'
}

async function fetchFeedbacks() {
  const userId = userStore.userInfo?.id
  if (!userId) return
  loadingList.value = true
  try {
    const res = await getFeedback({ userId })
    if (res.code === 200) {
      feedbacks.value = res.data?.records ?? res.data?.list ?? res.data ?? []
    }
  } catch {
    ElMessage.error('获取反馈记录失败')
  } finally {
    loadingList.value = false
  }
}

function formatDate(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchFeedbacks()
})
</script>

<style lang="scss" scoped>
.feedback-page {
  .page-card {
    border-radius: 12px;
    height: 100%;

    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 600;
      color: #2a3547;

      .el-icon {
        font-size: 18px;
        color: #5d87ff;
      }

      .refresh-btn {
        margin-left: auto;
      }
    }
  }

  .list-loading,
  .list-empty {
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

  .feedback-item {
    padding: 12px;
    margin-bottom: 10px;
    border: 1px solid #f0f0f0;
    border-radius: 8px;
    transition: box-shadow 0.2s;

    &:last-child {
      margin-bottom: 0;
    }

    &:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .feedback-item-header {
      display: flex;
      gap: 6px;
      margin-bottom: 8px;

      .status-tag {
        margin-left: auto;
      }
    }

    .feedback-title {
      font-size: 13px;
      font-weight: 600;
      color: #2a3547;
      margin-bottom: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .feedback-content {
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

    .feedback-time {
      font-size: 11px;
      color: #a1a5b7;
    }
  }
}
</style>
