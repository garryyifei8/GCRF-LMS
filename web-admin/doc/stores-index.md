# Pinia Stores 文档索引

本目录包含GCRF图书馆管理系统Pinia状态管理的完整文档。

## 文档列表

### 1. 快速开始 (推荐从这里开始)

**文件**: `stores-quickstart.md`

5分钟快速上手指南，包含：

- 安装步骤
- 基本配置
- 常用功能示例
- 立即测试

适合：初次使用，快速集成

---

### 2. 使用文档 (API参考)

**文件**: `stores-usage.md`

完整的API参考文档，包含：

- 所有Store的详细说明
- 每个方法的参数和返回值
- Getters和State说明
- 持久化策略
- 最佳实践
- 常见问题

适合：日常开发查阅

---

### 3. 实战示例 (场景驱动)

**文件**: `stores-example.md`

5个完整的实战场景：

- 场景1: 图书管理页面
- 场景2: 借书流程
- 场景3: 还书流程
- 场景4: 系统设置
- 场景5: 应用初始化

每个场景都包含完整的Vue组件代码。

适合：学习如何在实际项目中使用

---

### 4. 持久化配置指南

**文件**: `pinia-persist-setup.md`

持久化插件安装和配置指南，包含：

- 插件安装步骤
- 配置方法
- 持久化策略设计
- localStorage vs sessionStorage
- 测试方法
- 常见问题

适合：配置持久化功能

---

### 5. 开发总结

**文件**: `stores-README.md`

开发总结文档，包含：

- 已完成工作清单
- 功能特性说明
- 设计理念
- 使用场景
- 代码统计
- 下一步建议

适合：了解整体架构和设计思路

---

## 快速导航

### 我想...

**快速上手** → `stores-quickstart.md`

**查询API** → `stores-usage.md`

**看实例** → `stores-example.md`

**配置持久化** → `pinia-persist-setup.md`

**了解架构** → `stores-README.md`

---

## Store文件位置

所有Store文件位于：`/web-admin/src/stores/`

```
stores/
├── index.js          # 统一导出
├── user.js           # 用户认证、权限
├── book.js           # 图书分类、热门、历史
├── reader.js         # 读者类型、当前读者
├── circulation.js    # 借阅归还、购物车、罚款
└── system.js         # 系统配置、角色权限
```

---

## 使用方式

### 导入单个Store

```javascript
import { useBookStore } from '@/stores'
const bookStore = useBookStore()
```

### 导入多个Store

```javascript
import { useBookStore, useReaderStore, useCirculationStore } from '@/stores'

const bookStore = useBookStore()
const readerStore = useReaderStore()
const circulationStore = useCirculationStore()
```

---

## 主要功能

### bookStore - 图书管理

- 图书分类缓存（树形结构）
- 热门图书推荐
- 最近访问记录
- 搜索历史

### readerStore - 读者管理

- 读者类型配置
- 当前操作读者
- 借阅能力计算
- 最近操作记录

### circulationStore - 流通管理

- 借阅购物车
- 归还购物车
- 罚款计算
- 业务规则配置

### systemStore - 系统配置

- UI配置（主题、侧边栏等）
- 角色权限缓存
- 借阅规则（按读者类型）
- 罚款规则

### userStore - 用户管理

- 用户认证
- Token管理
- 权限检查

---

## 代码统计

- Store文件数量: 6个
- 代码总行数: 1,377行
- 文档文件数量: 5个
- 文档总字数: 约20,000字

---

## 技术栈

- Pinia 2.x
- Vue 3 Composition API
- pinia-plugin-persistedstate (可选)
- localStorage / sessionStorage

---

## 更新日志

- 2024-11-30: 创建所有Store和文档
- 初始版本，包含完整的缓存机制和持久化配置

---

## 下一步

1. 阅读 `stores-quickstart.md` 快速上手
2. 安装持久化插件（可选）
3. 在项目中集成使用
4. 查阅 `stores-usage.md` 了解详细API

---

**祝使用愉快！**
