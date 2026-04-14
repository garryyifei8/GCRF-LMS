import { http, HttpResponse } from 'msw'
import { generateBooks, generateBookStats, categories } from '../data/books'

// 生成并缓存图书数据
let booksData = generateBooks(100)

// 条码序列号
let barcodeSequence = 1000

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
      filteredBooks = filteredBooks.filter(
        (book) =>
          book.title.includes(keyword) ||
          book.author.includes(keyword) ||
          book.isbn.includes(keyword)
      )
    }

    if (category) {
      filteredBooks = filteredBooks.filter((book) => book.category === category)
    }

    if (status) {
      filteredBooks = filteredBooks.filter((book) => book.status === status)
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

    // 排除特殊路由
    if (id === 'categories' || id === 'stats' || id === 'health' || id === 'import-template') {
      return
    }

    const book = booksData.find((b) => b.id === parseInt(id))

    if (!book) {
      return HttpResponse.json(
        {
          code: 404,
          message: '图书不存在'
        },
        { status: 404 }
      )
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
      barcode: `GCRF-${new Date().getFullYear()}-${String(++barcodeSequence).padStart(8, '0')}`,
      ...data,
      totalCopies: data.totalQuantity || 1,
      availableCopies: data.totalQuantity || 1,
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
    const index = booksData.findIndex((b) => b.id === parseInt(id))

    if (index === -1) {
      return HttpResponse.json(
        {
          code: 404,
          message: '图书不存在'
        },
        { status: 404 }
      )
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
    const index = booksData.findIndex((b) => b.id === parseInt(id))

    if (index === -1) {
      return HttpResponse.json(
        {
          code: 404,
          message: '图书不存在'
        },
        { status: 404 }
      )
    }

    booksData.splice(index, 1)

    return HttpResponse.json({
      code: 200,
      message: '图书删除成功'
    })
  }),

  // 批量删除图书 (新API - POST /api/v1/books/batch-delete)
  http.post('/api/v1/books/batch-delete', async ({ request }) => {
    const { ids } = await request.json()

    const errors = []
    let successCount = 0

    ids.forEach((id) => {
      const book = booksData.find((b) => b.id === id)
      if (!book) {
        errors.push({ rowNum: null, bookId: id, reason: '图书不存在', isbn: null })
      } else if (book.availableCopies < book.totalCopies) {
        errors.push({
          rowNum: null,
          bookId: id,
          reason: '该图书有在借记录，无法删除',
          isbn: book.isbn
        })
      } else {
        booksData = booksData.filter((b) => b.id !== id)
        successCount++
      }
    })

    return HttpResponse.json({
      code: 200,
      message: `成功删除${successCount}本图书`,
      data: {
        totalCount: ids.length,
        successCount,
        failedCount: errors.length,
        errors
      }
    })
  }),

  // 批量导入图书 (新API - POST /api/v1/books/batch-import)
  http.post('/api/v1/books/batch-import', async ({ request }) => {
    // 模拟处理上传的Excel文件
    const formData = await request.formData()
    const file = formData.get('file')

    // 模拟导入结果
    const importedCount = 5
    const errors = []

    for (let i = 0; i < importedCount; i++) {
      const newBook = {
        id: booksData.length + i + 1,
        barcode: `GCRF-${new Date().getFullYear()}-${String(++barcodeSequence).padStart(8, '0')}`,
        isbn: `978711${String(Math.floor(Math.random() * 1000000)).padStart(7, '0')}`,
        title: `导入图书${i + 1}`,
        author: '导入作者',
        publisher: '导入出版社',
        totalCopies: 1,
        availableCopies: 1,
        borrowCount: 0,
        status: 'available',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
      booksData.push(newBook)
    }

    return HttpResponse.json({
      code: 200,
      message: `成功导入${importedCount}本图书`,
      data: {
        totalCount: importedCount,
        successCount: importedCount,
        failedCount: 0,
        errors
      }
    })
  }),

  // 下载导入模板 (新API - GET /api/v1/books/import-template)
  http.get('/api/v1/books/import-template', () => {
    // 返回模拟的Excel文件下载
    return new HttpResponse(
      new Blob(['模拟Excel模板'], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      }),
      {
        headers: {
          'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          'Content-Disposition': 'attachment; filename="book-import-template.xlsx"'
        }
      }
    )
  }),

  // ISBN查询 (新API - GET /api/v1/books/isbn/:isbn)
  http.get('/api/v1/books/isbn/:isbn', ({ params }) => {
    const { isbn } = params

    // 模拟从第三方API获取图书信息
    const mockIsbnData = {
      found: true,
      isbn: isbn,
      title: `ISBN ${isbn} 对应的图书`,
      author: '作者姓名',
      publisher: '出版社名称',
      publishDate: '2024-01-01',
      pages: 300,
      price: 59.0,
      description: '这是通过ISBN查询到的图书简介...',
      coverUrl: `https://picsum.photos/seed/${isbn}/200/300`,
      language: 'zh-CN',
      source: 'Open Library'
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: mockIsbnData
    })
  }),

  // 批量生成条码 (新API - POST /api/v1/books/barcode/generate)
  http.post('/api/v1/books/barcode/generate', async ({ request }) => {
    const { bookIds, prefix } = await request.json()
    const barcodePrefix = prefix || 'GCRF'
    const results = []

    bookIds.forEach((bookId) => {
      const book = booksData.find((b) => b.id === bookId)
      if (book) {
        if (!book.barcode) {
          book.barcode = `${barcodePrefix}-${new Date().getFullYear()}-${String(++barcodeSequence).padStart(8, '0')}`
        }
        results.push({
          bookId: book.id,
          isbn: book.isbn,
          title: book.title,
          barcode: book.barcode,
          generatedAt: new Date().toISOString()
        })
      }
    })

    return HttpResponse.json({
      code: 200,
      message: `成功生成${results.length}个条码`,
      data: results
    })
  }),

  // 条码查询 (新API - GET /api/v1/books/barcode/:barcode)
  http.get('/api/v1/books/barcode/:barcode', ({ params }) => {
    const { barcode } = params
    const book = booksData.find((b) => b.barcode === barcode)

    if (!book) {
      return HttpResponse.json(
        {
          code: 404,
          message: '未找到该条码对应的图书'
        },
        { status: 404 }
      )
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: book
    })
  }),

  // 获取库存信息 (新API - GET /api/v1/books/:id/inventory)
  http.get('/api/v1/books/:id/inventory', ({ params }) => {
    const { id } = params
    const book = booksData.find((b) => b.id === parseInt(id))

    if (!book) {
      return HttpResponse.json(
        {
          code: 404,
          message: '图书不存在'
        },
        { status: 404 }
      )
    }

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        bookId: book.id,
        isbn: book.isbn,
        title: book.title,
        totalCopies: book.totalCopies || 0,
        availableCopies: book.availableCopies || 0,
        borrowedCopies: (book.totalCopies || 0) - (book.availableCopies || 0),
        reservedCopies: 0,
        lastInventoryCheck: new Date().toISOString()
      }
    })
  }),

  // 更新库存 (新API - PUT /api/v1/books/:id/inventory)
  http.put('/api/v1/books/:id/inventory', async ({ params, request }) => {
    const { id } = params
    const { totalCopies, reason } = await request.json()
    const book = booksData.find((b) => b.id === parseInt(id))

    if (!book) {
      return HttpResponse.json(
        {
          code: 404,
          message: '图书不存在'
        },
        { status: 404 }
      )
    }

    const borrowedCount = (book.totalCopies || 0) - (book.availableCopies || 0)
    if (totalCopies < borrowedCount) {
      return HttpResponse.json(
        {
          code: 400,
          message: `新的总量(${totalCopies})不能小于已借出数量(${borrowedCount})`
        },
        { status: 400 }
      )
    }

    book.totalCopies = totalCopies
    book.availableCopies = totalCopies - borrowedCount
    book.updatedAt = new Date().toISOString()

    return HttpResponse.json({
      code: 200,
      message: '库存更新成功',
      data: {
        bookId: book.id,
        isbn: book.isbn,
        title: book.title,
        totalCopies: book.totalCopies,
        availableCopies: book.availableCopies,
        borrowedCopies: borrowedCount,
        reservedCopies: 0,
        lastInventoryCheck: new Date().toISOString()
      }
    })
  }),

  // 全文搜索 (新API - POST /api/v1/books/search)
  http.post('/api/v1/books/search', async ({ request }) => {
    const {
      query,
      pageNum = 1,
      pageSize = 10,
      categoryId,
      publisher,
      language,
      availableOnly
    } = await request.json()

    let filteredBooks = booksData

    if (query) {
      const lowerQuery = query.toLowerCase()
      filteredBooks = filteredBooks.filter(
        (book) =>
          book.title?.toLowerCase().includes(lowerQuery) ||
          book.author?.toLowerCase().includes(lowerQuery) ||
          book.isbn?.includes(query) ||
          book.description?.toLowerCase().includes(lowerQuery)
      )
    }

    if (categoryId) {
      filteredBooks = filteredBooks.filter((book) => book.category === String(categoryId))
    }

    if (publisher) {
      filteredBooks = filteredBooks.filter((book) => book.publisher?.includes(publisher))
    }

    if (availableOnly) {
      filteredBooks = filteredBooks.filter((book) => book.availableCopies > 0)
    }

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

  // 获取图书分类
  http.get('/api/v1/books/categories', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: categories
    })
  }),

  // 获取图书统计数据
  http.get('/api/v1/books/stats', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateBookStats()
    })
  }),

  // 健康检查
  http.get('/api/v1/books/health', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: 'Book Service is running'
    })
  })
]
