import { generateReader } from './factory'

/**
 * 读者模拟数据
 * 包含学生读者和教师读者
 */

// 生成70个读者(50个学生 + 20个教师)
export const mockReaders = Array.from({ length: 70 }, (_, i) => {
  const reader = generateReader(i + 1)

  // 前50个是学生,后20个是教师
  if (i < 50) {
    return {
      ...reader,
      readerType: 'student',
      maxBorrowCount: 10,
      department: ['计算机学院', '文学院', '经济管理学院', '外语学院', '机械工程学院', '艺术学院', '法学院', '数学系'][i % 8],
      grade: ['一年级1班', '二年级1班', '三年级1班', '四年级1班', '五年级1班'][i % 5],
      studentId: `2020${(100001 + i).toString()}`
    }
  } else {
    return {
      ...reader,
      readerType: 'teacher',
      maxBorrowCount: 20,
      department: ['计算机学院', '文学院', '物理系', '数学系', '化学系'][i % 5],
      title: ['教授', '副教授', '讲师', '助教'][i % 4],
      teacherId: `T${(1001 + i - 50).toString()}`
    }
  }
})

export const findReaderById = (id: number) => {
  return mockReaders.find(reader => reader.readerId === id)
}

export const findReaderByCardNumber = (cardNumber: string) => {
  return mockReaders.find(reader => reader.cardNumber === cardNumber)
}
