import { http, HttpResponse } from 'msw'

const messages = [
  {
    id: 1,
    sessionId: 'demo-session',
    role: 'user',
    content: '请推荐一本编程书',
    createTime: '2026-04-01 10:00:00'
  },
  {
    id: 2,
    sessionId: 'demo-session',
    role: 'assistant',
    content: '推荐《代码大全》和《设计模式》',
    createTime: '2026-04-01 10:00:05'
  }
]

const hotQuestions = [
  { id: 1, question: '如何借书？', count: 120 },
  { id: 2, question: '逾期罚款规则？', count: 95 },
  { id: 3, question: '图书馆开放时间？', count: 78 }
]

export const chatHandlers = [
  http.post('/api/v1/chat/message', async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        id: messages.length + 1,
        sessionId: body.sessionId || 'mock-session',
        role: 'assistant',
        content: '这是 mock 回复：' + (body.content || ''),
        createTime: new Date().toISOString()
      }
    })
  }),

  http.get('/api/v1/chat/history/:sessionId', ({ params }) => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: messages.filter((m) => m.sessionId === params.sessionId)
    })
  }),

  http.post('/api/v1/chat/feedback', () =>
    HttpResponse.json({
      code: 200,
      message: 'success'
    })
  ),

  http.get('/api/v1/chat/hot-questions', ({ request }) => {
    const url = new URL(request.url)
    const limit = Number(url.searchParams.get('limit') || 10)
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: hotQuestions.slice(0, limit)
    })
  }),

  http.get('/api/v1/chat/stats', () =>
    HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        totalSessions: 156,
        totalMessages: 892,
        avgResponseTime: 1.2,
        satisfactionRate: 0.92
      }
    })
  ),

  http.post('/api/v1/chat/cache/refresh', () =>
    HttpResponse.json({
      code: 200,
      message: 'success'
    })
  )
]
