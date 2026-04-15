import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} }))
}))

import request from '@/utils/request'
import * as api from '@/api/department'

describe('api/department', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getDepartmentList calls request with correct shape', async () => {
    const params = { pageNum: 1, pageSize: 10 }
    await api.getDepartmentList(params)

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments',
      method: 'get',
      params
    })
  })

  it('getDepartmentById calls request with correct shape', async () => {
    const id = 123
    await api.getDepartmentById(id)

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments/123',
      method: 'get'
    })
  })

  it('createDepartment calls request with correct shape', async () => {
    const data = { name: 'IT Department', description: 'Information Technology' }
    await api.createDepartment(data)

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments',
      method: 'post',
      data
    })
  })

  it('updateDepartment calls request with correct shape', async () => {
    const data = { id: 123, name: 'Updated IT', description: 'Updated Description' }
    await api.updateDepartment(data)

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments',
      method: 'put',
      data
    })
  })

  it('deleteDepartment calls request with correct shape', async () => {
    const id = 123
    await api.deleteDepartment(id)

    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/system/departments/123',
      method: 'delete'
    })
  })
})
