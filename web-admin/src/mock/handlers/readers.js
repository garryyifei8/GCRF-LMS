import { http, HttpResponse } from 'msw'
import { generateReaders, generateReaderStats, readerTypes } from '../data/readers'

// 生成并缓存读者数据
let readersData = generateReaders(200)

export const readersHandlers = [
  // 获取读者列表
  http.get('/api/v1/readers', ({ request }) => {
    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10')
    const keyword = url.searchParams.get('keyword') || ''
    const readerType = url.searchParams.get('readerType') || ''
    const status = url.searchParams.get('status') || ''

    // 过滤数据
    let filteredReaders = readersData

    if (keyword) {
      filteredReaders = filteredReaders.filter(reader =>
        reader.realName.includes(keyword) ||
        reader.cardNo.includes(keyword) ||
        reader.phone.includes(keyword) ||
        reader.email.includes(keyword)
      )
    }

    if (readerType) {
      filteredReaders = filteredReaders.filter(reader => reader.readerType === readerType)
    }

    if (status) {
      filteredReaders = filteredReaders.filter(reader => reader.status === status)
    }

    // 分页
    const total = filteredReaders.length
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    const readers = filteredReaders.slice(start, end)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        records: readers,
        total,
        pageNum,
        pageSize,
        pages: Math.ceil(total / pageSize)
      }
    })
  }),

  // 获取读者详情
  http.get('/api/v1/readers/:id', ({ params }) => {
    const { id } = params
    const reader = readersData.find(r => r.id === parseInt(id))

    if (!reader) {
      return HttpResponse.json({
        code: 404,
        message: '读者不存在'
      }, { status: 404 })
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: reader
    })
  }),

  // 新增读者
  http.post('/api/v1/readers', async ({ request }) => {
    const data = await request.json()
    const readerType = readerTypes.find(t => t.value === data.readerType)

    const newReader = {
      id: readersData.length + 1,
      cardNo: `RD${String(readersData.length + 1).padStart(8, '0')}`,
      ...data,
      readerTypeName: readerType?.label || '',
      maxBorrowCount: data.maxBorrowCount || readerType?.maxBorrowCount || 5,
      borrowDays: data.borrowDays || readerType?.borrowDays || 30,
      depositAmount: data.depositAmount || readerType?.depositAmount || 0,
      currentBorrowCount: 0,
      totalBorrowCount: 0,
      overdueCount: 0,
      hasOverdue: false,
      status: data.status || 'active',
      registeredDate: new Date().toISOString().split('T')[0],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }

    readersData.push(newReader)

    return HttpResponse.json({
      code: 200,
      message: '读者添加成功',
      data: newReader
    })
  }),

  // 更新读者
  http.put('/api/v1/readers/:id', async ({ params, request }) => {
    const { id } = params
    const data = await request.json()
    const index = readersData.findIndex(r => r.id === parseInt(id))

    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        message: '读者不存在'
      }, { status: 404 })
    }

    readersData[index] = {
      ...readersData[index],
      ...data,
      updatedAt: new Date().toISOString()
    }

    return HttpResponse.json({
      code: 200,
      message: '读者更新成功',
      data: readersData[index]
    })
  }),

  // 删除读者
  http.delete('/api/v1/readers/:id', ({ params }) => {
    const { id } = params
    const index = readersData.findIndex(r => r.id === parseInt(id))

    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        message: '读者不存在'
      }, { status: 404 })
    }

    readersData.splice(index, 1)

    return HttpResponse.json({
      code: 200,
      message: '读者删除成功'
    })
  }),

  // 批量删除读者
  http.delete('/api/v1/readers/batch', ({ request }) => {
    const url = new URL(request.url)
    const ids = url.searchParams.get('ids')?.split(',').map(id => parseInt(id)) || []

    readersData = readersData.filter(r => !ids.includes(r.id))

    return HttpResponse.json({
      code: 200,
      message: `成功删除${ids.length}个读者`
    })
  }),

  // 获取读者类型
  http.get('/api/v1/readers/types', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: readerTypes
    })
  }),

  // 办理/更新借阅证
  http.post('/api/v1/readers/:id/card', async ({ params, request }) => {
    const { id } = params
    const data = await request.json()
    const index = readersData.findIndex(r => r.id === parseInt(id))

    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        message: '读者不存在'
      }, { status: 404 })
    }

    readersData[index] = {
      ...readersData[index],
      ...data,
      updatedAt: new Date().toISOString()
    }

    return HttpResponse.json({
      code: 200,
      message: '借阅证办理成功',
      data: readersData[index]
    })
  }),

  // 更新读者状态
  http.put('/api/v1/readers/:id/status', async ({ params, request }) => {
    const { id } = params
    const { status } = await request.json()
    const index = readersData.findIndex(r => r.id === parseInt(id))

    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        message: '读者不存在'
      }, { status: 404 })
    }

    readersData[index].status = status
    readersData[index].updatedAt = new Date().toISOString()

    return HttpResponse.json({
      code: 200,
      message: '状态更新成功',
      data: readersData[index]
    })
  }),

  // 获取读者统计数据
  http.get('/api/v1/readers/stats', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateReaderStats()
    })
  })
]
