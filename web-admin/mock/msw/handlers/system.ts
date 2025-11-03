import { http, HttpResponse, delay } from 'msw'
import { mockDepartments, findDepartmentById } from '../../data/departments'
import { successResponse, errorResponse, pageResponse, generateDepartment } from '../../data/factory'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const systemHandlers = [
  // 获取部门列表（分页）
  http.get(`${BASE_URL}/api/v1/system/departments`, async ({ request }) => {
    await delay(300)

    const url = new URL(request.url)
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
    const pageSize = parseInt(url.searchParams.get('pageSize') || '20')
    const keyword = url.searchParams.get('keyword') || ''
    const status = url.searchParams.get('status') || ''

    // 过滤数据
    let filteredDepts = [...mockDepartments]

    if (keyword) {
      filteredDepts = filteredDepts.filter(dept =>
        dept.deptName.includes(keyword) ||
        dept.deptCode.includes(keyword) ||
        dept.leader?.includes(keyword)
      )
    }

    if (status) {
      filteredDepts = filteredDepts.filter(dept => dept.status === status)
    }

    // 分页
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    const records = filteredDepts.slice(start, end)

    return HttpResponse.json(
      pageResponse(records, filteredDepts.length, pageNum, pageSize)
    )
  }),

  // 获取部门树形结构
  http.get(`${BASE_URL}/api/v1/system/departments/tree`, async () => {
    await delay(200)

    // 构建树形结构
    const buildTree = (parentId: number): any[] => {
      return mockDepartments
        .filter(dept => dept.parentId === parentId)
        .map(dept => ({
          ...dept,
          children: buildTree(dept.id)
        }))
    }

    const tree = buildTree(0)

    return HttpResponse.json(successResponse(tree))
  }),

  // 获取部门详情
  http.get(`${BASE_URL}/api/v1/system/departments/:id`, async ({ params }) => {
    await delay(200)

    const id = parseInt(params.id as string)
    const dept = findDepartmentById(id)

    if (!dept) {
      return HttpResponse.json(
        errorResponse(404, '部门不存在'),
        { status: 404 }
      )
    }

    return HttpResponse.json(successResponse(dept))
  }),

  // 创建部门
  http.post(`${BASE_URL}/api/v1/system/departments`, async ({ request }) => {
    await delay(400)

    const body = await request.json() as any

    // 验证必填字段
    if (!body.deptCode || !body.deptName) {
      return HttpResponse.json(
        errorResponse(400, '部门编码和名称不能为空'),
        { status: 400 }
      )
    }

    // 检查编码是否重复
    const exists = mockDepartments.find(dept => dept.deptCode === body.deptCode)
    if (exists) {
      return HttpResponse.json(
        errorResponse(400, '部门编码已存在'),
        { status: 400 }
      )
    }

    // 创建新部门
    const newDept = {
      id: Math.max(...mockDepartments.map(d => d.id)) + 1,
      deptCode: body.deptCode,
      deptName: body.deptName,
      parentId: body.parentId || 0,
      deptType: body.deptType || 'functional',
      leader: body.leader || '',
      phone: body.phone || '',
      email: body.email || '',
      status: body.status || 'active',
      sort: body.sort || 100,
      description: body.description || '',
      createdTime: new Date().toISOString(),
      updatedTime: new Date().toISOString()
    }

    mockDepartments.push(newDept)

    return HttpResponse.json(successResponse(newDept), { status: 201 })
  }),

  // 更新部门
  http.put(`${BASE_URL}/api/v1/system/departments/:id`, async ({ params, request }) => {
    await delay(400)

    const id = parseInt(params.id as string)
    const body = await request.json() as any

    const index = mockDepartments.findIndex(dept => dept.id === id)
    if (index === -1) {
      return HttpResponse.json(
        errorResponse(404, '部门不存在'),
        { status: 404 }
      )
    }

    // 检查编码是否与其他部门重复
    if (body.deptCode) {
      const exists = mockDepartments.find(
        dept => dept.deptCode === body.deptCode && dept.id !== id
      )
      if (exists) {
        return HttpResponse.json(
          errorResponse(400, '部门编码已存在'),
          { status: 400 }
        )
      }
    }

    // 不允许将部门的父部门设置为自己或自己的子部门
    if (body.parentId === id) {
      return HttpResponse.json(
        errorResponse(400, '不能将自己设置为父部门'),
        { status: 400 }
      )
    }

    // 更新部门
    mockDepartments[index] = {
      ...mockDepartments[index],
      ...body,
      id, // 保持ID不变
      updatedTime: new Date().toISOString()
    }

    return HttpResponse.json(successResponse(mockDepartments[index]))
  }),

  // 删除部门
  http.delete(`${BASE_URL}/api/v1/system/departments/:id`, async ({ params }) => {
    await delay(300)

    const id = parseInt(params.id as string)
    const index = mockDepartments.findIndex(dept => dept.id === id)

    if (index === -1) {
      return HttpResponse.json(
        errorResponse(404, '部门不存在'),
        { status: 404 }
      )
    }

    // 检查是否有子部门
    const hasChildren = mockDepartments.some(dept => dept.parentId === id)
    if (hasChildren) {
      return HttpResponse.json(
        errorResponse(400, '该部门下有子部门，不能删除'),
        { status: 400 }
      )
    }

    // 删除部门
    mockDepartments.splice(index, 1)

    return HttpResponse.json(successResponse(null))
  }),

  // 批量删除部门
  http.delete(`${BASE_URL}/api/v1/system/departments/batch`, async ({ request }) => {
    await delay(400)

    const body = await request.json() as any
    const ids = body.ids as number[]

    if (!ids || ids.length === 0) {
      return HttpResponse.json(
        errorResponse(400, '请选择要删除的部门'),
        { status: 400 }
      )
    }

    // 检查是否有子部门
    for (const id of ids) {
      const hasChildren = mockDepartments.some(dept => dept.parentId === id)
      if (hasChildren) {
        return HttpResponse.json(
          errorResponse(400, `部门ID ${id} 下有子部门，不能删除`),
          { status: 400 }
        )
      }
    }

    // 批量删除
    ids.forEach(id => {
      const index = mockDepartments.findIndex(dept => dept.id === id)
      if (index !== -1) {
        mockDepartments.splice(index, 1)
      }
    })

    return HttpResponse.json(successResponse({ deletedCount: ids.length }))
  })
]
