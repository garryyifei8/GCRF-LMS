<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 p-8">
    <div class="max-w-7xl mx-auto">
      <!-- 页面标题 -->
      <div class="mb-8">
        <h1 class="text-4xl font-bold text-gray-900 dark:text-gray-100 mb-2">
          UI 组件库演示
        </h1>
        <p class="text-gray-600 dark:text-gray-400">
          基于 Tailwind CSS 和 Vue 3 的现代化组件库
        </p>
      </div>

      <!-- Button 组件演示 -->
      <section class="mb-12">
        <h2 class="text-2xl font-semibold text-gray-900 dark:text-gray-100 mb-6">
          Button 按钮组件
        </h2>

        <!-- 按钮变体 -->
        <div class="card p-6 mb-6">
          <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
            按钮变体
          </h3>
          <div class="flex flex-wrap gap-4">
            <Button variant="primary" @click="handleClick">Primary 按钮</Button>
            <Button variant="secondary">Secondary 按钮</Button>
            <Button variant="danger">Danger 按钮</Button>
            <Button variant="text">Text 按钮</Button>
            <Button variant="ai">AI 按钮</Button>
          </div>
        </div>

        <!-- 按钮尺寸 -->
        <div class="card p-6 mb-6">
          <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
            按钮尺寸
          </h3>
          <div class="flex flex-wrap items-center gap-4">
            <Button size="xs">超小按钮</Button>
            <Button size="sm">小按钮</Button>
            <Button size="md">中等按钮</Button>
            <Button size="lg">大按钮</Button>
            <Button size="xl">超大按钮</Button>
          </div>
        </div>

        <!-- 按钮状态 -->
        <div class="card p-6">
          <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
            按钮状态
          </h3>
          <div class="flex flex-wrap gap-4">
            <Button :loading="true">加载中...</Button>
            <Button :disabled="true">禁用按钮</Button>
            <Button icon="Plus">带图标</Button>
            <Button icon="Download" icon-position="right">右侧图标</Button>
            <Button full-width>全宽按钮</Button>
          </div>
        </div>
      </section>

      <!-- Card 组件演示 -->
      <section class="mb-12">
        <h2 class="text-2xl font-semibold text-gray-900 dark:text-gray-100 mb-6">
          Card 卡片组件
        </h2>

        <!-- 基础卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <Card variant="default">
            <template #header>
              <h3 class="text-lg font-semibold">默认卡片</h3>
            </template>
            <p class="text-gray-600 dark:text-gray-400">
              这是一个默认样式的卡片组件
            </p>
          </Card>

          <Card variant="bordered">
            <template #header>
              <h3 class="text-lg font-semibold">边框卡片</h3>
            </template>
            <p class="text-gray-600 dark:text-gray-400">
              这是一个带边框的卡片组件
            </p>
          </Card>

          <Card variant="elevated" hoverable>
            <template #header>
              <h3 class="text-lg font-semibold">悬浮卡片</h3>
            </template>
            <p class="text-gray-600 dark:text-gray-400">
              鼠标悬停时会有动画效果
            </p>
          </Card>

          <Card variant="flat">
            <template #header>
              <h3 class="text-lg font-semibold">扁平卡片</h3>
            </template>
            <p class="text-gray-600 dark:text-gray-400">
              这是一个扁平样式的卡片
            </p>
          </Card>
        </div>

        <!-- 统计卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <StatCard
            title="总图书数"
            :value="1234"
            icon="Book"
            color="primary"
            trend="up"
            trend-value="+12.5%"
            trend-text="较上月"
          />
          <StatCard
            title="在借图书"
            :value="567"
            icon="Reading"
            color="success"
            trend="up"
            trend-value="+8.3%"
            trend-text="较上月"
          />
          <StatCard
            title="读者人数"
            :value="890"
            icon="User"
            color="warning"
            trend="down"
            trend-value="-3.2%"
            trend-text="较上月"
          />
          <StatCard
            title="今日借阅"
            :value="45"
            icon="TrendCharts"
            color="info"
            trend="flat"
            trend-value="0%"
            trend-text="与昨日持平"
          />
        </div>

        <!-- AI 推荐卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <AICard
            title="三体"
            description="刘慈欣创作的科幻小说,讲述了地球文明和三体文明的信息交流、生死搏杀及两个文明在宇宙中的兴衰历程。"
            image="https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=400&h=300&fit=crop"
            :tags="['科幻', '刘慈欣', '雨果奖']"
            :score="95"
            :show-a-i-badge="true"
          >
            <template #actions>
              <Button size="sm" variant="primary" full-width>查看详情</Button>
            </template>
          </AICard>

          <AICard
            title="百年孤独"
            description="加西亚·马尔克斯的代表作,描写了布恩迪亚家族七代人的传奇故事,以及加勒比海沿岸小镇马孔多的百年兴衰。"
            image="https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400&h=300&fit=crop"
            :tags="['文学', '魔幻现实', '诺贝尔奖']"
            :score="92"
          >
            <template #actions>
              <Button size="sm" variant="primary" full-width>查看详情</Button>
            </template>
          </AICard>

          <AICard
            title="人类简史"
            description="尤瓦尔·赫拉利的畅销书,从认知革命、农业革命到科学革命,讲述了人类的历史发展进程。"
            image="https://images.unsplash.com/photo-1491841573634-28140fc7ced7?w=400&h=300&fit=crop"
            :tags="['历史', '人类学', '畅销书']"
            :score="88"
          >
            <template #actions>
              <Button size="sm" variant="primary" full-width>查看详情</Button>
            </template>
          </AICard>
        </div>
      </section>

      <!-- Input 组件演示 -->
      <section class="mb-12">
        <h2 class="text-2xl font-semibold text-gray-900 dark:text-gray-100 mb-6">
          Input 输入框组件
        </h2>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- 基础输入框 -->
          <div class="card p-6">
            <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
              基础输入框
            </h3>
            <div class="space-y-4">
              <Input
                v-model="form.name"
                label="姓名"
                placeholder="请输入姓名"
                :required="true"
              />
              <Input
                v-model="form.email"
                type="email"
                label="邮箱"
                placeholder="请输入邮箱地址"
                help-text="我们会保护您的邮箱隐私"
              />
              <Input
                v-model="form.password"
                type="password"
                label="密码"
                placeholder="请输入密码"
              />
            </div>
          </div>

          <!-- 带图标的输入框 -->
          <div class="card p-6">
            <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
              带图标输入框
            </h3>
            <div class="space-y-4">
              <Input
                v-model="form.search"
                label="搜索"
                placeholder="搜索图书..."
                prefix-icon="Search"
                :clearable="true"
              />
              <Input
                v-model="form.phone"
                type="tel"
                label="手机号"
                placeholder="请输入手机号"
                prefix-icon="Phone"
              />
              <Input
                v-model="form.website"
                type="url"
                label="网站"
                placeholder="https://example.com"
                prefix-icon="Link"
              />
            </div>
          </div>

          <!-- 输入框尺寸 -->
          <div class="card p-6">
            <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
              输入框尺寸
            </h3>
            <div class="space-y-4">
              <Input
                v-model="form.small"
                size="sm"
                label="小尺寸"
                placeholder="小尺寸输入框"
              />
              <Input
                v-model="form.medium"
                size="md"
                label="中等尺寸"
                placeholder="中等尺寸输入框"
              />
              <Input
                v-model="form.large"
                size="lg"
                label="大尺寸"
                placeholder="大尺寸输入框"
              />
            </div>
          </div>

          <!-- 输入框状态 -->
          <div class="card p-6">
            <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
              输入框状态
            </h3>
            <div class="space-y-4">
              <Input
                v-model="form.success"
                label="成功状态"
                placeholder="验证成功"
                status="success"
                help-text="格式正确"
              />
              <Input
                v-model="form.error"
                label="错误状态"
                placeholder="验证失败"
                status="error"
                error-text="该字段不能为空"
              />
              <Input
                v-model="form.warning"
                label="警告状态"
                placeholder="需要注意"
                status="warning"
                help-text="密码强度较弱"
              />
              <Input
                label="禁用状态"
                placeholder="禁用的输入框"
                :disabled="true"
                model-value="不可编辑"
              />
            </div>
          </div>

          <!-- 字数限制 -->
          <div class="card p-6 lg:col-span-2">
            <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
              字数限制
            </h3>
            <Input
              v-model="form.description"
              label="图书简介"
              placeholder="请输入图书简介,不超过100字"
              :maxlength="100"
              :show-count="true"
              help-text="简短描述这本书的主要内容"
            />
          </div>
        </div>
      </section>

      <!-- 提示信息 -->
      <div class="card p-6 bg-indigo-50 dark:bg-indigo-900/20 border-indigo-200 dark:border-indigo-800">
        <div class="flex items-start gap-3">
          <el-icon class="text-indigo-600 dark:text-indigo-400 mt-0.5" :size="20">
            <InfoFilled />
          </el-icon>
          <div>
            <h4 class="font-semibold text-indigo-900 dark:text-indigo-100 mb-1">
              组件库说明
            </h4>
            <p class="text-sm text-indigo-700 dark:text-indigo-300">
              以上是基于 Tailwind CSS 和 Vue 3 Composition API 构建的现代化组件库演示。
              所有组件都支持深色模式、TypeScript 类型定义,并且完全响应式设计。
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import Button from '@/components/ui/Button'
import { Card, StatCard, AICard } from '@/components/ui/Card'
import Input from '@/components/ui/Input'

// 表单数据
const form = reactive({
  name: '',
  email: '',
  password: '',
  search: '',
  phone: '',
  website: '',
  small: '',
  medium: '',
  large: '',
  success: '正确的输入',
  error: '',
  warning: '123456',
  description: ''
})

// 处理点击事件
const handleClick = () => {
  console.log('按钮被点击了!')
}
</script>

<style scoped>
/* 使用全局 Tailwind 样式 */
</style>
