import { http, HttpResponse, delay } from 'msw'
import { successResponse, errorResponse, pageResponse } from '../../data/factory'
import { mockBooks, findBookById } from '../../data/books'

const BASE_URL = import.meta.env.VITE_API_BASE_URL
const MOCK_DELAY = parseInt(import.meta.env.VITE_MOCK_DELAY) || 300

// 内存存储,模拟持久化
let booksStore = [...mockBooks]
let nextId = Math.max(...booksStore.map(b => b.bookId)) + 1

// 图书分类列表
const bookCategories = [
  { code: 'I247', name: '中国小说' },
  { code: 'K', name: '历史' },
  { code: 'B', name: '哲学' },
  { code: 'TP', name: '计算机' },
  { code: 'F', name: '经济' },
  { code: 'H', name: '语言学' },
  { code: 'O', name: '数学' },
  { code: 'J', name: '艺术' }
]

/**
 * Books API Handlers
 * 图书管理服务 Mock API
 */
export const booksHandlers = [
  /**
   * GET /api/v1/books
   * 获取图书列表(分页+搜索)
   */
  http.get(`${BASE_URL}/api/v1/books`, async ({ request }) => {
    await delay(MOCK_DELAY)

    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '20')
    const keyword = url.searchParams.get('keyword') || ''
    const category = url.searchParams.get('category') || ''
    const status = url.searchParams.get('status') || ''

    // 过滤数据
    let filteredBooks = [...booksStore]

    if (keyword) {
      const kw = keyword.toLowerCase()
      filteredBooks = filteredBooks.filter(book =>
        book.title.toLowerCase().includes(kw) ||
        book.author.toLowerCase().includes(kw) ||
        book.isbn.includes(kw)
      )
    }

    if (category) {
      filteredBooks = filteredBooks.filter(book => book.category === category)
    }

    if (status) {
      filteredBooks = filteredBooks.filter(book => book.status === status)
    }

    // 分页
    const total = filteredBooks.length
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    const records = filteredBooks.slice(start, end)

    return HttpResponse.json(
      pageResponse(records, total, pageNum, pageSize)
    )
  }),

  /**
   * GET /api/v1/books/:id
   * 获取图书详情
   */
  http.get(`${BASE_URL}/api/v1/books/:id`, async ({ params }) => {
    await delay(MOCK_DELAY)

    const bookId = parseInt(params.id as string)
    const book = booksStore.find(b => b.bookId === bookId)

    if (!book) {
      return HttpResponse.json(
        errorResponse(404, '图书不存在'),
        { status: 404 }
      )
    }

    return HttpResponse.json(successResponse(book))
  }),

  /**
   * POST /api/v1/books
   * 新增图书
   */
  http.post(`${BASE_URL}/api/v1/books`, async ({ request }) => {
    await delay(MOCK_DELAY)

    const body = await request.json() as any

    // 验证必填字段
    if (!body.isbn || !body.title || !body.author) {
      return HttpResponse.json(
        errorResponse(400, '缺少必填字段'),
        { status: 400 }
      )
    }

    // 检查ISBN是否已存在
    if (booksStore.some(b => b.isbn === body.isbn)) {
      return HttpResponse.json(
        errorResponse(409, 'ISBN已存在'),
        { status: 409 }
      )
    }

    const newBook = {
      bookId: nextId++,
      isbn: body.isbn,
      title: body.title,
      author: body.author,
      publisher: body.publisher || '',
      publishDate: body.publishDate || new Date().toISOString().split('T')[0],
      category: body.category || '',
      categoryName: body.categoryName || '',
      price: body.price || 0,
      coverUrl: body.coverUrl || '',
      description: body.description || '',
      totalCopies: body.totalCopies || 1,
      availableCopies: body.availableCopies || 1,
      borrowCount: 0,
      rating: 0,
      status: body.status || 'available',
      location: body.location || '',
      callNumber: body.callNumber || '',
      createdTime: new Date().toISOString(),
      updatedTime: new Date().toISOString()
    }

    booksStore.push(newBook)

    return HttpResponse.json(
      successResponse(newBook),
      { status: 201 }
    )
  }),

  /**
   * PUT /api/v1/books/:id
   * 更新图书
   */
  http.put(`${BASE_URL}/api/v1/books/:id`, async ({ params, request }) => {
    await delay(MOCK_DELAY)

    const bookId = parseInt(params.id as string)
    const body = await request.json() as any

    const bookIndex = booksStore.findIndex(b => b.bookId === bookId)
    if (bookIndex === -1) {
      return HttpResponse.json(
        errorResponse(404, '图书不存在'),
        { status: 404 }
      )
    }

    // 如果修改了ISBN,检查是否与其他图书冲突
    if (body.isbn && body.isbn !== booksStore[bookIndex].isbn) {
      if (booksStore.some(b => b.isbn === body.isbn && b.bookId !== bookId)) {
        return HttpResponse.json(
          errorResponse(409, 'ISBN已存在'),
          { status: 409 }
        )
      }
    }

    // 更新图书信息
    const updatedBook = {
      ...booksStore[bookIndex],
      ...body,
      bookId, // 确保ID不变
      updatedTime: new Date().toISOString()
    }

    booksStore[bookIndex] = updatedBook

    return HttpResponse.json(successResponse(updatedBook))
  }),

  /**
   * DELETE /api/v1/books/:id
   * 删除图书
   */
  http.delete(`${BASE_URL}/api/v1/books/:id`, async ({ params }) => {
    await delay(MOCK_DELAY)

    const bookId = parseInt(params.id as string)
    const bookIndex = booksStore.findIndex(b => b.bookId === bookId)

    if (bookIndex === -1) {
      return HttpResponse.json(
        errorResponse(404, '图书不存在'),
        { status: 404 }
      )
    }

    booksStore.splice(bookIndex, 1)

    return HttpResponse.json(
      successResponse({ deleted: true, bookId })
    )
  }),

  /**
   * DELETE /api/v1/books/batch
   * 批量删除图书
   */
  http.delete(`${BASE_URL}/api/v1/books/batch`, async ({ request }) => {
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

    // 删除指定的图书
    const deletedCount = ids.reduce((count, id) => {
      const index = booksStore.findIndex(b => b.bookId === id)
      if (index !== -1) {
        booksStore.splice(index, 1)
        return count + 1
      }
      return count
    }, 0)

    return HttpResponse.json(
      successResponse({ deletedCount, requestedIds: ids.length })
    )
  }),

  /**
   * GET /api/v1/books/categories
   * 获取图书分类列表
   */
  http.get(`${BASE_URL}/api/v1/books/categories`, async () => {
    await delay(MOCK_DELAY)

    return HttpResponse.json(
      successResponse(bookCategories)
    )
  }),

  /**
   * POST /api/v1/books/import
   * 导入图书数据
   */
  http.post(`${BASE_URL}/api/v1/books/import`, async ({ request }) => {
    await delay(MOCK_DELAY * 3) // 导入操作延迟更长

    const body = await request.json() as any
    const books = body.books || []

    if (!Array.isArray(books) || books.length === 0) {
      return HttpResponse.json(
        errorResponse(400, '导入数据为空或格式错误'),
        { status: 400 }
      )
    }

    let successCount = 0
    let failCount = 0
    const errors: string[] = []

    books.forEach((book: any, index: number) => {
      // 验证必填字段
      if (!book.isbn || !book.title || !book.author) {
        failCount++
        errors.push(`第${index + 1}条: 缺少必填字段`)
        return
      }

      // 检查ISBN是否已存在
      if (booksStore.some(b => b.isbn === book.isbn)) {
        failCount++
        errors.push(`第${index + 1}条: ISBN ${book.isbn} 已存在`)
        return
      }

      // 添加图书
      const newBook = {
        bookId: nextId++,
        isbn: book.isbn,
        title: book.title,
        author: book.author,
        publisher: book.publisher || '',
        publishDate: book.publishDate || new Date().toISOString().split('T')[0],
        category: book.category || '',
        categoryName: book.categoryName || '',
        price: book.price || 0,
        coverUrl: book.coverUrl || '',
        description: book.description || '',
        totalCopies: book.totalCopies || 1,
        availableCopies: book.availableCopies || 1,
        borrowCount: 0,
        rating: 0,
        status: book.status || 'available',
        location: book.location || '',
        callNumber: book.callNumber || '',
        createdTime: new Date().toISOString(),
        updatedTime: new Date().toISOString()
      }

      booksStore.push(newBook)
      successCount++
    })

    return HttpResponse.json(
      successResponse({
        total: books.length,
        successCount,
        failCount,
        errors: errors.slice(0, 10) // 最多返回10条错误信息
      })
    )
  })
]
