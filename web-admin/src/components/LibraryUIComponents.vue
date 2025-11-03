<template>
  <!-- 智慧图书馆UI组件库 -->
  <div class="library-ui-components">
    <!-- 统计卡片组件 -->
    <div class="component-section">
      <h3>统计卡片 StatCard</h3>
      <el-row :gutter="20">
        <el-col :span="6" v-for="stat in stats" :key="stat.title">
          <div class="stat-card" :style="{ '--gradient': stat.gradient }">
            <div class="stat-content">
              <div class="stat-title">{{ stat.title }}</div>
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-trend">
                <span :class="stat.trendUp ? 'trend-up' : 'trend-down'">
                  {{ stat.trend }}
                </span>
                <span class="trend-text">{{ stat.trendText }}</span>
              </div>
            </div>
            <div class="stat-icon">
              <el-icon :size="28"><component :is="stat.icon" /></el-icon>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 快速操作卡片 -->
    <div class="component-section">
      <h3>快速操作 QuickActions</h3>
      <el-row :gutter="16">
        <el-col :span="4" v-for="action in quickActions" :key="action.title">
          <div class="quick-action-card" @click="handleQuickAction(action)">
            <div class="action-icon" :style="{ background: action.color }">
              <el-icon :size="24"><component :is="action.icon" /></el-icon>
            </div>
            <div class="action-title">{{ action.title }}</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 数据表格 -->
    <div class="component-section">
      <h3>数据表格 DataTable</h3>
      <div class="table-card">
        <div class="table-header">
          <h4>最新借阅记录</h4>
          <el-button type="primary" size="small" round>查看全部</el-button>
        </div>
        <el-table :data="tableData" style="width: 100%" :row-class-name="tableRowClassName">
          <el-table-column prop="bookName" label="图书名称" />
          <el-table-column prop="borrower" label="借阅人" width="120" />
          <el-table-column prop="borrowDate" label="借阅日期" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag
                :type="scope.row.status === 'returned' ? 'success' :
                       scope.row.status === 'overdue' ? 'danger' : 'warning'"
                size="small"
                effect="light"
              >
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default>
              <el-button link type="primary" size="small">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 图表组件 -->
    <div class="component-section">
      <h3>数据可视化 Charts</h3>
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="chart-card">
            <h4>借阅趋势</h4>
            <div ref="trendChart" class="chart-container"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="chart-card">
            <h4>分类统计</h4>
            <div ref="categoryChart" class="chart-container"></div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 通知消息 -->
    <div class="component-section">
      <h3>通知消息 Notifications</h3>
      <div class="notification-card">
        <el-timeline>
          <el-timeline-item
            v-for="(activity, index) in activities"
            :key="index"
            :timestamp="activity.timestamp"
            :type="activity.type"
            :hollow="true"
          >
            {{ activity.content }}
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  Reading,
  User,
  TrendCharts,
  Warning,
  Plus,
  Search,
  Upload,
  Setting,
  Document,
  DataAnalysis
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

// 统计数据
const stats = ref([
  {
    title: '总藏书量',
    value: '25,678',
    trend: '+12.5%',
    trendText: '较上月',
    trendUp: true,
    icon: Reading,
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    title: '活跃读者',
    value: '3,456',
    trend: '+8.3%',
    trendText: '较上月',
    trendUp: true,
    icon: User,
    gradient: 'linear-gradient(135deg, #5AD8A6 0%, #52c41a 100%)'
  },
  {
    title: '今日借阅',
    value: '234',
    trend: '-2.1%',
    trendText: '较昨日',
    trendUp: false,
    icon: TrendCharts,
    gradient: 'linear-gradient(135deg, #5B8FF9 0%, #1890ff 100%)'
  },
  {
    title: '逾期未还',
    value: '12',
    trend: '+3',
    trendText: '待处理',
    trendUp: false,
    icon: Warning,
    gradient: 'linear-gradient(135deg, #FF6B6B 0%, #FF8787 100%)'
  }
])

// 快速操作
const quickActions = ref([
  { title: '新增图书', icon: Plus, color: '#667eea' },
  { title: '图书查询', icon: Search, color: '#5AD8A6' },
  { title: '批量导入', icon: Upload, color: '#5B8FF9' },
  { title: '系统设置', icon: Setting, color: '#F6BD16' },
  { title: '生成报表', icon: Document, color: '#FF6B6B' },
  { title: '数据分析', icon: DataAnalysis, color: '#764ba2' }
])

// 表格数据
const tableData = ref([
  {
    bookName: '深度学习',
    borrower: '张三',
    borrowDate: '2024-01-15',
    status: 'borrowing'
  },
  {
    bookName: '人工智能导论',
    borrower: '李四',
    borrowDate: '2024-01-14',
    status: 'returned'
  },
  {
    bookName: '算法导论',
    borrower: '王五',
    borrowDate: '2024-01-10',
    status: 'overdue'
  },
  {
    bookName: '设计模式',
    borrower: '赵六',
    borrowDate: '2024-01-13',
    status: 'borrowing'
  }
])

// 活动记录
const activities = ref([
  {
    content: '新增图书《深度学习实战》入库',
    timestamp: '2024-01-15 10:30',
    type: 'success'
  },
  {
    content: '读者张三借阅《算法导论》',
    timestamp: '2024-01-15 11:45',
    type: 'primary'
  },
  {
    content: '图书《设计模式》逾期未还提醒',
    timestamp: '2024-01-15 14:20',
    type: 'warning'
  },
  {
    content: '系统维护通知：今晚22:00-23:00',
    timestamp: '2024-01-15 15:00',
    type: 'danger'
  }
])

// 图表实例
const trendChart = ref(null)
const categoryChart = ref(null)

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    borrowing: '借阅中',
    returned: '已归还',
    overdue: '已逾期'
  }
  return statusMap[status] || status
}

// 表格行样式
const tableRowClassName = ({ row }) => {
  if (row.status === 'overdue') {
    return 'warning-row'
  }
  return ''
}

// 处理快速操作
const handleQuickAction = (action) => {
  ElMessage.success(`执行操作: ${action.title}`)
}

// 初始化图表
const initCharts = () => {
  // 借阅趋势图
  if (trendChart.value) {
    const chart = echarts.init(trendChart.value)
    const option = {
      color: ['#667eea'],
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '借阅量',
          type: 'bar',
          data: [120, 200, 150, 80, 70, 110, 130],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#667eea' },
              { offset: 1, color: '#764ba2' }
            ])
          },
          barWidth: '60%'
        }
      ]
    }
    chart.setOption(option)
  }

  // 分类统计图
  if (categoryChart.value) {
    const chart = echarts.init(categoryChart.value)
    const option = {
      tooltip: {
        trigger: 'item'
      },
      series: [
        {
          name: '图书分类',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
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
          data: [
            { value: 1048, name: '文学类', itemStyle: { color: '#667eea' } },
            { value: 735, name: '科技类', itemStyle: { color: '#5AD8A6' } },
            { value: 580, name: '历史类', itemStyle: { color: '#5B8FF9' } },
            { value: 484, name: '艺术类', itemStyle: { color: '#F6BD16' } },
            { value: 300, name: '其他', itemStyle: { color: '#FF6B6B' } }
          ]
        }
      ]
    }
    chart.setOption(option)
  }
}

onMounted(() => {
  initCharts()
})
</script>

<style scoped>
.library-ui-components {
  padding: 20px;
}

.component-section {
  margin-bottom: 40px;
}

.component-section h3 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #303133;
  display: flex;
  align-items: center;
}

.component-section h3::before {
  content: '';
  width: 4px;
  height: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
  margin-right: 10px;
}

/* 统计卡片样式 */
.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(0, 0, 0, 0.05);
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  background: var(--gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 8px;
}

.stat-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.trend-up {
  color: #5AD8A6;
}

.trend-down {
  color: #FF6B6B;
}

.trend-text {
  color: #C0C4CC;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

/* 快速操作卡片 */
.quick-action-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.quick-action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  color: white;
}

.action-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* 表格卡片 */
.table-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.table-header h4 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

:deep(.el-table) {
  --el-table-border-color: #f0f2f5;
}

:deep(.warning-row) {
  background-color: #fef0f0;
}

/* 图表卡片 */
.chart-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.chart-card h4 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

/* 通知卡片 */
.notification-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* Element Plus 样式覆盖 */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4191 100%);
}

:deep(.el-tag--light) {
  border-radius: 16px;
  padding: 0 12px;
}
</style>