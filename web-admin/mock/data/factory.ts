import { faker } from '@faker-js/faker/locale/zh_CN'

/**
 * 通用响应格式 - 成功
 */
export const successResponse = <T>(data: T) => ({
  code: 200,
  message: 'success',
  data,
  timestamp: Date.now(),
  traceId: faker.string.uuid()
})

/**
 * 通用响应格式 - 失败
 */
export const errorResponse = (code: number, message: string) => ({
  code,
  message,
  data: null,
  timestamp: Date.now(),
  traceId: faker.string.uuid()
})

/**
 * 分页响应格式
 */
export const pageResponse = <T>(records: T[], total: number, pageNum: number, pageSize: number) => ({
  code: 200,
  message: 'success',
  data: {
    records,
    total,
    pageNum,
    pageSize,
    pages: Math.ceil(total / pageSize)
  },
  timestamp: Date.now(),
  traceId: faker.string.uuid()
})

/**
 * 生成部门数据
 */
export const generateDepartment = (id?: number, parentId?: number) => ({
  id: id || faker.number.int({ min: 1, max: 10000 }),
  deptCode: faker.string.alphanumeric({ length: 8 }).toUpperCase(),
  deptName: faker.helpers.arrayElement([
    '图书流通部', '图书编目部', '参考咨询部', '技术服务部',
    '读者服务部', '信息技术部', '行政管理部', '采购部'
  ]),
  parentId: parentId !== undefined ? parentId : (faker.datatype.boolean() ? 1 : 0),
  deptType: faker.helpers.arrayElement(['functional', 'business', 'support']),
  leader: faker.person.fullName(),
  phone: faker.phone.number(),
  email: faker.internet.email(),
  status: faker.helpers.arrayElement(['active', 'inactive']),
  sort: faker.number.int({ min: 1, max: 100 }),
  description: faker.lorem.sentence(),
  createdTime: faker.date.past({ years: 2 }).toISOString(),
  updatedTime: faker.date.recent({ days: 30 }).toISOString()
})

/**
 * 生成用户数据
 */
export const generateUser = (id?: number) => ({
  userId: id || faker.number.int({ min: 1, max: 10000 }),
  username: faker.internet.userName(),
  realName: faker.person.fullName(),
  email: faker.internet.email(),
  phone: faker.phone.number(),
  gender: faker.helpers.arrayElement(['male', 'female', 'other']),
  avatar: faker.image.avatar(),
  deptId: faker.number.int({ min: 1, max: 8 }),
  deptName: faker.helpers.arrayElement([
    '图书流通部', '图书编目部', '参考咨询部', '技术服务部'
  ]),
  position: faker.person.jobTitle(),
  role: faker.helpers.arrayElement(['ADMIN', 'LIBRARIAN', 'DIRECTOR', 'STAFF']),
  status: faker.helpers.arrayElement(['active', 'inactive', 'locked']),
  createdTime: faker.date.past({ years: 2 }).toISOString(),
  lastLoginTime: faker.date.recent({ days: 7 }).toISOString()
})

/**
 * 生成图书数据
 */
export const generateBook = (id?: number) => ({
  bookId: id || faker.number.int({ min: 1, max: 10000 }),
  isbn: faker.string.numeric(13),
  title: faker.helpers.arrayElement([
    '三体', '活着', '红楼梦', '百年孤独', '人类简史',
    '未来简史', '1984', '动物庄园', '围城', '平凡的世界',
    '白夜行', '解忧杂货店', '追风筝的人', '小王子', '哈利波特'
  ]),
  author: faker.person.fullName(),
  publisher: faker.helpers.arrayElement([
    '人民文学出版社', '上海译文出版社', '中信出版社',
    '商务印书馆', '三联书店', '北京大学出版社'
  ]),
  publishDate: faker.date.past({ years: 10 }).toISOString().split('T')[0],
  category: faker.helpers.arrayElement(['I247', 'K', 'B', 'TP', 'F', 'H', 'O']),
  categoryName: faker.helpers.arrayElement([
    '中国小说', '历史', '哲学', '计算机', '经济', '语言学', '数学'
  ]),
  price: parseFloat(faker.commerce.price({ min: 20, max: 200, dec: 2 })),
  coverUrl: `https://picsum.photos/seed/${faker.number.int({ min: 1, max: 1000 })}/300/400`,
  description: faker.lorem.paragraph(),
  totalCopies: faker.number.int({ min: 1, max: 10 }),
  availableCopies: faker.number.int({ min: 0, max: 8 }),
  borrowCount: faker.number.int({ min: 0, max: 500 }),
  rating: faker.number.float({ min: 3.5, max: 5.0, fractionDigits: 1 }),
  status: faker.helpers.arrayElement(['available', 'borrowed', 'reserved', 'damaged']),
  location: faker.helpers.arrayElement(['一楼借阅区', '二楼参考区', '三楼自习区', '地下书库']),
  callNumber: faker.string.alphanumeric({ length: 10 }).toUpperCase(),
  createdTime: faker.date.past({ years: 2 }).toISOString(),
  updatedTime: faker.date.recent({ days: 30 }).toISOString()
})

/**
 * 生成读者数据
 */
export const generateReader = (id?: number) => ({
  readerId: id || faker.number.int({ min: 1, max: 10000 }),
  cardNumber: faker.string.numeric(10),
  realName: faker.person.fullName(),
  gender: faker.helpers.arrayElement(['male', 'female', 'other']),
  idCard: faker.string.numeric(18),
  phone: faker.phone.number(),
  email: faker.internet.email(),
  address: faker.location.streetAddress(),
  readerType: faker.helpers.arrayElement(['student', 'teacher', 'staff', 'public']),
  maxBorrowCount: faker.number.int({ min: 5, max: 20 }),
  currentBorrowCount: faker.number.int({ min: 0, max: 5 }),
  depositAmount: parseFloat(faker.commerce.price({ min: 0, max: 500, dec: 2 })),
  creditScore: faker.number.int({ min: 60, max: 100 }),
  status: faker.helpers.arrayElement(['active', 'suspended', 'expired', 'blacklisted']),
  cardExpireDate: faker.date.future({ years: 1 }).toISOString().split('T')[0],
  createdTime: faker.date.past({ years: 2 }).toISOString(),
  lastBorrowTime: faker.date.recent({ days: 30 }).toISOString()
})

/**
 * 生成借阅记录数据
 */
export const generateCirculation = (id?: number) => ({
  circulationId: id || faker.number.int({ min: 1, max: 10000 }),
  readerId: faker.number.int({ min: 1, max: 1000 }),
  readerName: faker.person.fullName(),
  bookId: faker.number.int({ min: 1, max: 5000 }),
  bookTitle: faker.helpers.arrayElement([
    '三体', '活着', '红楼梦', '百年孤独', '人类简史'
  ]),
  borrowDate: faker.date.past({ days: 60 }).toISOString().split('T')[0],
  dueDate: faker.date.future({ days: 30 }).toISOString().split('T')[0],
  returnDate: faker.datatype.boolean()
    ? faker.date.recent({ days: 10 }).toISOString().split('T')[0]
    : null,
  renewCount: faker.number.int({ min: 0, max: 3 }),
  status: faker.helpers.arrayElement(['borrowed', 'returned', 'overdue', 'lost']),
  overduedays: faker.number.int({ min: 0, max: 30 }),
  fineAmount: parseFloat(faker.commerce.price({ min: 0, max: 50, dec: 2 })),
  librarianId: faker.number.int({ min: 1, max: 50 }),
  librarianName: faker.person.fullName(),
  createdTime: faker.date.past({ years: 1 }).toISOString()
})
