import { Random } from 'mockjs'

// 模拟用户数据
const users = [
  {
    id: 1,
    username: 'admin',
    password: '123456',
    name: '管理员',
    role: 'admin',
    avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
    token: 'admin-token-' + Random.guid()
  },
  {
    id: 2,
    username: 'librarian',
    password: '123456',
    name: '图书管理员',
    role: 'librarian',
    avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
    token: 'librarian-token-' + Random.guid()
  }
]

export default [
  // 登录
  {
    url: '/api/auth/login',
    method: 'post',
    response: ({ body }) => {
      const { username, password } = body
      const user = users.find(u => u.username === username && u.password === password)

      if (user) {
        return {
          code: 200,
          message: '登录成功',
          data: {
            token: user.token,
            userInfo: {
              id: user.id,
              username: user.username,
              name: user.name,
              role: user.role,
              avatar: user.avatar
            }
          }
        }
      } else {
        return {
          code: 401,
          message: '用户名或密码错误',
          data: null
        }
      }
    }
  },

  // 获取用户信息
  {
    url: '/api/auth/userInfo',
    method: 'get',
    response: ({ headers }) => {
      const token = headers.authorization?.replace('Bearer ', '')
      const user = users.find(u => u.token === token)

      if (user) {
        return {
          code: 200,
          message: '成功',
          data: {
            id: user.id,
            username: user.username,
            name: user.name,
            role: user.role,
            avatar: user.avatar
          }
        }
      } else {
        return {
          code: 401,
          message: '未授权',
          data: null
        }
      }
    }
  },

  // 登出
  {
    url: '/api/auth/logout',
    method: 'post',
    response: () => {
      return {
        code: 200,
        message: '登出成功',
        data: null
      }
    }
  }
]
