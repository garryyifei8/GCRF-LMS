# 快速启动指南

## 5分钟启动项目

### 步骤1: 环境检查

确保已安装Node.js:

```bash
node --version  # 需要 >= 16.0
npm --version   # 需要 >= 8.0
```

### 步骤2: 安装依赖

```bash
cd web-admin
npm install
```

如果速度慢,可使用国内镜像:

```bash
npm install --registry=https://registry.npmmirror.com
```

### 步骤3: 启动开发服务器

```bash
npm run dev
```

看到以下输出表示成功:

```
  VITE v5.0.0  ready in xxx ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

### 步骤4: 访问系统

打开浏览器访问: http://localhost:3000

### 步骤5: 登录

- **用户名**: 随意输入(如: admin)
- **密码**: 至少6位(如: 123456)

点击"登录"即可进入系统!

## 项目结构一览

```
web-admin/
├── src/
│   ├── views/              # 页面
│   │   ├── login/          # 登录页 ✅已完成
│   │   ├── dashboard/      # 首页 ✅已完成
│   │   ├── circulation/    # 流通管理 ⏳待完善
│   │   ├── books/          # 图书管理 ⏳待完善
│   │   ├── readers/        # 读者管理 ⏳待完善
│   │   └── system/         # 系统管理 ⏳待完善
│   ├── layouts/            # 布局组件 ✅已完成
│   ├── router/             # 路由配置 ✅已完成
│   ├── stores/             # 状态管理 ✅已完成
│   ├── utils/              # 工具函数 ✅已完成
│   └── styles/             # 全局样式 ✅已完成
├── package.json            # 依赖配置
├── vite.config.js          # 构建配置
└── README.md               # 项目说明
```

## 快速功能测试

### 1. 测试登录

访问 http://localhost:3000/login

- 输入任意用户名和密码(≥6位)
- 点击登录
- 自动跳转到首页

### 2. 测试首页

- 查看核心数据卡片
- 查看图表(流通趋势、分类占比)
- 查看排行榜
- 点击右下角快捷操作按钮

### 3. 测试导航

- 点击左侧菜单,访问各个页面
- 点击折叠按钮,收起/展开侧边栏
- 查看顶部面包屑导航
- 点击用户头像,查看下拉菜单

### 4. 测试路由

直接访问:
- http://localhost:3000/ - 自动跳转登录页(未登录)或首页(已登录)
- http://localhost:3000/dashboard - 首页
- http://localhost:3000/circulation/borrow - 图书借出
- http://localhost:3000/books/list - 图书列表

## 下一步开发

### 优先级P0 - 核心页面

1. **图书借出页面** (`src/views/circulation/borrow.vue`)
   - 扫描读者证
   - 扫描图书
   - 验证借阅规则
   - 确认借出

2. **图书归还页面** (`src/views/circulation/return.vue`)
   - 扫描图书
   - 匹配借阅记录
   - 处理逾期
   - 确认归还

3. **图书列表页面** (`src/views/books/list.vue`)
   - 数据表格
   - 搜索筛选
   - 详情查看
   - 编辑删除

### 优先级P1 - 重要页面

4. **流通记录** (`src/views/circulation/records.vue`)
5. **图书编目** (`src/views/books/catalog.vue`)
6. **图书典藏** (`src/views/books/collection.vue`)
7. **读者管理** (`src/views/readers/students.vue`)

## 开发建议

### 1. 使用Element Plus组件

已自动导入,直接使用:

```vue
<el-button type="primary">按钮</el-button>
<el-input v-model="value" />
<el-table :data="tableData" />
```

### 2. 使用全局样式

```vue
<div class="page-container">
  <div class="page-header">
    <h1 class="page-header-title">页面标题</h1>
  </div>
  <div class="card">
    <!-- 内容 -->
  </div>
</div>
```

### 3. API调用

```javascript
import request from '@/utils/request'

const fetchData = async () => {
  const data = await request.get('/api/endpoint')
}
```

### 4. 状态管理

```javascript
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
console.log(userStore.userInfo)
```

## 常见问题

### Q1: npm install失败?

**A**: 尝试清除缓存后重装:
```bash
npm cache clean --force
npm install
```

### Q2: 端口3000被占用?

**A**: 修改 `vite.config.js` 中的端口:
```javascript
server: {
  port: 3001  // 改为其他端口
}
```

### Q3: 页面空白?

**A**: 检查浏览器控制台是否有错误,确保所有依赖已正确安装

### Q4: 如何连接后端API?

**A**: 修改 `.env.development` 中的 `VITE_API_BASE_URL`

### Q5: 如何Mock数据?

**A**: 在页面中直接定义Mock数据:
```javascript
const mockData = ref([{ id: 1, name: '测试' }])
```

## 技术支持

- Vue 3文档: https://vuejs.org/
- Element Plus文档: https://element-plus.org/
- Vite文档: https://vitejs.dev/

## 联系我们

遇到问题请查阅:
1. [README.md](./README.md) - 完整项目文档
2. [DEVELOPMENT.md](./DEVELOPMENT.md) - 开发说明
3. 项目设计文档(/doc目录)

---

**祝开发顺利!** 🚀
