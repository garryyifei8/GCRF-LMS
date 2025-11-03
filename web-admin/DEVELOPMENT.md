# Web管理端开发说明

## 项目概述

**国创睿峰智能图书馆管理系统 - Web管理端** 已完成基础架构搭建和核心模块开发。

## 已完成内容

### ✅ 阶段1: 项目初始化和基础设施

1. **项目结构** - 完整的Vue 3 + Vite项目结构
2. **依赖管理** - package.json配置完成,包含所有必要依赖
3. **构建配置** - Vite配置、ESLint、Prettier配置完成
4. **全局样式** - 严格遵循UI设计规范的样式系统

### ✅ 阶段2: 核心基础模块

1. **路由系统** (`src/router/index.js`)
   - Vue Router 4配置
   - 路由守卫(权限验证)
   - 页面标题自动设置
   - NProgress加载进度条

2. **HTTP请求** (`src/utils/request.js`)
   - Axios封装
   - 请求/响应拦截器
   - 统一错误处理
   - Token自动添加

3. **状态管理** (`src/stores/user.js`)
   - Pinia状态管理
   - 用户状态管理
   - 登录/登出逻辑
   - 权限判断方法

4. **布局组件** (`src/layouts/MainLayout.vue`)
   - 侧边栏导航(支持折叠)
   - 顶部导航栏
   - 面包屑导航
   - 用户下拉菜单
   - 全屏切换

### ✅ 阶段3: 页面开发

#### P0 核心页面

1. **登录页面** (`src/views/login/index.vue`)
   - 用户名密码登录
   - 表单验证
   - 记住密码
   - 响应式设计

2. **借阅概览/首页** (`src/views/dashboard/index.vue`)
   - 核心数据卡片(总馆藏、读者数、今日借阅、逾期数)
   - ECharts图表(流通趋势、分类占比)
   - 图书借阅排行TOP20
   - 读者借阅排行TOP20
   - 快捷操作悬浮按钮

3. **其他核心页面模板**
   - 图书借出 (`circulation/borrow.vue`)
   - 图书归还 (`circulation/return.vue`)
   - 图书列表 (`books/list.vue`)
   - 读者管理 (`readers/students.vue`)

#### P1 重要页面模板

- 流通记录 (`circulation/records.vue`)
- 图书编目 (`books/catalog.vue`)
- 图书典藏 (`books/collection.vue`)
- 读者证办理 (`readers/card.vue`)
- 用户管理 (`system/users.vue`)

#### P2 次要页面模板

- 预约管理 (`circulation/reservations.vue`)
- 图书盘点 (`books/inventory.vue`)
- 系统配置 (`system/config.vue`)
- 个人中心 (`profile/info.vue`)
- 404页面 (`error/404.vue`)

## 项目启动

### 1. 安装依赖

```bash
cd web-admin
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3000

### 3. 登录系统

- 用户名: 任意
- 密码: 任意(长度≥6位)

目前使用Mock登录,会自动生成token和用户信息。

## 开发指南

### 页面开发流程

1. **创建页面文件**
   在 `src/views/` 对应目录创建 `.vue` 文件

2. **编写页面组件**
   ```vue
   <template>
     <div class="page-container">
       <div class="page-header">
         <h1 class="page-header-title">页面标题</h1>
       </div>
       <!-- 页面内容 -->
     </div>
   </template>

   <script setup>
   import { ref } from 'vue'
   // 页面逻辑
   </script>

   <style lang="scss" scoped>
   // 页面样式
   </style>
   ```

3. **配置路由**
   在 `src/router/index.js` 中已配置好所有路由,无需额外配置

4. **API调用**
   ```javascript
   import request from '@/utils/request'

   // 获取数据
   const fetchData = async () => {
     const data = await request.get('/api/endpoint')
   }
   ```

### 样式开发规范

使用全局样式变量 (`src/styles/variables.scss`):

```scss
// 使用主色
color: $primary-color;

// 使用间距
padding: $spacing-md;

// 使用圆角
border-radius: $border-radius-base;
```

常用工具类(已在 `src/styles/common.scss` 定义):

```html
<div class="flex-between mb-md">
  <span class="text-primary font-semibold">标题</span>
  <el-button>操作</el-button>
</div>
```

### 组件使用

Element Plus组件已自动导入,直接使用:

```vue
<template>
  <el-button type="primary">按钮</el-button>
  <el-input v-model="value" placeholder="请输入" />
  <el-table :data="tableData">
    <el-table-column prop="name" label="姓名" />
  </el-table>
</template>
```

## 后续开发建议

### 1. 完善核心页面逻辑

目前大部分页面只有模板,需要添加具体业务逻辑:

- 图书借出: 扫描读者证、扫描图书、验证规则、确认借出
- 图书归还: 扫描图书、匹配记录、处理逾期、确认归还
- 图书列表: 查询、筛选、分页、详情
- 读者管理: CRUD操作、批量导入

### 2. 创建公共组件

建议在 `src/components/` 创建可复用组件:

- `PageHeader.vue` - 页面头部
- `DataTable.vue` - 数据表格
- `SearchForm.vue` - 搜索表单
- `BarcodeScanner.vue` - 条码扫描
- `FaceCapture.vue` - 人脸采集

### 3. API接口开发

在 `src/api/` 创建API模块:

```javascript
// src/api/books.js
import request from '@/utils/request'

export function getBookList(params) {
  return request.get('/books/list', { params })
}

export function createBook(data) {
  return request.post('/books/create', data)
}
```

### 4. Mock数据

在后端API未就绪前,建议使用Mock数据:

- 安装 `vite-plugin-mock`
- 创建 `mock/` 目录
- 编写Mock接口

### 5. 添加单元测试

- 安装 `vitest`
- 为关键组件和工具函数添加测试

### 6. 性能优化

- 路由懒加载(已实现)
- 图片懒加载
- 虚拟滚动(长列表)
- 组件缓存(keep-alive)

## 技术亮点

1. **Vue 3 Composition API** - 使用setup语法糖,代码更简洁
2. **Vite构建** - 快速的开发服务器和HMR
3. **Element Plus自动导入** - 无需手动导入组件
4. **Pinia状态管理** - 比Vuex更轻量、更简单
5. **SCSS变量系统** - 严格遵循UI设计规范
6. **路由权限控制** - 完善的权限验证机制
7. **Axios拦截器** - 统一处理请求和响应
8. **NProgress进度条** - 优秀的用户体验

## 注意事项

1. **代码规范**: 提交前运行 `npm run lint` 检查代码
2. **样式规范**: 严格使用设计文档中的颜色、字号、间距
3. **组件命名**: 使用大驼峰命名(PascalCase)
4. **文件命名**: 使用小写加连字符(kebab-case)
5. **提交规范**: 使用语义化提交信息

## 常用命令

```bash
# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview

# 代码检查
npm run lint

# 代码格式化
npm run format
```

## 相关文档

- [README.md](./README.md) - 项目说明
- [UI设计规范](/doc/ui-design-guidelines.md)
- [交互设计文档](/doc/interaction-design.md)
- [信息架构文档](/doc/information-architecture.md)
- [PRD产品需求](/doc/PRD.md)

## 问题反馈

开发过程中遇到问题,请查阅:

1. Vue 3官方文档
2. Element Plus官方文档
3. 项目README和设计文档
4. 或联系技术负责人

---

**最后更新**: 2025-10-10
**当前版本**: v1.0.0
**项目状态**: 基础架构完成,待完善业务逻辑
