import { setupWorker } from 'msw/browser'
import { authHandlers } from './handlers/auth'
import { booksHandlers } from './handlers/books'
import { readersHandlers } from './handlers/readers'
import { circulationHandlers } from './handlers/circulation'
import { analyticsHandlers } from './handlers/analytics'
import { recommendHandlers } from './handlers/recommend'
import { inventoryHandlers } from './handlers/inventory'
import { chatHandlers } from './handlers/chat'

// 合并所有handlers
export const handlers = [
  ...authHandlers,
  ...booksHandlers,
  ...readersHandlers,
  ...circulationHandlers,
  ...analyticsHandlers,
  ...recommendHandlers,
  ...inventoryHandlers,
  ...chatHandlers
]

console.log('[MSW] Loading', handlers.length, 'request handlers')
console.log('[MSW] Auth handlers:', authHandlers.length)

// 创建Service Worker
export const worker = setupWorker(...handlers)
