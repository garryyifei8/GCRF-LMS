<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '260px'" class="main-aside">
      <div class="logo-container" :class="{ 'is-collapse': isCollapse }">
        <div class="logo-wrapper">
          <div class="logo-icon">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="8" fill="url(#gradient)" />
              <path d="M16 8L8 12V20L16 24L24 20V12L16 8Z" fill="white" />
              <defs>
                <linearGradient id="gradient" x1="0" y1="0" x2="32" y2="32">
                  <stop offset="0%" stop-color="#667eea" />
                  <stop offset="100%" stop-color="#764ba2" />
                </linearGradient>
              </defs>
            </svg>
          </div>
          <div v-if="!isCollapse" class="logo-content">
            <div class="logo-text">Modernize</div>
            <span class="logo-subtitle">图书馆管理系统</span>
          </div>
        </div>
      </div>

      <el-scrollbar class="menu-scrollbar">
        <div v-for="group in menuGroups" :key="group.title" class="menu-group">
          <div v-if="!isCollapse" class="menu-group-title">{{ group.title }}</div>

          <el-menu
            :default-active="activeMenu"
            :collapse="isCollapse"
            :collapse-transition="false"
            :router="true"
            :unique-opened="false"
            background-color="transparent"
            text-color="#2A3547"
            active-text-color="#5D87FF"
          >
            <template v-for="route in group.routes" :key="route.path">
              <!-- 一级菜单 (有子菜单) -->
              <el-sub-menu v-if="route.children && route.children.length > 0" :index="route.path">
                <template #title>
                  <el-icon v-if="route.meta?.icon">
                    <component :is="route.meta.icon" />
                  </el-icon>
                  <span>{{ route.meta?.title }}</span>
                </template>
                <el-menu-item
                  v-for="child in route.children"
                  :key="child.path"
                  :index="`/${route.path}/${child.path}`"
                >
                  {{ child.meta?.title }}
                </el-menu-item>
              </el-sub-menu>

              <!-- 一级菜单 (无子菜单) -->
              <el-menu-item v-else :index="route.path">
                <el-icon v-if="route.meta?.icon">
                  <component :is="route.meta.icon" />
                </el-icon>
                <span>{{ route.meta?.title }}</span>
              </el-menu-item>
            </template>
          </el-menu>
        </div>
      </el-scrollbar>

      <!-- 用户信息卡片 -->
      <div v-if="!isCollapse" class="user-card">
        <el-avatar :size="40" :src="userStore.userInfo.avatar">
          {{ userStore.userInfo.name?.charAt(0) }}
        </el-avatar>
        <div class="user-card-info">
          <div class="user-card-name">{{ userStore.userInfo.name }}</div>
          <div class="user-card-role">管理员</div>
        </div>
        <el-icon class="user-card-icon" @click="handleCommand('logout')">
          <SwitchButton />
        </el-icon>
      </div>
    </el-aside>

    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="main-header">
        <div class="header-left">
          <el-icon class="collapse-icon" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>

          <!-- 面包屑 -->
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
              {{ item.meta?.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 全屏 -->
          <el-tooltip content="全屏" placement="bottom">
            <el-icon class="header-icon" @click="toggleFullscreen">
              <FullScreen />
            </el-icon>
          </el-tooltip>

          <!-- 消息中心 -->
          <MessageCenter />

          <!-- 用户信息 -->
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo.avatar">
                {{ userStore.userInfo.name?.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo.name }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import MessageCenter from '@/components/MessageCenter.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 侧边栏折叠状态
const isCollapse = ref(false)

// 获取菜单路由
const menuRoutes = computed(() => {
  const routes = router.getRoutes()
  const mainRoute = routes.find((r) => r.path === '/')
  return mainRoute?.children?.filter((r) => r.meta?.title) || []
})

// 菜单分组
const menuGroups = computed(() => {
  const routes = menuRoutes.value

  return [
    {
      title: 'HOME',
      routes: routes.filter((r) => r.meta?.group === 'home' || r.path === '/dashboard')
    },
    {
      title: 'APPS',
      routes: routes.filter(
        (r) => r.meta?.group === 'apps' || ['books', 'circulation', 'readers'].includes(r.path)
      )
    },
    {
      title: 'SYSTEM',
      routes: routes.filter(
        (r) => r.meta?.group === 'system' || ['system', 'profile'].includes(r.path)
      )
    }
  ].filter((group) => group.routes.length > 0)
})

// 当前激活的菜单
const activeMenu = computed(() => {
  return route.path
})

// 面包屑
const breadcrumbs = computed(() => {
  return route.matched.filter((r) => r.meta?.title && r.path !== '/')
})

// 切换侧边栏折叠
function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

// 切换全屏
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 下拉菜单命令
function handleCommand(command) {
  switch (command) {
    case 'profile':
      router.push({ name: 'ProfileInfo' })
      break
    case 'password':
      router.push({ name: 'ProfilePassword' })
      break
    case 'logout':
      ElMessageBox.confirm('确定退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        userStore.logout()
      })
      break
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  width: 100%;
  height: 100vh;
}

.main-aside {
  background: #fff;
  border-right: 1px solid #f0f0f0;
  transition: width 0.3s;
  display: flex;
  flex-direction: column;

  .logo-container {
    height: 80px;
    display: flex;
    align-items: center;
    padding: 0 20px;
    border-bottom: 1px solid #f0f0f0;
    transition: all 0.3s;

    .logo-wrapper {
      display: flex;
      align-items: center;
      gap: 12px;
      width: 100%;

      .logo-icon {
        flex-shrink: 0;
      }

      .logo-content {
        display: flex;
        flex-direction: column;
        overflow: hidden;

        .logo-text {
          font-size: 20px;
          font-weight: 700;
          color: #2a3547;
          line-height: 1.2;
          letter-spacing: -0.5px;
        }

        .logo-subtitle {
          font-size: 12px;
          font-weight: 500;
          color: #5a6a85;
          line-height: 1.2;
          white-space: nowrap;
        }
      }
    }

    &.is-collapse {
      justify-content: center;
      padding: 0;
    }
  }

  .menu-scrollbar {
    flex: 1;
    overflow: hidden;

    .menu-group {
      padding: 0 16px;

      &:not(:first-child) {
        margin-top: 24px;
      }

      .menu-group-title {
        font-size: 11px;
        font-weight: 600;
        color: #a1a5b7;
        letter-spacing: 0.5px;
        text-transform: uppercase;
        padding: 8px 8px 8px;
        margin-bottom: 4px;
      }
    }
  }

  .user-card {
    padding: 16px 20px;
    border-top: 1px solid #f0f0f0;
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    transition: background 0.3s;

    &:hover {
      background: #f6f9fc;
    }

    .user-card-info {
      flex: 1;
      overflow: hidden;

      .user-card-name {
        font-size: 14px;
        font-weight: 600;
        color: #2a3547;
        line-height: 1.4;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .user-card-role {
        font-size: 12px;
        color: #5a6a85;
        line-height: 1.4;
      }
    }

    .user-card-icon {
      font-size: 16px;
      color: #5a6a85;
      flex-shrink: 0;
    }
  }

  :deep(.el-menu) {
    border-right: none;

    .el-menu-item,
    .el-sub-menu__title {
      height: 44px;
      line-height: 44px;
      margin: 2px 0;
      border-radius: 8px;
      transition: all 0.3s;
      font-size: 14px;
      font-weight: 500;

      .el-icon {
        font-size: 20px;
        color: #5a6a85;
        margin-right: 12px;
      }

      &:hover {
        background-color: #f6f9fc !important;
      }

      &.is-active {
        background-color: #5d87ff !important;
        color: #fff !important;
        font-weight: 600;

        .el-icon {
          color: #fff !important;
        }
      }
    }

    .el-sub-menu {
      .el-sub-menu__title {
        .el-icon {
          color: #5a6a85;
        }

        &:hover .el-icon {
          color: #5d87ff;
        }
      }

      .el-menu {
        background-color: transparent !important;
      }

      .el-menu-item {
        height: 40px;
        line-height: 40px;
        padding-left: 48px !important;
        font-size: 13px;
        font-weight: 500;

        &:hover {
          background-color: #f6f9fc !important;
          color: #5d87ff !important;
        }

        &.is-active {
          background-color: rgba(93, 135, 255, 0.1) !important;
          color: #5d87ff !important;
          font-weight: 600;

          .el-icon {
            color: #5d87ff !important;
          }
        }
      }
    }
  }
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 24px;

  .header-left {
    display: flex;
    align-items: center;

    .collapse-icon {
      font-size: 20px;
      cursor: pointer;
      margin-right: 16px;
      transition: color 0.3s;

      &:hover {
        color: #1890ff;
      }
    }

    .breadcrumb {
      font-size: 14px;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .header-icon {
      font-size: 18px;
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: #1890ff;
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      .user-name {
        font-size: 14px;
        color: rgba(0, 0, 0, 0.85);
      }
    }
  }
}

.main-content {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  padding: 24px;
  overflow-y: auto;
}

// 路由过渡动画
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
