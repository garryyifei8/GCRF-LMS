<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">AI 智能推荐</h1>
      <p class="page-header-description">基于协同过滤的个性化图书推荐系统</p>
    </div>

    <!-- 推荐算法说明 -->
    <el-row :gutter="16" class="mb-lg">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="algorithm-card">
          <el-icon class="algorithm-icon" :size="32" color="#1890ff"><DataAnalysis /></el-icon>
          <div class="algorithm-title">用户协同过滤</div>
          <div class="algorithm-desc">基于用户借阅行为相似度推荐</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="algorithm-card">
          <el-icon class="algorithm-icon" :size="32" color="#52c41a"><Document /></el-icon>
          <div class="algorithm-title">物品协同过滤</div>
          <div class="algorithm-desc">基于图书被借阅模式相似度</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="algorithm-card">
          <el-icon class="algorithm-icon" :size="32" color="#fa8c16"><TrendCharts /></el-icon>
          <div class="algorithm-title">热门推荐</div>
          <div class="algorithm-desc">基于借阅统计的热门图书</div>
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
                <el-select
                  v-model="recommendConfig.readerType"
                  placeholder="选择读者类型"
                  style="width: 100%"
                >
                  <el-option label="全部读者" value="all" />
                  <el-option label="学生读者" value="STUDENT" />
                  <el-option label="教师读者" value="TEACHER" />
                  <el-option label="职工读者" value="STAFF" />
                  <el-option label="校外读者" value="EXTERNAL" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="推荐算法">
                <el-select
                  v-model="recommendConfig.algorithm"
                  placeholder="选择推荐算法"
                  style="width: 100%"
                >
                  <el-option label="混合推荐" value="HYBRID" />
                  <el-option label="用户协同过滤" value="USER_CF" />
                  <el-option label="物品协同过滤" value="ITEM_CF" />
                  <el-option label="热门推荐" value="POPULAR" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="推荐数量">
                <el-input-number
                  v-model="recommendConfig.countPerReader"
                  :min="5"
                  :max="50"
                  :step="5"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="推荐场景">
                <el-select
                  v-model="recommendConfig.scene"
                  placeholder="选择推荐场景"
                  style="width: 100%"
                >
                  <el-option label="首页推荐" value="HOMEPAGE" />
                  <el-option label="详情页推荐" value="DETAIL" />
                  <el-option label="搜索推荐" value="SEARCH" />
                  <el-option label="主题推荐" value="TOPIC" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item>
            <el-button
              type="primary"
              :icon="Refresh"
              :loading="loading"
              @click="generateRecommendations"
            >
              生成推荐
            </el-button>
            <el-button :icon="Download" @click="exportRecommendations">导出结果</el-button>
            <el-button
              type="warning"
              :icon="RefreshRight"
              @click="recomputeMatrix"
              :loading="recomputeLoading"
            >
              重算相似度
            </el-button>
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
          <el-progress
            :percentage="recommendStats.precision"
            :stroke-width="8"
            :show-text="false"
          />
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="stat-card-alt">
          <div class="stat-value">{{ recommendStats.ctr }}%</div>
          <div class="stat-label">点击率 (CTR)</div>
          <el-progress
            :percentage="recommendStats.ctr"
            :stroke-width="8"
            :show-text="false"
            color="#52c41a"
          />
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="stat-card-alt">
          <div class="stat-value">{{ recommendStats.conversion }}%</div>
          <div class="stat-label">借阅转化率</div>
          <el-progress
            :percentage="recommendStats.conversion"
            :stroke-width="8"
            :show-text="false"
            color="#fa8c16"
          />
        </div>
      </el-col>
    </el-row>

    <!-- 推荐结果展示 -->
    <div class="card">
      <div class="card-title">
        推荐结果
        <el-tag v-if="recommendations.length > 0" class="ml-sm"
          >共 {{ pagination.total }} 条</el-tag
        >
      </div>
      <div class="card-content">
        <el-table v-loading="loading" :data="recommendations" stripe>
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="readerId" label="读者ID" width="100" />
          <el-table-column prop="readerName" label="读者姓名" width="120" />
          <el-table-column prop="bookId" label="图书ID" width="100" />
          <el-table-column
            prop="bookTitle"
            label="推荐图书"
            show-overflow-tooltip
            min-width="200"
          />
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
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <ActionIcons
                :actions="[
                  { key: 'view', label: '查看详情', icon: View, variant: 'primary' },
                  { key: 'push', label: '推送给读者', icon: Promotion, variant: 'success' }
                ]"
                @action="(k) => (k === 'view' ? handleViewDetail(row) : handlePushToReader(row))"
              />
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.pageNum"
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
          <el-descriptions-item label="读者姓名">{{
            currentRecommendation.readerName
          }}</el-descriptions-item>
          <el-descriptions-item label="读者ID">{{
            currentRecommendation.readerId
          }}</el-descriptions-item>
          <el-descriptions-item label="图书名称" :span="2">{{
            currentRecommendation.bookTitle
          }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{
            currentRecommendation.author
          }}</el-descriptions-item>
          <el-descriptions-item label="推荐分数">{{
            currentRecommendation.score.toFixed(3)
          }}</el-descriptions-item>
          <el-descriptions-item label="推荐算法">{{
            getAlgorithmName(currentRecommendation.algorithm)
          }}</el-descriptions-item>
          <el-descriptions-item label="推荐场景">{{
            getSceneName(currentRecommendation.scene)
          }}</el-descriptions-item>
          <el-descriptions-item label="推荐理由" :span="2">{{
            currentRecommendation.reason
          }}</el-descriptions-item>
        </el-descriptions>

        <!-- 读者历史偏好 -->
        <div class="mt-md" v-if="currentRecommendation.readerPreferences?.length">
          <h4>读者历史偏好</h4>
          <el-tag
            v-for="tag in currentRecommendation.readerPreferences"
            :key="tag"
            class="mr-sm mb-sm"
          >
            {{ tag }}
          </el-tag>
        </div>

        <!-- 图书特征 -->
        <div class="mt-md" v-if="currentRecommendation.bookTags?.length">
          <h4>图书特征标签</h4>
          <el-tag
            v-for="tag in currentRecommendation.bookTags"
            :key="tag"
            type="success"
            class="mr-sm mb-sm"
          >
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
import { Refresh, Download, View, Promotion, RefreshRight } from '@element-plus/icons-vue'
import { DataAnalysis, Document, TrendCharts, Connection } from '@element-plus/icons-vue'
import ActionIcons from '@/components/ActionIcons.vue'
import {
  batchRecommend,
  getRecommendStats,
  recomputeSimilarityMatrix,
  recordClick
} from '@/api/recommend'

// 推荐配置
const recommendConfig = reactive({
  readerType: 'all',
  algorithm: 'HYBRID',
  countPerReader: 10,
  scene: 'HOMEPAGE'
})

// 推荐统计
const recommendStats = ref({
  precision: 0,
  ctr: 0,
  conversion: 0
})

// 表格数据
const loading = ref(false)
const recomputeLoading = ref(false)
const recommendations = ref([])

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentRecommendation = ref(null)

// 算法名称映射
const algorithmMap = {
  USER_CF: '用户协同过滤',
  ITEM_CF: '物品协同过滤',
  POPULAR: '热门推荐',
  HYBRID: '混合推荐'
}

// 场景名称映射
const sceneMap = {
  HOMEPAGE: '首页推荐',
  DETAIL: '详情页推荐',
  SEARCH: '搜索推荐',
  TOPIC: '主题推荐'
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

// 加载推荐统计
const loadRecommendStats = async () => {
  try {
    const res = await getRecommendStats(30)
    if (res.code === 200 && res.data) {
      recommendStats.value = {
        precision: res.data.precision || 0,
        ctr: res.data.ctr || 0,
        conversion: res.data.conversion || 0
      }
    }
  } catch (error) {
    console.error('Failed to load recommend stats:', error)
  }
}

// 生成推荐
const generateRecommendations = async () => {
  loading.value = true
  try {
    const res = await batchRecommend({
      readerType: recommendConfig.readerType,
      algorithm: recommendConfig.algorithm,
      countPerReader: recommendConfig.countPerReader,
      scene: recommendConfig.scene,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })

    if (res.code === 200 && res.data) {
      recommendations.value = res.data.records || []
      pagination.total = res.data.total || 0
      ElMessage.success(`成功生成 ${recommendations.value.length} 条推荐`)
    } else {
      ElMessage.error(res.message || '生成推荐失败')
    }
  } catch (error) {
    console.error('Generate recommendations error:', error)
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

  // 简单的CSV导出
  const headers = [
    '排名',
    '读者ID',
    '读者姓名',
    '图书ID',
    '推荐图书',
    '作者',
    '推荐分数',
    '算法',
    '推荐理由'
  ]
  const rows = recommendations.value.map((rec, index) => [
    index + 1,
    rec.readerId,
    rec.readerName,
    rec.bookId,
    rec.bookTitle,
    rec.author,
    rec.score.toFixed(3),
    getAlgorithmName(rec.algorithm),
    rec.reason
  ])

  const csvContent = [headers.join(','), ...rows.map((row) => row.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `推荐结果_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)

  ElMessage.success('推荐结果导出成功')
}

// 重新计算相似度矩阵
const recomputeMatrix = async () => {
  try {
    await ElMessageBox.confirm('重新计算相似度矩阵可能需要较长时间，确定要执行吗？', '确认操作', {
      type: 'warning'
    })

    recomputeLoading.value = true
    const res = await recomputeSimilarityMatrix()
    if (res.code === 200) {
      ElMessage.success('相似度矩阵重新计算已触发')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Recompute matrix error:', error)
      ElMessage.error('重新计算失败')
    }
  } finally {
    recomputeLoading.value = false
  }
}

// 查看详情
const handleViewDetail = async (row) => {
  currentRecommendation.value = row
  detailDialogVisible.value = true

  // 记录点击
  try {
    await recordClick(row.readerId, row.bookId)
  } catch (error) {
    console.warn('Failed to record click:', error)
  }
}

// 推送给读者
const handlePushToReader = (row) => {
  ElMessageBox.confirm(
    `确定要将《${row.bookTitle}》推送给读者 ${row.readerName} 吗？`,
    '推送确认',
    {
      type: 'info'
    }
  )
    .then(async () => {
      try {
        // TODO: 实际调用推送API（需要对接通知服务）
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
  pagination.pageNum = page
  generateRecommendations()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  generateRecommendations()
}

// 组件挂载时加载初始数据
onMounted(() => {
  loadRecommendStats()
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
