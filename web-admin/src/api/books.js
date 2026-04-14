import request from '@/utils/request'

/**
 * 获取图书列表(分页+搜索)
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.keyword - 搜索关键词(书名/作者/ISBN)
 * @param {string} params.category - 图书分类
 * @param {string} params.status - 图书状态 (available/borrowed/reserved/damaged)
 */
export function getBooks(params) {
  return request({
    url: '/api/v1/books',
    method: 'get',
    params
  })
}

/**
 * 获取图书详情
 * @param {number} id - 图书ID
 */
export function getBookById(id) {
  return request({
    url: `/api/v1/books/${id}`,
    method: 'get'
  })
}

/**
 * 新增图书
 * @param {Object} data - 图书数据
 * @param {string} data.isbn - ISBN编号(必填)
 * @param {string} data.title - 书名(必填)
 * @param {string} data.author - 作者(必填)
 * @param {string} data.publisher - 出版社
 * @param {string} data.publishDate - 出版日期
 * @param {string} data.category - 分类代码
 * @param {string} data.categoryName - 分类名称
 * @param {number} data.price - 价格
 * @param {string} data.coverUrl - 封面图URL
 * @param {string} data.description - 简介
 * @param {number} data.totalQuantity - 总副本数
 * @param {string} data.status - 状态
 */
export function createBook(data) {
  return request({
    url: '/api/v1/books',
    method: 'post',
    data
  })
}

/**
 * 更新图书信息
 * @param {number} id - 图书ID
 * @param {Object} data - 图书数据
 */
export function updateBook(id, data) {
  return request({
    url: `/api/v1/books/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除图书
 * @param {number} id - 图书ID
 */
export function deleteBook(id) {
  return request({
    url: `/api/v1/books/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除图书 (新API)
 * @param {Array<number>} ids - 图书ID数组
 */
export function batchDeleteBooks(ids) {
  return request({
    url: '/api/v1/books/batch-delete',
    method: 'post',
    data: { ids }
  })
}

/**
 * 批量导入图书 (新API - 上传Excel文件)
 * @param {File} file - Excel文件
 */
export function batchImportBooks(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/v1/books/batch-import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 下载导入模板 (新API)
 */
export function downloadImportTemplate() {
  return request({
    url: '/api/v1/books/import-template',
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 获取图书分类列表
 */
export function getBookCategories() {
  return request({
    url: '/api/v1/books/categories',
    method: 'get'
  })
}

/**
 * 根据条码号查询图书信息 (新API)
 * @param {string} barcode - 图书条码号
 */
export function getBookByBarcode(barcode) {
  return request({
    url: `/api/v1/books/barcode/${barcode}`,
    method: 'get'
  })
}

/**
 * 根据ISBN查询图书信息（从第三方API）(新API)
 * @param {string} isbn - ISBN号
 */
export function lookupByIsbn(isbn) {
  return request({
    url: `/api/v1/books/isbn/${isbn}`,
    method: 'get'
  })
}

/**
 * 批量生成条码 (新API)
 * @param {Array<number>} bookIds - 图书ID数组
 * @param {string} prefix - 条码前缀（可选）
 */
export function generateBarcodes(bookIds, prefix = 'GCRF') {
  return request({
    url: '/api/v1/books/barcode/generate',
    method: 'post',
    data: { bookIds, prefix }
  })
}

/**
 * 全文搜索图书 (新API)
 * @param {Object} params - 搜索参数
 * @param {string} params.query - 搜索关键词
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {number} params.categoryId - 分类ID（可选）
 * @param {string} params.publisher - 出版社（可选）
 * @param {string} params.language - 语言（可选）
 * @param {boolean} params.availableOnly - 仅显示可借（可选）
 */
export function searchBooks(params) {
  return request({
    url: '/api/v1/books/search',
    method: 'post',
    data: params
  })
}

/**
 * 获取图书库存信息 (新API)
 * @param {number} bookId - 图书ID
 */
export function getBookInventory(bookId) {
  return request({
    url: `/api/v1/books/${bookId}/inventory`,
    method: 'get'
  })
}

/**
 * 更新图书库存 (新API)
 * @param {number} bookId - 图书ID
 * @param {Object} data - 库存数据
 * @param {number} data.totalCopies - 新的总数量
 * @param {string} data.reason - 调整原因
 */
export function updateBookInventory(bookId, data) {
  return request({
    url: `/api/v1/books/${bookId}/inventory`,
    method: 'put',
    data
  })
}

/**
 * 获取图书统计数据
 */
export function getBookStats() {
  return request({
    url: '/api/v1/books/stats',
    method: 'get'
  })
}

/**
 * 健康检查
 */
export function healthCheck() {
  return request({
    url: '/api/v1/books/health',
    method: 'get'
  })
}

// 保留兼容性的别名
export const searchBookByISBN = lookupByIsbn
export const getCategoryTree = getBookCategories
export const importBooks = batchImportBooks
