import { generateBook } from './factory'

/**
 * 图书模拟数据
 */
export const mockBooks = Array.from({ length: 100 }, (_, i) => generateBook(i + 1))

export const findBookById = (id: number) => {
  return mockBooks.find(book => book.bookId === id)
}
