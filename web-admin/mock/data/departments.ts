import { generateDepartment } from './factory'

/**
 * 部门模拟数据
 */
export const mockDepartments = [
  // 根部门
  {
    id: 1,
    deptCode: 'ROOT',
    deptName: '国创睿峰图书馆',
    parentId: 0,
    deptType: 'root',
    leader: '张馆长',
    phone: '020-12345678',
    email: 'director@gcrf.com',
    status: 'active',
    sort: 1,
    description: '图书馆总部',
    createdTime: '2023-01-01T00:00:00.000Z',
    updatedTime: '2025-01-01T00:00:00.000Z'
  },
  // 一级部门
  {
    id: 2,
    deptCode: 'CIRC001',
    deptName: '图书流通部',
    parentId: 1,
    deptType: 'functional',
    leader: '李主任',
    phone: '020-12345679',
    email: 'circulation@gcrf.com',
    status: 'active',
    sort: 10,
    description: '负责图书借还、预约、续借等流通业务',
    createdTime: '2023-01-15T00:00:00.000Z',
    updatedTime: '2025-01-10T00:00:00.000Z'
  },
  {
    id: 3,
    deptCode: 'CAT001',
    deptName: '图书编目部',
    parentId: 1,
    deptType: 'functional',
    leader: '王主任',
    phone: '020-12345680',
    email: 'cataloging@gcrf.com',
    status: 'active',
    sort: 20,
    description: '负责图书分类、编目、入库管理',
    createdTime: '2023-01-15T00:00:00.000Z',
    updatedTime: '2025-01-10T00:00:00.000Z'
  },
  {
    id: 4,
    deptCode: 'REF001',
    deptName: '参考咨询部',
    parentId: 1,
    deptType: 'business',
    leader: '赵主任',
    phone: '020-12345681',
    email: 'reference@gcrf.com',
    status: 'active',
    sort: 30,
    description: '提供信息咨询、文献检索、学科服务',
    createdTime: '2023-01-15T00:00:00.000Z',
    updatedTime: '2025-01-10T00:00:00.000Z'
  },
  {
    id: 5,
    deptCode: 'TECH001',
    deptName: '技术服务部',
    parentId: 1,
    deptType: 'support',
    leader: '刘主任',
    phone: '020-12345682',
    email: 'tech@gcrf.com',
    status: 'active',
    sort: 40,
    description: '负责图书馆信息系统维护、技术支持',
    createdTime: '2023-01-15T00:00:00.000Z',
    updatedTime: '2025-01-10T00:00:00.000Z'
  },
  {
    id: 6,
    deptCode: 'ADMIN001',
    deptName: '行政管理部',
    parentId: 1,
    deptType: 'support',
    leader: '陈主任',
    phone: '020-12345683',
    email: 'admin@gcrf.com',
    status: 'active',
    sort: 50,
    description: '负责人事、财务、后勤等行政管理',
    createdTime: '2023-01-15T00:00:00.000Z',
    updatedTime: '2025-01-10T00:00:00.000Z'
  },
  // 二级部门 - 图书流通部下属
  {
    id: 7,
    deptCode: 'CIRC002',
    deptName: '借阅服务组',
    parentId: 2,
    deptType: 'functional',
    leader: '周组长',
    phone: '020-12345684',
    email: 'borrowing@gcrf.com',
    status: 'active',
    sort: 11,
    description: '负责一线借还书服务',
    createdTime: '2023-02-01T00:00:00.000Z',
    updatedTime: '2025-01-10T00:00:00.000Z'
  },
  {
    id: 8,
    deptCode: 'CIRC003',
    deptName: '读者服务组',
    parentId: 2,
    deptType: 'functional',
    leader: '吴组长',
    phone: '020-12345685',
    email: 'reader@gcrf.com',
    status: 'active',
    sort: 12,
    description: '负责读者卡办理、读者培训',
    createdTime: '2023-02-01T00:00:00.000Z',
    updatedTime: '2025-01-10T00:00:00.000Z'
  },
  // 二级部门 - 技术服务部下属
  {
    id: 9,
    deptCode: 'TECH002',
    deptName: '系统运维组',
    parentId: 5,
    deptType: 'support',
    leader: '郑组长',
    phone: '020-12345686',
    email: 'ops@gcrf.com',
    status: 'active',
    sort: 41,
    description: '负责系统日常运维、监控',
    createdTime: '2023-02-01T00:00:00.000Z',
    updatedTime: '2025-01-10T00:00:00.000Z'
  },
  {
    id: 10,
    deptCode: 'TECH003',
    deptName: '数据管理组',
    parentId: 5,
    deptType: 'support',
    leader: '孙组长',
    phone: '020-12345687',
    email: 'data@gcrf.com',
    status: 'active',
    sort: 42,
    description: '负责数据库管理、数据分析',
    createdTime: '2023-02-01T00:00:00.000Z',
    updatedTime: '2025-01-10T00:00:00.000Z'
  }
]

// 生成额外的随机部门数据（用于分页测试）
for (let i = 11; i <= 30; i++) {
  mockDepartments.push(generateDepartment(i, i % 3 === 0 ? 1 : (i % 2 === 0 ? 2 : 5)))
}

export const findDepartmentById = (id: number) => {
  return mockDepartments.find(dept => dept.id === id)
}

export const findDepartmentsByParentId = (parentId: number) => {
  return mockDepartments.filter(dept => dept.parentId === parentId)
}
