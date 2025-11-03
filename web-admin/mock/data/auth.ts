/**
 * 认证相关模拟数据
 */

// 模拟用户数据库
export const mockUsers = [
  {
    userId: 1,
    username: 'admin',
    password: 'admin123', // 实际应用中应该是加密后的密码
    realName: '系统管理员',
    email: 'admin@gcrf.com',
    phone: '13800138000',
    avatar: 'https://picsum.photos/200',
    deptId: 1,
    deptName: '国创睿峰图书馆',
    position: '馆长',
    role: 'ADMIN',
    status: 'active',
    permissions: [
      'system:*',
      'book:*',
      'circulation:*',
      'reader:*',
      'analytics:*'
    ]
  },
  {
    userId: 2,
    username: 'librarian',
    password: 'librarian123',
    realName: '图书管理员',
    email: 'librarian@gcrf.com',
    phone: '13800138001',
    avatar: 'https://picsum.photos/201',
    deptId: 2,
    deptName: '图书流通部',
    position: '馆员',
    role: 'LIBRARIAN',
    status: 'active',
    permissions: [
      'book:read',
      'circulation:*',
      'reader:read',
      'reader:create'
    ]
  },
  {
    userId: 3,
    username: 'director',
    password: 'director123',
    realName: '部门主任',
    email: 'director@gcrf.com',
    phone: '13800138002',
    avatar: 'https://picsum.photos/202',
    deptId: 2,
    deptName: '图书流通部',
    position: '主任',
    role: 'DIRECTOR',
    status: 'active',
    permissions: [
      'book:read',
      'circulation:read',
      'reader:read',
      'analytics:read'
    ]
  }
]

// 模拟Token存储
export const mockTokens: Record<string, { token: string; refreshToken: string; user: any }> = {}

// 生成模拟Token
export const generateMockToken = (user: any) => {
  const token = `mock_token_${user.userId}_${Date.now()}`
  const refreshToken = `mock_refresh_${user.userId}_${Date.now()}`

  mockTokens[token] = {
    token,
    refreshToken,
    user
  }

  return { token, refreshToken }
}

// 验证Token
export const validateToken = (token: string) => {
  return mockTokens[token]
}

// 查找用户
export const findUserByUsername = (username: string) => {
  return mockUsers.find(u => u.username === username)
}

export const findUserById = (userId: number) => {
  return mockUsers.find(u => u.userId === userId)
}
