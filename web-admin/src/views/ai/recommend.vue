<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">AI 智能推荐</h1>
      <p class="page-header-description">基于机器学习的个性化图书推荐系统</p>
    </div>

    <!-- 推荐算法说明 -->
    <el-row :gutter="16" class="mb-lg">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="algorithm-card">
          <el-icon class="algorithm-icon" :size="32" color="#1890ff"><DataAnalysis /></el-icon>
          <div class="algorithm-title">协同过滤</div>
          <div class="algorithm-desc">基于用户借阅行为相似度推荐</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="algorithm-card">
          <el-icon class="algorithm-icon" :size="32" color="#52c41a"><Document /></el-icon>
          <div class="algorithm-title">内容推荐</div>
          <div class="algorithm-desc">基于图书分类和标签相似度</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="algorithm-card">
          <el-icon class="algorithm-icon" :size="32" color="#fa8c16"><TrendCharts /></el-icon>
          <div class="algorithm-title">深度学习</div>
          <div class="algorithm-desc">Wide & Deep 模型预测偏好</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="algorithm-card">
          <el-icon class="algorithm-icon" :size="32" color="#722ed1"><Connection /></el-icon>
          <div class="algorithm-title">混合推荐</div>
          <div class="algorithm-desc">多策略融合加权排序</div>
        </div>
      </el-col>
    </el-row>

    <!-- 推荐配置 -->
    <div class="card mb-md">
      <div class="card-title">推荐配置</div>
      <div class="card-content">
        <el-form :model="recommendConfig" label-width="120px">
          <el-row :gutter="24">
            <el-col :xs="24" :md="12">
              <el-form-item label="读者筛选">
                <el-select v-model="recommendConfig.readerType" placeholder="选择读者类型" style="width: 100%">
                  <el-option label="全部读者" value="all" />
                  <el-option label="学生读者" value="student" />
                  <el-option label="教师读者" value="teacher" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="推荐算法">
                <el-select v-model="recommendConfig.algorithm" placeholder="选择推荐算法" style="width: 100%">
                  <el-option label="协同过滤" value="cf" />
                  <el-option label="内容推荐" value="content" />
                  <el-option label="深度学习" value="dl" />
                  <el-option label="混合推荐" value="hybrid" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="推荐数量">
                <el-input-number v-model="recommendConfig.count" :min="10" :max="100" :step="10" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="推荐场景">
                <el-select v-model="recommendConfig.scene" placeholder="选择推荐场景" style="width: 100%">
                  <el-option label="首页推荐" value="homepage" />
                  <el-option label="详情页推荐" value="detail" />
                  <el-option label="搜索推荐" value="search" />
                  <el-option label="主题推荐" value="topic" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item>
            <el-button type="primary" :icon="Refresh" :loading="loading" @click="generateRecommendations">
              生成推荐
            </el-button>
            <el-button :icon="Download" @click="exportRecommendations">导出结果</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 推荐效果统计 -->
    <el-row :gutter="16" class="mb-md">
      <el-col :xs="24" :md="8">
        <div class="stat-card-alt">
          <div class="stat-value">{{ recommendStats.precision }}%</div>
          <div class="stat-label">推荐准确率</div>
          <el-progress :percentage="recommendStats.precision" :stroke-width="8" :show-text="false" />
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="stat-card-alt">
          <div class="stat-value">{{ recommendStats.ctr }}%</div>
          <div class="stat-label">点击率 (CTR)</div>
          <el-progress :percentage="recommendStats.ctr" :stroke-width="8" :show-text="false" color="#52c41a" />
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="stat-card-alt">
          <div class="stat-value">{{ recommendStats.conversion }}%</div>
          <div class="stat-label">借阅转化率</div>
          <el-progress :percentage="recommendStats.conversion" :stroke-width="8" :show-text="false" color="#fa8c16" />
        </div>
      </el-col>
    </el-row>

    <!-- 推荐结果展示 -->
    <div class="card">
      <div class="card-title">
        推荐结果
        <el-tag v-if="recommendations.length > 0" class="ml-sm">共 {{ recommendations.length }} 条</el-tag>
      </div>
      <div class="card-content">
        <el-table v-loading="loading" :data="recommendations" stripe>
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="readerId" label="读者ID" width="100" />
          <el-table-column prop="readerName" label="读者姓名" width="120" />
          <el-table-column prop="bookId" label="图书ID" width="100" />
          <el-table-column prop="bookTitle" label="推荐图书" show-overflow-tooltip min-width="200" />
          <el-table-column prop="author" label="作者" width="120" />
          <el-table-column label="推荐分数" width="120">
            <template #default="{ row }">
              <el-tag :type="getScoreType(row.score)">{{ row.score.toFixed(3) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="algorithm" label="算法" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ getAlgorithmName(row.algorithm) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="推荐理由" show-overflow-tooltip min-width="200">
            <template #default="{ row }">
              <span class="text-secondary">{{ row.reason }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :icon="View" @click="handleViewDetail(row)">详情</el-button>
              <el-button type="primary" link :icon="Discount" @click="handlePushToReader(row)">推送</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 推荐详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="推荐详情" width="700px">
      <div v-if="currentRecommendation">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="读者姓名">{{ currentRecommendation.readerName }}</el-descriptions-item>
          <el-descriptions-item label="读者ID">{{ currentRecommendation.readerId }}</el-descriptions-item>
          <el-descriptions-item label="图书名称" :span="2">{{ currentRecommendation.bookTitle }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ currentRecommendation.author }}</el-descriptions-item>
          <el-descriptions-item label="推荐分数">{{ currentRecommendation.score.toFixed(3) }}</el-descriptions-item>
          <el-descriptions-item label="推荐算法">{{ getAlgorithmName(currentRecommendation.algorithm) }}</el-descriptions-item>
          <el-descriptions-item label="推荐场景">{{ getSceneName(currentRecommendation.scene) }}</el-descriptions-item>
          <el-descriptions-item label="推荐理由" :span="2">{{ currentRecommendation.reason }}</el-descriptions-item>
        </el-descriptions>

        <!-- 读者历史偏好 -->
        <div class="mt-md">
          <h4>读者历史偏好</h4>
          <el-tag v-for="tag in currentRecommendation.readerPreferences" :key="tag" class="mr-sm mb-sm">
            {{ tag }}
          </el-tag>
        </div>

        <!-- 图书特征 -->
        <div class="mt-md">
          <h4>图书特征标签</h4>
          <el-tag v-for="tag in currentRecommendation.bookTags" :key="tag" type="success" class="mr-sm mb-sm">
            {{ tag }}
          </el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 推荐配置
const recommendConfig = reactive({
  readerType: 'all',
  algorithm: 'hybrid',
  count: 20,
  scene: 'homepage'
})

// 推荐统计
const recommendStats = ref({
  precision: 78.5,
  ctr: 12.3,
  conversion: 8.7
})

// 表格数据
const loading = ref(false)
const recommendations = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentRecommendation = ref(null)

// 算法名称映射
const algorithmMap = {
  cf: '协同过滤',
  content: '内容推荐',
  dl: '深度学习',
  hybrid: '混合推荐'
}

// 场景名称映射
const sceneMap = {
  homepage: '首页推荐',
  detail: '详情页推荐',
  search: '搜索推荐',
  topic: '主题推荐'
}

// 获取算法名称
const getAlgorithmName = (algorithm) => {
  return algorithmMap[algorithm] || algorithm
}

// 获取场景名称
const getSceneName = (scene) => {
  return sceneMap[scene] || scene
}

// 获取分数类型
const getScoreType = (score) => {
  if (score >= 0.8) return 'success'
  if (score >= 0.6) return 'warning'
  return 'info'
}

// 生成推荐
const generateRecommendations = async () => {
  loading.value = true
  try {
    // TODO: 调用AI推荐API
    // const res = await request.post('/api/ai/recommend', recommendConfig)

    // Mock数据
    await new Promise((resolve) => setTimeout(resolve, 1000))

    const mockRecommendations = []
    const readers = ['张三', '李四', '王五', '赵六', '钱七', '孙八', '周九', '吴十']
    const books = [
      { title: '人工智能基础', author: '周志华' },
      { title: 'Python编程从入门到实践', author: 'Eric Matthes' },
      { title: '深度学习', author: 'Ian Goodfellow' },
      { title: '机器学习实战', author: 'Peter Harrington' },
      { title: '统计学习方法', author: '李航' },
      { title: '数据结构与算法', author: '严蔚敏' },
      { title: '计算机网络', author: '谢希仁' },
      { title: '操作系统概念', author: 'Abraham Silberschatz' }
    ]
    const algorithms = ['cf', 'content', 'dl', 'hybrid']
    const reasons = [
      '与你最近借阅的图书相似',
      '借过此书的读者还借过这些书',
      '基于你的阅读偏好推荐',
      '当前热门图书推荐',
      '符合你的年级和专业',
      '与你收藏的图书相关'
    ]

    for (let i = 1; i <= recommendConfig.count; i++) {
      const book = books[i % books.length]
      mockRecommendations.push({
        id: i,
        readerId: `R${(i + 1000).toString()}`,
        readerName: readers[i % readers.length],
        bookId: `B${(i + 2000).toString()}`,
        bookTitle: book.title,
        author: book.author,
        score: Math.random() * 0.3 + 0.7, // 0.7-1.0之间
        algorithm: algorithms[i % algorithms.length],
        scene: recommendConfig.scene,
        reason: reasons[i % reasons.length],
        readerPreferences: ['计算机', '人工智能', '编程', '算法'],
        bookTags: ['技术', '编程', 'AI', '入门']
      })
    }

    // 按分数降序排序
    mockRecommendations.sort((a, b) => b.score - a.score)

    recommendations.value = mockRecommendations
    pagination.total = mockRecommendations.length

    ElMessage.success(`成功生成 ${mockRecommendations.length} 条推荐`)
  } catch (error) {
    ElMessage.error('生成推荐失败')
  } finally {
    loading.value = false
  }
}

// 导出推荐结果
const exportRecommendations = () => {
  if (recommendations.value.length === 0) {
    ElMessage.warning('暂无推荐数据可导出')
    return
  }

  ElMessage.success('推荐结果导出成功')
  // TODO: 实现导出功能
}

// 查看详情
const handleViewDetail = (row) => {
  currentRecommendation.value = row
  detailDialogVisible.value = true
}

// 推送给读者
const handlePushToReader = (row) => {
  ElMessageBox.confirm(`确定要将《${row.bookTitle}》推送给读者 ${row.readerName} 吗？`, '推送确认', {
    type: 'info'
  })
    .then(async () => {
      try {
        // TODO: 调用推送API
        await new Promise((resolve) => setTimeout(resolve, 500))
        ElMessage.success('推送成功')
      } catch (error) {
        ElMessage.error('推送失败')
      }
    })
    .catch(() => {})
}

// 分页处理
const handlePageChange = (page) => {
  pagination.page = page
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
}

// 组件挂载时加载初始数据
onMounted(() => {
  generateRecommendations()
})
</script>

<style lang="scss" scoped>
.algorithm-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  .algorithm-icon {
    margin-bottom: 12px;
  }

  .algorithm-title {
    font-size: 16px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 8px;
  }

  .algorithm-desc {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
    text-align: center;
  }
}

.stat-card-alt {
  padding: 24px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);

  .stat-value {
    font-size: 32px;
    font-weight: 600;
    color: #1890ff;
    margin-bottom: 8px;
  }

  .stat-label {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.45);
    margin-bottom: 12px;
  }
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-secondary {
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}
</style>
