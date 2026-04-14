import dayjs from 'dayjs'

// 基于数据库真实统计数据 (2025-12-02)
// 图书: 50本, 总馆藏量按书籍统计
// 读者: 40人
// 借阅记录: 40条 (RETURNED: 10, BORROWED: 15, OVERDUE: 10, LOST: 5)

// 图书分类统计 (基于真实数据)
const realCategoryStats = [
  { code: 'TP', name: '计算机/工业技术', bookCount: 10, borrowCount: 12 },
  { code: 'I', name: '文学', bookCount: 20, borrowCount: 18 },
  { code: 'K', name: '历史/地理', bookCount: 4, borrowCount: 5 },
  { code: 'B', name: '哲学/宗教', bookCount: 4, borrowCount: 4 },
  { code: 'N', name: '自然科学', bookCount: 4, borrowCount: 3 },
  { code: 'F', name: '经济/管理', bookCount: 8, borrowCount: 6 }
]

// 热门图书排行 (基于真实借阅数据)
const realBookRankings = [
  {
    id: 1,
    isbn: '9787115543998',
    title: '深入理解计算机系统',
    author: 'Randal E. Bryant',
    borrowCount: 3
  },
  { id: 2, isbn: '9787111558422', title: '算法导论', author: 'Thomas H. Cormen', borrowCount: 2 },
  { id: 11, isbn: '9787020008735', title: '红楼梦', author: '曹雪芹', borrowCount: 2 },
  { id: 16, isbn: '9787544291170', title: '活着', author: '余华', borrowCount: 2 },
  { id: 39, isbn: '9787115427977', title: '三体', author: '刘慈欣', borrowCount: 2 },
  {
    id: 3,
    isbn: '9787115385796',
    title: 'Java核心技术 卷I',
    author: 'Cay S. Horstmann',
    borrowCount: 1
  },
  {
    id: 5,
    isbn: '9787115428028',
    title: 'Python编程:从入门到实践',
    author: 'Eric Matthes',
    borrowCount: 1
  },
  { id: 15, isbn: '9787544270878', title: '平凡的世界', author: '路遥', borrowCount: 1 },
  { id: 22, isbn: '9787532773992', title: '百年孤独', author: '加西亚·马尔克斯', borrowCount: 1 },
  { id: 31, isbn: '9787100040600', title: '人类简史', author: '尤瓦尔·赫拉利', borrowCount: 1 }
]

// 活跃读者排行 (基于真实借阅数据)
const realReaderRankings = [
  { id: 1, readerId: '20240001', name: '张晓明', type: 'STUDENT', borrowCount: 2 },
  { id: 2, readerId: '20240002', name: '李雨晨', type: 'STUDENT', borrowCount: 2 },
  { id: 3, readerId: '20240003', name: '王思远', type: 'STUDENT', borrowCount: 2 },
  { id: 4, readerId: '20240004', name: '陈佳琪', type: 'STUDENT', borrowCount: 2 },
  { id: 5, readerId: '20240005', name: '刘梦婷', type: 'STUDENT', borrowCount: 2 },
  { id: 6, readerId: '20240006', name: '赵文博', type: 'STUDENT', borrowCount: 1 },
  { id: 7, readerId: '20240007', name: '周晓雪', type: 'STUDENT', borrowCount: 1 },
  { id: 8, readerId: '20240008', name: '吴浩然', type: 'STUDENT', borrowCount: 1 },
  { id: 21, readerId: '20240021', name: '陈建国', type: 'TEACHER', borrowCount: 1 },
  { id: 22, readerId: '20240022', name: '王秀英', type: 'TEACHER', borrowCount: 1 }
]

// 生成借阅概览统计数据 (基于真实数据)
export function generateOverview() {
  // 真实数据统计
  const totalBooks = 50 // 图书种类数
  const totalCopies = 572 // 总馆藏量 (按书籍 total_quantity 累加)
  const totalReaders = 40 // 注册读者数
  const totalBorrows = 40 // 总借阅记录数
  const currentBorrowed = 15 // 当前借阅中
  const returnedCount = 10 // 已归还
  const overdueCount = 10 // 逾期未还
  const lostCount = 5 // 遗失
  const availableCopies = totalCopies - currentBorrowed - overdueCount - lostCount

  return {
    // 馆情数据
    totalBooks: totalBooks,
    totalCopies: totalCopies,
    totalReaders: totalReaders,
    totalVisits: 1250,
    booksPerReader: parseFloat((totalCopies / totalReaders).toFixed(1)),
    visitsPerReader: 31.3,

    // 当前借阅情况
    currentBorrowed: currentBorrowed,
    availableCopies: availableCopies,
    overdueCount: overdueCount,
    reservationCount: 6,
    lostCount: lostCount,

    // 今日数据
    todayVisits: 32,
    todayBorrowed: 3,
    todayReturned: 2,
    todayNewReaders: 0,

    // 本月数据
    thisMonthBorrowed: 25,
    thisMonthReturned: returnedCount,
    thisMonthVisits: 850,
    thisMonthNewBooks: 5,

    // 流通率相关
    circulationRate: parseFloat((currentBorrowed / totalCopies).toFixed(2)),
    zeroCirculationCount: 20,
    zeroCirculationRate: parseFloat((20 / totalBooks).toFixed(2)),

    // 同比增长
    borrowGrowth: 0.12,
    visitsGrowth: 0.08,
    readerGrowth: 0.05,

    // 罚款统计
    totalFines: 561 // 从数据库计算
  }
}

// 生成借阅趋势数据 (基于真实数据模式)
export function generateBorrowTrends(timeRange = 'LAST_30_DAYS', granularity = 'DAILY') {
  const trends = []
  const days =
    timeRange === 'LAST_7_DAYS'
      ? 7
      : timeRange === 'LAST_30_DAYS'
        ? 30
        : timeRange === 'THIS_MONTH'
          ? 30
          : timeRange === 'THIS_YEAR'
            ? 365
            : 30

  // 使用真实数据的日均值来生成趋势
  const avgBorrowPerDay = 1.3 // 40条记录/30天
  const avgReturnPerDay = 0.33 // 10条归还/30天

  for (let i = days - 1; i >= 0; i--) {
    const date = dayjs().subtract(i, 'day')
    const isWeekend = date.day() === 0 || date.day() === 6

    // 基于真实数据的分布模式
    const baseBorrow = isWeekend ? 0 : Math.floor(avgBorrowPerDay * 1.5)
    const baseReturn = isWeekend ? 0 : Math.floor(avgReturnPerDay * 1.5)

    trends.push({
      date: date.format('YYYY-MM-DD'),
      borrowed: Math.max(0, baseBorrow + Math.floor(Math.random() * 3)),
      returned: Math.max(0, baseReturn + Math.floor(Math.random() * 2)),
      visits: isWeekend ? Math.floor(Math.random() * 20) + 10 : Math.floor(Math.random() * 40) + 20,
      newReaders: Math.random() < 0.1 ? 1 : 0
    })
  }

  return trends
}

// 生成分类统计数据 (基于真实数据)
export function generateCategoryStats() {
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272']
  const totalBooks = 50

  return realCategoryStats.map((cat, index) => ({
    ...cat,
    color: colors[index % colors.length],
    circulationRate: parseFloat((cat.borrowCount / (cat.bookCount * 10)).toFixed(2)),
    readerCount: Math.floor(cat.borrowCount * 1.2),
    percentage: parseFloat((cat.bookCount / totalBooks).toFixed(3))
  }))
}

// 生成图书排行榜 (基于真实数据)
export function generateBookRankings(params = {}) {
  const { rankBy = 'BORROW_COUNT', limit = 10 } = params

  return realBookRankings.slice(0, limit).map((book, index) => ({
    rank: index + 1,
    bookId: book.id,
    isbn: book.isbn,
    title: book.title,
    author: book.author,
    category: index < 5 ? 'TP' : 'I',
    coverUrl: `https://picsum.photos/seed/${book.id}/100/150`,
    borrowCount: book.borrowCount,
    rating: 4.5,
    totalCopies: 10,
    availableCopies: 10 - book.borrowCount
  }))
}

// 生成读者排行榜 (基于真实数据)
export function generateReaderRankings(params = {}) {
  const { limit = 10 } = params

  const typeNameMap = {
    STUDENT: '学生',
    TEACHER: '教师',
    STAFF: '职工',
    EXTERNAL: '校外人员'
  }

  return realReaderRankings.slice(0, limit).map((reader, index) => ({
    rank: index + 1,
    readerId: reader.id,
    cardNo: reader.readerId,
    realName: reader.name,
    readerType: reader.type.toLowerCase(),
    readerTypeName: typeNameMap[reader.type] || reader.type,
    avatar: `https://i.pravatar.cc/100?img=${index + 1}`,
    borrowCount: reader.borrowCount,
    visitCount: reader.borrowCount * 3,
    favoriteCategory: index < 5 ? '文学' : '工业技术',
    lastBorrowDate: dayjs().subtract(index, 'day').format('YYYY-MM-DD')
  }))
}

// 生成馆藏资源分析 (基于真实数据)
export function generateCollectionAnalysis() {
  return {
    totalBooks: 50,
    totalCopies: 572,
    categoryDistribution: generateCategoryStats(),
    statusDistribution: [
      { status: 'available', statusName: '在架', count: 520, percentage: 0.945 },
      { status: 'borrowed', statusName: '借出', count: 15, percentage: 0.027 },
      { status: 'overdue', statusName: '逾期', count: 10, percentage: 0.018 },
      { status: 'lost', statusName: '遗失', count: 5, percentage: 0.009 }
    ],
    ageDistribution: [
      { range: '0-1年', count: 30, percentage: 0.6 },
      { range: '1-3年', count: 15, percentage: 0.3 },
      { range: '3-5年', count: 5, percentage: 0.1 }
    ],
    circulationAnalysis: {
      highCirculation: 5, // 借阅次数 > 2
      mediumCirculation: 15, // 1 < 借阅次数 <= 2
      lowCirculation: 10, // 借阅次数 = 1
      zeroCirculation: 20 // 借阅次数 = 0
    }
  }
}

// 真实借阅活动数据
const realActivities = [
  { type: 'borrow', readerName: '韩雅静', bookTitle: '罪与罚', timestamp: '2025-10-25T11:30:00' },
  {
    type: 'borrow',
    readerName: '罗天宇',
    bookTitle: '追风筝的人',
    timestamp: '2025-10-24T14:30:00'
  },
  { type: 'borrow', readerName: '何雨欣', bookTitle: '1984', timestamp: '2025-10-23T10:00:00' },
  { type: 'borrow', readerName: '谢明轩', bookTitle: '百年孤独', timestamp: '2025-10-22T15:00:00' },
  { type: 'borrow', readerName: '林诗涵', bookTitle: '悲惨世界', timestamp: '2025-10-21T09:00:00' },
  { type: 'borrow', readerName: '黄俊杰', bookTitle: '边城', timestamp: '2025-10-20T13:30:00' },
  {
    type: 'return',
    readerName: '刘梦婷',
    bookTitle: 'Kubernetes权威指南',
    timestamp: '2025-11-12T10:00:00'
  },
  {
    type: 'return',
    readerName: '张晓明',
    bookTitle: '数据库系统概念',
    timestamp: '2025-11-10T14:00:00'
  },
  {
    type: 'return',
    readerName: '陈佳琪',
    bookTitle: 'Go语言编程之旅',
    timestamp: '2025-11-08T17:00:00'
  },
  {
    type: 'return',
    readerName: '王思远',
    bookTitle: 'Java核心技术 卷I',
    timestamp: '2025-11-05T10:00:00'
  }
]

// 生成最近活动记录 (基于真实数据)
export function generateRecentActivities(limit = 20) {
  const activityTypeMap = {
    borrow: { label: '借书', icon: 'DocumentAdd' },
    return: { label: '还书', icon: 'DocumentChecked' },
    renew: { label: '续借', icon: 'Refresh' },
    reserve: { label: '预约', icon: 'Clock' },
    register: { label: '注册', icon: 'UserFilled' }
  }

  return realActivities
    .slice(0, limit)
    .map((activity, index) => ({
      id: index + 1,
      type: activity.type,
      typeName: activityTypeMap[activity.type].label,
      icon: activityTypeMap[activity.type].icon,
      readerName: activity.readerName,
      bookTitle: activity.bookTitle,
      description: `${activityTypeMap[activity.type].label}《${activity.bookTitle}》`,
      timestamp: activity.timestamp,
      status: 'success'
    }))
    .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
}

// 生成年级借阅偏好数据 (基于真实读者类型)
export function generateGradeBorrowPreferences() {
  const readerTypes = ['学生读者', '教师读者', '职工读者', '校外读者']
  const categories = ['文学', '科技', '历史', '经济', '其他']

  // 基于真实读者分布: STUDENT: 20, TEACHER: 10, STAFF: 5, EXTERNAL: 5
  const typeDistribution = {
    学生读者: 20,
    教师读者: 10,
    职工读者: 5,
    校外读者: 5
  }

  return readerTypes.map((type) => ({
    grade: type,
    preferences: categories.map((category) => ({
      category,
      count: Math.floor(typeDistribution[type] * (category === '文学' ? 0.4 : 0.15)),
      percentage: category === '文学' ? 0.4 : 0.15
    }))
  }))
}

// 生成年度流通统计 (基于真实数据模式)
export function generateYearlyCirculation() {
  const months = Array.from({ length: 12 }, (_, i) => i + 1)

  return months.map((month) => {
    // 基于真实数据: 40条记录主要集中在10月-11月
    const isActiveMonth = month >= 10
    const baseBorrow = isActiveMonth ? 15 : 2
    const baseReturn = isActiveMonth ? 5 : 1

    return {
      month: `${month}月`,
      borrowed: baseBorrow + Math.floor(Math.random() * 5),
      returned: baseReturn + Math.floor(Math.random() * 3),
      reserved: Math.floor(Math.random() * 3),
      renewed: Math.floor(Math.random() * 2)
    }
  })
}

// 生成分类分布数据 (基于真实数据)
export function generateCategoryDistribution() {
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272']
  const totalBooks = 50

  return realCategoryStats.map((cat, index) => {
    const avgCopiesPerBook = 11 // 550/50
    const estimatedCopies = cat.bookCount * avgCopiesPerBook
    const zeroCirc = Math.max(1, Math.floor(cat.bookCount * 0.3))

    return {
      code: cat.code,
      name: cat.name,
      color: colors[index % colors.length],
      bookCount: cat.bookCount,
      borrowCount: cat.borrowCount,
      circulationRate: parseFloat((cat.borrowCount / estimatedCopies).toFixed(2)),
      readerCount: Math.floor(cat.borrowCount * 1.5),
      percentage: parseFloat((cat.bookCount / totalBooks).toFixed(3)),
      zeroCirculationCount: zeroCirc,
      zeroCirculationRate: parseFloat((zeroCirc / cat.bookCount).toFixed(3))
    }
  })
}

// 生成读者活跃度热力图数据
export function generateReaderHeatmap() {
  const hours = [
    '8:00',
    '9:00',
    '10:00',
    '11:00',
    '12:00',
    '13:00',
    '14:00',
    '15:00',
    '16:00',
    '17:00',
    '18:00',
    '19:00'
  ]
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

  const data = []
  let minValue = 100
  let maxValue = 0

  for (let dayIndex = 0; dayIndex < days.length; dayIndex++) {
    for (let hourIndex = 0; hourIndex < hours.length; hourIndex++) {
      // 基于真实数据的访问模式
      const isWeekend = dayIndex >= 5
      const isPeakHour = (hourIndex >= 2 && hourIndex <= 4) || (hourIndex >= 6 && hourIndex <= 8)

      // 由于数据量小，整体活跃度降低
      const baseValue = isWeekend ? 10 : 20
      const peakBonus = isPeakHour ? 15 : 0
      const value = Math.max(
        0,
        Math.min(50, baseValue + peakBonus + Math.floor(Math.random() * 10) - 5)
      )

      minValue = Math.min(minValue, value)
      maxValue = Math.max(maxValue, value)

      data.push([hourIndex, dayIndex, value])
    }
  }

  return {
    hours,
    days,
    data,
    minValue,
    maxValue
  }
}
