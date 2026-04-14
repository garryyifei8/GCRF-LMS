# 流通管理模块API集成文档

**项目**: GCRF图书馆管理系统
**模块**: 流通管理 (Circulation Management)
**完成日期**: 2025-11-26
**完成度**: 100%

---

## 概述

本文档记录了流通管理模块前端页面与后端API的完整集成过程。所有Mock数据已替换为真实API调用，包含完整的错误处理和loading状态。

---

## 集成内容

### 1. borrow.vue (借书页面)

#### 新增API方法

- `getReaderByCardNumber(cardNumber)` - 根据借阅证号查询读者信息
- `getBookByBarcode(barcode)` - 根据条码号查询图书信息
- `borrowBook(data)` - 借阅图书

#### 修改的函数

1. **searchReader()** - 读者信息查询
   - 调用 `getReaderByCardNumber` API
   - 字段映射:
     - `readerId/id` → `id`
     - `realName` → `name`
     - `cardNumber` → `cardNo`
     - `currentBorrowCount` → `borrowedCount`
     - `maxBorrowCount` → `maxBorrow`
     - `status: 'active'` → `status: 'normal'`
   - 添加错误处理和控制台日志

2. **addBook()** - 图书查询和添加
   - 调用 `getBookByBarcode` API
   - 验证图书可借状态 (`status === 'available' && availableCopies > 0`)
   - 添加错误处理

3. **confirmBorrow()** - 确认借出
   - 批量调用 `borrowBook` API (每本图书单独调用)
   - 支持部分成功: 成功的图书从列表移除，失败的保留
   - 详细的成功/失败反馈
   - 更新读者已借数量

#### 关键特性

- 完整的 `try/catch/finally` 错误处理
- 使用 `v-loading` 显示加载状态
- 使用 `ElMessage` 提供用户反馈
- 支持批量借书的部分成功处理

---

### 2. return.vue (还书页面)

#### 新增API方法

- `getBorrowRecordByBarcode(barcode)` - 根据条码查询借阅记录
- `returnBook(data)` - 归还图书

#### 修改的函数

1. **scanBook()** - 扫描图书并查询借阅记录
   - 调用 `getBorrowRecordByBarcode` API
   - 自动计算逾期天数和罚款
   - 罚款规则:
     - 宽限期: 3天
     - 每天罚款: ¥0.1
     - 罚款上限: ¥50
   - 字段映射:
     - `recordId/id` → `id`
     - `readerCardNo/cardNumber` → `readerCardNo`
   - 添加错误处理

2. **handleConfirmReturn()** - 确认归还
   - 批量调用 `returnBook` API
   - 支持部分成功处理
   - 罚款信息记录到备注
   - 详细的成功/失败反馈

#### 关键特性

- 实时罚款计算
- 批量归还的部分成功处理
- 完整的错误处理和用户反馈
- 罚款信息自动记录到备注字段

---

### 3. records.vue (流通记录)

#### 使用API方法

- `getCirculationRecords(params)` - 获取流通记录列表

#### 修改的函数

1. **loadRecordList()** - 加载流通记录
   - 调用 `getCirculationRecords` API
   - 支持多条件查询:
     - `keyword` - 关键词搜索
     - `status` - 记录状态
     - `dateRange` - 日期范围
     - `pageNum/pageSize` - 分页参数
   - 字段映射和日期格式化
   - 自动计算逾期天数
   - 统计信息处理:
     - 优先使用后端返回的统计数据
     - 后备方案: 前端根据数据计算统计
   - 添加错误处理

2. **handleExport()** - 导出Excel (CSV格式)
   - 获取所有数据 (pageSize: 10000)
   - 生成CSV格式文件
   - 添加BOM支持中文
   - 自动下载文件名: `流通记录_YYYYMMDD_HHmmss.csv`
   - 包含所有必要字段

#### 关键特性

- 完整的多条件搜索
- 分页支持
- 统计信息自动计算
- CSV导出功能 (支持中文)
- 灵活的数据映射 (支持不同的字段名)

---

### 4. reservations.vue (预约管理)

#### 新增API方法

- `getReservations(params)` - 获取预约列表
- `processReservation(reservationId, data)` - 处理预约
- `cancelReservation(data)` - 取消预约
- `notifyReservation(reservationId)` - 发送预约通知

#### 修改的函数

1. **loadReservationList()** - 加载预约列表
   - 调用 `getReservations` API
   - 支持多条件查询
   - 字段映射和日期格式化
   - 统计信息处理
   - 添加错误处理

2. **submitProcess()** - 处理预约
   - 调用 `processReservation` API
   - 支持两种处理结果:
     - `ready` - 图书已到馆，可取书
     - `cancel` - 无法满足，取消预约
   - 可选发送通知
   - 添加错误处理

3. **handleCancel()** - 取消预约
   - 调用 `cancelReservation` API
   - 确认对话框
   - 添加错误处理

4. **handleNotify()** - 发送通知
   - 调用 `notifyReservation` API
   - 确认对话框
   - 添加错误处理

#### 关键特性

- 完整的预约管理流程
- 灵活的处理选项
- 通知功能集成
- 完整的错误处理

---

## 字段映射规范

### 读者信息

```javascript
// 后端 → 前端
readerId / id → id
realName → name
readerType → type
cardNumber → cardNo
currentBorrowCount → borrowedCount
maxBorrowCount → maxBorrow
status: 'active' → status: 'normal'
```

### 图书信息

```javascript
// 后端 → 前端
bookId / id → id
availableCopies → 检查是否可借
```

### 流通记录

```javascript
// 后端 → 前端
recordId / id → id
readerCardNo / cardNumber → readerCardNo
borrowDate → 格式化为 'YYYY-MM-DD'
dueDate → 格式化为 'YYYY-MM-DD'
returnDate → 格式化为 'YYYY-MM-DD' (可能为null)
```

### 预约信息

```javascript
// 后端 → 前端
reservationId / id → id
readerCardNo / cardNumber → readerCardNo
readerPhone / phone → readerPhone
reserveDate → 格式化为 'YYYY-MM-DD'
expiryDate → 格式化为 'YYYY-MM-DD' (可能为null)
processTime → 格式化为 'YYYY-MM-DD HH:mm:ss' (可能为null)
```

---

## 技术规范

### API调用模式

```javascript
try {
  const res = await apiFunction(params);

  if (res.code !== 200) {
    ElMessage.error(res.message || "操作失败");
    return;
  }

  const data = res.data;
  // 处理数据...

  ElMessage.success("操作成功");
} catch (error) {
  console.error("操作失败:", error);
  ElMessage.error(error.message || "操作失败");
} finally {
  loading.value = false; // 如果有loading状态
}
```

### Loading状态

- 所有异步操作使用 `v-loading` 指令
- `loading.value = true` 在API调用前
- `loading.value = false` 在 `finally` 块中

### 错误处理

- 所有API调用包含 `try/catch/finally`
- 检查响应码: `res.code !== 200`
- 控制台输出错误: `console.error('操作失败:', error)`
- 用户友好的错误消息: `ElMessage.error()`

### 数据映射

- 支持多种可能的字段名 (`recordId || id`)
- 日期使用 `dayjs` 格式化
- 处理可能为 `null` 或 `undefined` 的值

---

## 测试清单

### borrow.vue

- [ ] 读者证号查询 (存在/不存在)
- [ ] 图书条码查询 (可借/不可借/不存在)
- [ ] 单本图书借出
- [ ] 批量图书借出
- [ ] 借阅上限验证
- [ ] 读者状态验证
- [ ] 部分成功场景
- [ ] 错误处理

### return.vue

- [ ] 图书条码扫描 (有借阅记录/无借阅记录)
- [ ] 逾期天数计算
- [ ] 罚款计算
- [ ] 单本图书归还
- [ ] 批量图书归还
- [ ] 部分成功场景
- [ ] 罚款规则验证
- [ ] 错误处理

### records.vue

- [ ] 列表加载
- [ ] 关键词搜索
- [ ] 状态筛选
- [ ] 日期范围筛选
- [ ] 分页功能
- [ ] 统计信息显示
- [ ] 详情查看
- [ ] Excel导出
- [ ] 中文显示正确
- [ ] 错误处理

### reservations.vue

- [ ] 列表加载
- [ ] 关键词搜索
- [ ] 状态筛选
- [ ] 日期范围筛选
- [ ] 分页功能
- [ ] 统计信息显示
- [ ] 处理预约 (可取书/取消)
- [ ] 取消预约
- [ ] 发送通知
- [ ] 详情查看
- [ ] 错误处理

---

## API端点总结

### 读者相关

- `GET /api/v1/readers/card/{cardNumber}` - 根据借阅证号查询读者

### 图书相关

- `GET /api/v1/books/barcode/{barcode}` - 根据条码查询图书

### 流通相关

- `POST /api/v1/circulation/borrow` - 借阅图书
- `POST /api/v1/circulation/return` - 归还图书
- `GET /api/v1/circulation/barcode/{barcode}` - 根据条码查询借阅记录
- `GET /api/v1/circulation/records` - 获取流通记录列表

### 预约相关

- `GET /api/v1/circulation/reservations` - 获取预约列表
- `POST /api/v1/circulation/reservations/{id}/process` - 处理预约
- `POST /api/v1/circulation/cancel-reservation` - 取消预约
- `POST /api/v1/circulation/reservations/{id}/notify` - 发送通知

---

## 已知问题和限制

1. **批量操作**
   - 借书和还书都是逐个调用API，性能可能不是最优
   - 建议后端提供批量API端点

2. **Excel导出**
   - 目前使用CSV格式
   - 如需更复杂的Excel格式，可使用 `xlsx` 库

3. **字段映射**
   - 使用了兼容性处理 (`recordId || id`)
   - 需要与后端确认最终的字段命名规范

4. **统计信息**
   - records.vue 和 reservations.vue 的统计信息依赖后端提供
   - 如果后端不提供，前端会根据当前页数据计算 (不准确)

---

## 后续改进建议

1. **性能优化**
   - 实现批量借书/还书API
   - 添加请求缓存机制
   - 优化分页加载

2. **用户体验**
   - 添加操作确认对话框
   - 改进错误提示信息
   - 添加操作历史记录

3. **功能增强**
   - 支持扫码枪输入
   - 添加打印借书凭证功能
   - 实现高级搜索功能

4. **代码优化**
   - 提取公共的字段映射函数
   - 统一错误处理逻辑
   - 添加单元测试

---

## 参考文档

- [CLAUDE.md](../CLAUDE.md) - 项目开发指南
- [architect.md](../docs/architecture/architect.md) - 系统架构文档
- [Vue 3 文档](https://vuejs.org/)
- [Element Plus 文档](https://element-plus.org/)

---

**最后更新**: 2025-11-26
**维护者**: Claude Code Agent
