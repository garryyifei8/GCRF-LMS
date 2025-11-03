import { http, HttpResponse } from 'msw'
import {
  generateCirculationRecords,
  generateReservations,
  generateCirculationStats,
  circulationStatus,
  reservationStatus
} from '../data/circulation'

// 生成并缓存流通记录数据
let circulationData = generateCirculationRecords(500)
let reservationData = generateReservations(100)

export const circulationHandlers = [
  // 获取流通记录列表
  http.get('/api/v1/circulation/records', ({ request }) => {
    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10')
    const keyword = url.searchParams.get('keyword') || ''
    const status = url.searchParams.get('status') || ''
    const startDate = url.searchParams.get('startDate') || ''
    const endDate = url.searchParams.get('endDate') || ''

    // 过滤数据
    let filteredRecords = circulationData

    if (keyword) {
      filteredRecords = filteredRecords.filter(record =>
        record.bookTitle.includes(keyword) ||
        record.readerName.includes(keyword) ||
        record.readerCardNo.includes(keyword) ||
        record.bookBarcode.includes(keyword)
      )
    }

    if (status) {
      filteredRecords = filteredRecords.filter(record => record.status === status)
    }

    if (startDate) {
      filteredRecords = filteredRecords.filter(record =>
        new Date(record.borrowDate) >= new Date(startDate)
      )
    }

    if (endDate) {
      filteredRecords = filteredRecords.filter(record =>
        new Date(record.borrowDate) <= new Date(endDate)
      )
    }

    // 分页
    const total = filteredRecords.length
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    const records = filteredRecords.slice(start, end)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        list: records,
        total,
        pageNum,
        pageSize,
        pages: Math.ceil(total / pageSize)
      }
    })
  }),

  // 借书
  http.post('/api/v1/circulation/borrow', async ({ request }) => {
    const { readerId, bookId } = await request.json()

    const newRecord = {
      id: circulationData.length + 1,
      recordNo: `CR${String(circulationData.length + 1).padStart(10, '0')}`,
      bookId,
      bookTitle: `图书${bookId}`,
      bookBarcode: `BK${String(bookId).padStart(8, '0')}`,
      readerId,
      readerName: '测试读者',
      readerCardNo: `RD${String(readerId).padStart(8, '0')}`,
      borrowDate: new Date().toISOString(),
      dueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
      returnDate: null,
      renewCount: 0,
      isOverdue: false,
      overdueDays: 0,
      overdueFine: 0,
      status: circulationStatus.BORROWED,
      operatorId: 1,
      operatorName: '管理员',
      remark: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }

    circulationData.unshift(newRecord)

    return HttpResponse.json({
      code: 200,
      message: '借书成功',
      data: newRecord
    })
  }),

  // 还书
  http.post('/api/v1/circulation/return', async ({ request }) => {
    const { recordId } = await request.json()

    const index = circulationData.findIndex(r => r.id === recordId)
    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        message: '借阅记录不存在'
      }, { status: 404 })
    }

    circulationData[index] = {
      ...circulationData[index],
      returnDate: new Date().toISOString(),
      status: circulationStatus.RETURNED,
      updatedAt: new Date().toISOString()
    }

    return HttpResponse.json({
      code: 200,
      message: '还书成功',
      data: circulationData[index]
    })
  }),

  // 续借
  http.post('/api/v1/circulation/renew', async ({ request }) => {
    const { recordId } = await request.json()

    const index = circulationData.findIndex(r => r.id === recordId)
    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        message: '借阅记录不存在'
      }, { status: 404 })
    }

    const record = circulationData[index]
    if (record.renewCount >= 2) {
      return HttpResponse.json({
        code: 400,
        message: '已达到最大续借次数'
      }, { status: 400 })
    }

    circulationData[index] = {
      ...record,
      dueDate: new Date(new Date(record.dueDate).getTime() + 30 * 24 * 60 * 60 * 1000).toISOString(),
      renewCount: record.renewCount + 1,
      status: circulationStatus.RENEWED,
      updatedAt: new Date().toISOString()
    }

    return HttpResponse.json({
      code: 200,
      message: '续借成功',
      data: circulationData[index]
    })
  }),

  // 获取预约列表
  http.get('/api/v1/circulation/reservations', ({ request }) => {
    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10')
    const keyword = url.searchParams.get('keyword') || ''
    const status = url.searchParams.get('status') || ''

    // 过滤数据
    let filteredReservations = reservationData

    if (keyword) {
      filteredReservations = filteredReservations.filter(reservation =>
        reservation.bookTitle.includes(keyword) ||
        reservation.readerName.includes(keyword) ||
        reservation.readerCardNo.includes(keyword)
      )
    }

    if (status) {
      filteredReservations = filteredReservations.filter(reservation => reservation.status === status)
    }

    // 分页
    const total = filteredReservations.length
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    const reservations = filteredReservations.slice(start, end)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        list: reservations,
        total,
        pageNum,
        pageSize,
        pages: Math.ceil(total / pageSize)
      }
    })
  }),

  // 预约图书
  http.post('/api/v1/circulation/reserve', async ({ request }) => {
    const { readerId, bookId } = await request.json()

    const newReservation = {
      id: reservationData.length + 1,
      reservationNo: `RV${String(reservationData.length + 1).padStart(10, '0')}`,
      bookId,
      bookTitle: `图书${bookId}`,
      bookBarcode: `BK${String(bookId).padStart(8, '0')}`,
      readerId,
      readerName: '测试读者',
      readerCardNo: `RD${String(readerId).padStart(8, '0')}`,
      reservationDate: new Date().toISOString(),
      readyDate: null,
      pickupDate: null,
      expireDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
      status: reservationStatus.PENDING,
      statusName: '等待中',
      queue: 1,
      remark: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }

    reservationData.unshift(newReservation)

    return HttpResponse.json({
      code: 200,
      message: '预约成功',
      data: newReservation
    })
  }),

  // 取消预约
  http.post('/api/v1/circulation/cancel-reservation', async ({ request }) => {
    const { reservationId } = await request.json()

    const index = reservationData.findIndex(r => r.id === reservationId)
    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        message: '预约记录不存在'
      }, { status: 404 })
    }

    reservationData[index] = {
      ...reservationData[index],
      status: reservationStatus.CANCELLED,
      statusName: '已取消',
      updatedAt: new Date().toISOString()
    }

    return HttpResponse.json({
      code: 200,
      message: '取消预约成功',
      data: reservationData[index]
    })
  }),

  // 获取流通统计数据
  http.get('/api/v1/circulation/stats', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateCirculationStats()
    })
  })
]
