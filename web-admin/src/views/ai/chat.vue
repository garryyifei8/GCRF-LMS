<template>
  <div class="page-container chat-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">AI 智能问答</h1>
      <p class="page-header-description">基于自然语言处理的智能客服助手</p>
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
              <el-button :icon="Download" size="small" @click="handleExportChat">导出记录</el-button>
            </div>
          </div>
          <div class="card-content">
            <!-- 消息列表 -->
            <div ref="messageListRef" class="message-list">
              <div v-for="message in messages" :key="message.id" class="message-item" :class="message.role">
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
                      <!-- 推荐图书卡片 -->
                      <div v-if="message.books && message.books.length > 0" class="book-recommend-list">
                        <div v-for="book in message.books" :key="book.id" class="book-recommend-item">
                          <div class="book-info">
                            <div class="book-title">{{ book.title }}</div>
                            <div class="book-meta">{{ book.author }} | {{ book.publisher }}</div>
                          </div>
                          <el-button type="primary" size="small" link @click="handleViewBook(book)">查看</el-button>
                        </div>
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
                placeholder="请输入您的问题，例如：有没有《三体》这本书？"
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
                  <el-button type="primary" :icon="Promotion" :disabled="!userInput.trim() || isTyping" @click="handleSendMessage">
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
            <div class="hot-question-list">
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

// 消息列表
const messages = ref([
  {
    id: 1,
    role: 'assistant',
    content: '您好！我是图书馆AI助手，有什么可以帮您的吗？<br/>您可以向我咨询图书信息、借阅规则、操作指导等问题。',
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
])

const messageListRef = ref()
const userInput = ref('')
const isTyping = ref(false)

// 快捷问题
const quickQuestions = ref([
  '有《三体》这本书吗？',
  '推荐一些科幻小说',
  '如何办理读者证？',
  '借书期限是多久？'
])

// 热门问题
const hotQuestions = ref([
  { question: '如何借阅图书？', count: 1234 },
  { question: '忘记密码怎么办？', count: 856 },
  { question: '借书期限是多久？', count: 745 },
  { question: '如何续借图书？', count: 623 },
  { question: '逾期了怎么办？', count: 512 }
])

// 对话统计
const chatStats = reactive({
  totalQuestions: 15678,
  successRate: 87.5,
  avgResponseTime: 0.8,
  satisfaction: 4.5
})

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
    // TODO: 调用AI问答API
    // const res = await request.post('/api/ai/chat', { question })

    // Mock AI响应
    await new Promise((resolve) => setTimeout(resolve, 1000))
    const aiResponse = generateMockResponse(question)

    // 添加AI响应
    const assistantMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      content: aiResponse.content,
      books: aiResponse.books,
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
    messages.value.push(assistantMessage)

    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage.error('获取回复失败')
  } finally {
    isTyping.value = false
  }
}

// 生成Mock响应
const generateMockResponse = (question) => {
  // 图书查询类
  if (question.includes('三体') || question.includes('有') || question.includes('查')) {
    return {
      content: '我为您查询到以下图书：',
      books: [
        {
          id: 1,
          title: '三体',
          author: '刘慈欣',
          publisher: '重庆出版社',
          status: '在架',
          location: 'A3书架'
        },
        {
          id: 2,
          title: '三体Ⅱ：黑暗森林',
          author: '刘慈欣',
          publisher: '重庆出版社',
          status: '借出',
          location: 'A3书架'
        },
        {
          id: 3,
          title: '三体Ⅲ：死神永生',
          author: '刘慈欣',
          publisher: '重庆出版社',
          status: '在架',
          location: 'A3书架'
        }
      ]
    }
  }

  // 推荐类
  if (question.includes('推荐') || question.includes('科幻')) {
    return {
      content: '根据您的需求，为您推荐以下科幻小说：',
      books: [
        { id: 4, title: '流浪地球', author: '刘慈欣', publisher: '长江出版社' },
        { id: 5, title: '银河帝国：基地', author: '阿西莫夫', publisher: '江苏凤凰文艺出版社' },
        { id: 6, title: '沙丘', author: '弗兰克·赫伯特', publisher: '江苏凤凰文艺出版社' }
      ]
    }
  }

  // 借阅规则类
  if (question.includes('借') || question.includes('期限') || question.includes('多久')) {
    return {
      content: '关于借阅规则的说明：<br/><br/>' +
        '• 学生读者：每次可借 <strong>5本</strong>，借期 <strong>30天</strong><br/>' +
        '• 教师读者：每次可借 <strong>10本</strong>，借期 <strong>60天</strong><br/>' +
        '• 到期前可续借 <strong>1次</strong>，续借期限与原借期相同<br/>' +
        '• 逾期归还将产生罚款，每天 <strong>0.1元/本</strong><br/><br/>' +
        '如需了解更多，请查看 <a href="#" style="color: #1890ff">详细借阅规则</a>'
    }
  }

  // 办证类
  if (question.includes('办') || question.includes('读者证') || question.includes('证')) {
    return {
      content: '办理读者证的流程如下：<br/><br/>' +
        '1. 准备材料：身份证或学生证<br/>' +
        '2. 前往图书馆服务台填写申请表<br/>' +
        '3. 工作人员录入信息并拍照<br/>' +
        '4. 缴纳押金（学生20元，教师50元）<br/>' +
        '5. 领取读者证，即可开始借阅<br/><br/>' +
        '温馨提示：我们已支持人脸识别借书，办证后可直接刷脸借还哦！'
    }
  }

  // 续借类
  if (question.includes('续借')) {
    return {
      content: '续借图书有以下几种方式：<br/><br/>' +
        '• <strong>微信小程序</strong>：打开"我的借阅"，点击"续借"按钮<br/>' +
        '• <strong>网站</strong>：登录个人中心，在借阅记录中操作<br/>' +
        '• <strong>到馆续借</strong>：携带图书和读者证到服务台办理<br/><br/>' +
        '注意事项：<br/>' +
        '• 每本书只能续借1次<br/>' +
        '• 如有预约读者，则不能续借<br/>' +
        '• 已逾期的图书不能续借'
    }
  }

  // 默认响应
  return {
    content: '抱歉，我还不太理解您的问题。您可以尝试换一种方式提问，或者选择下面的快捷问题。<br/><br/>' +
      '如果问题仍未解决，建议您联系图书馆工作人员，电话：<strong>0571-12345678</strong>'
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
  // TODO: 实现语音识别功能
}

// 清空对话
const handleClearChat = () => {
  messages.value = [messages.value[0]] // 保留欢迎消息
  ElMessage.success('对话已清空')
}

// 导出对话
const handleExportChat = () => {
  if (messages.value.length <= 1) {
    ElMessage.warning('暂无对话记录可导出')
    return
  }
  ElMessage.success('对话记录导出成功')
  // TODO: 实现导出功能
}

// 查看图书
const handleViewBook = (book) => {
  ElMessage.info(`查看图书：${book.title}`)
  // TODO: 跳转到图书详情页
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

onMounted(() => {
  scrollToBottom()
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
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.6;
  }
  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

.book-recommend-list {
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.book-recommend-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  .book-info {
    flex: 1;
  }

  .book-title {
    font-size: 14px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 4px;
  }

  .book-meta {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
  }
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
