import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} }))
}))

import request from '@/utils/request'
import * as api from '@/api/circulation'

describe('api/circulation', () => {
  beforeEach(() => vi.clearAllMocks())

  describe('getCirculationRecords', () => {
    it('should call request with correct shape', () => {
      const params = { pageNum: 1, pageSize: 10, keyword: 'test' }
      api.getCirculationRecords(params)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/borrows',
        method: 'get',
        params
      })
    })
  })

  describe('getCirculationRecordById', () => {
    it('should call request with correct shape and interpolated ID', () => {
      api.getCirculationRecordById(42)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/borrows/42',
        method: 'get'
      })
    })
  })

  describe('borrowBook', () => {
    it('should call request with correct shape', () => {
      const data = { readerId: 1, bookId: 2, remark: 'test' }
      api.borrowBook(data)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/borrows/borrow',
        method: 'post',
        data
      })
    })
  })

  describe('returnBook', () => {
    it('should call request with correct shape', () => {
      const data = { borrowId: 1, payFine: false, remark: 'test' }
      api.returnBook(data)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/borrows/return',
        method: 'post',
        data
      })
    })
  })

  describe('renewBook', () => {
    it('should call request with correct shape', () => {
      const data = { borrowId: 1, renewDays: 30, remark: 'test' }
      api.renewBook(data)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/borrows/renew',
        method: 'post',
        data
      })
    })
  })

  describe('getReservations', () => {
    it('should call request with correct shape', () => {
      const params = { pageNum: 1, pageSize: 10, status: 'RESERVED' }
      api.getReservations(params)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/reserves',
        method: 'get',
        params
      })
    })
  })

  describe('reserveBook', () => {
    it('should call request with correct shape', () => {
      const data = { readerId: 1, bookId: 2, remark: 'test' }
      api.reserveBook(data)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/reserves/reserve',
        method: 'post',
        data
      })
    })
  })

  describe('cancelReservation', () => {
    it('should call request with correct shape and interpolated ID', () => {
      api.cancelReservation(42)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/reserves/42/cancel',
        method: 'post'
      })
    })
  })

  describe('getCirculationStats', () => {
    it('should call request with correct shape', () => {
      const params = { startDate: '2025-01-01', endDate: '2025-12-31' }
      api.getCirculationStats(params)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/analytics/circulation',
        method: 'get',
        params
      })
    })
  })

  describe('batchReturnBooks', () => {
    it('should call request with correct shape', () => {
      const borrowIds = [1, 2, 3]
      api.batchReturnBooks(borrowIds)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/borrows/batch-return',
        method: 'post',
        data: { borrowIds }
      })
    })
  })

  describe('getReaderBorrowHistory', () => {
    it('should call request with correct shape and merged params', () => {
      const readerId = 42
      const params = { pageNum: 1, pageSize: 10 }
      api.getReaderBorrowHistory(readerId, params)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/borrows',
        method: 'get',
        params: { pageNum: 1, pageSize: 10, readerId: 42 }
      })
    })
  })

  describe('getBookCirculationHistory', () => {
    it('should call request with correct shape and merged params', () => {
      const bookId = 42
      const params = { pageNum: 1, pageSize: 10 }
      api.getBookCirculationHistory(bookId, params)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/borrows',
        method: 'get',
        params: { pageNum: 1, pageSize: 10, bookId: 42 }
      })
    })
  })

  describe('getBorrowRecordByBarcode', () => {
    it('should call request with correct shape', () => {
      const barcode = 'ISBN123456'
      api.getBorrowRecordByBarcode(barcode)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/borrows',
        method: 'get',
        params: { barcode }
      })
    })
  })

  describe('pickupReservation', () => {
    it('should call request with correct shape and interpolated ID', () => {
      api.pickupReservation(42)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/reserves/42/pickup',
        method: 'post'
      })
    })
  })

  describe('getOverdueBorrows', () => {
    it('should call request with correct shape', () => {
      api.getOverdueBorrows()

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/borrows/overdue',
        method: 'get'
      })
    })
  })

  describe('processReservation', () => {
    it('should call request with correct shape and interpolated ID', () => {
      api.processReservation(42)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/reserves/42/pickup',
        method: 'post'
      })
    })
  })

  describe('notifyReservation', () => {
    it('should call request with correct shape and interpolated ID', () => {
      api.notifyReservation(42)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/reserves/42/notify',
        method: 'post'
      })
    })
  })
})
