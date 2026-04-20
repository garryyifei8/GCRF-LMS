import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import { useUserStore } from '@/stores/user'

// 路由配置
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '借阅概览', icon: 'House' }
      },
      {
        path: 'circulation',
        name: 'Circulation',
        meta: { title: '流通管理', icon: 'Coin' },
        children: [
          {
            path: 'borrow',
            name: 'CirculationBorrow',
            component: () => import('@/views/circulation/borrow.vue'),
            meta: { title: '图书借出' }
          },
          {
            path: 'return',
            name: 'CirculationReturn',
            component: () => import('@/views/circulation/return.vue'),
            meta: { title: '图书归还' }
          },
          {
            path: 'records',
            name: 'CirculationRecords',
            component: () => import('@/views/circulation/records.vue'),
            meta: { title: '流通记录' }
          },
          {
            path: 'reservations',
            name: 'CirculationReservations',
            component: () => import('@/views/circulation/reservations.vue'),
            meta: { title: '预约管理' }
          }
        ]
      },
      {
        path: 'books',
        name: 'Books',
        meta: { title: '图书管理', icon: 'Reading' },
        children: [
          {
            path: 'list',
            name: 'BooksList',
            component: () => import('@/views/books/list.vue'),
            meta: { title: '图书列表' }
          },
          {
            path: 'catalog',
            name: 'BooksCatalog',
            component: () => import('@/views/books/catalog.vue'),
            meta: { title: '图书编目' }
          },
          {
            path: 'collection',
            name: 'BooksCollection',
            component: () => import('@/views/books/collection.vue'),
            meta: { title: '图书典藏' }
          },
          {
            path: 'inventory',
            name: 'BooksInventory',
            component: () => import('@/views/books/inventory.vue'),
            meta: { title: '图书盘点' }
          }
        ]
      },
      {
        path: 'readers',
        name: 'Readers',
        meta: { title: '读者管理', icon: 'User' },
        children: [
          {
            path: 'students',
            name: 'ReadersStudents',
            component: () => import('@/views/readers/students.vue'),
            meta: { title: '学生读者' }
          },
          {
            path: 'teachers',
            name: 'ReadersTeachers',
            component: () => import('@/views/readers/teachers.vue'),
            meta: { title: '教师读者' }
          },
          {
            path: 'card',
            name: 'ReadersCard',
            component: () => import('@/views/readers/card.vue'),
            meta: { title: '读者证办理' }
          }
        ]
      },
      {
        path: 'ai',
        name: 'AI',
        meta: { title: 'AI智能功能', icon: 'MagicStick' },
        children: [
          {
            path: 'recommend',
            name: 'AIRecommend',
            component: () => import('@/views/ai/recommend.vue'),
            meta: { title: '智能推荐' }
          },
          {
            path: 'chat',
            name: 'AIChat',
            component: () => import('@/views/ai/chat.vue'),
            meta: { title: '智能问答' }
          },
          {
            path: 'analytics',
            name: 'AIAnalytics',
            component: () => import('@/views/ai/analytics.vue'),
            meta: { title: '数据分析' }
          }
        ]
      },
      {
        path: 'system',
        name: 'System',
        meta: { title: '系统管理', icon: 'Setting' },
        children: [
          {
            path: 'users',
            name: 'SystemUsers',
            component: () => import('@/views/system/users.vue'),
            meta: { title: '用户管理' }
          },
          {
            path: 'roles',
            name: 'SystemRoles',
            component: () => import('@/views/system/roles.vue'),
            meta: { title: '角色权限' }
          },
          {
            path: 'departments',
            name: 'SystemDepartments',
            component: () => import('@/views/system/departments.vue'),
            meta: { title: '部门管理' }
          },
          {
            path: 'config',
            name: 'SystemConfig',
            component: () => import('@/views/system/config.vue'),
            meta: { title: '系统配置' }
          },
          {
            path: 'backup',
            name: 'SystemBackup',
            component: () => import('@/views/system/backup.vue'),
            meta: { title: '数据备份' }
          }
        ]
      },
      {
        path: 'profile',
        name: 'Profile',
        meta: { title: '个人中心', icon: 'UserFilled' },
        children: [
          {
            path: 'info',
            name: 'ProfileInfo',
            component: () => import('@/views/profile/info.vue'),
            meta: { title: '个人信息' }
          },
          {
            path: 'password',
            name: 'ProfilePassword',
            component: () => import('@/views/profile/password.vue'),
            meta: { title: '修改密码' }
          }
        ]
      },
      {
        path: 'demo',
        name: 'Demo',
        meta: { title: 'UI组件演示', icon: 'Box' },
        children: [
          {
            path: 'components',
            name: 'DemoComponents',
            component: () => import('@/views/demo/components.vue'),
            meta: { title: '组件库演示' }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  NProgress.start()

  // 设置页面标题
  document.title = to.meta.title
    ? `${to.meta.title} - 国创睿峰智能图书馆管理系统`
    : '国创睿峰智能图书馆管理系统'

  // 权限验证
  const userStore = useUserStore()
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth !== false)

  if (requiresAuth && !userStore.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
