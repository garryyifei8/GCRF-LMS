import { http, HttpResponse } from 'msw'

const users = [
  {
    id: 1,
    username: 'admin',
    realName: '管理员',
    email: 'admin@gcrf.com',
    phone: '13800000000',
    status: 1,
    roleId: 1,
    roleName: '超级管理员',
    deptId: 1,
    deptName: 'IT部',
    createTime: '2026-01-01 10:00:00'
  },
  {
    id: 2,
    username: 'librarian',
    realName: '王图书',
    email: 'lib@gcrf.com',
    phone: '13800000001',
    status: 1,
    roleId: 2,
    roleName: '图书管理员',
    deptId: 2,
    deptName: '图书馆',
    createTime: '2026-01-02 10:00:00'
  },
  {
    id: 3,
    username: 'reader',
    realName: '李读者',
    email: 'reader@gcrf.com',
    phone: '13800000002',
    status: 0,
    roleId: 3,
    roleName: '普通读者',
    deptId: 3,
    deptName: '前台',
    createTime: '2026-01-03 10:00:00'
  }
]

const roles = [
  {
    id: 1,
    roleName: '超级管理员',
    roleCode: 'SUPER_ADMIN',
    description: '系统超级管理员',
    status: 1,
    createTime: '2026-01-01'
  },
  {
    id: 2,
    roleName: '图书管理员',
    roleCode: 'LIBRARIAN',
    description: '图书管理',
    status: 1,
    createTime: '2026-01-01'
  },
  {
    id: 3,
    roleName: '普通读者',
    roleCode: 'READER',
    description: '普通读者',
    status: 1,
    createTime: '2026-01-01'
  }
]

const permissions = [
  { id: 1, name: '系统管理', code: 'system', type: 'menu', parentId: 0, path: '/system' },
  {
    id: 2,
    name: '用户管理',
    code: 'system:users',
    type: 'menu',
    parentId: 1,
    path: '/system/users'
  },
  {
    id: 3,
    name: '角色管理',
    code: 'system:roles',
    type: 'menu',
    parentId: 1,
    path: '/system/roles'
  }
]

const departments = [
  {
    id: 1,
    deptName: 'IT部',
    parentId: 0,
    leader: '张三',
    phone: '13800000000',
    email: 'it@gcrf.com',
    status: 1,
    sort: 1
  },
  {
    id: 2,
    deptName: '图书馆',
    parentId: 0,
    leader: '李四',
    phone: '13800000001',
    email: 'lib@gcrf.com',
    status: 1,
    sort: 2
  },
  {
    id: 3,
    deptName: '前台',
    parentId: 2,
    leader: '王五',
    phone: '13800000002',
    email: 'desk@gcrf.com',
    status: 1,
    sort: 3
  }
]

const buildPaginated = (list, request) => {
  const url = new URL(request.url)
  const pageNum = Number(url.searchParams.get('pageNum') || 1)
  const pageSize = Number(url.searchParams.get('pageSize') || 10)
  const keyword = url.searchParams.get('keyword') || ''
  const filtered = keyword ? list.filter((item) => JSON.stringify(item).includes(keyword)) : list
  return {
    code: 200,
    message: 'success',
    data: {
      records: filtered.slice((pageNum - 1) * pageSize, pageNum * pageSize),
      total: filtered.length,
      pageNum,
      pageSize
    }
  }
}

export const systemHandlers = [
  // ===== Users =====
  http.get('/api/v1/system/users', ({ request }) =>
    HttpResponse.json(buildPaginated(users, request))
  ),

  // IMPORTANT: specific routes before general /:id pattern
  http.delete('/api/v1/system/users/batch', () =>
    HttpResponse.json({ code: 200, message: 'success' })
  ),

  http.get('/api/v1/system/users/:id', ({ params }) => {
    const user = users.find((u) => u.id === Number(params.id))
    return user
      ? HttpResponse.json({ code: 200, data: user })
      : HttpResponse.json({ code: 404, message: 'Not found' }, { status: 404 })
  }),

  http.post('/api/v1/system/users', async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: { id: users.length + 1, ...body }
    })
  }),

  http.put('/api/v1/system/users/:id/password/reset', () =>
    HttpResponse.json({ code: 200, message: 'success' })
  ),
  http.put('/api/v1/system/users/:id/status', () =>
    HttpResponse.json({ code: 200, message: 'success' })
  ),
  http.put('/api/v1/system/users/:id', () => HttpResponse.json({ code: 200, message: 'success' })),
  http.delete('/api/v1/system/users/:id', () =>
    HttpResponse.json({ code: 200, message: 'success' })
  ),

  // ===== Roles =====
  // IMPORTANT: /roles/all before /roles/:id
  http.get('/api/v1/system/roles/all', () => HttpResponse.json({ code: 200, data: roles })),

  http.get('/api/v1/system/roles', ({ request }) =>
    HttpResponse.json(buildPaginated(roles, request))
  ),

  http.get('/api/v1/system/roles/:id/permissions', () =>
    HttpResponse.json({ code: 200, data: [1, 2, 3] })
  ),

  http.get('/api/v1/system/roles/:id', ({ params }) => {
    const role = roles.find((r) => r.id === Number(params.id))
    return role
      ? HttpResponse.json({ code: 200, data: role })
      : HttpResponse.json({ code: 404, message: 'Not found' }, { status: 404 })
  }),

  http.post('/api/v1/system/roles', async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ code: 200, data: { id: roles.length + 1, ...body } })
  }),

  http.put('/api/v1/system/roles/:id/permissions', () =>
    HttpResponse.json({ code: 200, message: 'success' })
  ),
  http.put('/api/v1/system/roles/:id', () => HttpResponse.json({ code: 200, message: 'success' })),
  http.delete('/api/v1/system/roles/:id', () =>
    HttpResponse.json({ code: 200, message: 'success' })
  ),

  // ===== Permissions =====
  // IMPORTANT: /permissions/tree before /permissions
  http.get('/api/v1/system/permissions/tree', () =>
    HttpResponse.json({ code: 200, data: permissions })
  ),
  http.get('/api/v1/system/permissions', () => HttpResponse.json({ code: 200, data: permissions })),

  // ===== Departments =====
  // IMPORTANT: /departments/tree before /departments/:id
  http.get('/api/v1/system/departments/tree', () =>
    HttpResponse.json({ code: 200, data: departments })
  ),
  http.get('/api/v1/system/departments', () => HttpResponse.json({ code: 200, data: departments })),

  http.get('/api/v1/system/departments/:id', ({ params }) => {
    const dept = departments.find((d) => d.id === Number(params.id))
    return dept
      ? HttpResponse.json({ code: 200, data: dept })
      : HttpResponse.json({ code: 404, message: 'Not found' }, { status: 404 })
  }),

  http.post('/api/v1/system/departments', async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ code: 200, data: { id: departments.length + 1, ...body } })
  }),

  http.put('/api/v1/system/departments/:id', () =>
    HttpResponse.json({ code: 200, message: 'success' })
  ),
  http.put('/api/v1/system/departments', () =>
    HttpResponse.json({ code: 200, message: 'success' })
  ),
  http.delete('/api/v1/system/departments/:id', () =>
    HttpResponse.json({ code: 200, message: 'success' })
  )
]
