import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} }))
}))

import request from '@/utils/request'
import * as api from '@/api/readers'

describe('api/readers', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getReaders_shouldCallWithParams', () => {
    const params = {
      pageNum: 1,
      pageSize: 10,
      keyword: 'test',
      readerType: 'student',
      status: 'active'
    }
    api.getReaders(params)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/readers',
      method: 'get',
      params
    })
  })

  it('getReaderById_shouldInterpolateId', () => {
    api.getReaderById(42)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/readers/42',
      method: 'get'
    })
  })

  it('createReader_shouldPostData', () => {
    const data = {
      realName: 'John Doe',
      phone: '13800138000',
      readerType: 'student',
      gender: 'male',
      idCard: '110101199003071234',
      email: 'john@example.com',
      address: 'Beijing',
      maxBorrowCount: 10,
      depositAmount: 100,
      cardExpireDate: '2026-12-31',
      status: 'active'
    }
    api.createReader(data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/readers',
      method: 'post',
      data
    })
  })

  it('updateReader_shouldInterpolateIdAndPutData', () => {
    const data = { realName: 'Jane Doe', phone: '13800138001' }
    api.updateReader(42, data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/readers/42',
      method: 'put',
      data
    })
  })

  it('deleteReader_shouldInterpolateId', () => {
    api.deleteReader(42)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/readers/42',
      method: 'delete'
    })
  })

  it('batchDeleteReaders_shouldJoinIdsAsCommaSeparatedString', () => {
    api.batchDeleteReaders([1, 2, 3, 4])
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/readers/batch',
      method: 'delete',
      params: {
        ids: '1,2,3,4'
      }
    })
  })

  it('getReaderTypes_shouldGetWithoutParams', () => {
    api.getReaderTypes()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/readers/types',
      method: 'get'
    })
  })

  it('issueCard_shouldInterpolateIdAndPostData', () => {
    const data = { cardExpireDate: '2026-12-31', depositAmount: 150 }
    api.issueCard(42, data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/readers/42/card',
      method: 'post',
      data
    })
  })

  it('updateReaderStatus_shouldInterpolateIdAndPutStatus', () => {
    api.updateReaderStatus(42, 'suspended')
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/readers/42/status',
      method: 'put',
      data: { status: 'suspended' }
    })
  })

  it('getReaderByCardNumber_shouldInterpolateCardNumber', () => {
    api.getReaderByCardNumber('ABC123456')
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/readers/card/ABC123456',
      method: 'get'
    })
  })
})
