import { http, HttpResponse, delay } from 'msw'
import {
  findUserByUsername,
  generateMockToken,
  validateToken
} from '../../data/auth'
import { successResponse, errorResponse } from '../../data/factory'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const authHandlers = [
  // 用户登录
  http.post(`${BASE_URL}/api/v1/auth/login`, async ({ request }) => {
    await delay(300) // 模拟网络延迟

    const body = await request.json() as any
    const { username, password } = body

    // 查找用户
    const user = findUserByUsername(username)

    if (!user) {
      return HttpResponse.json(
        errorResponse(401, '用户名或密码错误'),
        { status: 401 }
      )
    }

    if (user.password !== password) {
      return HttpResponse.json(
        errorResponse(401, '用户名或密码错误'),
        { status: 401 }
      )
    }

    if (user.status !== 'active') {
      return HttpResponse.json(
        errorResponse(403, '账号已被禁用'),
        { status: 403 }
      )
    }

    // 生成Token
    const { token, refreshToken } = generateMockToken(user)

    // 返回登录响应（不包含密码）
    const { password: _, ...userWithoutPassword } = user

    return HttpResponse.json(
      successResponse({
        token,
        refreshToken,
        expiresIn: 7200,
        user: userWithoutPassword
      })
    )
  }),

  // 用户登出
  http.post(`${BASE_URL}/api/v1/auth/logout`, async () => {
    await delay(200)
    return HttpResponse.json(successResponse(null))
  }),

  // 刷新Token
  http.post(`${BASE_URL}/api/v1/auth/refresh`, async ({ request }) => {
    await delay(200)

    const body = await request.json() as any
    const { refreshToken } = body

    // 简单验证（实际应该验证refreshToken）
    if (!refreshToken) {
      return HttpResponse.json(
        errorResponse(401, '无效的刷新令牌'),
        { status: 401 }
      )
    }

    // 生成新Token
    const token = `mock_token_new_${Date.now()}`
    const newRefreshToken = `mock_refresh_new_${Date.now()}`

    return HttpResponse.json(
      successResponse({
        token,
        refreshToken: newRefreshToken,
        expiresIn: 7200
      })
    )
  }),

  // 获取当前用户信息
  http.get(`${BASE_URL}/api/v1/auth/user/info`, async ({ request }) => {
    await delay(200)

    const authHeader = request.headers.get('Authorization')
    if (!authHeader) {
      return HttpResponse.json(
        errorResponse(401, '未授权'),
        { status: 401 }
      )
    }

    const token = authHeader.replace('Bearer ', '')
    const tokenData = validateToken(token)

    if (!tokenData) {
      return HttpResponse.json(
        errorResponse(401, '无效的令牌'),
        { status: 401 }
      )
    }

    const { password: _, ...userWithoutPassword } = tokenData.user

    return HttpResponse.json(successResponse(userWithoutPassword))
  }),

  // 修改密码
  http.put(`${BASE_URL}/api/v1/auth/password`, async ({ request }) => {
    await delay(300)

    const body = await request.json() as any
    const { oldPassword, newPassword } = body

    const authHeader = request.headers.get('Authorization')
    if (!authHeader) {
      return HttpResponse.json(
        errorResponse(401, '未授权'),
        { status: 401 }
      )
    }

    // 简单验证
    if (!oldPassword || !newPassword) {
      return HttpResponse.json(
        errorResponse(400, '参数错误'),
        { status: 400 }
      )
    }

    return HttpResponse.json(successResponse(null))
  })
]
