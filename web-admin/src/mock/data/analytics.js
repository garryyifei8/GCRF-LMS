import { faker } from '@faker-js/faker/locale/zh_CN'
import dayjs from 'dayjs'

// 生成借阅概览统计数据
export function generateOverview() {
  return {
    // 馆情数据
    totalBooks: 10000,
    totalCopies: 25000,
    totalReaders: 5000,
    totalVisits: 125000,
    booksPerReader: 5.0,
    visitsPerReader: 25.0,

    // 当前借阅情况
    currentBorrowed: 2500,
    availableCopies: 22500,
    overdueCount: 350,
    reservationCount: 180,

    // 今日数据
    todayVisits: 320,
    todayBorrowed: 85,
    todayReturned: 92,
    todayNewReaders: 5,

    // 本月数据
    thisMonthBorrowed: 1800,
    thisMonthReturned: 1650,
    thisMonthVisits: 8500,
    thisMonthNewBooks: 120,

    // 流通率相关
    circulationRate: 0.65,
    zeroCirculationCount: 800,
    zeroCirculationRate: 0.08,

    // 同比增长
    borrowGrowth: 0.15,
    visitsGrowth: 0.12,
    readerGrowth: 0.08
  }
}

// 生成借阅趋势数据
export function generateBorrowTrends(timeRange = 'LAST_30_DAYS', granularity = 'DAILY') {
  const trends = []
  const days = timeRange === 'LAST_7_DAYS' ? 7 :
                timeRange === 'LAST_30_DAYS' ? 30 :
                timeRange === 'THIS_MONTH' ? 30 :
                timeRange === 'THIS_YEAR' ? 365 : 30

  for (let i = days - 1; i >= 0; i--) {
    const date = dayjs().subtract(i, 'day')
    const isWeekend = date.day() === 0 || date.day() === 6

    trends.push({
      date: date.format('YYYY-MM-DD'),
      borrowed: faker.number.int({ min: isWeekend ? 30 : 60, max: isWeekend ? 80 : 150 }),
      returned: faker.number.int({ min: isWeekend ? 25 : 55, max: isWeekend ? 75 : 145 }),
      visits: faker.number.int({ min: isWeekend ? 100 : 200, max: isWeekend ? 300 : 500 }),
      newReaders: faker.number.int({ min: 0, max: isWeekend ? 3 : 8 })
    })
  }

  return trends
}

// 生成分类统计数据
export function generateCategoryStats() {
  const categories = [
    { code: 'I', name: '文学', color: '#5470c6' },
    { code: 'TP', name: '工业技术', color: '#91cc75' },
    { code: 'K', name: '历史、地理', color: '#fac858' },
    { code: 'F', name: '经济', color: '#ee6666' },
    { code: 'B', name: '哲学、宗教', color: '#73c0de' },
    { code: 'J', name: '艺术', color: '#3ba272' },
    { code: 'H', name: '语言、文字', color: '#fc8452' },
    { code: 'G', name: '文化、科学、教育、体育', color: '#9a60b4' },
    { code: 'O', name: '数理科学和化学', color: '#ea7ccc' },
    { code: 'D', name: '政治、法律', color: '#5470c6' }
  ]

  return categories.map(cat => ({
    ...cat,
    bookCount: faker.number.int({ min: 500, max: 2000 }),
    borrowCount: faker.number.int({ min: 200, max: 1500 }),
    circulationRate: faker.number.float({ min: 0.3, max: 0.9, fractionDigits: 2 }),
    readerCount: faker.number.int({ min: 100, max: 800 })
  }))
}

// 生成图书排行榜
export function generateBookRankings(params = {}) {
  const { rankBy = 'BORROW_COUNT', limit = 20 } = params
  const rankings = []

  const bookTitles = [
    '三体', '活着', '百年孤独', 'Python编程:从入门到实践', '算法导论',
    '深入理解计算机系统', '人类简史', '未来简史', '原则', '自控力',
    '围城', '平凡的世界', '红楼梦', '西游记', '三国演义',
    '水浒传', '追风筝的人', '解忧杂货店', '白夜行', '嫌疑人X的献身'
  ]

  for (let i = 0; i < Math.min(limit, bookTitles.length); i++) {
    rankings.push({
      rank: i + 1,
      bookId: i + 1,
      isbn: `978${faker.string.numeric(10)}`,
      title: bookTitles[i],
      author: faker.person.fullName(),
      category: faker.helpers.arrayElement(['I', 'TP', 'K', 'F', 'B']),
      coverUrl: `https://picsum.photos/seed/${i}/100/150`,
      borrowCount: faker.number.int({ min: 100 - i * 3, max: 500 - i * 10 }),
      rating: faker.number.float({ min: 3.5, max: 5.0, fractionDigits: 1 }),
      totalCopies: faker.number.int({ min: 3, max: 10 }),
      availableCopies: faker.number.int({ min: 0, max: 5 })
    })
  }

  return rankings
}

// 生成读者排行榜
export function generateReaderRankings(params = {}) {
  const { limit = 20 } = params
  const rankings = []

  for (let i = 0; i < limit; i++) {
    rankings.push({
      rank: i + 1,
      readerId: faker.number.int({ min: 1, max: 200 }),
      cardNo: `RD${String(faker.number.int({ min: 1, max: 200 })).padStart(8, '0')}`,
      realName: faker.person.fullName(),
      readerType: faker.helpers.arrayElement(['student', 'teacher', 'staff']),
      readerTypeName: faker.helpers.arrayElement(['学生', '教师', '职工']),
      avatar: `https://i.pravatar.cc/100?img=${i + 1}`,
      borrowCount: faker.number.int({ min: 50 - i * 2, max: 200 - i * 5 }),
      visitCount: faker.number.int({ min: 100 - i * 3, max: 500 - i * 15 }),
      favoriteCategory: faker.helpers.arrayElement(['文学', '工业技术', '历史、地理', '经济']),
      lastBorrowDate: faker.date.recent({ days: 7 }).toISOString()
    })
  }

  return rankings
}

// 生成馆藏资源分析
export function generateCollectionAnalysis() {
  return {
    totalBooks: 10000,
    totalCopies: 25000,
    categoryDistribution: generateCategoryStats(),
    statusDistribution: [
      { status: 'available', statusName: '在架', count: 22500, percentage: 0.9 },
      { status: 'borrowed', statusName: '借出', count: 2500, percentage: 0.1 },
      { status: 'damaged', statusName: '破损', count: 150, percentage: 0.006 },
      { status: 'lost', statusName: '遗失', count: 80, percentage: 0.003 }
    ],
    ageDistribution: [
      { range: '0-1年', count: 1200, percentage: 0.12 },
      { range: '1-3年', count: 2500, percentage: 0.25 },
      { range: '3-5年', count: 3000, percentage: 0.30 },
      { range: '5-10年', count: 2300, percentage: 0.23 },
      { range: '10年以上', count: 1000, percentage: 0.10 }
    ],
    circulationAnalysis: {
      highCirculation: 3500,    // 借阅次数 > 50
      mediumCirculation: 4500,  // 10 < 借阅次数 <= 50
      lowCirculation: 1200,     // 0 < 借阅次数 <= 10
      zeroCirculation: 800      // 借阅次数 = 0
    }
  }
}

// 生成最近活动记录
export function generateRecentActivities(limit = 50) {
  const activities = []
  const activityTypes = [
    { type: 'borrow', label: '借书', icon: 'DocumentAdd' },
    { type: 'return', label: '还书', icon: 'DocumentChecked' },
    { type: 'renew', label: '续借', icon: 'Refresh' },
    { type: 'reserve', label: '预约', icon: 'Clock' },
    { type: 'register', label: '注册', icon: 'UserFilled' }
  ]

  for (let i = 0; i < limit; i++) {
    const activity = faker.helpers.arrayElement(activityTypes)
    activities.push({
      id: i + 1,
      type: activity.type,
      typeName: activity.label,
      icon: activity.icon,
      readerName: faker.person.fullName(),
      bookTitle: activity.type === 'register' ? null : `图书${faker.number.int({ min: 1, max: 100 })}`,
      description: activity.type === 'register'
        ? `新读者注册`
        : `${activity.label}《图书${faker.number.int({ min: 1, max: 100 })}》`,
      timestamp: faker.date.recent({ days: 1 }).toISOString(),
      status: 'success'
    })
  }

  return activities.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
}

// 生成年级借阅偏好数据
export function generateGradeBorrowPreferences() {
  const grades = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级']
  const categories = ['文学', '科技', '历史', '艺术', '其他']

  return grades.map(grade => ({
    grade,
    preferences: categories.map(category => ({
      category,
      count: faker.number.int({ min: 50, max: 500 }),
      percentage: faker.number.float({ min: 0.1, max: 0.4, fractionDigits: 2 })
    }))
  }))
}

// 生成年度流通统计
export function generateYearlyCirculation() {
  const months = Array.from({ length: 12 }, (_, i) => i + 1)

  return months.map(month => ({
    month: `${month}月`,
    borrowed: faker.number.int({ min: 1000, max: 2500 }),
    returned: faker.number.int({ min: 900, max: 2400 }),
    reserved: faker.number.int({ min: 100, max: 500 }),
    renewed: faker.number.int({ min: 50, max: 300 })
  }))
}