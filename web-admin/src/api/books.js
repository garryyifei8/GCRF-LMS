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
 * @param {number} data.totalCopies - 总副本数
 * @param {number} data.availableCopies - 可借副本数
 * @param {string} data.location - 存放位置
 * @param {string} data.callNumber - 索书号
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
 * 批量删除图书
 * @param {Array<number>} ids - 图书ID数组
 */
export function batchDeleteBooks(ids) {
  return request({
    url: '/api/v1/books/batch',
    method: 'delete',
    params: {
      ids: ids.join(',')
    }
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
 * 导入图书数据
 * @param {Array<Object>} books - 图书数据数组
 */
export function importBooks(books) {
  return request({
    url: '/api/v1/books/import',
    method: 'post',
    data: {
      books
    }
  })
}
