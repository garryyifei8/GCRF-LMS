import dayjs from 'dayjs'

// 流通状态
export const circulationStatus = {
  BORROWED: 'BORROWED', // 借出中
  RETURNED: 'RETURNED', // 已归还
  OVERDUE: 'OVERDUE', // 已逾期
  RENEWED: 'RENEWED', // 已续借
  LOST: 'LOST' // 遗失
}

// 预约状态
export const reservationStatus = {
  PENDING: 'PENDING', // 等待中
  READY: 'READY', // 已到书
  PICKED_UP: 'PICKED_UP', // 已取书
  CANCELLED: 'CANCELLED', // 已取消
  EXPIRED: 'EXPIRED' // 已过期
}

// 数据库真实借阅数据 (from circulation_service.borrows)
const realBorrowsData = [
  {
    borrow_id: 'BW-20250801-0001',
    reader_id: 31,
    book_id: 36,
    borrow_date: '2025-08-01T09:00:00',
    due_date: '2025-08-31T23:59:59',
    return_date: null,
    status: 'LOST',
    renew_count: 0,
    fine_amount: 50.0
  },
  {
    borrow_id: 'BW-20250802-0001',
    reader_id: 32,
    book_id: 37,
    borrow_date: '2025-08-02T10:00:00',
    due_date: '2025-09-01T23:59:59',
    return_date: null,
    status: 'LOST',
    renew_count: 1,
    fine_amount: 45.0
  },
  {
    borrow_id: 'BW-20250803-0001',
    reader_id: 33,
    book_id: 38,
    borrow_date: '2025-08-03T11:00:00',
    due_date: '2025-09-02T23:59:59',
    return_date: null,
    status: 'LOST',
    renew_count: 0,
    fine_amount: 60.0
  },
  {
    borrow_id: 'BW-20250804-0001',
    reader_id: 34,
    book_id: 39,
    borrow_date: '2025-08-04T14:00:00',
    due_date: '2025-09-03T23:59:59',
    return_date: null,
    status: 'LOST',
    renew_count: 0,
    fine_amount: 55.0
  },
  {
    borrow_id: 'BW-20250805-0001',
    reader_id: 35,
    book_id: 40,
    borrow_date: '2025-08-05T15:00:00',
    due_date: '2025-09-04T23:59:59',
    return_date: null,
    status: 'LOST',
    renew_count: 2,
    fine_amount: 50.0
  },
  {
    borrow_id: 'BW-20250901-0001',
    reader_id: 21,
    book_id: 26,
    borrow_date: '2025-09-01T09:00:00',
    due_date: '2025-10-01T23:59:59',
    return_date: null,
    status: 'OVERDUE',
    renew_count: 0,
    fine_amount: 27.0
  },
  {
    borrow_id: 'BW-20250902-0001',
    reader_id: 22,
    book_id: 27,
    borrow_date: '2025-09-02T10:00:00',
    due_date: '2025-10-02T23:59:59',
    return_date: null,
    status: 'OVERDUE',
    renew_count: 1,
    fine_amount: 26.0
  },
  {
    borrow_id: 'BW-20250903-0001',
    reader_id: 23,
    book_id: 28,
    borrow_date: '2025-09-03T11:00:00',
    due_date: '2025-10-03T23:59:59',
    return_date: null,
    status: 'OVERDUE',
    renew_count: 0,
    fine_amount: 25.0
  },
  {
    borrow_id: 'BW-20250904-0001',
    reader_id: 24,
    book_id: 29,
    borrow_date: '2025-09-04T14:00:00',
    due_date: '2025-10-04T23:59:59',
    return_date: null,
    status: 'OVERDUE',
    renew_count: 0,
    fine_amount: 24.0
  },
  {
    borrow_id: 'BW-20250905-0001',
    reader_id: 25,
    book_id: 30,
    borrow_date: '2025-09-05T15:00:00',
    due_date: '2025-10-05T23:59:59',
    return_date: null,
    status: 'OVERDUE',
    renew_count: 2,
    fine_amount: 23.0
  },
  {
    borrow_id: 'BW-20250910-0001',
    reader_id: 26,
    book_id: 31,
    borrow_date: '2025-09-10T09:00:00',
    due_date: '2025-10-10T23:59:59',
    return_date: null,
    status: 'OVERDUE',
    renew_count: 0,
    fine_amount: 18.0
  },
  {
    borrow_id: 'BW-20250915-0001',
    reader_id: 27,
    book_id: 32,
    borrow_date: '2025-09-15T10:00:00',
    due_date: '2025-10-15T23:59:59',
    return_date: null,
    status: 'OVERDUE',
    renew_count: 1,
    fine_amount: 13.0
  },
  {
    borrow_id: 'BW-20250920-0001',
    reader_id: 28,
    book_id: 33,
    borrow_date: '2025-09-20T11:00:00',
    due_date: '2025-10-20T23:59:59',
    return_date: null,
    status: 'OVERDUE',
    renew_count: 0,
    fine_amount: 8.0
  },
  {
    borrow_id: 'BW-20250922-0001',
    reader_id: 29,
    book_id: 34,
    borrow_date: '2025-09-22T13:00:00',
    due_date: '2025-10-22T23:59:59',
    return_date: null,
    status: 'OVERDUE',
    renew_count: 0,
    fine_amount: 6.0
  },
  {
    borrow_id: 'BW-20250925-0001',
    reader_id: 30,
    book_id: 35,
    borrow_date: '2025-09-25T15:00:00',
    due_date: '2025-10-25T23:59:59',
    return_date: null,
    status: 'OVERDUE',
    renew_count: 1,
    fine_amount: 3.0
  },
  {
    borrow_id: 'BW-20251001-0001',
    reader_id: 1,
    book_id: 1,
    borrow_date: '2025-10-01T09:00:00',
    due_date: '2025-10-31T23:59:59',
    return_date: '2025-10-25T14:30:00',
    status: 'RETURNED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251002-0001',
    reader_id: 2,
    book_id: 2,
    borrow_date: '2025-10-02T10:30:00',
    due_date: '2025-11-01T23:59:59',
    return_date: '2025-10-20T16:00:00',
    status: 'RETURNED',
    renew_count: 1,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251003-0001',
    reader_id: 3,
    book_id: 3,
    borrow_date: '2025-10-03T14:00:00',
    due_date: '2025-11-02T23:59:59',
    return_date: '2025-11-05T10:00:00',
    status: 'RETURNED',
    renew_count: 0,
    fine_amount: 3.0
  },
  {
    borrow_id: 'BW-20251004-0001',
    reader_id: 4,
    book_id: 4,
    borrow_date: '2025-10-04T11:00:00',
    due_date: '2025-11-03T23:59:59',
    return_date: '2025-10-28T09:00:00',
    status: 'RETURNED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251005-0001',
    reader_id: 5,
    book_id: 5,
    borrow_date: '2025-10-05T15:30:00',
    due_date: '2025-11-04T23:59:59',
    return_date: '2025-11-04T18:00:00',
    status: 'RETURNED',
    renew_count: 1,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251006-0001',
    reader_id: 1,
    book_id: 6,
    borrow_date: '2025-10-06T09:00:00',
    due_date: '2025-11-05T23:59:59',
    return_date: '2025-11-10T14:00:00',
    status: 'RETURNED',
    renew_count: 0,
    fine_amount: 5.0
  },
  {
    borrow_id: 'BW-20251007-0001',
    reader_id: 2,
    book_id: 7,
    borrow_date: '2025-10-07T13:00:00',
    due_date: '2025-11-06T23:59:59',
    return_date: '2025-10-30T11:00:00',
    status: 'RETURNED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251008-0001',
    reader_id: 3,
    book_id: 8,
    borrow_date: '2025-10-08T10:00:00',
    due_date: '2025-11-07T23:59:59',
    return_date: '2025-11-01T15:00:00',
    status: 'RETURNED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251009-0001',
    reader_id: 4,
    book_id: 9,
    borrow_date: '2025-10-09T16:00:00',
    due_date: '2025-11-08T23:59:59',
    return_date: '2025-11-08T17:00:00',
    status: 'RETURNED',
    renew_count: 2,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251010-0001',
    reader_id: 5,
    book_id: 10,
    borrow_date: '2025-10-10T14:00:00',
    due_date: '2025-11-09T23:59:59',
    return_date: '2025-11-12T10:00:00',
    status: 'RETURNED',
    renew_count: 0,
    fine_amount: 3.0
  },
  {
    borrow_id: 'BW-20251011-0001',
    reader_id: 6,
    book_id: 11,
    borrow_date: '2025-10-11T09:30:00',
    due_date: '2025-11-10T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251012-0001',
    reader_id: 7,
    book_id: 12,
    borrow_date: '2025-10-12T10:00:00',
    due_date: '2025-11-11T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 1,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251013-0001',
    reader_id: 8,
    book_id: 13,
    borrow_date: '2025-10-13T11:30:00',
    due_date: '2025-11-12T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251014-0001',
    reader_id: 9,
    book_id: 14,
    borrow_date: '2025-10-14T15:00:00',
    due_date: '2025-11-13T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251015-0001',
    reader_id: 10,
    book_id: 15,
    borrow_date: '2025-10-15T09:00:00',
    due_date: '2025-11-14T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 1,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251016-0001',
    reader_id: 11,
    book_id: 16,
    borrow_date: '2025-10-16T14:00:00',
    due_date: '2025-11-15T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251017-0001',
    reader_id: 12,
    book_id: 17,
    borrow_date: '2025-10-17T10:30:00',
    due_date: '2025-11-16T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 2,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251018-0001',
    reader_id: 13,
    book_id: 18,
    borrow_date: '2025-10-18T16:00:00',
    due_date: '2025-11-17T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251019-0001',
    reader_id: 14,
    book_id: 19,
    borrow_date: '2025-10-19T11:00:00',
    due_date: '2025-11-18T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 1,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251020-0001',
    reader_id: 15,
    book_id: 20,
    borrow_date: '2025-10-20T13:30:00',
    due_date: '2025-11-19T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251021-0001',
    reader_id: 16,
    book_id: 21,
    borrow_date: '2025-10-21T09:00:00',
    due_date: '2025-11-20T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251022-0001',
    reader_id: 17,
    book_id: 22,
    borrow_date: '2025-10-22T15:00:00',
    due_date: '2025-11-21T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 1,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251023-0001',
    reader_id: 18,
    book_id: 23,
    borrow_date: '2025-10-23T10:00:00',
    due_date: '2025-11-22T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251024-0001',
    reader_id: 19,
    book_id: 24,
    borrow_date: '2025-10-24T14:30:00',
    due_date: '2025-11-23T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 0,
    fine_amount: 0.0
  },
  {
    borrow_id: 'BW-20251025-0001',
    reader_id: 20,
    book_id: 25,
    borrow_date: '2025-10-25T11:30:00',
    due_date: '2025-11-24T23:59:59',
    return_date: null,
    status: 'BORROWED',
    renew_count: 2,
    fine_amount: 0.0
  }
]

// 图书信息映射 (from book_service.books)
const booksMap = {
  1: { title: '深入理解计算机系统', isbn: '9787115543998', author: 'Randal E. Bryant' },
  2: { title: '算法导论', isbn: '9787111558422', author: 'Thomas H. Cormen' },
  3: { title: 'Java核心技术 卷I', isbn: '9787115385796', author: 'Cay S. Horstmann' },
  4: { title: 'Spring Boot实战', isbn: '9787111606178', author: 'Craig Walls' },
  5: { title: 'Python编程:从入门到实践', isbn: '9787115428028', author: 'Eric Matthes' },
  6: { title: '数据库系统概念', isbn: '9787111544692', author: 'Abraham Silberschatz' },
  7: { title: 'JavaScript高级程序设计', isbn: '9787115443663', author: 'Matt Frisbie' },
  8: { title: 'Vue.js设计与实现', isbn: '9787115549440', author: '霍春阳' },
  9: { title: 'Go语言编程之旅', isbn: '9787115591258', author: '陈剑煜' },
  10: { title: 'Kubernetes权威指南', isbn: '9787111641322', author: '龚正' },
  11: { title: '红楼梦', isbn: '9787020008735', author: '曹雪芹' },
  12: { title: '三国演义', isbn: '9787020002207', author: '罗贯中' },
  13: { title: '水浒传', isbn: '9787020015016', author: '施耐庵' },
  14: { title: '西游记', isbn: '9787020105106', author: '吴承恩' },
  15: { title: '平凡的世界', isbn: '9787544270878', author: '路遥' },
  16: { title: '活着', isbn: '9787544291170', author: '余华' },
  17: { title: '围城', isbn: '9787536692930', author: '钱钟书' },
  18: { title: '白鹿原', isbn: '9787532754694', author: '陈忠实' },
  19: { title: '骆驼祥子', isbn: '9787020129928', author: '老舍' },
  20: { title: '边城', isbn: '9787540478940', author: '沈从文' },
  21: { title: '悲惨世界', isbn: '9787020002023', author: '维克多·雨果' },
  22: { title: '百年孤独', isbn: '9787532773992', author: '加西亚·马尔克斯' },
  23: { title: '1984', isbn: '9787020105854', author: '乔治·奥威尔' },
  24: { title: '追风筝的人', isbn: '9787532776290', author: '卡勒德·胡赛尼' },
  25: { title: '罪与罚', isbn: '9787020125579', author: '陀思妥耶夫斯基' },
  26: { title: '小王子', isbn: '9787532771486', author: '圣埃克苏佩里' },
  27: { title: '老人与海', isbn: '9787532783120', author: '海明威' },
  28: { title: '战争与和平', isbn: '9787020002092', author: '列夫·托尔斯泰' },
  29: { title: '史记', isbn: '9787101003048', author: '司马迁' },
  30: { title: '明朝那些事儿', isbn: '9787108047311', author: '当年明月' },
  31: { title: '人类简史', isbn: '9787100040600', author: '尤瓦尔·赫拉利' },
  32: { title: '未来简史', isbn: '9787213079580', author: '尤瓦尔·赫拉利' },
  33: { title: '理想国', isbn: '9787100040617', author: '柏拉图' },
  34: { title: '资本论', isbn: '9787100099110', author: '卡尔·马克思' },
  35: { title: '论语', isbn: '9787100090759', author: '孔子' },
  36: { title: '道德经', isbn: '9787100069649', author: '老子' },
  37: { title: '时间简史', isbn: '9787115442307', author: '史蒂芬·霍金' },
  38: { title: '自私的基因', isbn: '9787542859037', author: '理查德·道金斯' },
  39: { title: '三体', isbn: '9787115427977', author: '刘慈欣' },
  40: { title: '三体II:黑暗森林', isbn: '9787115450210', author: '刘慈欣' }
}

// 读者信息映射 (from reader_service.readers)
const readersMap = {
  1: { readerId: '20240001', name: '张晓明' },
  2: { readerId: '20240002', name: '李雨晨' },
  3: { readerId: '20240003', name: '王思远' },
  4: { readerId: '20240004', name: '陈佳琪' },
  5: { readerId: '20240005', name: '刘梦婷' },
  6: { readerId: '20240006', name: '赵文博' },
  7: { readerId: '20240007', name: '周晓雪' },
  8: { readerId: '20240008', name: '吴浩然' },
  9: { readerId: '20240009', name: '郑雅琴' },
  10: { readerId: '20240010', name: '孙志强' },
  11: { readerId: '20240011', name: '马晓宇' },
  12: { readerId: '20240012', name: '徐梦洁' },
  13: { readerId: '20240013', name: '冯博文' },
  14: { readerId: '20240014', name: '杨雪莉' },
  15: { readerId: '20240015', name: '黄俊杰' },
  16: { readerId: '20240016', name: '林诗涵' },
  17: { readerId: '20240017', name: '谢明轩' },
  18: { readerId: '20240018', name: '何雨欣' },
  19: { readerId: '20240019', name: '罗天宇' },
  20: { readerId: '20240020', name: '韩雅静' },
  21: { readerId: '20240021', name: '陈建国' },
  22: { readerId: '20240022', name: '王秀英' },
  23: { readerId: '20240023', name: '李文斌' },
  24: { readerId: '20240024', name: '张丽华' },
  25: { readerId: '20240025', name: '刘志强' },
  26: { readerId: '20240026', name: '赵明阳' },
  27: { readerId: '20240027', name: '周华民' },
  28: { readerId: '20240028', name: '吴晓峰' },
  29: { readerId: '20240029', name: '郑文杰' },
  30: { readerId: '20240030', name: '孙美玲' },
  31: { readerId: '20240031', name: '马建军' },
  32: { readerId: '20240032', name: '徐晓红' },
  33: { readerId: '20240033', name: '冯国庆' },
  34: { readerId: '20240034', name: '杨丽萍' },
  35: { readerId: '20240035', name: '黄志伟' },
  36: { readerId: '20240036', name: '林海涛' },
  37: { readerId: '20240037', name: '谢敏敏' },
  38: { readerId: '20240038', name: '何建华' },
  39: { readerId: '20240039', name: '罗晓芳' },
  40: { readerId: '20240040', name: '韩东明' }
}

// 状态映射
const statusNameMap = {
  BORROWED: '借阅中',
  RETURNED: '已归还',
  OVERDUE: '已逾期',
  RENEWED: '已续借',
  LOST: '已遗失'
}

// 生成流通记录
export function generateCirculationRecords(count = 40) {
  return realBorrowsData
    .slice(0, count)
    .map((record, index) => {
      const book = booksMap[record.book_id] || {
        title: `图书${record.book_id}`,
        isbn: '',
        author: ''
      }
      const reader = readersMap[record.reader_id] || {
        readerId: `RD${record.reader_id}`,
        name: `读者${record.reader_id}`
      }
      const dueDate = dayjs(record.due_date)
      const isOverdue = record.status === 'OVERDUE' || record.status === 'LOST'
      const overdueDays = isOverdue ? dayjs().diff(dueDate, 'day') : 0

      return {
        id: index + 1,
        recordNo: record.borrow_id,
        bookId: record.book_id,
        bookTitle: book.title,
        bookIsbn: book.isbn,
        bookBarcode: `BK${String(record.book_id).padStart(8, '0')}`,
        readerId: record.reader_id,
        readerName: reader.name,
        readerCardNo: reader.readerId,
        borrowDate: record.borrow_date,
        dueDate: record.due_date,
        returnDate: record.return_date,
        renewCount: record.renew_count,
        isOverdue: isOverdue,
        overdueDays: overdueDays,
        overdueFine: record.fine_amount,
        status: record.status,
        statusName: statusNameMap[record.status] || record.status,
        operatorId: 1,
        operatorName: '系统管理员',
        remark: '',
        createdAt: record.borrow_date,
        updatedAt: record.return_date || record.borrow_date
      }
    })
    .sort((a, b) => new Date(b.borrowDate) - new Date(a.borrowDate))
}

// 数据库真实预约数据 (模拟基于借阅数据生成)
const realReservationsData = [
  {
    id: 1,
    bookId: 41,
    readerId: 1,
    reservationDate: '2025-11-25T09:00:00',
    status: 'PENDING',
    queue: 1
  },
  {
    id: 2,
    bookId: 42,
    readerId: 2,
    reservationDate: '2025-11-25T10:00:00',
    status: 'PENDING',
    queue: 2
  },
  {
    id: 3,
    bookId: 43,
    readerId: 3,
    reservationDate: '2025-11-26T11:00:00',
    status: 'READY',
    readyDate: '2025-11-28T14:00:00'
  },
  {
    id: 4,
    bookId: 44,
    readerId: 4,
    reservationDate: '2025-11-26T14:00:00',
    status: 'PICKED_UP',
    readyDate: '2025-11-27T10:00:00',
    pickupDate: '2025-11-28T15:00:00'
  },
  { id: 5, bookId: 45, readerId: 5, reservationDate: '2025-11-20T09:00:00', status: 'EXPIRED' },
  { id: 6, bookId: 46, readerId: 6, reservationDate: '2025-11-22T10:00:00', status: 'CANCELLED' },
  {
    id: 7,
    bookId: 47,
    readerId: 7,
    reservationDate: '2025-11-28T11:00:00',
    status: 'PENDING',
    queue: 1
  },
  {
    id: 8,
    bookId: 48,
    readerId: 8,
    reservationDate: '2025-11-29T14:00:00',
    status: 'PENDING',
    queue: 1
  },
  {
    id: 9,
    bookId: 49,
    readerId: 9,
    reservationDate: '2025-11-30T09:00:00',
    status: 'READY',
    readyDate: '2025-12-01T10:00:00'
  },
  {
    id: 10,
    bookId: 50,
    readerId: 10,
    reservationDate: '2025-11-30T15:00:00',
    status: 'PENDING',
    queue: 3
  }
]

// 扩展图书映射
const extendedBooksMap = {
  ...booksMap,
  41: { title: '三体III:死神永生', isbn: '9787115485038', author: '刘慈欣' },
  42: { title: '经济学原理', isbn: '9787111546937', author: 'N.格里高利·曼昆' },
  43: { title: '贫穷的本质', isbn: '9787213079337', author: '阿比吉特·班纳吉' },
  44: { title: '金字塔原理', isbn: '9787508663326', author: '芭芭拉·明托' },
  45: { title: '从0到1', isbn: '9787508680013', author: '彼得·蒂尔' },
  46: { title: '创新者的窘境', isbn: '9787508691480', author: '克莱顿·克里斯坦森' },
  47: { title: '原则', isbn: '9787508678092', author: '瑞·达利欧' },
  48: { title: '认知觉醒', isbn: '9787121267819', author: '周岭' },
  49: { title: '高效能人士的七个习惯', isbn: '9787111544708', author: '史蒂芬·柯维' },
  50: { title: '非暴力沟通', isbn: '9787111626169', author: '马歇尔·卢森堡' }
}

// 预约状态名称映射
const reservationStatusNameMap = {
  PENDING: '等待中',
  READY: '已到书',
  PICKED_UP: '已取书',
  CANCELLED: '已取消',
  EXPIRED: '已过期'
}

// 生成预约记录
export function generateReservations(count = 10) {
  return realReservationsData
    .slice(0, count)
    .map((reservation, index) => {
      const book = extendedBooksMap[reservation.bookId] || {
        title: `图书${reservation.bookId}`,
        isbn: '',
        author: ''
      }
      const reader = readersMap[reservation.readerId] || {
        readerId: `RD${reservation.readerId}`,
        name: `读者${reservation.readerId}`
      }
      const reservationDate = dayjs(reservation.reservationDate)
      const expireDate = reservation.readyDate
        ? dayjs(reservation.readyDate).add(3, 'day')
        : reservationDate.add(7, 'day')

      return {
        id: reservation.id,
        reservationNo: `RV${String(reservation.id).padStart(10, '0')}`,
        bookId: reservation.bookId,
        bookTitle: book.title,
        bookIsbn: book.isbn,
        bookBarcode: `BK${String(reservation.bookId).padStart(8, '0')}`,
        readerId: reservation.readerId,
        readerName: reader.name,
        readerCardNo: reader.readerId,
        reservationDate: reservation.reservationDate,
        readyDate: reservation.readyDate || null,
        pickupDate: reservation.pickupDate || null,
        expireDate: expireDate.format('YYYY-MM-DDTHH:mm:ss'),
        status: reservation.status,
        statusName: reservationStatusNameMap[reservation.status],
        queue: reservation.queue || 0,
        remark: '',
        createdAt: reservation.reservationDate,
        updatedAt: reservation.pickupDate || reservation.readyDate || reservation.reservationDate
      }
    })
    .sort((a, b) => new Date(b.reservationDate) - new Date(a.reservationDate))
}

// 生成流通统计数据 (基于真实数据计算)
export function generateCirculationStats() {
  const records = realBorrowsData
  const totalRecords = records.length
  const returned = records.filter((r) => r.status === 'RETURNED').length
  const borrowed = records.filter((r) => r.status === 'BORROWED').length
  const overdue = records.filter((r) => r.status === 'OVERDUE').length
  const lost = records.filter((r) => r.status === 'LOST').length
  const reservations = realReservationsData.filter(
    (r) => r.status === 'PENDING' || r.status === 'READY'
  ).length
  const totalFines = records.reduce((sum, r) => sum + (r.fine_amount || 0), 0)

  return {
    totalBorrowed: totalRecords,
    totalReturned: returned,
    currentBorrowed: borrowed,
    overdueCount: overdue,
    lostCount: lost,
    reservationCount: reservations,
    todayBorrowed: 3,
    todayReturned: 2,
    thisMonthBorrowed: borrowed + returned,
    thisMonthReturned: returned,
    averageBorrowDays: 25,
    circulationRate: 0.68,
    totalFines: totalFines
  }
}
