import { Random } from 'mockjs'

export default [
  // 获取统计数据
  {
    url: '/api/dashboard/stats',
    method: 'get',
    response: () => {
      return {
        code: 200,
        message: '成功',
        data: {
          totalBooks: Random.integer(10000, 50000),
          totalReaders: Random.integer(1000, 5000),
          todayBorrow: Random.integer(50, 200),
          todayReturn: Random.integer(40, 180),
          availableBooks: Random.integer(8000, 40000),
          borrowedBooks: Random.integer(2000, 10000),
          overdueBooks: Random.integer(10, 100),
          reservedBooks: Random.integer(50, 300)
        }
      }
    }
  },

  // 获取借阅趋势数据
  {
    url: '/api/dashboard/borrowTrend',
    method: 'get',
    response: () => {
      const dates = []
      const borrowData = []
      const returnData = []

      for (let i = 6; i >= 0; i--) {
        const date = new Date()
        date.setDate(date.getDate() - i)
        dates.push(`${date.getMonth() + 1}/${date.getDate()}`)
        borrowData.push(Random.integer(50, 150))
        returnData.push(Random.integer(40, 140))
      }

      return {
        code: 200,
        message: '成功',
        data: {
          dates,
          borrowData,
          returnData
        }
      }
    }
  },

  // 获取图书分类统计
  {
    url: '/api/dashboard/categoryStats',
    method: 'get',
    response: () => {
      const categories = [
        { name: '文学', value: Random.integer(3000, 8000) },
        { name: '历史', value: Random.integer(2000, 5000) },
        { name: '哲学', value: Random.integer(1000, 3000) },
        { name: '艺术', value: Random.integer(1500, 4000) },
        { name: '科学', value: Random.integer(2500, 6000) },
        { name: '技术', value: Random.integer(3000, 7000) },
        { name: '教育', value: Random.integer(2000, 5000) },
        { name: '其他', value: Random.integer(1000, 3000) }
      ]

      return {
        code: 200,
        message: '成功',
        data: categories
      }
    }
  },

  // 获取热门图书
  {
    url: '/api/dashboard/popularBooks',
    method: 'get',
    response: () => {
      const books = []
      for (let i = 0; i < 10; i++) {
        books.push({
          id: Random.id(),
          title: Random.ctitle(5, 20),
          author: Random.cname(),
          borrowCount: Random.integer(50, 500)
        })
      }

      return {
        code: 200,
        message: '成功',
        data: books
      }
    }
  },

  // 获取最新借阅记录
  {
    url: '/api/dashboard/recentBorrows',
    method: 'get',
    response: () => {
      const records = []
      for (let i = 0; i < 10; i++) {
        records.push({
          id: Random.id(),
          readerName: Random.cname(),
          bookTitle: Random.ctitle(5, 20),
          borrowDate: Random.datetime('yyyy-MM-dd HH:mm:ss'),
          status: Random.pick(['已借出', '已归还', '逾期'])
        })
      }

      return {
        code: 200,
        message: '成功',
        data: records
      }
    }
  }
]
