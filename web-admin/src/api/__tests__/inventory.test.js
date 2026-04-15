import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} }))
}))

import request from '@/utils/request'
import * as api from '@/api/inventory'

describe('api/inventory', () => {
  beforeEach(() => vi.clearAllMocks())

  // ========== 库存管理 APIs ==========

  describe('getInventoryList', () => {
    it('should call request with correct shape', async () => {
      const params = { pageNum: 1, pageSize: 10, location: 'A1', keyword: 'test' }
      await api.getInventoryList(params)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory',
        method: 'get',
        params
      })
    })
  })

  describe('getInventoryById', () => {
    it('should call request with correct shape and interpolated id', async () => {
      const id = 123
      await api.getInventoryById(id)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/123',
        method: 'get'
      })
    })
  })

  describe('adjustInventory', () => {
    it('should call request with correct shape', async () => {
      const data = { bookId: 42, adjustQuantity: 5, reason: 'Restocking' }
      await api.adjustInventory(data)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/adjust',
        method: 'post',
        data
      })
    })
  })

  describe('getInventoryAlerts', () => {
    it('should call request with correct shape', async () => {
      const params = { pageNum: 1, pageSize: 20 }
      await api.getInventoryAlerts(params)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/alerts',
        method: 'get',
        params
      })
    })
  })

  describe('getInventoryStats', () => {
    it('should call request with correct shape', async () => {
      await api.getInventoryStats()

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/stats',
        method: 'get'
      })
    })
  })

  // ========== 盘点任务 APIs ==========

  describe('getInventoryTasks', () => {
    it('should call request with correct shape', async () => {
      const params = { pageNum: 1, pageSize: 15, status: 'IN_PROGRESS', taskType: 'FULL' }
      await api.getInventoryTasks(params)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/tasks',
        method: 'get',
        params
      })
    })
  })

  describe('getInventoryTaskById', () => {
    it('should call request with correct shape and interpolated id', async () => {
      const id = 456
      await api.getInventoryTaskById(id)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/tasks/456',
        method: 'get'
      })
    })
  })

  describe('createInventoryTask', () => {
    it('should call request with correct shape', async () => {
      const data = {
        taskName: 'Q2 Inventory Check',
        taskType: 'FULL',
        totalBooks: 5000,
        operatorId: 1,
        operatorName: 'John Doe',
        notes: 'Spring inventory'
      }
      await api.createInventoryTask(data)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/tasks',
        method: 'post',
        data
      })
    })
  })

  describe('updateInventoryTask', () => {
    it('should call request with correct shape and interpolated id', async () => {
      const id = 456
      const data = { taskName: 'Updated Task', notes: 'Updated notes' }
      await api.updateInventoryTask(id, data)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/tasks/456',
        method: 'put',
        data
      })
    })
  })

  describe('startInventoryTask', () => {
    it('should call request with correct shape and interpolated id', async () => {
      const id = 456
      await api.startInventoryTask(id)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/tasks/456/start',
        method: 'post'
      })
    })
  })

  describe('completeInventoryTask', () => {
    it('should call request with correct shape and interpolated id', async () => {
      const id = 456
      await api.completeInventoryTask(id)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/tasks/456/complete',
        method: 'post'
      })
    })
  })

  describe('cancelInventoryTask', () => {
    it('should call request with correct shape and interpolated id', async () => {
      const id = 456
      await api.cancelInventoryTask(id)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/tasks/456/cancel',
        method: 'post'
      })
    })
  })

  describe('getInventoryTaskItems', () => {
    it('should call request with correct shape and interpolated taskId', async () => {
      const taskId = 789
      const params = { pageNum: 1, pageSize: 50, status: 'PENDING' }
      await api.getInventoryTaskItems(taskId, params)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/tasks/789/items',
        method: 'get',
        params
      })
    })
  })

  describe('recordInventoryItem', () => {
    it('should call request with correct shape and interpolated taskId', async () => {
      const taskId = 789
      const data = { bookId: 100, actualQuantity: 45 }
      await api.recordInventoryItem(taskId, data)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/tasks/789/items',
        method: 'post',
        data
      })
    })
  })

  describe('exportInventoryReport', () => {
    it('should call request with correct shape and responseType', async () => {
      const taskId = 789
      await api.exportInventoryReport(taskId)

      expect(request).toHaveBeenCalledWith({
        url: '/api/v1/inventory/tasks/789/export',
        method: 'get',
        responseType: 'blob'
      })
    })
  })

  // ========== Alias Tests ==========

  describe('aliases', () => {
    it('getInventoryDiffs should alias to getInventoryTaskItems', () => {
      expect(api.getInventoryDiffs).toBe(api.getInventoryTaskItems)
    })

    it('recordInventoryResult should alias to recordInventoryItem', () => {
      expect(api.recordInventoryResult).toBe(api.recordInventoryItem)
    })

    it('deleteInventoryTask should alias to cancelInventoryTask', () => {
      expect(api.deleteInventoryTask).toBe(api.cancelInventoryTask)
    })

    it('pauseInventoryTask should alias to cancelInventoryTask', () => {
      expect(api.pauseInventoryTask).toBe(api.cancelInventoryTask)
    })

    it('resumeInventoryTask should alias to startInventoryTask', () => {
      expect(api.resumeInventoryTask).toBe(api.startInventoryTask)
    })
  })
})
