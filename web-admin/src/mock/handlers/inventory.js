import { http, HttpResponse } from 'msw'

// 模拟库存数据
let inventoryData = Array.from({ length: 50 }, (_, i) => ({
  id: i + 1,
  bookId: i + 1,
  isbn: `978711${String(100000 + i).padStart(7, '0')}`,
  title: `图书${i + 1}`,
  location: ['一楼书库', '二楼阅览室', '三楼参考室', '四楼特藏室'][i % 4],
  shelfNumber: `${String.fromCharCode(65 + (i % 4))}-${String(Math.floor(i / 4) + 1).padStart(2, '0')}-${String((i % 10) + 1).padStart(2, '0')}`,
  totalQuantity: 10 + (i % 5),
  availableQuantity: 8 + (i % 3),
  alertThreshold: 3,
  lastCheckTime: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString()
}))

// 模拟盘点任务数据
let inventoryTasks = [
  {
    id: 1,
    taskName: '2024年第四季度全面盘点',
    taskType: 'FULL',
    status: 'COMPLETED',
    startTime: '2024-10-01T09:00:00',
    endTime: '2024-10-15T18:00:00',
    operatorId: 1,
    operatorName: '管理员',
    totalBooks: 500,
    checkedBooks: 500,
    discrepancyCount: 3,
    notes: '年度例行盘点',
    createdAt: '2024-09-25T10:00:00',
    updatedAt: '2024-10-15T18:00:00'
  },
  {
    id: 2,
    taskName: '一楼书库抽样盘点',
    taskType: 'PARTIAL',
    status: 'IN_PROGRESS',
    startTime: '2024-12-01T09:00:00',
    endTime: null,
    operatorId: 2,
    operatorName: '图书管理员',
    totalBooks: 100,
    checkedBooks: 65,
    discrepancyCount: 1,
    notes: '一楼书库抽样检查',
    createdAt: '2024-11-28T14:00:00',
    updatedAt: new Date().toISOString()
  },
  {
    id: 3,
    taskName: '二楼阅览室盘点',
    taskType: 'PARTIAL',
    status: 'PENDING',
    startTime: null,
    endTime: null,
    operatorId: null,
    operatorName: null,
    totalBooks: 150,
    checkedBooks: 0,
    discrepancyCount: 0,
    notes: '计划下周进行',
    createdAt: '2024-12-10T10:00:00',
    updatedAt: '2024-12-10T10:00:00'
  }
]

// 模拟盘点明细数据
let taskItems = inventoryData.slice(0, 20).map((inv, i) => ({
  id: i + 1,
  taskId: 2,
  bookId: inv.bookId,
  isbn: inv.isbn,
  title: inv.title,
  location: inv.location,
  expectedQuantity: inv.totalQuantity,
  actualQuantity: i < 15 ? inv.totalQuantity : inv.totalQuantity - 1,
  discrepancy: i < 15 ? 0 : -1,
  status: i < 15 ? 'CHECKED' : 'PENDING',
  checkedTime: i < 15 ? new Date().toISOString() : null,
  checkerId: i < 15 ? 2 : null
}))

export const inventoryHandlers = [
  // 获取库存列表
  http.get('/api/v1/inventory', ({ request }) => {
    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '20')
    const location = url.searchParams.get('location') || ''
    const keyword = url.searchParams.get('keyword') || ''

    let filtered = inventoryData

    if (location) {
      filtered = filtered.filter((inv) => inv.location === location)
    }

    if (keyword) {
      filtered = filtered.filter(
        (inv) =>
          inv.title.includes(keyword) ||
          inv.isbn.includes(keyword) ||
          inv.shelfNumber.includes(keyword)
      )
    }

    const total = filtered.length
    const start = (pageNum - 1) * pageSize
    const records = filtered.slice(start, start + pageSize)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        records,
        total,
        pageNum,
        pageSize,
        pages: Math.ceil(total / pageSize)
      }
    })
  }),

  // 获取库存详情
  http.get('/api/v1/inventory/:id', ({ params }) => {
    const { id } = params
    const inventory = inventoryData.find((inv) => inv.id === parseInt(id))

    if (!inventory) {
      return HttpResponse.json(
        {
          code: 404,
          message: '库存记录不存在'
        },
        { status: 404 }
      )
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: inventory
    })
  }),

  // 库存调整
  http.post('/api/v1/inventory/adjust', async ({ request }) => {
    const { bookId, adjustQuantity, reason } = await request.json()
    const inventory = inventoryData.find((inv) => inv.bookId === bookId)

    if (!inventory) {
      return HttpResponse.json(
        {
          code: 404,
          message: '图书库存记录不存在'
        },
        { status: 404 }
      )
    }

    const newTotal = inventory.totalQuantity + adjustQuantity
    if (newTotal < 0) {
      return HttpResponse.json(
        {
          code: 400,
          message: '调整后库存不能为负数'
        },
        { status: 400 }
      )
    }

    inventory.totalQuantity = newTotal
    inventory.availableQuantity = Math.max(0, inventory.availableQuantity + adjustQuantity)
    inventory.updatedAt = new Date().toISOString()

    return HttpResponse.json({
      code: 200,
      message: '库存调整成功',
      data: inventory
    })
  }),

  // 库存预警列表
  http.get('/api/v1/inventory/alerts', ({ request }) => {
    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '20')

    // 筛选低于预警阈值的库存
    const alerts = inventoryData.filter((inv) => inv.availableQuantity <= inv.alertThreshold)

    const total = alerts.length
    const start = (pageNum - 1) * pageSize
    const records = alerts.slice(start, start + pageSize)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        records,
        total,
        pageNum,
        pageSize,
        pages: Math.ceil(total / pageSize)
      }
    })
  }),

  // ========== 盘点任务 APIs ==========

  // 创建盘点任务
  http.post('/api/v1/inventory/tasks', async ({ request }) => {
    const data = await request.json()
    const newTask = {
      id: inventoryTasks.length + 1,
      ...data,
      status: 'PENDING',
      startTime: null,
      endTime: null,
      checkedBooks: 0,
      discrepancyCount: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }

    inventoryTasks.push(newTask)

    return HttpResponse.json({
      code: 200,
      message: '盘点任务创建成功',
      data: newTask
    })
  }),

  // 获取盘点任务列表
  http.get('/api/v1/inventory/tasks', ({ request }) => {
    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10')
    const status = url.searchParams.get('status') || ''
    const taskType = url.searchParams.get('taskType') || ''

    let filtered = inventoryTasks

    if (status) {
      filtered = filtered.filter((task) => task.status === status)
    }

    if (taskType) {
      filtered = filtered.filter((task) => task.taskType === taskType)
    }

    const total = filtered.length
    const start = (pageNum - 1) * pageSize
    const records = filtered.slice(start, start + pageSize)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        records,
        total,
        pageNum,
        pageSize,
        pages: Math.ceil(total / pageSize)
      }
    })
  }),

  // 获取盘点任务详情
  http.get('/api/v1/inventory/tasks/:id', ({ params }) => {
    const { id } = params

    // 排除特殊路由
    if (id === 'stats') return

    const task = inventoryTasks.find((t) => t.id === parseInt(id))

    if (!task) {
      return HttpResponse.json(
        {
          code: 404,
          message: '盘点任务不存在'
        },
        { status: 404 }
      )
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: task
    })
  }),

  // 更新盘点任务
  http.put('/api/v1/inventory/tasks/:id', async ({ params, request }) => {
    const { id } = params
    const data = await request.json()
    const task = inventoryTasks.find((t) => t.id === parseInt(id))

    if (!task) {
      return HttpResponse.json(
        {
          code: 404,
          message: '盘点任务不存在'
        },
        { status: 404 }
      )
    }

    if (task.status !== 'PENDING') {
      return HttpResponse.json(
        {
          code: 400,
          message: '只能修改待开始的任务'
        },
        { status: 400 }
      )
    }

    Object.assign(task, data, { updatedAt: new Date().toISOString() })

    return HttpResponse.json({
      code: 200,
      message: '任务更新成功',
      data: task
    })
  }),

  // 开始盘点任务
  http.post('/api/v1/inventory/tasks/:id/start', ({ params }) => {
    const { id } = params
    const task = inventoryTasks.find((t) => t.id === parseInt(id))

    if (!task) {
      return HttpResponse.json(
        {
          code: 404,
          message: '盘点任务不存在'
        },
        { status: 404 }
      )
    }

    if (task.status !== 'PENDING') {
      return HttpResponse.json(
        {
          code: 400,
          message: '只能开始待开始的任务'
        },
        { status: 400 }
      )
    }

    task.status = 'IN_PROGRESS'
    task.startTime = new Date().toISOString()
    task.updatedAt = new Date().toISOString()

    return HttpResponse.json({
      code: 200,
      message: '任务已开始',
      data: task
    })
  }),

  // 完成盘点任务
  http.post('/api/v1/inventory/tasks/:id/complete', ({ params }) => {
    const { id } = params
    const task = inventoryTasks.find((t) => t.id === parseInt(id))

    if (!task) {
      return HttpResponse.json(
        {
          code: 404,
          message: '盘点任务不存在'
        },
        { status: 404 }
      )
    }

    if (task.status !== 'IN_PROGRESS') {
      return HttpResponse.json(
        {
          code: 400,
          message: '只能完成进行中的任务'
        },
        { status: 400 }
      )
    }

    task.status = 'COMPLETED'
    task.endTime = new Date().toISOString()
    task.checkedBooks = task.totalBooks
    task.updatedAt = new Date().toISOString()

    return HttpResponse.json({
      code: 200,
      message: '任务已完成',
      data: task
    })
  }),

  // 取消盘点任务
  http.post('/api/v1/inventory/tasks/:id/cancel', ({ params }) => {
    const { id } = params
    const task = inventoryTasks.find((t) => t.id === parseInt(id))

    if (!task) {
      return HttpResponse.json(
        {
          code: 404,
          message: '盘点任务不存在'
        },
        { status: 404 }
      )
    }

    if (task.status === 'COMPLETED') {
      return HttpResponse.json(
        {
          code: 400,
          message: '已完成的任务无法取消'
        },
        { status: 400 }
      )
    }

    task.status = 'CANCELLED'
    task.updatedAt = new Date().toISOString()

    return HttpResponse.json({
      code: 200,
      message: '任务已取消',
      data: task
    })
  }),

  // 获取盘点明细
  http.get('/api/v1/inventory/tasks/:taskId/items', ({ params, request }) => {
    const { taskId } = params
    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '20')
    const status = url.searchParams.get('status') || ''

    let items = taskItems.filter((item) => item.taskId === parseInt(taskId))

    if (status) {
      items = items.filter((item) => item.status === status)
    }

    const total = items.length
    const start = (pageNum - 1) * pageSize
    const records = items.slice(start, start + pageSize)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        records,
        total,
        pageNum,
        pageSize,
        pages: Math.ceil(total / pageSize)
      }
    })
  }),

  // 录入盘点结果
  http.post('/api/v1/inventory/tasks/:taskId/items', async ({ params, request }) => {
    const { taskId } = params
    const { bookId, actualQuantity } = await request.json()

    const task = inventoryTasks.find((t) => t.id === parseInt(taskId))
    if (!task) {
      return HttpResponse.json(
        {
          code: 404,
          message: '盘点任务不存在'
        },
        { status: 404 }
      )
    }

    if (task.status !== 'IN_PROGRESS') {
      return HttpResponse.json(
        {
          code: 400,
          message: '只能在进行中的任务录入结果'
        },
        { status: 400 }
      )
    }

    let item = taskItems.find((i) => i.taskId === parseInt(taskId) && i.bookId === bookId)
    if (!item) {
      // 创建新的盘点项
      const inventory = inventoryData.find((inv) => inv.bookId === bookId)
      item = {
        id: taskItems.length + 1,
        taskId: parseInt(taskId),
        bookId,
        isbn: inventory?.isbn || '',
        title: inventory?.title || '',
        location: inventory?.location || '',
        expectedQuantity: inventory?.totalQuantity || 0,
        actualQuantity,
        discrepancy: actualQuantity - (inventory?.totalQuantity || 0),
        status: 'CHECKED',
        checkedTime: new Date().toISOString(),
        checkerId: 1
      }
      taskItems.push(item)
    } else {
      item.actualQuantity = actualQuantity
      item.discrepancy = actualQuantity - item.expectedQuantity
      item.status = 'CHECKED'
      item.checkedTime = new Date().toISOString()
      item.checkerId = 1
    }

    // 更新任务进度
    const checkedCount = taskItems.filter(
      (i) => i.taskId === parseInt(taskId) && i.status === 'CHECKED'
    ).length
    const discrepancyCount = taskItems.filter(
      (i) => i.taskId === parseInt(taskId) && i.discrepancy !== 0
    ).length
    task.checkedBooks = checkedCount
    task.discrepancyCount = discrepancyCount
    task.updatedAt = new Date().toISOString()

    return HttpResponse.json({
      code: 200,
      message: '盘点结果录入成功',
      data: item
    })
  }),

  // 导出盘点任务报告
  http.get('/api/v1/inventory/tasks/:id/export', () => {
    return new HttpResponse('Mock CSV: inventory task export', {
      status: 200,
      headers: { 'Content-Type': 'application/octet-stream' }
    })
  }),

  // 获取库存统计
  http.get('/api/v1/inventory/stats', () => {
    const totalBooks = inventoryData.length
    const totalQuantity = inventoryData.reduce((sum, inv) => sum + inv.totalQuantity, 0)
    const availableQuantity = inventoryData.reduce((sum, inv) => sum + inv.availableQuantity, 0)
    const alertCount = inventoryData.filter(
      (inv) => inv.availableQuantity <= inv.alertThreshold
    ).length
    const pendingTasks = inventoryTasks.filter((t) => t.status === 'PENDING').length
    const inProgressTasks = inventoryTasks.filter((t) => t.status === 'IN_PROGRESS').length

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        totalBooks,
        totalQuantity,
        availableQuantity,
        borrowedQuantity: totalQuantity - availableQuantity,
        alertCount,
        pendingTasks,
        inProgressTasks,
        lastFullInventory: '2024-10-15'
      }
    })
  })
]
