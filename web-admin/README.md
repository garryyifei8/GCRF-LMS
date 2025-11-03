# 国创睿峰智能图书馆管理系统 - Web管理端

> AI驱动的智能图书馆管理平台 Web端管理系统

## 项目简介

本项目是**国创睿峰智能图书馆管理系统**的Web管理端,采用Vue 3 + Element Plus + Vite构建,为图书馆管理员、馆员提供高效、智能的图书馆管理工具。

### 核心特性

- **现代化技术栈**: Vue 3 Composition API + Vite + Element Plus
- **完善的权限系统**: 基于RBAC的角色权限管理
- **响应式设计**: 适配1920px、1440px、1366px等主流分辨率
- **UI设计规范**: 严格遵循设计文档,统一的视觉语言
- **智能化功能**: 预留AI推荐、智能分析等接口
- **开发规范**: ESLint + Prettier代码规范

## 技术栈

### 核心框架

- **Vue 3.4+** - 渐进式JavaScript框架
- **Vite 5.0+** - 下一代前端构建工具
- **Vue Router 4** - 官方路由管理器
- **Pinia** - 新一代状态管理工具

### UI组件

- **Element Plus** - Vue 3 UI组件库
- **@element-plus/icons-vue** - Element Plus图标库
- **ECharts** - 数据可视化图表库

### 工具库

- **Axios** - HTTP请求库
- **dayjs** - 轻量级日期库
- **lodash-es** - JavaScript工具库
- **nprogress** - 页面加载进度条

### 开发工具

- **ESLint** - 代码质量检查
- **Prettier** - 代码格式化
- **Sass** - CSS预处理器

## 项目结构

```
web-admin/
├── public/                   # 静态资源
├── src/
│   ├── api/                  # API接口
│   ├── assets/               # 静态资源(图片、字体等)
│   ├── components/           # 公共组件
│   ├── composables/          # 组合式函数
│   ├── layouts/              # 布局组件
│   │   └── MainLayout.vue    # 主布局
│   ├── router/               # 路由配置
│   │   └── index.js          # 路由定义
│   ├── stores/               # Pinia状态管理
│   │   └── user.js           # 用户状态
│   ├── styles/               # 全局样式
│   │   ├── index.scss        # 样式入口
│   │   ├── variables.scss    # 样式变量
│   │   ├── reset.scss        # 重置样式
│   │   └── common.scss       # 公共样式
│   ├── utils/                # 工具函数
│   │   └── request.js        # Axios封装
│   ├── views/                # 页面组件
│   │   ├── login/            # 登录页
│   │   ├── dashboard/        # 首页(借阅概览)
│   │   ├── circulation/      # 流通管理
│   │   │   ├── borrow.vue    # 图书借出
│   │   │   ├── return.vue    # 图书归还
│   │   │   ├── records.vue   # 流通记录
│   │   │   └── reservations.vue # 预约管理
│   │   ├── books/            # 图书管理
│   │   │   ├── list.vue      # 图书列表
│   │   │   ├── catalog.vue   # 图书编目
│   │   │   ├── collection.vue # 图书典藏
│   │   │   └── inventory.vue # 图书盘点
│   │   ├── readers/          # 读者管理
│   │   │   ├── students.vue  # 学生读者
│   │   │   ├── teachers.vue  # 教师读者
│   │   │   └── card.vue      # 读者证办理
│   │   ├── system/           # 系统管理
│   │   │   ├── users.vue     # 用户管理
│   │   │   ├── roles.vue     # 角色权限
│   │   │   ├── config.vue    # 系统配置
│   │   │   └── backup.vue    # 数据备份
│   │   ├── profile/          # 个人中心
│   │   │   ├── info.vue      # 个人信息
│   │   │   └── password.vue  # 修改密码
│   │   └── error/            # 错误页面
│   │       └── 404.vue       # 404页面
│   ├── App.vue               # 根组件
│   └── main.js               # 入口文件
├── .eslintrc.cjs             # ESLint配置
├── .prettierrc               # Prettier配置
├── .gitignore                # Git忽略文件
├── vite.config.js            # Vite配置
├── package.json              # 项目依赖
├── index.html                # HTML模板
└── README.md                 # 项目说明

```

## 快速开始

### 环境要求

- Node.js >= 16.0
- npm >= 8.0 或 yarn >= 1.22 或 pnpm >= 7.0

### 安装依赖

```bash
# 进入项目目录
cd web-admin

# 安装依赖(推荐使用pnpm)
pnpm install

# 或使用npm
npm install

# 或使用yarn
yarn install
```

### 开发调试

```bash
# 启动开发服务器
npm run dev

# 访问地址: http://localhost:3000
```

### 生产构建

```bash
# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

### 代码规范

```bash
# ESLint检查
npm run lint

# Prettier格式化
npm run format
```

## 开发指南

### 样式规范

项目严格遵循 `/doc/ui-design-guidelines.md` UI设计规范:

- **主色**: #1890FF (蓝色)
- **成功色**: #52C41A (绿色)
- **警告色**: #FA8C16 (橙色)
- **错误色**: #F5222D (红色)
- **字体**: PingFang SC / Microsoft YaHei
- **字号**: 12px/14px/16px/18px/24px
- **间距**: 8px网格系统

### 路由规范

路由定义在 `src/router/index.js`,遵循以下规范:

- 使用懒加载: `component: () => import('@/views/...')`
- 设置meta元信息: `meta: { title: '页面标题', icon: '图标', requiresAuth: true }`
- 二级路由嵌套在对应的一级路由children中

### API调用规范

API请求使用封装的Axios实例 (`src/utils/request.js`):

```javascript
import request from '@/utils/request'

// GET请求
const data = await request.get('/api/books/list', { params: { page: 1 } })

// POST请求
const result = await request.post('/api/books/create', { title: '书名' })
```

### 状态管理规范

使用Pinia进行状态管理,示例:

```javascript
// 定义store
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: {}
  }),
  actions: {
    setToken(token) {
      this.token = token
    }
  }
})

// 使用store
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
```

### 组件开发规范

组件使用Vue 3 Composition API (setup语法糖):

```vue
<template>
  <div class="my-component">
    <h1>{{ title }}</h1>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const title = ref('组件标题')
</script>

<style lang="scss" scoped>
.my-component {
  padding: 24px;
}
</style>
```

## 核心功能模块

### 1. 登录模块 (`/login`)

- 用户名密码登录
- 记住密码
- 表单验证
- 登录成功后跳转

### 2. 借阅概览 (`/dashboard`)

- 核心数据卡片(总馆藏、读者数、今日借阅、逾期数)
- 全年流通趋势图表
- 分类借阅占比饼图
- 图书借阅排行TOP20
- 读者借阅排行TOP20
- 快捷操作悬浮按钮

### 3. 流通管理 (`/circulation/*`)

- 图书借出: 扫描读者证、扫描图书、确认借阅
- 图书归还: 扫描图书、匹配记录、处理逾期、确认归还
- 流通记录: 查询借阅/归还记录
- 预约管理: 预约图书管理

### 4. 图书管理 (`/books/*`)

- 图书列表: 查询、筛选、详情
- 图书编目: 智能编目(扫ISBN)、手工编目
- 图书典藏: 新增典藏、条码管理
- 图书盘点: 盘点任务、盘点结果

### 5. 读者管理 (`/readers/*`)

- 学生读者: 新增、编辑、批量导入
- 教师读者: 新增、编辑、批量导入
- 读者证办理: 人脸采集、生成读者证

### 6. 系统管理 (`/system/*`)

- 用户管理: 创建用户、分配角色
- 角色权限: 角色管理、权限配置
- 系统配置: 借阅规则、消息通知
- 数据备份: 备份/恢复数据

## Mock数据

在后端API未就绪前,项目使用Mock数据:

- 登录: 任意用户名密码均可登录(已实现)
- 其他接口: 需在对应页面中添加Mock数据

### 添加Mock数据示例

```javascript
// 在页面中添加Mock数据
const mockData = ref([
  { id: 1, title: '三体', author: '刘慈欣' },
  { id: 2, title: '活着', author: '余华' }
])
```

## 部署指南

### 开发环境

```bash
npm run dev
```

### 生产环境

```bash
# 1. 构建
npm run build

# 2. 部署dist目录到Web服务器(Nginx/Apache)

# Nginx配置示例
server {
  listen 80;
  server_name your-domain.com;
  root /path/to/dist;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api {
    proxy_pass http://backend-server:8080;
  }
}
```

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 开发路线图

### Phase 1: 基础设施 ✅

- [x] 项目初始化
- [x] 路由配置
- [x] Axios封装
- [x] Pinia状态管理
- [x] 全局样式

### Phase 2: 核心页面(进行中)

- [x] 登录页面
- [x] 首页(借阅概览)
- [ ] 图书借出页面
- [ ] 图书归还页面
- [ ] 图书列表页面
- [ ] 读者管理页面

### Phase 3: 完善功能

- [ ] 流通记录页面
- [ ] 图书编目页面
- [ ] 图书典藏页面
- [ ] 读者证办理页面
- [ ] 系统用户管理页面

### Phase 4: 优化与测试

- [ ] 性能优化
- [ ] 单元测试
- [ ] E2E测试
- [ ] 文档完善

## 常见问题

### 1. 端口冲突

如果3000端口被占用,修改 `vite.config.js` 中的 `server.port`

### 2. API代理配置

修改 `vite.config.js` 中的 `server.proxy` 配置后端API地址

### 3. 跨域问题

开发环境通过Vite代理解决,生产环境需配置Nginx反向代理

## 参考文档

- [Vue 3 官方文档](https://vuejs.org/)
- [Vite 官方文档](https://vitejs.dev/)
- [Element Plus 文档](https://element-plus.org/)
- [ECharts 文档](https://echarts.apache.org/)

## 项目设计文档

- 架构设计: `/doc/architect.md`
- PRD文档: `/doc/PRD.md`
- 信息架构: `/doc/information-architecture.md`
- UI设计规范: `/doc/ui-design-guidelines.md`
- 交互设计: `/doc/interaction-design.md`

## 开发团队

国创睿峰科技有限公司

## 许可证

Copyright © 2025 国创睿峰科技有限公司. All rights reserved.

---

**生成时间**: 2025-10-10
**版本**: v1.0.0
