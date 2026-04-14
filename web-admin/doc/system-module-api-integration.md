# 系统管理模块 API 集成文档

**项目**: GCRF 图书馆管理系统前端
**模块**: 系统管理 (System Management)
**日期**: 2025-11-30
**状态**: 已完成

---

## 概述

本文档记录了系统管理模块的 API 集成工作，包括用户管理、角色管理和部门管理三个子模块的前后端 API 对接。

## 完成的工作

### 1. 创建 system.js API 模块

**文件路径**: `/web-admin/src/api/system.js`

**功能模块**:

#### 1.1 用户管理 API

- `getUsers(params)` - 获取用户列表（分页+搜索）
- `getUserById(id)` - 获取用户详情
- `createUser(data)` - 创建用户
- `updateUser(id, data)` - 更新用户信息
- `deleteUser(id)` - 删除用户
- `resetPassword(id, newPassword)` - 重置用户密码
- `updateUserStatus(id, status)` - 更新用户状态（启用/停用）
- `batchDeleteUsers(ids)` - 批量删除用户

#### 1.2 角色管理 API

- `getRoles(params)` - 获取角色列表（分页）
- `getAllRoles()` - 获取所有角色（不分页）
- `getRoleById(id)` - 获取角色详情
- `createRole(data)` - 创建角色
- `updateRole(id, data)` - 更新角色信息
- `deleteRole(id)` - 删除角色
- `assignPermissions(roleId, permissions)` - 分配权限给角色
- `getRolePermissions(roleId)` - 获取角色的权限列表

#### 1.3 权限管理 API

- `getPermissionTree()` - 获取权限树（用于权限配置）
- `getAllPermissions()` - 获取所有权限列表

#### 1.4 部门管理 API

- `getDepartments(params)` - 获取部门列表（分页）
- `getDepartmentTree()` - 获取部门树形结构
- `getDepartmentById(id)` - 获取部门详情
- `createDepartment(data)` - 创建部门
- `updateDepartment(id, data)` - 更新部门信息
- `deleteDepartment(id)` - 删除部门

---

### 2. users.vue API 集成

**文件路径**: `/web-admin/src/views/system/users.vue`

**集成内容**:

1. **导入 API 模块**

   ```javascript
   import { getUsers, createUser, updateUser, resetPassword, updateUserStatus } from '@/api/system'
   ```

2. **替换 Mock 数据为真实 API 调用**
   - `loadUserList()` - 使用 `getUsers()` 加载用户列表
   - `handleSubmit()` - 使用 `createUser()` 和 `updateUser()` 提交表单
   - `handleResetPassword()` - 使用 `resetPassword()` 重置密码
   - `handleToggleStatus()` - 使用 `updateUserStatus()` 切换用户状态

3. **错误处理**
   - 所有 API 调用都包含 `try/catch/finally` 结构
   - 使用 `ElMessage` 显示成功/失败提示
   - 控制台输出错误信息便于调试

4. **加载状态管理**
   - 使用 `loading` 状态控制 `v-loading` 指令
   - 在请求开始时设置 `loading.value = true`
   - 在 `finally` 块中重置 `loading.value = false`

---

### 3. roles.vue API 集成

**文件路径**: `/web-admin/src/views/system/roles.vue`

**集成内容**:

1. **导入 API 模块**

   ```javascript
   import {
     getRoles,
     createRole,
     updateRole,
     deleteRole,
     assignPermissions,
     getPermissionTree
   } from '@/api/system'
   ```

2. **实现角色 CRUD 功能**
   - `loadRoleList()` - 使用 `getRoles()` 加载角色列表
   - `handleSubmit()` - 使用 `createRole()` 和 `updateRole()` 提交表单
   - `handleDelete()` - 使用 `deleteRole()` 删除角色
   - 删除角色时自动清空选中状态（如果删除的是当前选中角色）

3. **实现权限分配功能**
   - `loadPermissionTree()` - 使用 `getPermissionTree()` 加载权限树
   - 如果 API 未实现，使用默认权限树作为后备方案
   - `handleSavePermissions()` - 使用 `assignPermissions()` 保存权限配置
   - 包含半选节点（`halfCheckedKeys`）以支持树形权限结构

4. **初始化逻辑**
   ```javascript
   const init = async () => {
     await loadPermissionTree()
     await loadRoleList()
   }
   init()
   ```

---

### 4. departments.vue API 集成验证

**文件路径**: `/web-admin/src/views/system/departments.vue`

**优化内容**:

1. **统一 API 模块导入**
   - 从 `@/api/department` 改为 `@/api/system`
   - 函数名从 `getDepartmentList` 改为 `getDepartments`
   - 保持与其他系统管理模块的一致性

2. **改进响应处理**
   - 添加 `response.code === 200` 检查
   - 添加数据为空时的兜底逻辑
   - 改进 `updateDepartment()` 调用，传入 ID 作为第一个参数

3. **加载状态管理**
   - 在表单提交时添加 `loading` 状态控制

---

## API 规范

### 请求格式

所有 API 请求使用统一的请求格式：

```javascript
request({
  url: '/api/v1/system/users',
  method: 'get', // get, post, put, delete
  params: {}, // URL 查询参数（GET 请求）
  data: {} // 请求体数据（POST/PUT 请求）
})
```

### 响应格式

后端统一返回格式：

```javascript
{
  code: 200,              // 响应状态码
  message: "操作成功",    // 响应消息
  data: {                 // 响应数据
    records: [],          // 列表数据
    total: 0,             // 总记录数
    pageNum: 1,           // 当前页码
    pageSize: 10,         // 每页数量
    pages: 1              // 总页数
  },
  success: true,          // 是否成功
  timestamp: 1234567890   // 时间戳
}
```

### 错误处理

1. **网络错误**: 由 `request.js` 拦截器统一处理
2. **业务错误**: 由组件的 `catch` 块处理
3. **用户提示**: 使用 `ElMessage` 显示错误信息
4. **控制台日志**: 输出详细错误信息便于调试

---

## API 端点列表

### 用户管理

| 方法   | 端点                                      | 说明         |
| ------ | ----------------------------------------- | ------------ |
| GET    | `/api/v1/system/users`                    | 获取用户列表 |
| GET    | `/api/v1/system/users/:id`                | 获取用户详情 |
| POST   | `/api/v1/system/users`                    | 创建用户     |
| PUT    | `/api/v1/system/users/:id`                | 更新用户     |
| DELETE | `/api/v1/system/users/:id`                | 删除用户     |
| PUT    | `/api/v1/system/users/:id/password/reset` | 重置密码     |
| PUT    | `/api/v1/system/users/:id/status`         | 更新状态     |
| DELETE | `/api/v1/system/users/batch`              | 批量删除     |

### 角色管理

| 方法   | 端点                                   | 说明         |
| ------ | -------------------------------------- | ------------ |
| GET    | `/api/v1/system/roles`                 | 获取角色列表 |
| GET    | `/api/v1/system/roles/all`             | 获取所有角色 |
| GET    | `/api/v1/system/roles/:id`             | 获取角色详情 |
| POST   | `/api/v1/system/roles`                 | 创建角色     |
| PUT    | `/api/v1/system/roles/:id`             | 更新角色     |
| DELETE | `/api/v1/system/roles/:id`             | 删除角色     |
| PUT    | `/api/v1/system/roles/:id/permissions` | 分配权限     |
| GET    | `/api/v1/system/roles/:id/permissions` | 获取角色权限 |

### 权限管理

| 方法 | 端点                              | 说明         |
| ---- | --------------------------------- | ------------ |
| GET  | `/api/v1/system/permissions/tree` | 获取权限树   |
| GET  | `/api/v1/system/permissions`      | 获取所有权限 |

### 部门管理

| 方法   | 端点                              | 说明         |
| ------ | --------------------------------- | ------------ |
| GET    | `/api/v1/system/departments`      | 获取部门列表 |
| GET    | `/api/v1/system/departments/tree` | 获取部门树   |
| GET    | `/api/v1/system/departments/:id`  | 获取部门详情 |
| POST   | `/api/v1/system/departments`      | 创建部门     |
| PUT    | `/api/v1/system/departments/:id`  | 更新部门     |
| DELETE | `/api/v1/system/departments/:id`  | 删除部门     |

---

## 技术实现要点

### 1. Vue 3 Composition API

所有组件使用 `<script setup>` 语法：

```javascript
<script setup>
import { ref, reactive, computed } from 'vue'

const loading = ref(false)
const queryForm = reactive({
  keyword: '',
  status: ''
})
</script>
```

### 2. 错误处理模式

```javascript
const loadData = async () => {
  loading.value = true
  try {
    const response = await getUsers(queryForm)

    if (response.code === 200 && response.data) {
      userList.value = response.data.records || []
      pagination.total = response.data.total || 0
    } else {
      userList.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('Failed to load data:', error)
    ElMessage.error('加载数据失败')
    userList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}
```

### 3. 用户确认对话框

```javascript
ElMessageBox.confirm('确定要删除吗？', '提示', {
  type: 'warning'
})
  .then(async () => {
    try {
      await deleteUser(id)
      ElMessage.success('删除成功')
      await loadUserList()
    } catch (error) {
      console.error('Failed to delete:', error)
      ElMessage.error('删除失败')
    }
  })
  .catch(() => {})
```

### 4. 表单提交

```javascript
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    if (isEdit.value) {
      await updateUser(formData.id, formData)
      ElMessage.success('编辑成功')
    } else {
      await createUser(formData)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    await loadUserList()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('操作失败')
    }
  } finally {
    loading.value = false
  }
}
```

---

## 测试建议

### 1. API Mock 数据

在后端 API 未就绪时，可以使用 MSW (Mock Service Worker) 提供 Mock 数据。

**注意**: Mock 响应格式必须与前端组件期望的格式完全一致。

示例 Mock Handler (`/web-admin/src/mock/handlers/system.js`):

```javascript
import { http, HttpResponse } from 'msw'

export const systemHandlers = [
  // 获取用户列表
  http.get('/api/v1/system/users', ({ request }) => {
    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '20')

    return HttpResponse.json({
      code: 200,
      message: '操作成功',
      data: {
        records: mockUsers,
        total: mockUsers.length,
        pageNum,
        pageSize,
        pages: Math.ceil(mockUsers.length / pageSize)
      },
      success: true,
      timestamp: Date.now()
    })
  })
]
```

### 2. 端到端测试

使用浏览器 DevTools Network 面板验证：

1. **请求 URL** 是否正确
2. **请求方法** (GET/POST/PUT/DELETE)
3. **请求参数** 格式和内容
4. **响应状态码** (200, 401, 500 等)
5. **响应数据格式** 是否符合预期

### 3. 错误场景测试

- 网络超时
- 401 未授权
- 403 权限不足
- 404 资源不存在
- 500 服务器错误
- 空数据返回
- 分页边界情况

---

## 后续工作

### 1. 后端 API 开发

根据本文档中定义的 API 端点和数据格式，开发后端接口：

- 用户管理服务 (system-service)
- 角色权限管理
- 部门管理
- 权限树结构

### 2. 前端优化

- 添加更详细的表单验证规则
- 实现批量操作功能
- 添加数据导出功能
- 优化权限树的用户体验

### 3. 测试覆盖

- 单元测试 (Vitest)
- 组件测试 (Vue Test Utils)
- 端到端测试 (Playwright)
- API 集成测试

---

## 参考文档

- [Vue 3 官方文档](https://vuejs.org/guide/)
- [Element Plus 组件库](https://element-plus.org/en-US/)
- [Axios HTTP 客户端](https://axios-http.com/docs/intro)
- [MSW Mock Service Worker](https://mswjs.io/docs/)

---

## 变更记录

| 日期       | 作者        | 变更内容                            |
| ---------- | ----------- | ----------------------------------- |
| 2025-11-30 | Claude Code | 初始版本：完成系统管理模块 API 集成 |

---

**文档版本**: 1.0.0
**最后更新**: 2025-11-30
