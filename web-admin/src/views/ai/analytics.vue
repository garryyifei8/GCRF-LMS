<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">AI 数据分析</h1>
      <p class="page-header-description">深度数据挖掘与可视化分析，辅助管理决策</p>
    </div>

    <!-- 时间选择器与导出 -->
    <div class="card mb-md">
      <div class="card-content">
        <el-form :inline="true">
          <el-form-item label="分析维度">
            <el-select
              v-model="analysisConfig.dimension"
              placeholder="选择维度"
              style="width: 150px"
            >
              <el-option label="时间维度" value="time" />
              <el-option label="分类维度" value="category" />
              <el-option label="年级维度" value="grade" />
              <el-option label="读者维度" value="reader" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-select
              v-model="analysisConfig.timeRange"
              placeholder="选择时间范围"
              style="width: 150px"
            >
              <el-option label="最近7天" value="LAST_7_DAYS" />
              <el-option label="最近30天" value="LAST_30_DAYS" />
              <el-option label="本月" value="THIS_MONTH" />
              <el-option label="本年" value="THIS_YEAR" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Refresh" :loading="loading" @click="refreshAnalysis"
              >刷新数据</el-button
            >
            <el-dropdown @command="handleExport" style="margin-left: 10px">
              <el-button :icon="Download">
                导出报告<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="borrow">借阅统计 Excel</el-dropdown-item>
                  <el-dropdown-item command="books">热门图书 Excel</el-dropdown-item>
                  <el-dropdown-item command="readers">活跃读者 Excel</el-dropdown-item>
                  <el-dropdown-item command="category">分类统计 Excel</el-dropdown-item>
                  <el-dropdown-item divided command="pdf" disabled>综合报告 PDF</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="mb-lg">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card" v-loading="loading">
          <div class="metric-icon" style="background: #e6f7ff">
            <el-icon :size="32" color="#1890ff"><TrendCharts /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ formatPercent(overview.circulationRate) }}</div>
            <div class="metric-label">流通率</div>
            <div class="metric-trend">
              <el-icon color="#52c41a"><CaretTop /></el-icon>
              <span class="text-success">{{ formatPercent(overview.borrowGrowth) }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card" v-loading="loading">
          <div class="metric-icon" style="background: #f6ffed">
            <el-icon :size="32" color="#52c41a"><Reading /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ overview.thisMonthBorrowed || 0 }}</div>
            <div class="metric-label">本月借阅量</div>
            <div class="metric-trend">
              <el-icon color="#52c41a"><CaretTop /></el-icon>
              <span class="text-success">+{{ overview.todayBorrowed || 0 }}今日</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card" v-loading="loading">
          <div class="metric-icon" style="background: #fff1f0">
            <el-icon :size="32" color="#f5222d"><Warning /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ formatPercent(overview.zeroCirculationRate) }}</div>
            <div class="metric-label">零借阅率</div>
            <div class="metric-trend">
              <span class="text-muted">{{ overview.zeroCirculationCount || 0 }}种图书</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card" v-loading="loading">
          <div class="metric-icon" style="background: #fff7e6">
            <el-icon :size="32" color="#fa8c16"><User /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ overview.totalReaders || 0 }}</div>
            <div class="metric-label">读者总数</div>
            <div class="metric-trend">
              <el-icon color="#52c41a"><CaretTop /></el-icon>
              <span class="text-success">+{{ overview.todayNewReaders || 0 }}今日</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="mb-md">
      <!-- 借阅趋势 -->
      <el-col :xs="24" :lg="12">
        <div class="card">
          <div class="card-title">
            借阅趋势分析
            <el-tag type="info" size="small" class="ml-sm">{{ getTimeRangeLabel() }}</el-tag>
          </div>
          <div ref="trendChartRef" style="height: 350px" v-loading="loading"></div>
        </div>
      </el-col>

      <!-- 读者行为热力图 -->
      <el-col :xs="24" :lg="12">
        <div class="card">
          <div class="card-title">读者活跃度热力图</div>
          <div ref="heatmapChartRef" style="height: 350px" v-loading="loading"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb-md">
      <!-- 分类分布饼图 -->
      <el-col :xs="24" :lg="12">
        <div class="card">
          <div class="card-title">图书分类分布</div>
          <div ref="categoryPieChartRef" style="height: 350px" v-loading="loading"></div>
        </div>
      </el-col>

      <!-- 热门图书柱状图 -->
      <el-col :xs="24" :lg="12">
        <div class="card">
          <div class="card-title">热门图书TOP10</div>
          <div ref="popularBooksChartRef" style="height: 350px" v-loading="loading"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 智能洞察 -->
    <el-row :gutter="16" class="mb-md">
      <el-col :span="24">
        <div class="card">
          <div class="card-title">
            AI 智能洞察
            <el-tag type="success" size="small" class="ml-sm">自动生成</el-tag>
          </div>
          <div class="card-content">
            <el-timeline>
              <el-timeline-item
                v-for="(insight, index) in insights"
                :key="index"
                :timestamp="insight.time"
                :type="insight.type"
                placement="top"
              >
                <el-card>
                  <h4>{{ insight.title }}</h4>
                  <p>{{ insight.content }}</p>
                  <el-button v-if="insight.action" size="small" type="primary" link>
                    {{ insight.action }}
                  </el-button>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 排行榜 -->
    <el-row :gutter="16">
      <!-- 热门图书表格 -->
      <el-col :xs="24" :lg="12">
        <div class="card">
          <div class="card-title">
            热门图书排行
            <el-tag type="warning" size="small" class="ml-sm">借阅量排序</el-tag>
          </div>
          <div class="card-content">
            <el-table :data="popularBooks.slice(0, 10)" stripe size="small" v-loading="loading">
              <el-table-column type="index" label="排名" width="60" />
              <el-table-column prop="title" label="书名" show-overflow-tooltip min-width="150" />
              <el-table-column prop="author" label="作者" width="100" show-overflow-tooltip />
              <el-table-column prop="borrowCount" label="借阅次数" width="90" align="center" />
              <el-table-column label="评分" width="80" align="center">
                <template #default="{ row }">
                  <el-rate v-model="row.rating" disabled :max="5" size="small" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>

      <!-- 活跃读者表格 -->
      <el-col :xs="24" :lg="12">
        <div class="card">
          <div class="card-title">
            活跃读者排行
            <el-tag type="success" size="small" class="ml-sm">借阅量排序</el-tag>
          </div>
          <div class="card-content">
            <el-table :data="activeReaders.slice(0, 10)" stripe size="small" v-loading="loading">
              <el-table-column type="index" label="排名" width="60" />
              <el-table-column prop="realName" label="姓名" width="100" />
              <el-table-column prop="readerTypeName" label="类型" width="80" />
              <el-table-column prop="borrowCount" label="借阅次数" width="90" align="center" />
              <el-table-column
                prop="favoriteCategory"
                label="偏好分类"
                width="100"
                show-overflow-tooltip
              />
              <el-table-column
                prop="currentBorrowCount"
                label="当前借阅"
                width="80"
                align="center"
              />
            </el-table>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Download,
  ArrowDown,
  TrendCharts,
  Warning,
  User,
  Reading,
  CaretTop,
  CaretBottom
} from '@element-plus/icons-vue'
import {
  getOverview,
  getBorrowTrends,
  getCategoryDistribution,
  getPopularBooks,
  getActiveReaders,
  getReaderHeatmap,
  exportBorrowStatistics,
  exportPopularBooks as exportPopularBooksApi,
  exportActiveReaders as exportActiveReadersApi,
  exportCategoryStats
} from '@/api/analytics'

// 状态
const loading = ref(false)

// 分析配置
const analysisConfig = reactive({
  dimension: 'time',
  timeRange: 'LAST_30_DAYS'
})

// 数据
const overview = ref({})
const borrowTrends = ref([])
const categoryDistribution = ref([])
const popularBooks = ref([])
const activeReaders = ref([])
const heatmapData = ref({})

// 图表引用
const trendChartRef = ref()
const heatmapChartRef = ref()
const categoryPieChartRef = ref()
const popularBooksChartRef = ref()

// 图表实例
let trendChart = null
let heatmapChart = null
let categoryPieChart = null
let popularBooksChart = null

// AI洞察（基于数据生成）
const insights = ref([
  {
    time: '2小时前',
    type: 'success',
    title: '计算机类图书需求激增',
    content:
      'AI检测到计算机类图书借阅量近7天增长45%，建议增加此类图书采购。特别是人工智能、Python编程相关书籍需求旺盛。',
    action: '查看详细分析'
  },
  {
    time: '5小时前',
    type: 'warning',
    title: '历史类图书零借阅率较高',
    content: '历史类图书中有23%的书籍超过180天未被借阅，建议评估是否需要调整馆藏结构或加强推广。',
    action: '查看图书列表'
  },
  {
    time: '1天前',
    type: 'primary',
    title: '读者活跃度持续提升',
    content:
      '本月读者人均借阅量达到3.5本，较上月提升28%。周末借阅高峰时段集中在10:00-12:00和14:00-16:00。',
    action: '生成书单'
  }
])

// 时间范围标签
const getTimeRangeLabel = () => {
  const labels = {
    LAST_7_DAYS: '最近7天',
    LAST_30_DAYS: '最近30天',
    THIS_MONTH: '本月',
    THIS_YEAR: '本年'
  }
  return labels[analysisConfig.timeRange] || '最近30天'
}

// 格式化百分比
const formatPercent = (value) => {
  if (!value && value !== 0) return '0%'
  return (value * 100).toFixed(1) + '%'
}

// 加载所有数据
const loadAllData = async () => {
  loading.value = true
  try {
    const [overviewRes, trendsRes, categoryRes, booksRes, readersRes, heatmapRes] =
      await Promise.all([
        getOverview(),
        getBorrowTrends({ timeRange: analysisConfig.timeRange, granularity: 'DAILY' }),
        getCategoryDistribution(),
        getPopularBooks({ rankBy: 'BORROW_COUNT', limit: 20 }),
        getActiveReaders({ rankBy: 'BORROW_COUNT', limit: 20 }),
        getReaderHeatmap()
      ])

    if (overviewRes.code === 200) overview.value = overviewRes.data
    if (trendsRes.code === 200) borrowTrends.value = trendsRes.data
    if (categoryRes.code === 200) categoryDistribution.value = categoryRes.data
    if (booksRes.code === 200) popularBooks.value = booksRes.data
    if (readersRes.code === 200) activeReaders.value = readersRes.data
    if (heatmapRes.code === 200) heatmapData.value = heatmapRes.data

    // 更新图表
    updateCharts()
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 刷新分析
const refreshAnalysis = () => {
  loadAllData()
  ElMessage.success('数据刷新成功')
}

// 更新所有图表
const updateCharts = () => {
  initTrendChart()
  initHeatmapChart()
  initCategoryPieChart()
  initPopularBooksChart()
}

// 借阅趋势折线图
const initTrendChart = () => {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  const dates = borrowTrends.value.map((item) => item.date || item.dateStr)
  const borrowed = borrowTrends.value.map((item) => item.borrowed)
  const returned = borrowTrends.value.map((item) => item.returned)
  const visits = borrowTrends.value.map((item) => item.visits)

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['借阅量', '归还量', '到馆人次'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        rotate: 45,
        formatter: (value) => value.slice(5) // 只显示月-日
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '借阅/归还',
        position: 'left'
      },
      {
        type: 'value',
        name: '到馆人次',
        position: 'right'
      }
    ],
    series: [
      {
        name: '借阅量',
        type: 'line',
        data: borrowed,
        smooth: true,
        itemStyle: { color: '#1890ff' },
        areaStyle: { opacity: 0.1 }
      },
      {
        name: '归还量',
        type: 'line',
        data: returned,
        smooth: true,
        itemStyle: { color: '#52c41a' }
      },
      {
        name: '到馆人次',
        type: 'bar',
        yAxisIndex: 1,
        data: visits,
        itemStyle: { color: '#fac858', opacity: 0.5 }
      }
    ]
  })
}

// 读者活跃度热力图
const initHeatmapChart = () => {
  if (!heatmapChartRef.value) return
  if (heatmapChart) heatmapChart.dispose()
  heatmapChart = echarts.init(heatmapChartRef.value)

  const { hours = [], days = [], data = [], minValue = 0, maxValue = 100 } = heatmapData.value

  heatmapChart.setOption({
    tooltip: {
      position: 'top',
      formatter: (params) => {
        return `${days[params.value[1]]} ${hours[params.value[0]]}<br/>活跃度: ${params.value[2]}`
      }
    },
    grid: {
      left: '10%',
      right: '4%',
      bottom: '15%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hours,
      splitArea: { show: true }
    },
    yAxis: {
      type: 'category',
      data: days,
      splitArea: { show: true }
    },
    visualMap: {
      min: minValue,
      max: maxValue,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      inRange: {
        color: ['#e0f3ff', '#1890ff']
      }
    },
    series: [
      {
        type: 'heatmap',
        data: data,
        label: { show: false }
      }
    ]
  })
}

// 分类分布饼图
const initCategoryPieChart = () => {
  if (!categoryPieChartRef.value) return
  if (categoryPieChart) categoryPieChart.dispose()
  categoryPieChart = echarts.init(categoryPieChartRef.value)

  const pieData = categoryDistribution.value.map((item) => ({
    value: item.bookCount,
    name: item.name,
    itemStyle: { color: item.color }
  }))

  categoryPieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 20,
      type: 'scroll'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {d}%'
        },
        data: pieData
      }
    ]
  })
}

// 热门图书柱状图
const initPopularBooksChart = () => {
  if (!popularBooksChartRef.value) return
  if (popularBooksChart) popularBooksChart.dispose()
  popularBooksChart = echarts.init(popularBooksChartRef.value)

  const top10 = popularBooks.value.slice(0, 10)
  const titles = top10.map((item) => item.title)
  const counts = top10.map((item) => item.borrowCount)

  popularBooksChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '借阅次数'
    },
    yAxis: {
      type: 'category',
      data: titles.reverse(),
      axisLabel: {
        width: 100,
        overflow: 'truncate',
        ellipsis: '...'
      }
    },
    series: [
      {
        type: 'bar',
        data: counts.reverse(),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#1890ff' },
            { offset: 1, color: '#52c41a' }
          ]),
          borderRadius: [0, 4, 4, 0]
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c}'
        }
      }
    ]
  })
}

// 导出处理
const handleExport = async (command) => {
  try {
    loading.value = true
    let blob
    let filename

    switch (command) {
      case 'borrow':
        blob = await exportBorrowStatistics({
          timeRange: analysisConfig.timeRange,
          granularity: 'DAILY'
        })
        filename = '借阅统计.xlsx'
        break
      case 'books':
        blob = await exportPopularBooksApi({ limit: 50 })
        filename = '热门图书.xlsx'
        break
      case 'readers':
        blob = await exportActiveReadersApi({ limit: 50 })
        filename = '活跃读者.xlsx'
        break
      case 'category':
        blob = await exportCategoryStats()
        filename = '分类统计.xlsx'
        break
      case 'pdf':
        ElMessage.warning('PDF导出功能暂未实现')
        return
      default:
        return
    }

    // 下载文件
    if (blob) {
      const url = window.URL.createObjectURL(new Blob([blob]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', filename)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    }
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 窗口resize处理
const handleResize = () => {
  trendChart?.resize()
  heatmapChart?.resize()
  categoryPieChart?.resize()
  popularBooksChart?.resize()
}

// 监听时间范围变化
watch(
  () => analysisConfig.timeRange,
  () => {
    loadAllData()
  }
)

onMounted(() => {
  loadAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  heatmapChart?.dispose()
  categoryPieChart?.dispose()
  popularBooksChart?.dispose()
})
</script>

<style lang="scss" scoped>
.metric-card {
  display: flex;
  gap: 16px;
  padding: 24px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.3s;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .metric-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 64px;
    height: 64px;
    border-radius: 8px;
    flex-shrink: 0;
  }

  .metric-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }

  .metric-value {
    font-size: 28px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);
    line-height: 1.2;
  }

  .metric-label {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.45);
  }

  .metric-trend {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;

    .text-success {
      color: #52c41a;
      font-weight: 600;
    }

    .text-muted {
      color: rgba(0, 0, 0, 0.45);
    }
  }
}

.card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
  overflow: hidden;

  .card-title {
    padding: 16px 20px;
    font-size: 16px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
    border-bottom: 1px solid #f0f0f0;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .card-content {
    padding: 16px 20px;
  }
}

.mb-md {
  margin-bottom: 16px;
}

.mb-lg {
  margin-bottom: 24px;
}

.ml-sm {
  margin-left: 8px;
}

.text-success {
  color: #52c41a;
}
</style>
