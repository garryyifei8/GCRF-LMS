import { http, HttpResponse } from 'msw'

// 模拟读者数据
const readers = [
  { id: 1, name: '张三', type: 'STUDENT' },
  { id: 2, name: '李四', type: 'STUDENT' },
  { id: 3, name: '王五', type: 'TEACHER' },
  { id: 4, name: '赵六', type: 'STUDENT' },
  { id: 5, name: '钱七', type: 'TEACHER' },
  { id: 6, name: '孙八', type: 'STAFF' },
  { id: 7, name: '周九', type: 'STUDENT' },
  { id: 8, name: '吴十', type: 'EXTERNAL' }
]

// 模拟图书数据
const books = [
  {
    id: 1,
    title: 'Python编程从入门到实践',
    author: 'Eric Matthes',
    isbn: '9787115428028',
    category: 'TP311'
  },
  { id: 2, title: '深度学习', author: 'Ian Goodfellow', isbn: '9787115480149', category: 'TP181' },
  {
    id: 3,
    title: '机器学习实战',
    author: 'Peter Harrington',
    isbn: '9787115317957',
    category: 'TP181'
  },
  { id: 4, title: '统计学习方法', author: '李航', isbn: '9787302517276', category: 'TP181' },
  { id: 5, title: '数据结构与算法', author: '严蔚敏', isbn: '9787302023685', category: 'TP311' },
  { id: 6, title: '计算机网络', author: '谢希仁', isbn: '9787121302954', category: 'TP393' },
  {
    id: 7,
    title: '操作系统概念',
    author: 'Abraham Silberschatz',
    isbn: '9787111599531',
    category: 'TP316'
  },
  { id: 8, title: '人工智能基础', author: '周志华', isbn: '9787302482020', category: 'TP18' },
  { id: 10, title: '红楼梦', author: '曹雪芹', isbn: '9787020002207', category: 'I242' },
  { id: 11, title: '西游记', author: '吴承恩', isbn: '9787020008735', category: 'I242' },
  { id: 12, title: '三国演义', author: '罗贯中', isbn: '9787020008728', category: 'I242' },
  { id: 13, title: '水浒传', author: '施耐庵', isbn: '9787020008711', category: 'I242' },
  { id: 14, title: '围城', author: '钱钟书', isbn: '9787020024759', category: 'I247' },
  { id: 15, title: '经济学原理', author: '曼昆', isbn: '9787301150894', category: 'F0' },
  { id: 16, title: '管理学', author: '罗宾斯', isbn: '9787300108988', category: 'C93' },
  { id: 17, title: '金融学', author: '博迪', isbn: '9787300108995', category: 'F830' },
  { id: 18, title: '数据分析与决策', author: '周明', isbn: '9787302523178', category: 'TP311' },
  { id: 19, title: '人工智能导论', author: '王万森', isbn: '9787302523185', category: 'TP18' },
  { id: 20, title: '自然语言处理', author: '宗成庆', isbn: '9787302523192', category: 'TP391' }
]

// 推荐理由模板
const reasons = {
  USER_CF: [
    '与你阅读口味相似的读者都借阅了此书',
    '基于相似读者的借阅历史推荐',
    '与你阅读偏好相近的读者推荐'
  ],
  ITEM_CF: ['与你借阅过的图书相似', '借阅过此书的读者还借阅了这些书', '与你最近借阅的图书内容相关'],
  POPULAR: ['近30天内热门借阅', '本月最受欢迎的图书', '图书馆热门推荐'],
  HYBRID: ['综合多种推荐策略为你精选', '基于你的阅读历史智能推荐', '个性化精准推荐']
}

// 生成推荐结果
function generateRecommendations(readerId, limit, algorithm, scene) {
  const reader = readers.find((r) => r.id === parseInt(readerId)) || readers[0]
  const recommendations = []
  const usedBookIds = new Set()

  for (let i = 0; i < limit; i++) {
    let book
    do {
      book = books[Math.floor(Math.random() * books.length)]
    } while (usedBookIds.has(book.id))
    usedBookIds.add(book.id)

    const score = Math.random() * 0.3 + 0.7 // 0.7-1.0
    const algorithmType =
      algorithm === 'HYBRID'
        ? ['USER_CF', 'ITEM_CF', 'POPULAR'][Math.floor(Math.random() * 3)]
        : algorithm
    const reasonList = reasons[algorithmType] || reasons.HYBRID

    recommendations.push({
      readerId: reader.id,
      readerName: reader.name,
      bookId: book.id,
      bookTitle: book.title,
      author: book.author,
      isbn: book.isbn,
      categoryCode: book.category,
      score: score,
      algorithm: algorithm,
      scene: scene,
      reason: reasonList[Math.floor(Math.random() * reasonList.length)],
      readerPreferences: ['计算机', '人工智能', '编程', '算法'].slice(
        0,
        Math.floor(Math.random() * 3) + 2
      ),
      bookTags: ['技术', '编程', 'AI', '入门'].slice(0, Math.floor(Math.random() * 3) + 2)
    })
  }

  // 按分数降序排序
  return recommendations.sort((a, b) => b.score - a.score)
}

// 生成统计数据
function generateStats() {
  return {
    precision: parseFloat((Math.random() * 20 + 70).toFixed(1)), // 70-90
    ctr: parseFloat((Math.random() * 10 + 8).toFixed(1)), // 8-18
    conversion: parseFloat((Math.random() * 8 + 5).toFixed(1)), // 5-13
    totalRecommendations: Math.floor(Math.random() * 5000) + 10000,
    clickedCount: Math.floor(Math.random() * 1000) + 500,
    borrowedCount: Math.floor(Math.random() * 500) + 200,
    algorithmStats: [
      {
        algorithm: 'USER_CF',
        total: Math.floor(Math.random() * 2000) + 3000,
        clicked: Math.floor(Math.random() * 400) + 200,
        borrowed: Math.floor(Math.random() * 200) + 100,
        precision: parseFloat((Math.random() * 15 + 75).toFixed(1))
      },
      {
        algorithm: 'ITEM_CF',
        total: Math.floor(Math.random() * 2000) + 3000,
        clicked: Math.floor(Math.random() * 400) + 200,
        borrowed: Math.floor(Math.random() * 200) + 100,
        precision: parseFloat((Math.random() * 15 + 72).toFixed(1))
      },
      {
        algorithm: 'POPULAR',
        total: Math.floor(Math.random() * 2000) + 2000,
        clicked: Math.floor(Math.random() * 300) + 150,
        borrowed: Math.floor(Math.random() * 150) + 80,
        precision: parseFloat((Math.random() * 15 + 65).toFixed(1))
      },
      {
        algorithm: 'HYBRID',
        total: Math.floor(Math.random() * 3000) + 4000,
        clicked: Math.floor(Math.random() * 500) + 300,
        borrowed: Math.floor(Math.random() * 300) + 150,
        precision: parseFloat((Math.random() * 15 + 78).toFixed(1))
      }
    ]
  }
}

export const recommendHandlers = [
  // 获取读者个性化推荐
  http.get('/api/v1/recommend/books/:readerId', ({ params, request }) => {
    const { readerId } = params
    const url = new URL(request.url)
    const limit = parseInt(url.searchParams.get('limit') || '20')
    const algorithm = url.searchParams.get('algorithm') || 'HYBRID'
    const scene = url.searchParams.get('scene') || 'HOMEPAGE'

    const recommendations = generateRecommendations(readerId, limit, algorithm, scene)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: recommendations
    })
  }),

  // 获取热门图书
  http.get('/api/v1/recommend/popular', ({ request }) => {
    const url = new URL(request.url)
    const limit = parseInt(url.searchParams.get('limit') || '20')

    const recommendations = books.slice(0, Math.min(limit, books.length)).map((book, index) => ({
      bookId: book.id,
      bookTitle: book.title,
      author: book.author,
      isbn: book.isbn,
      categoryCode: book.category,
      score: parseFloat(((books.length - index) / books.length).toFixed(3)),
      algorithm: 'POPULAR',
      scene: 'HOMEPAGE',
      reason: `近30天内有${Math.floor(Math.random() * 50) + 20}位读者借阅`
    }))

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: recommendations
    })
  }),

  // 获取相似图书
  http.get('/api/v1/recommend/similar/:bookId', ({ params, request }) => {
    const { bookId } = params
    const url = new URL(request.url)
    const limit = parseInt(url.searchParams.get('limit') || '10')

    const targetBook = books.find((b) => b.id === parseInt(bookId))
    const similarBooks = books
      .filter((b) => b.id !== parseInt(bookId))
      .filter((b) =>
        targetBook ? b.category === targetBook.category || Math.random() > 0.5 : true
      )
      .slice(0, limit)
      .map((book, index) => ({
        bookId: book.id,
        bookTitle: book.title,
        author: book.author,
        isbn: book.isbn,
        categoryCode: book.category,
        score: parseFloat((0.9 - index * 0.05).toFixed(3)),
        algorithm: 'ITEM_CF',
        scene: 'DETAIL',
        reason: '借阅过此书的读者还借阅了这些书'
      }))

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: similarBooks
    })
  }),

  // 批量生成推荐
  http.post('/api/v1/recommend/batch', async ({ request }) => {
    const data = await request.json()
    const {
      readerType = 'all',
      algorithm = 'HYBRID',
      countPerReader = 10,
      scene = 'HOMEPAGE',
      pageNum = 1,
      pageSize = 20
    } = data

    // 过滤读者
    let filteredReaders =
      readerType === 'all' ? readers : readers.filter((r) => r.type === readerType.toUpperCase())

    // 为每个读者生成推荐
    const allRecommendations = []
    filteredReaders.forEach((reader) => {
      const recs = generateRecommendations(reader.id, countPerReader, algorithm, scene)
      allRecommendations.push(...recs)
    })

    // 分页
    const total = allRecommendations.length
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    const records = allRecommendations.slice(start, end)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        records,
        total,
        pageNum,
        pageSize,
        pages: Math.ceil(total / pageSize)
      }
    })
  }),

  // 获取推荐效果统计
  http.get('/api/v1/recommend/stats', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: generateStats()
    })
  }),

  // 记录推荐点击
  http.post('/api/v1/recommend/click', () => {
    return HttpResponse.json({
      code: 200,
      message: '点击记录成功'
    })
  }),

  // 记录推荐借阅
  http.post('/api/v1/recommend/borrow', () => {
    return HttpResponse.json({
      code: 200,
      message: '借阅记录成功'
    })
  }),

  // 触发相似度矩阵重新计算
  http.post('/api/v1/recommend/recompute', () => {
    return HttpResponse.json({
      code: 200,
      message: '相似度矩阵重新计算已触发'
    })
  })
]
