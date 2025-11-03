import { http, HttpResponse, delay } from 'msw'
import { successResponse } from '../../data/factory'

const BASE_URL = import.meta.env.VITE_API_BASE_URL
const MOCK_DELAY = parseInt(import.meta.env.VITE_MOCK_DELAY) || 300

/**
 * Analytics API Handlers
 * 数据分析服务 Mock API
 */
export const analyticsHandlers = [
  /**
   * GET /api/v1/analytics/overview
   * 获取借阅概览统计数据
   */
  http.get(`${BASE_URL}/api/v1/analytics/overview`, async () => {
    await delay(MOCK_DELAY)

    return HttpResponse.json(
      successResponse({
        totalBooks: 25678,        // 总馆藏量
        totalCopies: 38517,       // 总副本数
        availableCopies: 35861,   // 可借副本数
        borrowedCopies: 2656,     // 已借出副本数
        totalReaders: 1234,       // 注册读者
        activeReaders: 856,       // 活跃读者
        todayBorrows: 156,        // 今日借阅
        todayReturns: 142,        // 今日归还
        todayVisitors: 489,       // 今日访问量
        overdueCount: 23          // 逾期未还
      })
    )
  }),

  /**
   * GET /api/v1/analytics/borrow-trends
   * 获取借阅趋势数据
   */
  http.get(`${BASE_URL}/api/v1/analytics/borrow-trends`, async ({ request }) => {
    await delay(MOCK_DELAY)

    const url = new URL(request.url)
    const timeRange = url.searchParams.get('timeRange') || 'LAST_30_DAYS'
    const granularity = url.searchParams.get('granularity') || 'DAILY'

    // 生成最近12个月的数据
    const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
    const borrowCounts = [820, 932, 901, 934, 1290, 1330, 1320, 1100, 1200, 1300, 1400, 1500]
    const returnCounts = [780, 900, 880, 920, 1250, 1300, 1280, 1080, 1180, 1280, 1380, 1480]

    return HttpResponse.json(
      successResponse({
        labels: months,
        borrowCounts,
        returnCounts
      })
    )
  }),

  /**
   * GET /api/v1/analytics/category-stats
   * 获取分类统计数据
   */
  http.get(`${BASE_URL}/api/v1/analytics/category-stats`, async () => {
    await delay(MOCK_DELAY)

    return HttpResponse.json(
      successResponse([
        {
          category: 'I',
          categoryName: '文学',
          bookCount: 8234,
          borrowCount: 1048,
          percentage: 0.33
        },
        {
          category: 'TP',
          categoryName: '科技',
          bookCount: 6521,
          borrowCount: 735,
          percentage: 0.24
        },
        {
          category: 'K',
          categoryName: '历史',
          bookCount: 3456,
          borrowCount: 580,
          percentage: 0.18
        },
        {
          category: 'J',
          categoryName: '艺术',
          bookCount: 2987,
          borrowCount: 484,
          percentage: 0.15
        },
        {
          category: 'B',
          categoryName: '哲学',
          bookCount: 1234,
          borrowCount: 300,
          percentage: 0.10
        }
      ])
    )
  }),

  /**
   * GET /api/v1/analytics/book-rankings
   * 获取图书排行榜
   */
  http.get(`${BASE_URL}/api/v1/analytics/book-rankings`, async ({ request }) => {
    await delay(MOCK_DELAY)

    const url = new URL(request.url)
    const rankBy = url.searchParams.get('rankBy') || 'BORROW_COUNT'
    const timeRange = url.searchParams.get('timeRange') || 'THIS_MONTH'
    const limit = parseInt(url.searchParams.get('limit') || '10')

    // 热门图书数据
    const topBooks = [
      { rank: 1, bookId: 1, title: '活着', author: '余华', category: '文学', coverUrl: '', borrowCount: 234, rating: 4.9 },
      { rank: 2, bookId: 2, title: '三体', author: '刘慈欣', category: '科幻', coverUrl: '', borrowCount: 198, rating: 4.8 },
      { rank: 3, bookId: 3, title: '百年孤独', author: '马尔克斯', category: '文学', coverUrl: '', borrowCount: 176, rating: 4.7 },
      { rank: 4, bookId: 4, title: '人类简史', author: '尤瓦尔·赫拉利', category: '历史', coverUrl: '', borrowCount: 165, rating: 4.6 },
      { rank: 5, bookId: 5, title: '围城', author: '钱钟书', category: '文学', coverUrl: '', borrowCount: 152, rating: 4.5 },
      { rank: 6, bookId: 6, title: '小王子', author: '圣埃克苏佩里', category: '文学', coverUrl: '', borrowCount: 143, rating: 4.8 },
      { rank: 7, bookId: 7, title: '白夜行', author: '东野圭吾', category: '推理', coverUrl: '', borrowCount: 132, rating: 4.7 },
      { rank: 8, bookId: 8, title: '解忧杂货店', author: '东野圭吾', category: '文学', coverUrl: '', borrowCount: 128, rating: 4.6 },
      { rank: 9, bookId: 9, title: '追风筝的人', author: '卡勒德·胡赛尼', category: '文学', coverUrl: '', borrowCount: 121, rating: 4.5 },
      { rank: 10, bookId: 10, title: '明朝那些事儿', author: '当年明月', category: '历史', coverUrl: '', borrowCount: 117, rating: 4.4 }
    ]

    return HttpResponse.json(
      successResponse(topBooks.slice(0, limit))
    )
  }),

  /**
   * GET /api/v1/analytics/reader-rankings
   * 获取活跃读者排行榜
   */
  http.get(`${BASE_URL}/api/v1/analytics/reader-rankings`, async ({ request }) => {
    await delay(MOCK_DELAY)

    const url = new URL(request.url)
    const rankBy = url.searchParams.get('rankBy') || 'BORROW_COUNT'
    const timeRange = url.searchParams.get('timeRange') || 'THIS_MONTH'
    const limit = parseInt(url.searchParams.get('limit') || '10')

    // 活跃读者数据
    const topReaders = [
      { rank: 1, readerId: 100, readerNo: 'RD20250001', name: '张明远', type: '学生', department: '计算机学院', grade: '五年级1班', borrowCount: 45 },
      { rank: 2, readerId: 101, readerNo: 'RD20250002', name: '李教授', type: '教师', department: '文学院', grade: '', borrowCount: 38 },
      { rank: 3, readerId: 102, readerNo: 'RD20250003', name: '王小芳', type: '学生', department: '经济管理学院', grade: '四年级2班', borrowCount: 35 },
      { rank: 4, readerId: 103, readerNo: 'RD20250004', name: '陈博士', type: '教师', department: '物理系', grade: '', borrowCount: 32 },
      { rank: 5, readerId: 104, readerNo: 'RD20250005', name: '刘思思', type: '学生', department: '外语学院', grade: '三年级1班', borrowCount: 30 },
      { rank: 6, readerId: 105, readerNo: 'RD20250006', name: '赵强', type: '学生', department: '机械工程学院', grade: '五年级3班', borrowCount: 28 },
      { rank: 7, readerId: 106, readerNo: 'RD20250007', name: '孙教授', type: '教师', department: '数学系', grade: '', borrowCount: 27 },
      { rank: 8, readerId: 107, readerNo: 'RD20250008', name: '周雨', type: '学生', department: '艺术学院', grade: '二年级1班', borrowCount: 25 },
      { rank: 9, readerId: 108, readerNo: 'RD20250009', name: '吴敏', type: '学生', department: '法学院', grade: '四年级1班', borrowCount: 24 },
      { rank: 10, readerId: 109, readerNo: 'RD20250010', name: '郑老师', type: '教师', department: '化学系', grade: '', borrowCount: 22 }
    ]

    return HttpResponse.json(
      successResponse(topReaders.slice(0, limit))
    )
  }),

  /**
   * GET /api/v1/analytics/collection-analysis
   * 获取馆藏资源分析数据
   */
  http.get(`${BASE_URL}/api/v1/analytics/collection-analysis`, async () => {
    await delay(MOCK_DELAY)

    return HttpResponse.json(
      successResponse([
        { category: '文学艺术', total: 8234, available: 2156, circulationRate: 73.8, monthBorrow: 892, status: '正常' },
        { category: '科学技术', total: 6521, available: 1834, circulationRate: 71.9, monthBorrow: 756, status: '正常' },
        { category: '社会科学', total: 5123, available: 892, circulationRate: 82.6, monthBorrow: 623, status: '紧缺' },
        { category: '历史地理', total: 3456, available: 1234, circulationRate: 64.3, monthBorrow: 234, status: '正常' },
        { category: '经济管理', total: 2987, available: 45, circulationRate: 98.5, monthBorrow: 456, status: '紧缺' },
        { category: '哲学宗教', total: 1234, available: 567, circulationRate: 54.1, monthBorrow: 89, status: '充足' }
      ])
    )
  }),

  /**
   * GET /api/v1/analytics/recent-activities
   * 获取最近活动记录
   */
  http.get(`${BASE_URL}/api/v1/analytics/recent-activities`, async ({ request }) => {
    await delay(MOCK_DELAY)

    const url = new URL(request.url)
    const limit = parseInt(url.searchParams.get('limit') || '10')

    const activities = [
      {
        id: 1,
        time: '5分钟前',
        type: 'success',
        title: '新书上架',
        detail: '《算法导论》等12本新书已上架'
      },
      {
        id: 2,
        time: '12分钟前',
        type: 'primary',
        title: '图书归还',
        detail: '读者张三归还《数据结构》'
      },
      {
        id: 3,
        time: '30分钟前',
        type: 'warning',
        title: '逾期提醒',
        detail: '3本图书即将到期,已发送提醒'
      },
      {
        id: 4,
        time: '1小时前',
        type: 'success',
        title: '预约到书',
        detail: '《机器学习》预约图书已到馆'
      },
      {
        id: 5,
        time: '2小时前',
        type: 'primary',
        title: '借阅高峰',
        detail: '上午借阅量达到156本'
      }
    ]

    return HttpResponse.json(
      successResponse(activities.slice(0, limit))
    )
  })
]
