import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// 样式导入
// import './assets/tailwind.css' // Tailwind CSS (需优先导入) - 临时禁用，配置问题
import 'element-plus/dist/index.css'
import 'nprogress/nprogress.css'
import './styles/index.scss'

// Element Plus 延迟导入
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 创建并配置Vue应用
function mountApp() {
  const app = createApp(App)

  // 注册 Pinia 和 Router
  app.use(createPinia())
  app.use(router)

  // 注册 Element Plus
  app.use(ElementPlus, {
    locale: zhCn,
    size: 'default'
  })

  // 注册 Element Plus 图标
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.mount('#app')
  console.log('[App] Vue application mounted')
}

// MSW 已禁用 - 使用真实后端 API
// 启动Mock服务器(演示环境需要)
// 注意：对于演示部署包，生产环境也需要启动MSW
/*
import('./mock/browser').then(({ worker }) => {
  console.log('[MSW] Starting Mock Service Worker...')

  worker.start({
    onUnhandledRequest: 'warn', // 改为 warn 以查看未处理的请求
    quiet: false, // 显示详细日志
    serviceWorker: {
      url: '/mockServiceWorker.js'
    }
  }).then(() => {
    console.log('[MSW] Mock Service Worker started in', import.meta.env.MODE, 'mode')

    // 检查Service Worker是否控制当前页面
    if (!navigator.serviceWorker.controller) {
      console.log('[MSW] Service Worker registered but not controlling yet.')
      console.log('[MSW] This is the first visit. Page will reload in 1 second to activate Service Worker...')

      // 设置一个标记，防止无限刷新
      const hasReloaded = sessionStorage.getItem('msw-reloaded')
      if (!hasReloaded) {
        sessionStorage.setItem('msw-reloaded', 'true')

        // 等待Service Worker激活后刷新页面
        navigator.serviceWorker.ready.then(() => {
          console.log('[MSW] Service Worker ready, reloading page...')
          setTimeout(() => {
            window.location.reload()
          }, 1000)
        })

        // 显示加载提示
        document.body.innerHTML = `
          <div style="display: flex; align-items: center; justify-content: center; height: 100vh; flex-direction: column; font-family: Arial, sans-serif;">
            <div style="text-align: center;">
              <div style="font-size: 18px; color: #409EFF; margin-bottom: 10px;">初始化中...</div>
              <div style="font-size: 14px; color: #909399;">正在启动 Mock Service Worker</div>
              <div style="margin-top: 20px;">
                <div style="width: 40px; height: 40px; border: 4px solid #f3f3f3; border-top: 4px solid #409EFF; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto;"></div>
              </div>
            </div>
            <style>
              @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
              }
            </style>
          </div>
        `
        return
      }
    } else {
      console.log('[MSW] Service Worker is already controlling the page')
      // 清除刷新标记
      sessionStorage.removeItem('msw-reloaded')
    }

    // Service Worker已经控制页面，直接挂载应用
    mountApp()
  }).catch((error) => {
    console.error('[MSW] Failed to start Mock Service Worker:', error)
    console.warn('[MSW] Mounting app without MSW - API calls may fail')
    // 即使MSW失败也挂载应用，让用户看到界面
    mountApp()
  })
}).catch((error) => {
  console.error('[MSW] Failed to import Mock Service Worker module:', error)
  console.warn('[MSW] Mounting app without MSW - API calls may fail')
  // 导入失败时也挂载应用
  mountApp()
})
*/

// 直接挂载应用 - 使用真实后端 API
console.log('[App] MSW disabled - using real backend API')
mountApp()
