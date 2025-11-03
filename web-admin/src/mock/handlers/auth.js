import { http, HttpResponse } from 'msw'

// 模拟用户数据
const users = [
  {
    id: 1,
    username: 'admin',
    password: 'admin123', // 实际应用中应该加密
    realName: '系统管理员',
    role: 'admin',
    roleName: '管理员',
    avatar: 'https://i.pravatar.cc/150?img=1',
    email: 'admin@gcrf.com',
    phone: '13800138000',
    status: 'active',
    permissions: ['*']
  },
  {
    id: 2,
    username: 'librarian',
    password: 'lib123',
    realName: '图书管理员',
    role: 'librarian',
    roleName: '馆员',
    avatar: 'https://i.pravatar.cc/150?img=2',
    email: 'librarian@gcrf.com',
    phone: '13800138001',
    status: 'active',
    permissions: ['circulation:*', 'books:read', 'readers:read']
  },
  {
    id: 3,
    username: 'operator',
    password: 'op123',
    realName: '操作员',
    role: 'operator',
    roleName: '操作员',
    avatar: 'https://i.pravatar.cc/150?img=3',
    email: 'operator@gcrf.com',
    phone: '13800138002',
    status: 'active',
    permissions: ['circulation:borrow', 'circulation:return', 'readers:read']
  }
]

// 模拟token存储
let tokens = new Map()

export const authHandlers = [
  // 登录
  http.post('/api/v1/auth/login', async ({ request }) => {
    const { username, password } = await request.json()

    const user = users.find(u => u.username === username && u.password === password)

    if (!user) {
      return HttpResponse.json({
        code: 401,
        message: '用户名或密码错误'
      }, { status: 401 })
    }

    if (user.status !== 'active') {
      return HttpResponse.json({
        code: 403,
        message: '账号已被禁用'
      }, { status: 403 })
    }

    // 生成token(实际应该使用JWT)
    const token = `mock-token-${user.id}-${Date.now()}`
    tokens.set(token, user)

    // 不返回密码，构造符合前端期望的数据结构
    const { password: _, id, ...restUserInfo } = user

    return HttpResponse.json({
      code: 200,
      message: '登录成功',
      data: {
        token,
        user: {
          userId: id,  // 前端期望的字段名
          ...restUserInfo
        }
      }
    })
  }),

  // 退出登录
  http.post('/api/v1/auth/logout', ({ request }) => {
    const token = request.headers.get('Authorization')?.replace('Bearer ', '')
    if (token) {
      tokens.delete(token)
    }

    return HttpResponse.json({
      code: 200,
      message: '退出成功'
    })
  }),

  // 获取当前用户信息
  http.get('/api/v1/auth/user/info', ({ request }) => {
    const token = request.headers.get('Authorization')?.replace('Bearer ', '')

    if (!token || !tokens.has(token)) {
      return HttpResponse.json({
        code: 401,
        message: '未登录或登录已过期'
      }, { status: 401 })
    }

    const user = tokens.get(token)
    const { password: _, ...userInfo } = user

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: userInfo
    })
  }),

  // 修改密码
  http.post('/api/v1/auth/change-password', async ({ request }) => {
    const token = request.headers.get('Authorization')?.replace('Bearer ', '')
    const { oldPassword, newPassword } = await request.json()

    if (!token || !tokens.has(token)) {
      return HttpResponse.json({
        code: 401,
        message: '未登录或登录已过期'
      }, { status: 401 })
    }

    const user = tokens.get(token)

    if (user.password !== oldPassword) {
      return HttpResponse.json({
        code: 400,
        message: '原密码错误'
      }, { status: 400 })
    }

    // 更新密码
    const userIndex = users.findIndex(u => u.id === user.id)
    users[userIndex].password = newPassword

    return HttpResponse.json({
      code: 200,
      message: '密码修改成功'
    })
  }),

  // 更新用户信息
  http.put('/api/v1/auth/user/info', async ({ request }) => {
    const token = request.headers.get('Authorization')?.replace('Bearer ', '')
    const updateData = await request.json()

    if (!token || !tokens.has(token)) {
      return HttpResponse.json({
        code: 401,
        message: '未登录或登录已过期'
      }, { status: 401 })
    }

    const user = tokens.get(token)
    const userIndex = users.findIndex(u => u.id === user.id)

    // 更新用户信息(除了密码和权限)
    const { password, permissions, ...allowedUpdates } = updateData
    users[userIndex] = {
      ...users[userIndex],
      ...allowedUpdates
    }

    tokens.set(token, users[userIndex])

    const { password: _, ...userInfo } = users[userIndex]

    return HttpResponse.json({
      code: 200,
      message: '信息更新成功',
      data: userInfo
    })
  })
]
