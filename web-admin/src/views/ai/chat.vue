<template>
  <div class="page-container chat-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">AI 智能问答</h1>
      <p class="page-header-description">基于知识库的智能客服助手</p>
    </div>

    <!-- AI能力展示 -->
    <el-row :gutter="16" class="mb-lg">
      <el-col :xs="24" :md="6">
        <div class="capability-card">
          <el-icon :size="28" color="#1890ff"><Search /></el-icon>
          <div class="capability-title">图书查询</div>
          <div class="capability-desc">快速查询馆藏图书</div>
        </div>
      </el-col>
      <el-col :xs="24" :md="6">
        <div class="capability-card">
          <el-icon :size="28" color="#52c41a"><MessageBox /></el-icon>
          <div class="capability-title">借阅咨询</div>
          <div class="capability-desc">借阅规则和流程指导</div>
        </div>
      </el-col>
      <el-col :xs="24" :md="6">
        <div class="capability-card">
          <el-icon :size="28" color="#fa8c16"><Guide /></el-icon>
          <div class="capability-title">操作指导</div>
          <div class="capability-desc">系统使用帮助</div>
        </div>
      </el-col>
      <el-col :xs="24" :md="6">
        <div class="capability-card">
          <el-icon :size="28" color="#722ed1"><DataAnalysis /></el-icon>
          <div class="capability-title">数据查询</div>
          <div class="capability-desc">馆情数据和统计</div>
        </div>
      </el-col>
    </el-row>

    <!-- 主要内容区域 -->
    <el-row :gutter="16">
      <!-- 聊天区域 -->
      <el-col :xs="24" :lg="16">
        <div class="card chat-card">
          <div class="card-title">
            智能问答
            <div class="chat-actions">
              <el-button :icon="Refresh" size="small" @click="handleClearChat">清空对话</el-button>
              <el-button :icon="Download" size="small" @click="handleExportChat"
                >导出记录</el-button
              >
            </div>
          </div>
          <div class="card-content">
            <!-- 消息列表 -->
            <div ref="messageListRef" class="message-list">
              <div
                v-for="message in messages"
                :key="message.id"
                class="message-item"
                :class="message.role"
              >
                <!-- 用户消息 -->
                <div v-if="message.role === 'user'" class="message-wrapper">
                  <div class="message-content">
                    <div class="message-text">{{ message.content }}</div>
                    <div class="message-time">{{ message.time }}</div>
                  </div>
                  <div class="message-avatar">
                    <el-avatar :size="40" :icon="UserFilled" />
                  </div>
                </div>

                <!-- AI助手消息 -->
                <div v-else class="message-wrapper">
                  <div class="message-avatar">
                    <el-avatar :size="40" :icon="Robot" style="background: #1890ff" />
                  </div>
                  <div class="message-content">
                    <div class="message-text">
                      <div v-html="message.content"></div>
                      <!-- 相关问题推荐 -->
                      <div
                        v-if="message.relatedQuestions && message.relatedQuestions.length > 0"
                        class="related-questions"
                      >
                        <div class="related-title">相关问题：</div>
                        <div
                          v-for="rq in message.relatedQuestions"
                          :key="rq.faqId"
                          class="related-item"
                          @click="handleQuickQuestion(rq.question)"
                        >
                          {{ rq.question }}
                        </div>
                      </div>
                      <!-- 反馈按钮 -->
                      <div v-if="message.matchedFaqId" class="feedback-buttons">
                        <el-button
                          type="primary"
                          link
                          size="small"
                          @click="handleFeedback(message, 'helpful')"
                        >
                          <el-icon><CircleCheck /></el-icon> 有帮助
                        </el-button>
                        <el-button
                          type="info"
                          link
                          size="small"
                          @click="handleFeedback(message, 'unhelpful')"
                        >
                          <el-icon><CircleClose /></el-icon> 无帮助
                        </el-button>
                      </div>
                    </div>
                    <div class="message-time">{{ message.time }}</div>
                  </div>
                </div>
              </div>

              <!-- 加载中 -->
              <div v-if="isTyping" class="message-item assistant">
                <div class="message-wrapper">
                  <div class="message-avatar">
                    <el-avatar :size="40" :icon="Robot" style="background: #1890ff" />
                  </div>
                  <div class="message-content">
                    <div class="message-text typing-indicator">
                      <span></span><span></span><span></span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 输入区域 -->
            <div class="message-input-wrapper">
              <el-input
                v-model="userInput"
                type="textarea"
                :rows="3"
                placeholder="请输入您的问题，例如：借书期限是多久？"
                :disabled="isTyping"
                @keydown.ctrl.enter="handleSendMessage"
              />
              <div class="input-actions">
                <div class="quick-questions">
                  <el-tag
                    v-for="question in quickQuestions"
                    :key="question"
                    size="small"
                    class="quick-question-tag"
                    @click="handleQuickQuestion(question)"
                  >
                    {{ question }}
                  </el-tag>
                </div>
                <div class="send-button-wrapper">
                  <el-button :icon="Microphone" circle @click="handleVoiceInput" />
                  <el-button
                    type="primary"
                    :icon="Promotion"
                    :disabled="!userInput.trim() || isTyping"
                    @click="handleSendMessage"
                  >
                    发送 (Ctrl+Enter)
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 侧边栏 -->
      <el-col :xs="24" :lg="8">
        <!-- 对话统计 -->
        <div class="card mb-md">
          <div class="card-title">对话统计</div>
          <div class="card-content">
            <div class="stat-row">
              <span class="stat-label">总提问数</span>
              <span class="stat-value">{{ chatStats.totalQuestions }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">解答成功率</span>
              <span class="stat-value text-success">{{ chatStats.successRate }}%</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">平均响应时间</span>
              <span class="stat-value">{{ chatStats.avgResponseTime }}s</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">满意度评分</span>
              <el-rate v-model="chatStats.satisfaction" disabled show-score text-color="#ff9900" />
            </div>
          </div>
        </div>

        <!-- 热门问题 -->
        <div class="card">
          <div class="card-title">热门问题</div>
          <div class="card-content">
            <div v-loading="hotQuestionsLoading" class="hot-question-list">
              <div
                v-for="(question, index) in hotQuestions"
                :key="index"
                class="hot-question-item"
                @click="handleQuickQuestion(question.question)"
              >
                <div class="hot-question-rank">{{ index + 1 }}</div>
                <div class="hot-question-content">
                  <div class="hot-question-text">{{ question.question }}</div>
                  <div class="hot-question-count">{{ question.count }} 次提问</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sendMessage, getChatStats, getHotQuestions, submitFeedback } from '@/api/chat'
import {
  Search,
  MessageBox,
  Guide,
  DataAnalysis,
  Refresh,
  Download,
  Microphone,
  Promotion,
  UserFilled,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'

// 定义Robot图标（Element Plus没有自带Robot图标，用ChatDotRound代替）
const Robot = {
  name: 'Robot',
  render() {
    return h(
      'svg',
      {
        viewBox: '0 0 1024 1024',
        xmlns: 'http://www.w3.org/2000/svg'
      },
      [
        h('path', {
          fill: 'currentColor',
          d: 'M300 328a60 60 0 1 0 120 0 60 60 0 1 0-120 0zM604 328a60 60 0 1 0 120 0 60 60 0 1 0-120 0zM512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm263.6 600c0 4.4-3.6 8-8 8H256.4c-4.4 0-8-3.6-8-8v-32c0-4.4 3.6-8 8-8h511.2c4.4 0 8 3.6 8 8v32zm0-168c0 4.4-3.6 8-8 8H256.4c-4.4 0-8-3.6-8-8v-32c0-4.4 3.6-8 8-8h511.2c4.4 0 8 3.6 8 8v32z'
        })
      ]
    )
  }
}
import { h } from 'vue'

// 会话ID
const sessionId = ref('')

// 消息列表
const messages = ref([
  {
    id: 1,
    role: 'assistant',
    content:
      '您好！我是图书馆AI助手，有什么可以帮您的吗？<br/>您可以向我咨询图书信息、借阅规则、操作指导等问题。',
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
])

const messageListRef = ref()
const userInput = ref('')
const isTyping = ref(false)
const hotQuestionsLoading = ref(false)

// 快捷问题
const quickQuestions = ref([
  '借书期限是多久？',
  '如何续借图书？',
  '如何办理读者证？',
  '图书馆开放时间？'
])

// 热门问题
const hotQuestions = ref([])

// 对话统计
const chatStats = reactive({
  totalQuestions: 0,
  successRate: 0,
  avgResponseTime: 0,
  satisfaction: 4.5
})

// 加载热门问题
const loadHotQuestions = async () => {
  hotQuestionsLoading.value = true
  try {
    const res = await getHotQuestions(5)
    if (res.code === 200 && res.data) {
      hotQuestions.value = res.data
    }
  } catch (error) {
    console.error('Failed to load hot questions:', error)
    // 使用默认热门问题
    hotQuestions.value = [
      { question: '如何借阅图书？', count: 1234 },
      { question: '忘记密码怎么办？', count: 856 },
      { question: '借书期限是多久？', count: 745 },
      { question: '如何续借图书？', count: 623 },
      { question: '逾期了怎么办？', count: 512 }
    ]
  } finally {
    hotQuestionsLoading.value = false
  }
}

// 加载统计数据
const loadChatStats = async () => {
  try {
    const res = await getChatStats()
    if (res.code === 200 && res.data) {
      Object.assign(chatStats, res.data)
    }
  } catch (error) {
    console.error('Failed to load chat stats:', error)
    // 使用默认统计
    chatStats.totalQuestions = 15678
    chatStats.successRate = 87.5
    chatStats.avgResponseTime = 0.8
    chatStats.satisfaction = 4.5
  }
}

// 发送消息
const handleSendMessage = async () => {
  if (!userInput.value.trim() || isTyping.value) return

  const question = userInput.value.trim()
  userInput.value = ''

  // 添加用户消息
  const userMessage = {
    id: Date.now(),
    role: 'user',
    content: question,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  messages.value.push(userMessage)

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 显示输入中状态
  isTyping.value = true

  try {
    // 调用AI问答API
    const res = await sendMessage({
      sessionId: sessionId.value || null,
      content: question
    })

    if (res.code === 200 && res.data) {
      // 保存会话ID
      if (!sessionId.value) {
        sessionId.value = res.data.sessionId
      }

      // 添加AI响应
      const assistantMessage = {
        id: res.data.id || Date.now() + 1,
        role: 'assistant',
        content: res.data.content,
        matchedFaqId: res.data.matchedFaqId,
        relatedQuestions: res.data.relatedQuestions || [],
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      }
      messages.value.push(assistantMessage)
    } else {
      // API返回错误
      const errorMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: '抱歉，服务暂时不可用，请稍后再试。',
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      }
      messages.value.push(errorMessage)
    }

    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('Failed to send message:', error)
    // 添加错误消息
    const errorMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      content:
        '抱歉，网络连接失败，请检查网络后重试。<br/>如需帮助，请联系服务热线：<strong>0571-12345678</strong>',
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
    messages.value.push(errorMessage)

    await nextTick()
    scrollToBottom()
  } finally {
    isTyping.value = false
  }
}

// 快捷问题
const handleQuickQuestion = (question) => {
  userInput.value = question
  handleSendMessage()
}

// 语音输入
const handleVoiceInput = () => {
  ElMessage.info('语音输入功能开发中...')
}

// 清空对话
const handleClearChat = () => {
  messages.value = [messages.value[0]] // 保留欢迎消息
  sessionId.value = '' // 重置会话ID
  ElMessage.success('对话已清空')
}

// 导出对话
const handleExportChat = () => {
  if (messages.value.length <= 1) {
    ElMessage.warning('暂无对话记录可导出')
    return
  }

  // 生成导出内容
  const exportContent = messages.value
    .map((msg) => {
      const role = msg.role === 'user' ? '用户' : 'AI助手'
      // 去除HTML标签
      const content = msg.content.replace(/<[^>]+>/g, '')
      return `[${msg.time}] ${role}：${content}`
    })
    .join('\n\n')

  // 创建下载
  const blob = new Blob([exportContent], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `对话记录_${new Date().toISOString().slice(0, 10)}.txt`
  link.click()
  URL.revokeObjectURL(url)

  ElMessage.success('对话记录导出成功')
}

// 提交反馈
const handleFeedback = async (message, feedbackType) => {
  try {
    await submitFeedback({
      sessionId: sessionId.value,
      messageId: message.id,
      faqId: message.matchedFaqId,
      feedbackType
    })

    if (feedbackType === 'helpful') {
      ElMessage.success('感谢您的反馈！')
    } else {
      ElMessage.info('感谢您的反馈，我们会继续改进')
    }
  } catch (error) {
    console.error('Failed to submit feedback:', error)
    ElMessage.error('反馈提交失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

onMounted(() => {
  scrollToBottom()
  loadHotQuestions()
  loadChatStats()
})
</script>

<style lang="scss" scoped>
.chat-container {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.capability-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;
  cursor: pointer;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  .capability-title {
    font-size: 14px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);
    margin: 8px 0 4px;
  }

  .capability-desc {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
  }
}

.chat-card {
  height: calc(100vh - 280px);
  display: flex;
  flex-direction: column;

  .card-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 16px;
}

.message-item {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.message-wrapper {
  display: flex;
  gap: 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.message-text {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-item.user {
  .message-wrapper {
    flex-direction: row-reverse;
  }

  .message-content {
    align-items: flex-end;
  }

  .message-text {
    background: #1890ff;
    color: #fff;
    max-width: 70%;
  }
}

.message-item.assistant {
  .message-text {
    background: #fff;
    color: rgba(0, 0, 0, 0.85);
    max-width: 80%;
  }
}

.message-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 20px;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #d9d9d9;
    animation: typing 1.4s infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes typing {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.6;
  }
  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

.related-questions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;

  .related-title {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
    margin-bottom: 8px;
  }

  .related-item {
    display: inline-block;
    padding: 4px 12px;
    margin: 4px 8px 4px 0;
    background: #f5f5f5;
    border-radius: 4px;
    font-size: 12px;
    color: #1890ff;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background: #e6f7ff;
    }
  }
}

.feedback-buttons {
  margin-top: 8px;
  display: flex;
  gap: 16px;
}

.message-input-wrapper {
  background: #fff;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  padding: 12px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.quick-questions {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-question-tag {
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-1px);
  }
}

.send-button-wrapper {
  display: flex;
  gap: 8px;
  margin-left: 12px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  .stat-label {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.65);
  }

  .stat-value {
    font-size: 16px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);

    &.text-success {
      color: #52c41a;
    }
  }
}

.hot-question-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-question-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: #f0f0f0;
  }
}

.hot-question-rank {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1890ff;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}

.hot-question-content {
  flex: 1;
}

.hot-question-text {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 4px;
}

.hot-question-count {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
