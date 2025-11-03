import { http, HttpResponse } from 'msw'
import { generateBooks, generateBookStats, categories } from '../data/books'

// 生成并缓存图书数据
let booksData = generateBooks(100)

export const booksHandlers = [
  // 获取图书列表
  http.get('/api/v1/books', ({ request }) => {
    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10')
    const keyword = url.searchParams.get('keyword') || ''
    const category = url.searchParams.get('category') || ''
    const status = url.searchParams.get('status') || ''

    // 过滤数据
    let filteredBooks = booksData

    if (keyword) {
      filteredBooks = filteredBooks.filter(book =>
        book.title.includes(keyword) ||
        book.author.includes(keyword) ||
        book.isbn.includes(keyword)
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
    const books = filteredBooks.slice(start, end)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        records: books,
        total,
        pageNum,
        pageSize,
        pages: Math.ceil(total / pageSize)
      }
    })
  }),

  // 获取图书详情
  http.get('/api/v1/books/:id', ({ params }) => {
    const { id } = params
    const book = booksData.find(b => b.id === parseInt(id))

    if (!book) {
      return HttpResponse.json({
        code: 404,
        message: '图书不存在'
      }, { status: 404 })
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: book
    })
  }),

  // 新增图书
  http.post('/api/v1/books', async ({ request }) => {
    const data = await request.json()
    const newBook = {
      id: booksData.length + 1,
      barcode: `BK${String(booksData.length + 1).padStart(8, '0')}`,
      ...data,
      totalCopies: data.totalCopies || 1,
      availableCopies: data.availableCopies || 1,
      borrowCount: 0,
      status: 'available',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }

    booksData.push(newBook)

    return HttpResponse.json({
      code: 200,
      message: '图书添加成功',
      data: newBook
    })
  }),

  // 更新图书
  http.put('/api/v1/books/:id', async ({ params, request }) => {
    const { id } = params
    const data = await request.json()
    const index = booksData.findIndex(b => b.id === parseInt(id))

    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        message: '图书不存在'
      }, { status: 404 })
    }

    booksData[index] = {
      ...booksData[index],
      ...data,
      updatedAt: new Date().toISOString()
    }

    return HttpResponse.json({
      code: 200,
      message: '图书更新成功',
      data: booksData[index]
    })
  }),

  // 删除图书
  http.delete('/api/v1/books/:id', ({ params }) => {
    const { id } = params
    const index = booksData.findIndex(b => b.id === parseInt(id))

    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        message: '图书不存在'
      }, { status: 404 })
    }

    booksData.splice(index, 1)

    return HttpResponse.json({
      code: 200,
      message: '图书删除成功'
    })
  }),

  // 批量删除图书
  http.delete('/api/v1/books/batch', ({ request }) => {
    const url = new URL(request.url)
    const ids = url.searchParams.get('ids')?.split(',').map(id => parseInt(id)) || []

    booksData = booksData.filter(b => !ids.includes(b.id))

    return HttpResponse.json({
      code: 200,
      message: `成功删除${ids.length}本图书`
    })
  }),

  // 获取图书分类
  http.get('/api/v1/books/categories', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: categories
    })
  }),

  // 导入图书
  http.post('/api/v1/books/import', async ({ request }) => {
    const { books } = await request.json()

    const importedBooks = books.map((book, index) => ({
      id: booksData.length + index + 1,
      barcode: `BK${String(booksData.length + index + 1).padStart(8, '0')}`,
      ...book,
      totalCopies: book.totalCopies || 1,
      availableCopies: book.availableCopies || 1,
      borrowCount: 0,
      status: 'available',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }))

    booksData.push(...importedBooks)

    return HttpResponse.json({
      code: 200,
      message: `成功导入${books.length}本图书`,
      data: {
        successCount: books.length,
        failCount: 0
      }
    })
  }),

  // 获取图书统计数据
  http.get('/api/v1/books/stats', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateBookStats()
    })
  })
]
