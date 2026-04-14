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

// 根据环境变量决定是否启用MSW
const useMock = import.meta.env.VITE_USE_MOCK === 'true'

if (useMock) {
  // 启动Mock服务器
  console.log('[App] MSW enabled - loading Mock Service Worker...')
  import('./mock/browser')
    .then(({ worker }) => {
      console.log('[MSW] Starting Mock Service Worker...')

      worker
        .start({
          onUnhandledRequest: 'bypass', // bypass未处理的请求
          quiet: false, // 显示详细日志
          serviceWorker: {
            url: '/mockServiceWorker.js'
          }
        })
        .then(() => {
          console.log('[MSW] Mock Service Worker started in', import.meta.env.MODE, 'mode')
          // 直接挂载应用
          mountApp()
        })
        .catch((error) => {
          console.error('[MSW] Failed to start Mock Service Worker:', error)
          console.warn('[MSW] Mounting app without MSW - API calls may fail')
          mountApp()
        })
    })
    .catch((error) => {
      console.error('[MSW] Failed to import Mock Service Worker module:', error)
      console.warn('[MSW] Mounting app without MSW - API calls may fail')
      mountApp()
    })
} else {
  // 直接挂载应用 - 使用真实后端 API
  console.log('[App] MSW disabled - using real backend API')
  mountApp()
}
