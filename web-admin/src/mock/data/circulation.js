import { faker } from '@faker-js/faker/locale/zh_CN'
import dayjs from 'dayjs'

// 流通状态
export const circulationStatus = {
  BORROWED: 'borrowed',    // 借出中
  RETURNED: 'returned',    // 已归还
  OVERDUE: 'overdue',      // 已逾期
  RENEWED: 'renewed',      // 已续借
  LOST: 'lost'             // 遗失
}

// 预约状态
export const reservationStatus = {
  PENDING: 'pending',      // 等待中
  READY: 'ready',          // 已到书
  PICKED_UP: 'picked_up', // 已取书
  CANCELLED: 'cancelled',  // 已取消
  EXPIRED: 'expired'       // 已过期
}

// 生成流通记录
export function generateCirculationRecords(count = 500) {
  const records = []

  for (let i = 0; i < count; i++) {
    const borrowDate = faker.date.past({ years: 1 })
    const borrowDays = faker.number.int({ min: 15, max: 60 })
    const dueDate = dayjs(borrowDate).add(borrowDays, 'day').toDate()
    const isReturned = faker.datatype.boolean({ probability: 0.7 })
    const returnDate = isReturned
      ? dayjs(borrowDate).add(faker.number.int({ min: 1, max: borrowDays + 10 }), 'day').toDate()
      : null
    const isOverdue = returnDate ? dayjs(returnDate).isAfter(dueDate) : dayjs().isAfter(dueDate)
    const overdueDays = isOverdue
      ? dayjs(returnDate || new Date()).diff(dueDate, 'day')
      : 0
    const renewCount = faker.number.int({ min: 0, max: 2 })

    records.push({
      id: i + 1,
      recordNo: `CR${String(i + 1).padStart(10, '0')}`,
      bookId: faker.number.int({ min: 1, max: 100 }),
      bookTitle: `图书${faker.number.int({ min: 1, max: 100 })}`,
      bookBarcode: `BK${String(faker.number.int({ min: 1, max: 100 })).padStart(8, '0')}`,
      readerId: faker.number.int({ min: 1, max: 200 }),
      readerName: faker.person.fullName(),
      readerCardNo: `RD${String(faker.number.int({ min: 1, max: 200 })).padStart(8, '0')}`,
      borrowDate: borrowDate.toISOString(),
      dueDate: dueDate.toISOString(),
      returnDate: returnDate ? returnDate.toISOString() : null,
      renewCount,
      isOverdue,
      overdueDays,
      overdueFine: isOverdue ? overdueDays * 0.1 : 0,
      status: !isReturned && isOverdue ? circulationStatus.OVERDUE :
              isReturned ? circulationStatus.RETURNED :
              circulationStatus.BORROWED,
      operatorId: faker.number.int({ min: 1, max: 10 }),
      operatorName: faker.person.fullName(),
      remark: '',
      createdAt: borrowDate.toISOString(),
      updatedAt: (returnDate || new Date()).toISOString()
    })
  }

  return records.sort((a, b) => new Date(b.borrowDate) - new Date(a.borrowDate))
}

// 生成预约记录
export function generateReservations(count = 100) {
  const reservations = []

  for (let i = 0; i < count; i++) {
    const reservationDate = faker.date.recent({ days: 30 })
    const status = faker.helpers.arrayElement([
      reservationStatus.PENDING,
      reservationStatus.READY,
      reservationStatus.PICKED_UP,
      reservationStatus.CANCELLED,
      reservationStatus.EXPIRED
    ])
    const readyDate = [reservationStatus.READY, reservationStatus.PICKED_UP].includes(status)
      ? dayjs(reservationDate).add(faker.number.int({ min: 1, max: 7 }), 'day').toDate()
      : null
    const pickupDate = status === reservationStatus.PICKED_UP
      ? dayjs(readyDate).add(faker.number.int({ min: 1, max: 3 }), 'day').toDate()
      : null
    const expireDate = readyDate
      ? dayjs(readyDate).add(3, 'day').toDate()
      : dayjs(reservationDate).add(7, 'day').toDate()

    reservations.push({
      id: i + 1,
      reservationNo: `RV${String(i + 1).padStart(10, '0')}`,
      bookId: faker.number.int({ min: 1, max: 100 }),
      bookTitle: `图书${faker.number.int({ min: 1, max: 100 })}`,
      bookBarcode: `BK${String(faker.number.int({ min: 1, max: 100 })).padStart(8, '0')}`,
      readerId: faker.number.int({ min: 1, max: 200 }),
      readerName: faker.person.fullName(),
      readerCardNo: `RD${String(faker.number.int({ min: 1, max: 200 })).padStart(8, '0')}`,
      reservationDate: reservationDate.toISOString(),
      readyDate: readyDate ? readyDate.toISOString() : null,
      pickupDate: pickupDate ? pickupDate.toISOString() : null,
      expireDate: expireDate.toISOString(),
      status,
      statusName: {
        [reservationStatus.PENDING]: '等待中',
        [reservationStatus.READY]: '已到书',
        [reservationStatus.PICKED_UP]: '已取书',
        [reservationStatus.CANCELLED]: '已取消',
        [reservationStatus.EXPIRED]: '已过期'
      }[status],
      queue: status === reservationStatus.PENDING ? faker.number.int({ min: 1, max: 5 }) : 0,
      remark: '',
      createdAt: reservationDate.toISOString(),
      updatedAt: (pickupDate || readyDate || new Date()).toISOString()
    })
  }

  return reservations.sort((a, b) => new Date(b.reservationDate) - new Date(a.reservationDate))
}

// 生成流通统计数据
export function generateCirculationStats() {
  return {
    totalBorrowed: 15000,
    totalReturned: 12500,
    currentBorrowed: 2500,
    overdueCount: 350,
    reservationCount: 180,
    todayBorrowed: 85,
    todayReturned: 92,
    thisMonthBorrowed: 1800,
    thisMonthReturned: 1650,
    averageBorrowDays: 25.5,
    circulationRate: 0.65
  }
}