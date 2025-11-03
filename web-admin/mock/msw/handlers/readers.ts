import { http, HttpResponse, delay } from 'msw'
import { successResponse, errorResponse, pageResponse } from '../../data/factory'
import { mockReaders, findReaderById, findReaderByCardNumber } from '../../data/readers'

const BASE_URL = import.meta.env.VITE_API_BASE_URL
const MOCK_DELAY = parseInt(import.meta.env.VITE_MOCK_DELAY) || 300

// 内存存储,模拟持久化
let readersStore = [...mockReaders]
let nextId = Math.max(...readersStore.map(r => r.readerId)) + 1

// 读者类型列表
const readerTypes = [
  { code: 'student', name: '学生', maxBorrow: 10, depositAmount: 100 },
  { code: 'teacher', name: '教师', maxBorrow: 20, depositAmount: 200 },
  { code: 'staff', name: '职工', maxBorrow: 15, depositAmount: 150 },
  { code: 'public', name: '社会读者', maxBorrow: 5, depositAmount: 300 }
]

/**
 * Readers API Handlers
 * 读者管理服务 Mock API
 */
export const readersHandlers = [
  /**
   * GET /api/v1/readers
   * 获取读者列表(分页+搜索)
   */
  http.get(`${BASE_URL}/api/v1/readers`, async ({ request }) => {
    await delay(MOCK_DELAY)

    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '20')
    const keyword = url.searchParams.get('keyword') || ''
    const readerType = url.searchParams.get('readerType') || ''
    const status = url.searchParams.get('status') || ''

    // 过滤数据
    let filteredReaders = [...readersStore]

    if (keyword) {
      const kw = keyword.toLowerCase()
      filteredReaders = filteredReaders.filter(reader =>
        reader.realName.toLowerCase().includes(kw) ||
        reader.cardNumber.includes(kw) ||
        reader.phone?.includes(kw) ||
        reader.email?.toLowerCase().includes(kw)
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
    const records = filteredReaders.slice(start, end)

    return HttpResponse.json(
      pageResponse(records, total, pageNum, pageSize)
    )
  }),

  /**
   * GET /api/v1/readers/:id
   * 获取读者详情
   */
  http.get(`${BASE_URL}/api/v1/readers/:id`, async ({ params }) => {
    await delay(MOCK_DELAY)

    const readerId = parseInt(params.id as string)
    const reader = readersStore.find(r => r.readerId === readerId)

    if (!reader) {
      return HttpResponse.json(
        errorResponse(404, '读者不存在'),
        { status: 404 }
      )
    }

    return HttpResponse.json(successResponse(reader))
  }),

  /**
   * POST /api/v1/readers
   * 新增读者
   */
  http.post(`${BASE_URL}/api/v1/readers`, async ({ request }) => {
    await delay(MOCK_DELAY)

    const body = await request.json() as any

    // 验证必填字段
    if (!body.realName || !body.phone || !body.readerType) {
      return HttpResponse.json(
        errorResponse(400, '缺少必填字段'),
        { status: 400 }
      )
    }

    // 生成借阅证号(RD + 10位数字)
    const cardNumber = `RD${Date.now().toString().substr(-10)}`

    // 检查借阅证号是否已存在
    if (readersStore.some(r => r.cardNumber === cardNumber)) {
      return HttpResponse.json(
        errorResponse(409, '借阅证号已存在'),
        { status: 409 }
      )
    }

    const newReader = {
      readerId: nextId++,
      cardNumber,
      realName: body.realName,
      gender: body.gender || 'male',
      idCard: body.idCard || '',
      phone: body.phone,
      email: body.email || '',
      address: body.address || '',
      readerType: body.readerType,
      maxBorrowCount: body.maxBorrowCount || 10,
      currentBorrowCount: 0,
      depositAmount: body.depositAmount || 0,
      creditScore: 100,
      status: body.status || 'active',
      cardExpireDate: body.cardExpireDate || new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      createdTime: new Date().toISOString(),
      lastBorrowTime: null,
      // 扩展字段
      department: body.department || '',
      grade: body.grade || '',
      studentId: body.studentId || '',
      teacherId: body.teacherId || '',
      title: body.title || ''
    }

    readersStore.push(newReader)

    return HttpResponse.json(
      successResponse(newReader),
      { status: 201 }
    )
  }),

  /**
   * PUT /api/v1/readers/:id
   * 更新读者
   */
  http.put(`${BASE_URL}/api/v1/readers/:id`, async ({ params, request }) => {
    await delay(MOCK_DELAY)

    const readerId = parseInt(params.id as string)
    const body = await request.json() as any

    const readerIndex = readersStore.findIndex(r => r.readerId === readerId)
    if (readerIndex === -1) {
      return HttpResponse.json(
        errorResponse(404, '读者不存在'),
        { status: 404 }
      )
    }

    // 更新读者信息(不允许修改借阅证号)
    const updatedReader = {
      ...readersStore[readerIndex],
      ...body,
      readerId, // 确保ID不变
      cardNumber: readersStore[readerIndex].cardNumber, // 借阅证号不可修改
      updatedTime: new Date().toISOString()
    }

    readersStore[readerIndex] = updatedReader

    return HttpResponse.json(successResponse(updatedReader))
  }),

  /**
   * DELETE /api/v1/readers/:id
   * 删除读者
   */
  http.delete(`${BASE_URL}/api/v1/readers/:id`, async ({ params }) => {
    await delay(MOCK_DELAY)

    const readerId = parseInt(params.id as string)
    const readerIndex = readersStore.findIndex(r => r.readerId === readerId)

    if (readerIndex === -1) {
      return HttpResponse.json(
        errorResponse(404, '读者不存在'),
        { status: 404 }
      )
    }

    // 检查是否有未归还的图书
    const reader = readersStore[readerIndex]
    if (reader.currentBorrowCount > 0) {
      return HttpResponse.json(
        errorResponse(400, '该读者还有未归还的图书,无法删除'),
        { status: 400 }
      )
    }

    readersStore.splice(readerIndex, 1)

    return HttpResponse.json(
      successResponse({ deleted: true, readerId })
    )
  }),

  /**
   * DELETE /api/v1/readers/batch
   * 批量删除读者
   */
  http.delete(`${BASE_URL}/api/v1/readers/batch`, async ({ request }) => {
    await delay(MOCK_DELAY)

    const url = new URL(request.url)
    const idsParam = url.searchParams.get('ids') || ''
    const ids = idsParam.split(',').map(id => parseInt(id)).filter(id => !isNaN(id))

    if (ids.length === 0) {
      return HttpResponse.json(
        errorResponse(400, '未提供有效的ID列表'),
        { status: 400 }
      )
    }

    let deletedCount = 0
    const errors: string[] = []

    ids.forEach(id => {
      const index = readersStore.findIndex(r => r.readerId === id)
      if (index !== -1) {
        const reader = readersStore[index]
        if (reader.currentBorrowCount > 0) {
          errors.push(`读者 ${reader.realName} 还有未归还的图书`)
        } else {
          readersStore.splice(index, 1)
          deletedCount++
        }
      }
    })

    return HttpResponse.json(
      successResponse({
        deletedCount,
        requestedIds: ids.length,
        errors: errors.slice(0, 10) // 最多返回10条错误信息
      })
    )
  }),

  /**
   * GET /api/v1/readers/types
   * 获取读者类型列表
   */
  http.get(`${BASE_URL}/api/v1/readers/types`, async () => {
    await delay(MOCK_DELAY)

    return HttpResponse.json(
      successResponse(readerTypes)
    )
  }),

  /**
   * POST /api/v1/readers/:id/card
   * 办理/更新借阅证
   */
  http.post(`${BASE_URL}/api/v1/readers/:id/card`, async ({ params, request }) => {
    await delay(MOCK_DELAY)

    const readerId = parseInt(params.id as string)
    const body = await request.json() as any

    const readerIndex = readersStore.findIndex(r => r.readerId === readerId)
    if (readerIndex === -1) {
      return HttpResponse.json(
        errorResponse(404, '读者不存在'),
        { status: 404 }
      )
    }

    const reader = readersStore[readerIndex]

    // 更新借阅证信息
    const updatedReader = {
      ...reader,
      cardExpireDate: body.cardExpireDate || new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      depositAmount: body.depositAmount || reader.depositAmount,
      status: 'active',
      updatedTime: new Date().toISOString()
    }

    readersStore[readerIndex] = updatedReader

    return HttpResponse.json(
      successResponse({
        ...updatedReader,
        message: '借阅证办理成功'
      })
    )
  }),

  /**
   * PUT /api/v1/readers/:id/status
   * 更新读者状态(冻结/解冻)
   */
  http.put(`${BASE_URL}/api/v1/readers/:id/status`, async ({ params, request }) => {
    await delay(MOCK_DELAY)

    const readerId = parseInt(params.id as string)
    const body = await request.json() as any

    const readerIndex = readersStore.findIndex(r => r.readerId === readerId)
    if (readerIndex === -1) {
      return HttpResponse.json(
        errorResponse(404, '读者不存在'),
        { status: 404 }
      )
    }

    const reader = readersStore[readerIndex]

    // 更新状态
    const updatedReader = {
      ...reader,
      status: body.status,
      updatedTime: new Date().toISOString()
    }

    readersStore[readerIndex] = updatedReader

    const statusText = body.status === 'suspended' ? '冻结' : '解冻'
    return HttpResponse.json(
      successResponse({
        ...updatedReader,
        message: `读者账户${statusText}成功`
      })
    )
  })
]
