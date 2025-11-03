import { Random } from 'mockjs'

// 生成模拟图书数据
const generateBooks = (count = 50) => {
  const books = []
  const categories = ['文学', '历史', '哲学', '艺术', '科学', '技术', '教育', '经济', '医学', '法律']
  const publishers = ['人民出版社', '机械工业出版社', '清华大学出版社', '电子工业出版社', '中信出版社']
  const statuses = [0, 1] // 0-在库, 1-借出

  // Mock封面图片URL列表（使用不同风格的占位图）
  const mockCovers = [
    'https://picsum.photos/seed/book1/200/280',
    'https://picsum.photos/seed/book2/200/280',
    'https://picsum.photos/seed/book3/200/280',
    'https://picsum.photos/seed/book4/200/280',
    'https://picsum.photos/seed/book5/200/280',
    'https://picsum.photos/seed/book6/200/280',
    'https://picsum.photos/seed/book7/200/280',
    'https://picsum.photos/seed/book8/200/280',
    'https://picsum.photos/seed/book9/200/280',
    'https://picsum.photos/seed/book10/200/280'
  ]

  for (let i = 1; i <= count; i++) {
    const totalCopies = Random.integer(1, 20)
    const borrowedCopies = Random.integer(0, totalCopies)

    books.push({
      id: i,
      isbn: Random.string('number', 13),
      title: Random.ctitle(5, 25),
      author: Random.cname(),
      publisher: Random.pick(publishers),
      publishDate: Random.date('yyyy-MM-dd'),
      category: Random.pick(categories),
      price: Random.float(20, 200, 2, 2),
      totalCopies: totalCopies,
      availableCopies: totalCopies - borrowedCopies,
      borrowCount: Random.integer(0, 500),
      status: borrowedCopies < totalCopies ? 0 : 1,
      // 90%的图书有封面，10%没有封面（用于测试默认封面）
      coverUrl: i % 10 === 0 ? '' : mockCovers[i % mockCovers.length],
      cover: i % 10 === 0 ? '' : mockCovers[i % mockCovers.length], // 添加cover字段兼容性
      summary: Random.cparagraph(2, 4),
      createdAt: Random.datetime('yyyy-MM-dd HH:mm:ss'),
      updatedAt: Random.datetime('yyyy-MM-dd HH:mm:ss')
    })
  }

  return books
}

let books = generateBooks(50)

export default [
  // 获取图书列表
  {
    url: '/api/books',
    method: 'get',
    response: ({ query }) => {
      const { page = 1, pageSize = 10, title, author, category, isbn } = query

      // 过滤
      let filteredBooks = [...books]
      if (title) {
        filteredBooks = filteredBooks.filter(book => book.title.includes(title))
      }
      if (author) {
        filteredBooks = filteredBooks.filter(book => book.author.includes(author))
      }
      if (category) {
        filteredBooks = filteredBooks.filter(book => book.category === category)
      }
      if (isbn) {
        filteredBooks = filteredBooks.filter(book => book.isbn.includes(isbn))
      }

      // 分页
      const start = (page - 1) * pageSize
      const end = start + parseInt(pageSize)
      const pageData = filteredBooks.slice(start, end)

      return {
        code: 200,
        message: '成功',
        data: {
          list: pageData,
          total: filteredBooks.length,
          page: parseInt(page),
          pageSize: parseInt(pageSize)
        }
      }
    }
  },

  // 获取单个图书详情
  {
    url: '/api/books/:id',
    method: 'get',
    response: ({ query }) => {
      const { id } = query
      const book = books.find(b => b.id === parseInt(id))

      if (book) {
        return {
          code: 200,
          message: '成功',
          data: book
        }
      } else {
        return {
          code: 404,
          message: '图书不存在',
          data: null
        }
      }
    }
  },

  // 新增图书
  {
    url: '/api/books',
    method: 'post',
    response: ({ body }) => {
      const newBook = {
        id: books.length + 1,
        ...body,
        borrowCount: 0,
        status: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
      books.push(newBook)

      return {
        code: 200,
        message: '新增成功',
        data: newBook
      }
    }
  },

  // 更新图书
  {
    url: '/api/books/:id',
    method: 'put',
    response: ({ query, body }) => {
      const { id } = query
      const index = books.findIndex(b => b.id === parseInt(id))

      if (index !== -1) {
        books[index] = {
          ...books[index],
          ...body,
          updatedAt: new Date().toISOString()
        }

        return {
          code: 200,
          message: '更新成功',
          data: books[index]
        }
      } else {
        return {
          code: 404,
          message: '图书不存在',
          data: null
        }
      }
    }
  },

  // 删除图书
  {
    url: '/api/books/:id',
    method: 'delete',
    response: ({ query }) => {
      const { id } = query
      const index = books.findIndex(b => b.id === parseInt(id))

      if (index !== -1) {
        books.splice(index, 1)
        return {
          code: 200,
          message: '删除成功',
          data: null
        }
      } else {
        return {
          code: 404,
          message: '图书不存在',
          data: null
        }
      }
    }
  },

  // 批量删除图书
  {
    url: '/api/books/batch',
    method: 'delete',
    response: ({ body }) => {
      const { ids } = body
      books = books.filter(book => !ids.includes(book.id))

      return {
        code: 200,
        message: `成功删除 ${ids.length} 本图书`,
        data: null
      }
    }
  }
]
