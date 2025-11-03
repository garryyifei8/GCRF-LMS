import { faker } from '@faker-js/faker/locale/zh_CN'

// 读者类型
export const readerTypes = [
  { value: 'student', label: '学生', maxBorrowCount: 5, borrowDays: 30, depositAmount: 0 },
  { value: 'teacher', label: '教师', maxBorrowCount: 10, borrowDays: 60, depositAmount: 0 },
  { value: 'staff', label: '职工', maxBorrowCount: 8, borrowDays: 45, depositAmount: 0 },
  { value: 'public', label: '公众读者', maxBorrowCount: 3, borrowDays: 15, depositAmount: 100 }
]

// 年级列表
const grades = [
  '一年级', '二年级', '三年级', '四年级', '五年级', '六年级',
  '初一', '初二', '初三',
  '高一', '高二', '高三',
  '大一', '大二', '大三', '大四'
]

// 班级列表
const classes = ['1班', '2班', '3班', '4班', '5班', '6班', '7班', '8班']

// 部门列表
const departments = [
  '语文组', '数学组', '英语组', '物理组', '化学组', '生物组',
  '政治组', '历史组', '地理组', '体育组', '音乐组', '美术组',
  '信息技术组', '行政办公室', '教务处', '德育处', '总务处'
]

// 生成读者数据
export function generateReaders(count = 200) {
  const readers = []

  for (let i = 0; i < count; i++) {
    const readerType = faker.helpers.arrayElement(readerTypes)
    const isStudent = readerType.value === 'student'
    const isTeacher = readerType.value === 'teacher'
    const isStaff = readerType.value === 'staff'

    const currentBorrowCount = faker.number.int({ min: 0, max: readerType.maxBorrowCount })
    const totalBorrowCount = faker.number.int({ min: currentBorrowCount, max: 500 })
    const hasOverdue = faker.datatype.boolean({ probability: 0.1 })
    const overdueCount = hasOverdue ? faker.number.int({ min: 1, max: 3 }) : 0

    const reader = {
      id: i + 1,
      cardNo: `RD${String(i + 1).padStart(8, '0')}`,
      realName: faker.person.fullName(),
      gender: faker.helpers.arrayElement(['male', 'female']),
      phone: faker.phone.number('1##########'),
      email: faker.internet.email(),
      idCard: faker.string.numeric(18),
      readerType: readerType.value,
      readerTypeName: readerType.label,
      maxBorrowCount: readerType.maxBorrowCount,
      borrowDays: readerType.borrowDays,
      depositAmount: readerType.depositAmount,
      currentBorrowCount,
      totalBorrowCount,
      overdueCount,
      hasOverdue,
      status: hasOverdue && overdueCount > 2 ? 'suspended' :
              faker.datatype.boolean({ probability: 0.95 }) ? 'active' :
              faker.helpers.arrayElement(['suspended', 'expired']),
      cardExpireDate: faker.date.future({ years: 1 }).toISOString().split('T')[0],
      registeredDate: faker.date.past({ years: 3 }).toISOString().split('T')[0],
      lastBorrowDate: faker.date.recent({ days: 30 }).toISOString(),
      avatar: `https://i.pravatar.cc/150?img=${i + 1}`,
      address: faker.location.streetAddress(),
      remark: '',
      createdAt: faker.date.past({ years: 3 }).toISOString(),
      updatedAt: faker.date.recent().toISOString()
    }

    // 根据读者类型添加特定字段
    if (isStudent) {
      reader.grade = faker.helpers.arrayElement(grades)
      reader.class = faker.helpers.arrayElement(classes)
      reader.studentNo = `S${String(i + 1).padStart(8, '0')}`
    } else if (isTeacher || isStaff) {
      reader.department = faker.helpers.arrayElement(departments)
      reader.jobNo = `T${String(i + 1).padStart(6, '0')}`
      reader.title = faker.helpers.arrayElement(['助教', '讲师', '副教授', '教授'])
    }

    readers.push(reader)
  }

  return readers
}

// 生成读者统计数据
export function generateReaderStats() {
  return {
    totalReaders: 5000,
    activeReaders: 4500,
    suspendedReaders: 300,
    expiredReaders: 200,
    studentCount: 3500,
    teacherCount: 800,
    staffCount: 500,
    publicCount: 200,
    newReadersThisMonth: 150,
    activeReadersThisMonth: 2800
  }
}