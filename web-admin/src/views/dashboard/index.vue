<template>
  <div class="dashboard-container">
    <!-- 页面头部 -->
    <div class="dashboard-header">
      <div class="header-content">
        <div class="header-title">
          <h1>智慧图书馆数据中心</h1>
          <p>欢迎回来！今日图书馆运营概览</p>
        </div>
        <div class="header-actions">
          <el-button>
            <el-icon><Download /></el-icon>
            导出报表
          </el-button>
          <el-button type="primary">
            <el-icon><Plus /></el-icon>
            快速借阅
          </el-button>
          <el-button circle>
            <el-icon><Bell /></el-icon>
          </el-button>
          <el-button circle>
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 核心统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-content">
            <p class="stat-label">总馆藏量</p>
            <h3 class="stat-value">{{ formatNumber(dashboardData.totalBooks) }}</h3>
            <p class="stat-change">本月新增 +234 册</p>
          </div>
          <div class="stat-icon books">
            <el-icon :size="24"><Reading /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-content">
            <p class="stat-label">注册读者</p>
            <h3 class="stat-value">{{ formatNumber(dashboardData.totalReaders) }}</h3>
            <p class="stat-change">本周新增 +42 人</p>
          </div>
          <div class="stat-icon readers">
            <el-icon :size="24"><User /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-content">
            <p class="stat-label">今日借阅</p>
            <h3 class="stat-value">{{ dashboardData.borrowedToday }}</h3>
            <p class="stat-change positive">较昨日 +12.5%</p>
          </div>
          <div class="stat-icon borrowed">
            <el-icon :size="24"><Promotion /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-content">
            <p class="stat-label">逾期未还</p>
            <h3 class="stat-value">{{ dashboardData.overdueCount }}</h3>
            <p class="stat-change negative">需要处理</p>
          </div>
          <div class="stat-icon overdue">
            <el-icon :size="24"><Warning /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷操作卡片 -->
    <el-row :gutter="20" class="quick-action-cards">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="action-card" @click="$router.push('/circulation/borrow')">
          <div class="action-icon">
            <el-icon :size="24"><CirclePlus /></el-icon>
          </div>
          <div class="action-content">
            <h4>图书借出</h4>
            <p>扫码或搜索借阅图书</p>
            <el-button size="small" type="primary">立即借阅</el-button>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="action-card" @click="$router.push('/circulation/return')">
          <div class="action-icon">
            <el-icon :size="24"><Select /></el-icon>
          </div>
          <div class="action-content">
            <h4>图书归还</h4>
            <p>办理图书归还手续</p>
            <el-button size="small" type="primary">办理归还</el-button>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="action-card" @click="$router.push('/readers/list')">
          <div class="action-icon">
            <el-icon :size="24"><UserFilled /></el-icon>
          </div>
          <div class="action-content">
            <h4>读者管理</h4>
            <p>新增读者或办理借阅证</p>
            <el-button size="small" type="primary">管理读者</el-button>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="action-card" @click="$router.push('/books/list')">
          <div class="action-icon">
            <el-icon :size="24"><Collection /></el-icon>
          </div>
          <div class="action-content">
            <h4>图书入库</h4>
            <p>新书登记和上架管理</p>
            <el-button size="small" type="primary">图书管理</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 数据图表区域 -->
    <el-row :gutter="20" class="chart-container">
      <!-- 借阅趋势图 -->
      <el-col :xs="24" :lg="16">
        <div class="chart-card">
          <div class="chart-header">
            <h3>借阅流通趋势</h3>
            <div class="chart-actions">
              <el-radio-group v-model="trendPeriod" size="small">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="year">本年</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <div ref="trendChartRef" style="height: 320px"></div>
        </div>
      </el-col>

      <!-- 分类借阅占比 -->
      <el-col :xs="24" :lg="8">
        <div class="chart-card">
          <div class="chart-header">
            <h3>分类借阅分布</h3>
            <el-icon><More /></el-icon>
          </div>
          <div ref="categoryChartRef" style="height: 320px"></div>
        </div>
      </el-col>
    </el-row>

    <!-- AI推荐和热门图书 -->
    <el-row :gutter="20" class="content-cards">
      <!-- AI智能推荐 -->
      <el-col :xs="24" :lg="12">
        <div class="chart-card ai-card">
          <div class="chart-header">
            <h3>
              <el-icon><MagicStick /></el-icon>
              AI 智能推荐
            </h3>
            <el-tag type="success" size="small">协同过滤算法</el-tag>
          </div>
          <div class="ai-recommend-list">
            <div v-for="book in aiRecommendBooks" :key="book.id" class="recommend-item">
              <div class="book-cover">
                <el-image :src="book.cover" fit="cover" />
                <div class="recommend-score">
                  <el-icon><Star /></el-icon>
                  {{ book.score }}
                </div>
              </div>
              <div class="book-info">
                <h4>{{ book.title }}</h4>
                <p class="book-author">{{ book.author }}</p>
                <p class="book-reason">
                  <el-icon><InfoFilled /></el-icon>
                  {{ book.reason }}
                </p>
              </div>
              <el-button type="primary" size="small" plain>查看</el-button>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 实时动态 -->
      <el-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3>实时借阅动态</h3>
            <el-badge :value="12" class="item">
              <el-button size="small" circle>
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
          </div>
          <div class="activity-timeline">
            <el-timeline>
              <el-timeline-item
                v-for="(activity, index) in recentActivities"
                :key="index"
                :timestamp="activity.time"
                :type="activity.type"
                placement="top">
                <div class="timeline-content">
                  <p class="timeline-title">{{ activity.title }}</p>
                  <p class="timeline-detail">{{ activity.detail }}</p>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 排行榜区域 -->
    <el-row :gutter="20" class="ranking-container">
      <!-- 图书借阅排行 -->
      <el-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3>热门图书排行 TOP10</h3>
            <span class="chart-subtitle">本月数据</span>
          </div>
          <div class="ranking-list">
            <div class="ranking-item" v-for="(book, index) in topBooks" :key="index">
              <div class="ranking-number" :class="{ top: index < 3 }">
                {{ index + 1 }}
              </div>
              <div class="ranking-info">
                <div class="info-header">
                  <span class="name">{{ book.title }}</span>
                  <el-tag size="small" type="info">{{ book.category }}</el-tag>
                </div>
                <div class="info-footer">
                  <span>{{ book.author }}</span>
                  <span class="count">借阅 {{ book.borrowCount }} 次</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 活跃读者排行 -->
      <el-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3>活跃读者排行 TOP10</h3>
            <span class="chart-subtitle">本月数据</span>
          </div>
          <div class="ranking-list">
            <div class="ranking-item" v-for="(reader, index) in topReaders" :key="index">
              <div class="ranking-number" :class="{ top: index < 3 }">
                {{ index + 1 }}
              </div>
              <div class="ranking-info">
                <div class="info-header">
                  <span class="name">{{ reader.name }}</span>
                  <el-tag size="small" :type="reader.type === '教师' ? 'warning' : 'primary'">
                    {{ reader.type }}
                  </el-tag>
                </div>
                <div class="info-footer">
                  <span>{{ reader.department }}</span>
                  <span class="count">借阅 {{ reader.borrowCount }} 本</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 馆藏分析 -->
    <div class="table-card">
      <div class="table-header">
        <div>
          <h3>馆藏资源分析</h3>
          <p>各类图书存量及流通情况</p>
        </div>
        <el-button type="primary">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </div>
      <el-table :data="collectionAnalysis" style="width: 100%">
        <el-table-column prop="category" label="图书分类" width="150" />
        <el-table-column prop="total" label="馆藏数量" width="120">
          <template #default="scope">
            <span class="number-highlight">{{ scope.row.total.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="available" label="可借数量" width="120">
          <template #default="scope">
            <span :class="scope.row.available < 10 ? 'text-danger' : ''">
              {{ scope.row.available }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="流通率" width="180">
          <template #default="scope">
            <div class="progress-cell">
              <el-progress
                :percentage="scope.row.circulationRate"
                :show-text="false"
                :stroke-width="8"
              />
              <span>{{ scope.row.circulationRate }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="monthBorrow" label="月借阅量" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag
              :type="scope.row.status === '正常' ? 'success' : scope.row.status === '紧缺' ? 'danger' : 'warning'"
              size="small">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="120">
          <template #default>
            <el-button link type="primary" size="small">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import * as echarts from 'echarts'
import {
  Reading,
  User,
  Promotion,
  Warning,
  CirclePlus,
  Select,
  UserFilled,
  Collection,
  Download,
  Plus,
  Bell,
  Refresh,
  More,
  MagicStick,
  Star,
  InfoFilled
} from '@element-plus/icons-vue'
import {
  getOverview,
  getBorrowTrends,
  getCategoryStats,
  getBookRankings,
  getReaderRankings,
  getCollectionAnalysis,
  getRecentActivities
} from '@/api/analytics'

// 数据
const trendPeriod = ref('month')
const loading = ref(false)
const dashboardData = ref({
  totalBooks: 0,
  totalReaders: 0,
  borrowedToday: 0,
  overdueCount: 0
})

// AI推荐图书
const aiRecommendBooks = ref([
  {
    id: 1,
    title: '深度学习',
    author: 'Ian Goodfellow',
    cover: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iMTIwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSI4MCIgaGVpZ2h0PSIxMjAiIGZpbGw9IiM2NjdlZWEiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjIwIiBmaWxsPSIjZmZmZmZmIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+REw8L3RleHQ+PC9zdmc+',
    score: 0.95,
    reason: '基于您的借阅历史推荐'
  },
  {
    id: 2,
    title: '人工智能',
    author: 'Stuart Russell',
    cover: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iMTIwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSI4MCIgaGVpZ2h0PSIxMjAiIGZpbGw9IiM3NjRiYTIiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjIwIiBmaWxsPSIjZmZmZmZmIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+QUk8L3RleHQ+PC9zdmc+',
    score: 0.92,
    reason: '相似读者都在看'
  },
  {
    id: 3,
    title: 'Python编程',
    author: 'Eric Matthes',
    cover: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iMTIwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSI4MCIgaGVpZ2h0PSIxMjAiIGZpbGw9IiM1QjhGRjkiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjIwIiBmaWxsPSIjZmZmZmZmIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+UFk8L3RleHQ+PC9zdmc+',
    score: 0.89,
    reason: '热门技术图书'
  }
])

// 实时动态
const recentActivities = ref([])

// 热门图书
const topBooks = ref([])

// 活跃读者
const topReaders = ref([])

// 馆藏分析数据
const collectionAnalysis = ref([])

// 借阅趋势数据
const trendData = ref({
  labels: [],
  borrowCounts: [],
  returnCounts: []
})

// 分类统计数据
const categoryData = ref([])

// 图表引用
const trendChartRef = ref()
const categoryChartRef = ref()

// 格式化数字
const formatNumber = (num) => {
  return num.toLocaleString()
}

// 加载Dashboard数据
async function loadDashboardData() {
  try {
    loading.value = true

    // 并行加载所有数据
    const [
      overviewRes,
      trendsRes,
      categoryRes,
      booksRes,
      readersRes,
      collectionRes,
      activitiesRes
    ] = await Promise.all([
      getOverview(),
      getBorrowTrends({ timeRange: 'LAST_30_DAYS', granularity: 'DAILY' }),
      getCategoryStats(),
      getBookRankings({ rankBy: 'BORROW_COUNT', timeRange: 'THIS_MONTH', limit: 10 }),
      getReaderRankings({ rankBy: 'BORROW_COUNT', timeRange: 'THIS_MONTH', limit: 10 }),
      getCollectionAnalysis(),
      getRecentActivities({ limit: 5 })
    ])

    // 更新概览数据
    dashboardData.value = {
      totalBooks: overviewRes.data.totalBooks,
      totalReaders: overviewRes.data.totalReaders,
      borrowedToday: overviewRes.data.todayBorrows,
      overdueCount: overviewRes.data.overdueCount
    }

    // 更新趋势数据
    trendData.value = {
      labels: trendsRes.data.labels,
      borrowCounts: trendsRes.data.borrowCounts,
      returnCounts: trendsRes.data.returnCounts
    }

    // 更新分类数据
    categoryData.value = categoryRes.data.map(item => ({
      value: item.borrowCount,
      name: item.categoryName
    }))

    // 更新排行榜数据
    topBooks.value = booksRes.data.map(item => ({
      title: item.title,
      author: item.author,
      category: item.category,
      borrowCount: item.borrowCount
    }))

    topReaders.value = readersRes.data.map(item => ({
      name: item.name,
      type: item.type,
      department: item.department || item.grade,
      borrowCount: item.borrowCount
    }))

    // 更新馆藏分析
    collectionAnalysis.value = collectionRes.data

    // 更新活动数据
    recentActivities.value = activitiesRes.data

    // 初始化图表
    initTrendChart()
    initCategoryChart()
  } catch (error) {
    console.error('加载Dashboard数据失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboardData()
})

// 初始化趋势图
function initTrendChart() {
  if (!trendChartRef.value) return

  const chart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#667eea',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    legend: {
      data: ['借出', '归还'],
      right: 0,
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.value.labels
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#E4E7ED'
        }
      }
    },
    series: [
      {
        name: '借出',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: {
          color: '#667eea'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.05)' }
          ])
        },
        data: trendData.value.borrowCounts
      },
      {
        name: '归还',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: {
          color: '#764ba2'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(118, 75, 162, 0.3)' },
            { offset: 1, color: 'rgba(118, 75, 162, 0.05)' }
          ])
        },
        data: trendData.value.returnCounts
      }
    ]
  }
  chart.setOption(option)

  // 响应式
  window.addEventListener('resize', () => {
    chart.resize()
  })
}

// 初始化分类图表
function initCategoryChart() {
  if (!categoryChartRef.value) return

  const chart = echarts.init(categoryChartRef.value)
  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#667eea',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    color: ['#667eea', '#764ba2', '#5B8FF9', '#5AD8A6', '#F6BD16', '#E8684A'],
    series: [
      {
        type: 'pie',
        radius: ['45%', '75%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: categoryData.value
      }
    ]
  }
  chart.setOption(option)

  // 响应式
  window.addEventListener('resize', () => {
    chart.resize()
  })
}
</script>

<style lang="scss" scoped>
// 主容器 - 使用Figma的紫色渐变背景
.dashboard-container {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  padding: 24px;
}

// 头部样式
.dashboard-header {
  margin-bottom: 30px;

  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 20px;

    .header-title {
      h1 {
        color: #fff;
        font-size: 32px;
        font-weight: 700;
        margin: 0 0 8px;
        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }

      p {
        color: rgba(255, 255, 255, 0.95);
        font-size: 16px;
        margin: 0;
      }
    }

    .header-actions {
      display: flex;
      gap: 12px;

      :deep(.el-button) {
        background: rgba(255, 255, 255, 0.95);
        border: none;

        &.is-circle {
          background: rgba(255, 255, 255, 0.2);
          color: #fff;

          &:hover {
            background: rgba(255, 255, 255, 0.3);
          }
        }
      }
    }
  }
}

// 统计卡片 - 使用Figma的卡片风格
.stat-cards {
  margin-bottom: 24px;

  .stat-card {
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: blur(20px);
    border-radius: 16px;
    padding: 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    margin-bottom: 20px;
    border: 1px solid rgba(255, 255, 255, 0.5);

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
    }

    .stat-content {
      flex: 1;

      .stat-label {
        color: #606266;
        font-size: 14px;
        margin: 0 0 12px;
        font-weight: 500;
      }

      .stat-value {
        color: #303133;
        font-size: 36px;
        font-weight: 700;
        margin: 0 0 8px;
        line-height: 1.2;
        background: linear-gradient(135deg, #667eea, #764ba2);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }

      .stat-change {
        color: #909399;
        font-size: 14px;
        margin: 0;

        &.positive {
          color: #67c23a;
        }

        &.negative {
          color: #f56c6c;
        }
      }
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

      &.books {
        background: linear-gradient(135deg, #667eea, #5B8FF9);
      }

      &.readers {
        background: linear-gradient(135deg, #764ba2, #E91E63);
      }

      &.borrowed {
        background: linear-gradient(135deg, #5AD8A6, #52c41a);
      }

      &.overdue {
        background: linear-gradient(135deg, #FF6B6B, #FF8787);
      }
    }
  }
}

// 快捷操作卡片
.quick-action-cards {
  margin-bottom: 24px;

  .action-card {
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: blur(20px);
    border-radius: 16px;
    padding: 24px;
    display: flex;
    gap: 16px;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    margin-bottom: 20px;
    cursor: pointer;
    border: 1px solid rgba(255, 255, 255, 0.5);

    &:hover {
      transform: translateY(-4px) scale(1.02);
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);

      .action-icon {
        transform: rotate(5deg) scale(1.1);
      }
    }

    .action-icon {
      width: 48px;
      height: 48px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      flex-shrink: 0;
      transition: transform 0.3s;
    }

    .action-content {
      flex: 1;

      h4 {
        margin: 0 0 8px;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      p {
        margin: 0 0 16px;
        color: #606266;
        font-size: 14px;
      }

      :deep(.el-button) {
        background: linear-gradient(135deg, #667eea, #764ba2);
        border: none;
        color: #fff;

        &:hover {
          opacity: 0.9;
        }
      }
    }
  }
}

// 图表和内容卡片
.chart-container,
.content-cards,
.ranking-container {
  margin-bottom: 24px;

  .chart-card {
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: blur(20px);
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
    margin-bottom: 20px;
    height: 100%;
    border: 1px solid rgba(255, 255, 255, 0.5);

    &.ai-card {
      background: linear-gradient(135deg,
        rgba(255, 255, 255, 0.98),
        rgba(248, 249, 253, 0.98));
    }

    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      padding-bottom: 12px;
      border-bottom: 1px solid #f0f0f0;

      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: #303133;
        display: flex;
        align-items: center;
        gap: 8px;

        .el-icon {
          color: #667eea;
        }
      }

      .chart-subtitle {
        color: #909399;
        font-size: 14px;
      }

      .chart-actions {
        display: flex;
        gap: 12px;
      }
    }
  }
}

// AI推荐列表
.ai-recommend-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .recommend-item {
    display: flex;
    gap: 16px;
    padding: 16px;
    background: linear-gradient(135deg, #f9f9fb, #ffffff);
    border-radius: 12px;
    transition: all 0.3s;
    border: 1px solid #f0f0f0;

    &:hover {
      transform: translateX(4px);
      box-shadow: 0 4px 16px rgba(102, 126, 234, 0.1);
      border-color: #667eea;
    }

    .book-cover {
      position: relative;
      width: 80px;
      height: 120px;
      flex-shrink: 0;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

      .recommend-score {
        position: absolute;
        top: 4px;
        right: 4px;
        display: flex;
        align-items: center;
        gap: 2px;
        padding: 2px 6px;
        background: linear-gradient(135deg, #667eea, #764ba2);
        color: #fff;
        border-radius: 4px;
        font-size: 12px;
        font-weight: 600;
      }
    }

    .book-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: center;

      h4 {
        margin: 0 0 4px;
        font-size: 15px;
        font-weight: 600;
        color: #303133;
      }

      .book-author {
        font-size: 13px;
        color: #606266;
        margin: 0 0 8px;
      }

      .book-reason {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #909399;

        .el-icon {
          color: #667eea;
        }
      }
    }
  }
}

// 活动时间轴
.activity-timeline {
  .timeline-content {
    .timeline-title {
      font-weight: 500;
      color: #303133;
      margin: 0 0 4px;
    }

    .timeline-detail {
      font-size: 13px;
      color: #909399;
      margin: 0;
    }
  }

  :deep(.el-timeline-item__timestamp) {
    color: #909399;
    font-size: 13px;
  }

  :deep(.el-timeline-item__node) {
    background: linear-gradient(135deg, #667eea, #764ba2);
    border: none;
  }
}

// 排行榜列表
.ranking-list {
  .ranking-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;
    transition: all 0.3s;

    &:hover {
      padding-left: 8px;
      background: linear-gradient(90deg, rgba(102, 126, 234, 0.05), transparent);
    }

    &:last-child {
      border-bottom: none;
    }

    .ranking-number {
      width: 32px;
      height: 32px;
      background: #f0f0f0;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #606266;
      font-weight: 600;
      flex-shrink: 0;

      &.top {
        background: linear-gradient(135deg, #667eea, #764ba2);
        color: #fff;
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      }
    }

    .ranking-info {
      flex: 1;

      .info-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 4px;

        .name {
          font-size: 14px;
          font-weight: 500;
          color: #303133;
        }
      }

      .info-footer {
        font-size: 12px;
        color: #909399;
        display: flex;
        gap: 12px;

        .count {
          color: #667eea;
          font-weight: 500;
        }
      }
    }
  }
}

// 表格卡片
.table-card {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.5);

  .table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;

    h3 {
      margin: 0 0 8px;
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }

    p {
      margin: 0;
      color: #909399;
      font-size: 14px;
    }
  }

  .number-highlight {
    font-weight: 600;
    color: #667eea;
  }

  .text-danger {
    color: #f56c6c;
    font-weight: 500;
  }

  .progress-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    :deep(.el-progress) {
      flex: 1;
    }

    :deep(.el-progress-bar__outer) {
      background: #f0f0f0;
      border-radius: 100px;
    }

    :deep(.el-progress-bar__inner) {
      background: linear-gradient(90deg, #667eea, #764ba2);
      border-radius: 100px;
    }

    span {
      font-size: 13px;
      color: #606266;
      font-weight: 500;
      min-width: 40px;
    }
  }

  :deep(.el-table) {
    th {
      background: #f9f9fb;
      font-weight: 600;
      color: #606266;
    }

    tr:hover {
      td {
        background: linear-gradient(90deg, rgba(102, 126, 234, 0.03), transparent);
      }
    }
  }
}

// 响应式
@media (max-width: 768px) {
  .dashboard-container {
    padding: 16px;
  }

  .dashboard-header {
    .header-content {
      .header-title h1 {
        font-size: 24px;
      }
    }
  }

  .stat-card .stat-value {
    font-size: 28px;
  }
}
</style>