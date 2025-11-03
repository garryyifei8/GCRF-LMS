<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">AI 数据分析</h1>
      <p class="page-header-description">深度数据挖掘与可视化分析，辅助管理决策</p>
    </div>

    <!-- 时间选择器 -->
    <div class="card mb-md">
      <div class="card-content">
        <el-form :inline="true">
          <el-form-item label="分析维度">
            <el-select v-model="analysisConfig.dimension" placeholder="选择维度" style="width: 150px">
              <el-option label="时间维度" value="time" />
              <el-option label="分类维度" value="category" />
              <el-option label="年级维度" value="grade" />
              <el-option label="读者维度" value="reader" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="analysisConfig.dateRange"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 260px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Refresh" @click="refreshAnalysis">刷新数据</el-button>
            <el-button :icon="Download" @click="exportReport">导出报告</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="mb-lg">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card">
          <div class="metric-icon" style="background: #e6f7ff">
            <el-icon :size="32" color="#1890ff"><TrendCharts /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ metrics.borrowRate }}%</div>
            <div class="metric-label">借阅率</div>
            <div class="metric-trend">
              <el-icon color="#52c41a"><CaretTop /></el-icon>
              <span class="text-success">+2.3%</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card">
          <div class="metric-icon" style="background: #f6ffed">
            <el-icon :size="32" color="#52c41a"><Refresh /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ metrics.turnoverRate }}</div>
            <div class="metric-label">图书周转率</div>
            <div class="metric-trend">
              <el-icon color="#52c41a"><CaretTop /></el-icon>
              <span class="text-success">+0.5</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card">
          <div class="metric-icon" style="background: #fff1f0">
            <el-icon :size="32" color="#f5222d"><Warning /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ metrics.zeroBorrowRate }}%</div>
            <div class="metric-label">零借阅率</div>
            <div class="metric-trend">
              <el-icon color="#52c41a"><CaretBottom /></el-icon>
              <span class="text-success">-1.8%</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card">
          <div class="metric-icon" style="background: #fff7e6">
            <el-icon :size="32" color="#fa8c16"><User /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ metrics.activeReaderRate }}%</div>
            <div class="metric-label">读者活跃率</div>
            <div class="metric-trend">
              <el-icon color="#52c41a"><CaretTop /></el-icon>
              <span class="text-success">+3.1%</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="mb-md">
      <!-- 借阅趋势预测 -->
      <el-col :xs="24" :lg="12">
        <div class="card">
          <div class="card-title">
            借阅趋势预测
            <el-tag type="info" size="small" class="ml-sm">LSTM模型</el-tag>
          </div>
          <div ref="trendForecastChartRef" style="height: 350px"></div>
        </div>
      </el-col>

      <!-- 读者行为分析 -->
      <el-col :xs="24" :lg="12">
        <div class="card">
          <div class="card-title">读者行为热力图</div>
          <div ref="behaviorHeatmapRef" style="height: 350px"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb-md">
      <!-- 图书分类分析 -->
      <el-col :xs="24" :lg="12">
        <div class="card">
          <div class="card-title">图书分类借阅分析</div>
          <div ref="categoryAnalysisChartRef" style="height: 350px"></div>
        </div>
      </el-col>

      <!-- 读者群体细分 -->
      <el-col :xs="24" :lg="12">
        <div class="card">
          <div class="card-title">读者群体细分</div>
          <div ref="readerSegmentChartRef" style="height: 350px"></div>
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

    <!-- 采购建议 -->
    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <div class="card">
          <div class="card-title">
            AI 采购建议
            <el-tag type="warning" size="small" class="ml-sm">预测性分析</el-tag>
          </div>
          <div class="card-content">
            <el-table :data="purchaseRecommendations" stripe>
              <el-table-column type="index" label="优先级" width="80" />
              <el-table-column prop="category" label="分类" width="120" />
              <el-table-column prop="bookTitle" label="推荐图书" show-overflow-tooltip min-width="200" />
              <el-table-column prop="author" label="作者" width="120" />
              <el-table-column label="需求度" width="120">
                <template #default="{ row }">
                  <el-progress :percentage="row.demand" :stroke-width="12">
                    <span class="progress-text">{{ row.demand }}%</span>
                  </el-progress>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="推荐理由" show-overflow-tooltip min-width="180" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="handleAddToPurchase(row)">加入采购单</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>

      <!-- 剔旧建议 -->
      <el-col :xs="24" :lg="8">
        <div class="card">
          <div class="card-title">剔旧建议</div>
          <div class="card-content">
            <div class="weeding-list">
              <div v-for="book in weedingRecommendations" :key="book.id" class="weeding-item">
                <div class="weeding-info">
                  <div class="weeding-title">{{ book.title }}</div>
                  <div class="weeding-reason">
                    <el-tag size="small" type="danger">{{ book.reason }}</el-tag>
                  </div>
                  <div class="weeding-stats">
                    <span>{{ book.years }}年未借阅</span>
                    <span class="divider">|</span>
                    <span>出版于{{ book.publishYear }}</span>
                  </div>
                </div>
                <el-button type="danger" size="small" link @click="handleAddToWeeding(book)">
                  标记剔旧
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

// 分析配置
const analysisConfig = reactive({
  dimension: 'time',
  dateRange: []
})

// 核心指标
const metrics = ref({
  borrowRate: 85.2,
  turnoverRate: 3.8,
  zeroBorrowRate: 12.5,
  activeReaderRate: 76.8
})

// 图表引用
const trendForecastChartRef = ref()
const behaviorHeatmapRef = ref()
const categoryAnalysisChartRef = ref()
const readerSegmentChartRef = ref()

// AI洞察
const insights = ref([
  {
    time: '2小时前',
    type: 'success',
    title: '计算机类图书需求激增',
    content: 'AI检测到计算机类图书借阅量近7天增长45%，建议增加此类图书采购。特别是人工智能、Python编程相关书籍需求旺盛。',
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
    title: '高一年级阅读活跃度提升',
    content: '高一年级学生人均借阅量达到3.5本/月，较上月提升28%。建议针对该年级推出更多适读书单。',
    action: '生成书单'
  },
  {
    time: '2天前',
    type: 'info',
    title: '周末借阅高峰时段分析',
    content: '数据显示周六10:00-12:00和14:00-16:00是借阅高峰，建议在此时段增加服务人员配置。',
    action: '调整排班'
  }
])

// 采购建议
const purchaseRecommendations = ref([
  {
    id: 1,
    category: '计算机',
    bookTitle: 'ChatGPT原理与实践',
    author: '张三',
    demand: 95,
    reason: '基于荐购数据和搜索热度预测'
  },
  {
    id: 2,
    category: '文学',
    bookTitle: '人世间（全三册）',
    author: '梁晓声',
    demand: 88,
    reason: '近期影视改编，预计借阅需求上升'
  },
  {
    id: 3,
    category: '经济',
    bookTitle: '置身事内：中国政府与经济发展',
    author: '兰小欢',
    demand: 82,
    reason: '教师推荐书目，预约队列较长'
  },
  {
    id: 4,
    category: '科学',
    bookTitle: '生命是什么',
    author: '王立铭',
    demand: 76,
    reason: '科普类热门，符合读者阅读趋势'
  },
  {
    id: 5,
    category: '历史',
    bookTitle: '大明王朝的七张面孔',
    author: '张宏杰',
    demand: 71,
    reason: '历史类补充，提升馆藏丰富度'
  }
])

// 剔旧建议
const weedingRecommendations = ref([
  {
    id: 1,
    title: '早期计算机基础教程',
    reason: '内容过时',
    years: 5,
    publishYear: 2008
  },
  {
    id: 2,
    title: '老版教材合集',
    reason: '长期零借阅',
    years: 3,
    publishYear: 2012
  },
  {
    id: 3,
    title: '破损严重图书',
    reason: '损坏严重',
    years: 2,
    publishYear: 2015
  },
  {
    id: 4,
    title: '重复馆藏图书',
    reason: '副本过多',
    years: 4,
    publishYear: 2010
  }
])

// 初始化图表
const initCharts = () => {
  initTrendForecastChart()
  initBehaviorHeatmap()
  initCategoryAnalysisChart()
  initReaderSegmentChart()
}

// 借阅趋势预测图表
const initTrendForecastChart = () => {
  const chart = echarts.init(trendForecastChartRef.value)

  chart.setOption({
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['实际借阅', '预测借阅', '置信区间']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
    },
    yAxis: {
      type: 'value',
      name: '借阅量'
    },
    series: [
      {
        name: '实际借阅',
        type: 'line',
        data: [820, 932, 901, 934, 1290, 1330, 1320, null, null, null, null, null],
        smooth: true,
        itemStyle: { color: '#1890ff' }
      },
      {
        name: '预测借阅',
        type: 'line',
        data: [null, null, null, null, null, null, 1320, 1380, 1420, 1450, 1500, 1550],
        smooth: true,
        lineStyle: { type: 'dashed' },
        itemStyle: { color: '#52c41a' }
      },
      {
        name: '置信区间',
        type: 'line',
        data: [null, null, null, null, null, null, 1250, 1310, 1350, 1380, 1420, 1470],
        smooth: true,
        lineStyle: { type: 'dotted', opacity: 0.5 },
        itemStyle: { color: '#fa8c16' },
        areaStyle: { opacity: 0.2 }
      }
    ]
  })
}

// 读者行为热力图
const initBehaviorHeatmap = () => {
  const chart = echarts.init(behaviorHeatmapRef.value)

  const hours = ['8:00', '9:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00']
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

  const data = []
  for (let i = 0; i < days.length; i++) {
    for (let j = 0; j < hours.length; j++) {
      data.push([j, i, Math.floor(Math.random() * 100)])
    }
  }

  chart.setOption({
    tooltip: {
      position: 'top'
    },
    grid: {
      left: '10%',
      right: '4%',
      bottom: '10%',
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
      min: 0,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      inRange: {
        color: ['#e0f3ff', '#1890ff']
      }
    },
    series: [{
      type: 'heatmap',
      data: data,
      label: {
        show: false
      }
    }]
  })
}

// 图书分类分析图表
const initCategoryAnalysisChart = () => {
  const chart = echarts.init(categoryAnalysisChartRef.value)

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['借阅量', '零借阅率']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value'
    },
    yAxis: {
      type: 'category',
      data: ['计算机', '文学', '历史', '科学', '艺术', '经济', '哲学', '其他']
    },
    series: [
      {
        name: '借阅量',
        type: 'bar',
        data: [2340, 1890, 1250, 980, 760, 650, 420, 380],
        itemStyle: { color: '#1890ff' }
      },
      {
        name: '零借阅率',
        type: 'bar',
        data: [8, 12, 18, 15, 22, 25, 30, 35],
        itemStyle: { color: '#f5222d' }
      }
    ]
  })
}

// 读者群体细分图表
const initReaderSegmentChart = () => {
  const chart = echarts.init(readerSegmentChartRef.value)

  chart.setOption({
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 20
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
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
        data: [
          { value: 320, name: '高频阅读者', itemStyle: { color: '#52c41a' } },
          { value: 580, name: '活跃阅读者', itemStyle: { color: '#1890ff' } },
          { value: 280, name: '低频阅读者', itemStyle: { color: '#fa8c16' } },
          { value: 150, name: '流失读者', itemStyle: { color: '#f5222d' } }
        ]
      }
    ]
  })
}

// 刷新分析
const refreshAnalysis = () => {
  ElMessage.success('数据刷新成功')
  initCharts()
}

// 导出报告
const exportReport = () => {
  ElMessage.success('分析报告导出成功')
  // TODO: 实现导出功能
}

// 加入采购单
const handleAddToPurchase = (row) => {
  ElMessage.success(`已将《${row.bookTitle}》加入采购单`)
}

// 标记剔旧
const handleAddToWeeding = (book) => {
  ElMessage.success(`已将《${book.title}》标记为待剔旧`)
}

onMounted(() => {
  // 设置默认日期范围为最近30天
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 30)
  analysisConfig.dateRange = [
    start.toISOString().split('T')[0],
    end.toISOString().split('T')[0]
  ]

  initCharts()
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
  }
}

.progress-text {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.85);
}

.weeding-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.weeding-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
  transition: background 0.3s;

  &:hover {
    background: #f0f0f0;
  }

  .weeding-info {
    flex: 1;
  }

  .weeding-title {
    font-size: 14px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 8px;
  }

  .weeding-reason {
    margin-bottom: 8px;
  }

  .weeding-stats {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);

    .divider {
      margin: 0 8px;
    }
  }
}

.text-success {
  color: #52c41a;
}
</style>
