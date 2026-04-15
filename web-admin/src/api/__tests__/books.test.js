import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} }))
}))

import request from '@/utils/request'
import * as api from '@/api/books'

describe('api/books', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getBooks_shouldCallWithParamsAndGetMethod', () => {
    const params = { pageNum: 1, pageSize: 10 }
    api.getBooks(params)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books',
      method: 'get',
      params
    })
  })

  it('getBookById_shouldCallWithInterpolatedUrl', () => {
    api.getBookById(123)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/123',
      method: 'get'
    })
  })

  it('createBook_shouldCallWithPostAndData', () => {
    const data = { title: 'Test Book', author: 'Test Author', isbn: '123-456' }
    api.createBook(data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books',
      method: 'post',
      data
    })
  })

  it('updateBook_shouldCallWithPutAndInterpolatedUrl', () => {
    const data = { title: 'Updated Book' }
    api.updateBook(456, data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/456',
      method: 'put',
      data
    })
  })

  it('deleteBook_shouldCallWithDeleteAndInterpolatedUrl', () => {
    api.deleteBook(789)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/789',
      method: 'delete'
    })
  })

  it('batchDeleteBooks_shouldCallWithPostAndIdsData', () => {
    const ids = [1, 2, 3]
    api.batchDeleteBooks(ids)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/batch-delete',
      method: 'post',
      data: { ids }
    })
  })

  it('batchImportBooks_shouldCallWithFormDataAndHeaders', () => {
    const file = new File(['content'], 'books.xlsx', { type: 'application/vnd.ms-excel' })
    api.batchImportBooks(file)

    const callArgs = request.mock.calls[0][0]
    expect(callArgs.url).toBe('/api/v1/books/batch-import')
    expect(callArgs.method).toBe('post')
    expect(callArgs.data instanceof FormData).toBe(true)
    expect(callArgs.headers).toEqual({
      'Content-Type': 'multipart/form-data'
    })
  })

  it('downloadImportTemplate_shouldCallWithBlobResponseType', () => {
    api.downloadImportTemplate()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/import-template',
      method: 'get',
      responseType: 'blob'
    })
  })

  it('getBookCategories_shouldCallWithGetMethod', () => {
    api.getBookCategories()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/categories',
      method: 'get'
    })
  })

  it('getBookByBarcode_shouldCallWithInterpolatedUrl', () => {
    api.getBookByBarcode('BARCODE123')
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/barcode/BARCODE123',
      method: 'get'
    })
  })

  it('lookupByIsbn_shouldCallWithInterpolatedUrl', () => {
    api.lookupByIsbn('978-3-16-148410-0')
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/isbn/978-3-16-148410-0',
      method: 'get'
    })
  })

  it('generateBarcodes_shouldCallWithBookIdsAndPrefix', () => {
    const bookIds = [1, 2, 3]
    api.generateBarcodes(bookIds, 'GCRF')
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/barcode/generate',
      method: 'post',
      data: { bookIds, prefix: 'GCRF' }
    })
  })

  it('generateBarcodes_shouldUseDefaultPrefix', () => {
    const bookIds = [1, 2]
    api.generateBarcodes(bookIds)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/barcode/generate',
      method: 'post',
      data: { bookIds, prefix: 'GCRF' }
    })
  })

  it('searchBooks_shouldCallWithPostAndParamsAsData', () => {
    const params = { query: 'test', pageNum: 1, pageSize: 20 }
    api.searchBooks(params)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/search',
      method: 'post',
      data: params
    })
  })

  it('getBookInventory_shouldCallWithInterpolatedUrl', () => {
    api.getBookInventory(999)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/999/inventory',
      method: 'get'
    })
  })

  it('updateBookInventory_shouldCallWithPutAndInterpolatedUrl', () => {
    const data = { totalCopies: 50, reason: 'Stock adjustment' }
    api.updateBookInventory(999, data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/999/inventory',
      method: 'put',
      data
    })
  })

  it('getBookStats_shouldCallWithGetMethod', () => {
    api.getBookStats()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/stats',
      method: 'get'
    })
  })

  it('healthCheck_shouldCallWithGetMethod', () => {
    api.healthCheck()
    expect(request).toHaveBeenCalledWith({
      url: '/api/v1/books/health',
      method: 'get'
    })
  })

  // Test aliases
  it('searchBookByISBN_shouldBeAliasForLookupByIsbn', () => {
    expect(api.searchBookByISBN).toBe(api.lookupByIsbn)
  })

  it('getCategoryTree_shouldBeAliasForGetBookCategories', () => {
    expect(api.getCategoryTree).toBe(api.getBookCategories)
  })

  it('importBooks_shouldBeAliasForBatchImportBooks', () => {
    expect(api.importBooks).toBe(api.batchImportBooks)
  })
})
