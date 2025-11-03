import { setupWorker } from 'msw/browser'
import { authHandlers } from './handlers/auth'
import { systemHandlers } from './handlers/system'
import { analyticsHandlers } from './handlers/analytics'
import { booksHandlers } from './handlers/books'
import { readersHandlers } from './handlers/readers'

// 合并所有handlers
export const handlers = [
  ...authHandlers,
  ...systemHandlers,
  ...analyticsHandlers,
  ...booksHandlers,
  ...readersHandlers
]

// 创建MSW worker
export const worker = setupWorker(...handlers)

/**
 * 启动Mock服务器
 */
export const startMockServer = async () => {
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    try {
      await worker.start({
        onUnhandledRequest: 'warn',
        serviceWorker: {
          url: '/mockServiceWorker.js'
        }
      })
      console.log('%c🚀 Mock Server Started', 'color: #00b96b; font-weight: bold; font-size: 14px')
      console.log('%c📦 Mock API Base URL:', 'color: #1677ff; font-weight: bold', import.meta.env.VITE_API_BASE_URL)
      console.log('%c✅ Handlers registered:', 'color: #1677ff; font-weight: bold', handlers.length)
    } catch (error) {
      console.error('Failed to start Mock Server:', error)
    }
  }
}

/**
 * 停止Mock服务器
 */
export const stopMockServer = () => {
  worker.stop()
  console.log('🛑 Mock Server Stopped')
}
